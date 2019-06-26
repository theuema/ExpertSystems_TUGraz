import org.apache.jena.ontology.OntModel
import org.apache.jena.query.*
import org.apache.jena.rdf.model.Literal
import org.apache.jena.rdf.model.Resource
import org.apache.jena.vocabulary.OWL
import org.apache.jena.vocabulary.RDF

class QueryMaster(private val ontModel: OntModel, private val ontPrefix: String) {
    private val rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    private val owl = "http://www.w3.org/2002/07/owl#"
    private val rdfs = "http://www.w3.org/2000/01/rdf-schema#"
    private val xsd = "http://www.w3.org/2001/XMLSchema#"
    private val comp = "http://ias.cs.tum.edu/kb/srdl2-comp.owl#"
    private val queryPrefix: String
    private val control = "http://ias.cs.tum.edu/kb/srdl2-cap.owl#"

    init {
        if (ontPrefix != control) throw Exception("QueryMaster::init(): $ontPrefix != $control")
        queryPrefix = buildQueryPrefix()
    }

    /** @Section: Functions */

    private fun buildQueryPrefix(): String {
        return "PREFIX rdf: <$rdf> \n" +
                "PREFIX owl: <$owl> \n" +
                "PREFIX rdfs: <$rdfs> \n" +
                "PREFIX xsd: <$xsd> \n" +
                "PREFIX comp: <$comp> \n" +
                "PREFIX : <$ontPrefix> "
    }

    /** Look at Protege on how to create this dataclass.
     *  Go to your Class (e.g.: puttingThingsFromAtoB) you want to find the superclasses to it with Restrictions as you see on the right.
     *  Just write it down as you see it in Protege: "subAction exactly 1 Grabbing/Moving/Reaching/Releasing" (subActions is the part we want to get out of the Query)
     *  SpecifiedObjectPropertiesFromCategoryDo("puttingThingsFromAtoB", "Action", "subAction", "exactly", "subActions")
     **/
    data class SpecifiedObjectPropertiesFromCategoryDo(
            val category: String,
            val subClassOf: String,
            val objectProperty: String,
            val quantifier: String,
            val queryVariable: String,
            val objectPropertyPrefix: String = "",
            val superClassPrefix: String = "",
            val restriction1Predicate: String = "onProperty"
    ) {
        val queryString: String

        init {
            queryString = getSpecifiedObjectPropertiesFromCategory(category, subClassOf, restriction1Predicate, objectProperty,
                    quantifier, queryVariable, objectPropertyPrefix, superClassPrefix)
        }

        private fun getSpecifiedObjectPropertiesFromCategory(category: String,
                                                             superClass: String,
                                                             restriction1Predicate: String,
                                                             objectProperty: String,
                                                             quantifier: String,
                                                             queryVariable: String,
                                                             objectPropertyPrefix: String,
                                                             superClassPrefix: String): String {

            val resolvedRestriction2Predicate = when (quantifier) {
                "exactly" -> "onClass"
                "value" -> "hasValue"
                "some" -> "someValuesFrom"
                else -> throw Exception("QueryMaster::getSpecifiedObjectPropertiesFromCategory() $quantifier not handled yet!")
            }

            return "SELECT ?$queryVariable \n" +
                    "WHERE { \n" +
                    "$superClassPrefix:$category rdfs:subClassOf* $superClassPrefix:$superClass . \n" +
                    "$superClassPrefix:$category rdfs:subClassOf* ?Restriction . \n" +
                    "?Restriction owl:$restriction1Predicate $objectPropertyPrefix:$objectProperty . \n" +
                    "?Restriction owl:$resolvedRestriction2Predicate ?$queryVariable . \n" +
                    "}\n"
        }
    }

    /** @Section: Private Generic Query Helpers */

    /** Takes a SPARQL SELECT-query-string, adds the OWL-prefixes and executes the query against a model.
     *  @param selectQueryString SELECT-query we want to execute w/o prefixes
     *  @param useOntModel specifies if we need reasoning or not
     *  @return copy of the resulting ResultSet object
     */
    private fun executeSelectQuery(selectQueryString: String): ResultSet {
        val query = QueryFactory.create(queryPrefix + selectQueryString)

        // .use{} takes advantage of java's auto-close and closes the QueryExecution
        //        takes care of exception handling
        QueryExecutionFactory.create(query, ontModel).use {
            return ResultSetFactory.copyResults(it.execSelect())
        }
    }

    class QueryResult(val resource: Resource? = null, val alternativeResources: MutableList<Resource>? = null)

    /** Takes a ResultSet and a variable 'name' to generate a list of objects matching that name in the given ResultSet (QuerySolution)
     *  @param results is the outcome of a previous query as ResultSet (containing multiple QuerySolution objects)
     *  @param name of the variable we are looking for (e.g.: the "Action" we need to do next..)
     *  @return List of values of the specific variable, contained in the ResultSet
     */
    private fun getVariableFromResultSet(results: ResultSet, name: String): MutableList<QueryResult>? {
        if (!results.hasNext()) return null
        val ret: MutableList<QueryResult> = mutableListOf()

        while (results.hasNext()) {
            val obj = results.next()
            when (obj.get(name)) {
                is Resource -> {
                    val resource = obj.getResource(name)
                    if (resource.isAnon) {
                        val alternativeResources = mutableListOf<Resource>()
                        if (!resource.hasProperty(OWL.unionOf)) throw Exception("Only unionOf is supported as alternatives!")
                        var nextResourceOfUnionOf: Resource = resource.getPropertyResourceValue(OWL.unionOf)
                        while (nextResourceOfUnionOf.isAnon) {
                            alternativeResources.add(nextResourceOfUnionOf.getPropertyResourceValue(RDF.first))
                            nextResourceOfUnionOf = nextResourceOfUnionOf.getPropertyResourceValue(RDF.rest)
                        }
                        ret.add(QueryResult(null, alternativeResources))
                    } else {
                        ret.add(QueryResult(resource))
                    }
                }
                is Literal -> throw Exception("QueryMaster::getVariableFromResultSet(): result for $name was Literal.")
                // maybe we need to change else -> to optional: ret.add(null)
                // if we need to check for other object we actually don't want or just 'continue' if we find other objects..
                else -> throw Exception("QueryMaster::getVariableFromResultSet(): no variable $name in actual QuerySolution.")
            }
        }
        return ret
    }

    /** @Section: Public Specific Queries */
    fun getSubClassQuery(superclass: String): MutableList<QueryResult> {
        val s =
                "SELECT ?Subclass \n" +
                        "WHERE {?Subclass rdfs:subClassOf* :$superclass . \n" +
                        "FILTER (?Subclass != :$superclass) \n" +
                        "}\n"
        return getVariableFromResultSet(executeSelectQuery(s), "Subclass")
                ?: throw Exception("ExpertSystem::getNextSuperclassQuery(): no subclass of class $superclass found. Query: \n $s \n")
    }

    fun getObjectFromDataClass(c: SpecifiedObjectPropertiesFromCategoryDo, queryVariable: String): MutableList<QueryResult> {
        return getVariableFromResultSet(executeSelectQuery(c.queryString), queryVariable) ?: mutableListOf()
    }
}