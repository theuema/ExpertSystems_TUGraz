import org.apache.jena.ontology.OntModel
import org.apache.jena.query.*
import org.apache.jena.rdf.model.Literal
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Resource

class QueryMaster(private val model: Model, private val ontModel: OntModel, private val ontPrefix: String) {
    private val rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    private val rdfs = "http://www.w3.org/2000/01/rdf-schema#"
    private val owl = "http://www.w3.org/2002/07/owl#"
    private val xsd = "http://www.w3.org/2001/XMLSchema#"
    private val queryPrefix: String

    init {
        queryPrefix = buildQueryPrefix()
    }

    // Functions
    fun buildQueryPrefix(): String {
        return "PREFIX rdf: <$rdf> " +
                "PREFIX rdfs: <$rdfs> " +
                "PREFIX owl: <$owl> " +
                "PREFIX xsd: <$xsd> " +
                "PREFIX : <$ontPrefix> "
    }

    /** Takes a SPARQL SELECT-query-string, adds the OWL-prefixes and executes the query against a model.
     *  @param selectQueryString SELECT-query we want to execute w/o prefixes
     *  @param useOntModel specifies if we need reasoning or not
     *  @return copy of the resulting ResultSet object
     */
    fun executeSelectQuery(selectQueryString: String, useOntModel: Boolean): ResultSet {
        val m = if (useOntModel) ontModel else model
        val query = QueryFactory.create(queryPrefix + selectQueryString)

        // .use{} takes advantage of java's auto-close and closes the QueryExecution
        //        takes care of exception handling
        QueryExecutionFactory.create(query, m).use {
            return ResultSetFactory.copyResults(it.execSelect())
        }
    }

    /** Takes a ResultSet and a variable name to generate a list of objects matching that name in the given query(-solution)
     *  @param results is the outcome of a previous query as ResultSet (containing multiple QuerySolution objects)
     *  @param name of the variable we are looking for (e.g.: the "Action" we need to do next..)
     *  @return List of values of the specific variable, contained in the ResultSet
     */
    fun getVariableFromResultSet(results: ResultSet, name: String): MutableList<Any> {
        val ret: MutableList<Any> = mutableListOf()

        while (results.hasNext()) {
            val obj = results.next()
            when (obj.get(name)) {
                is Resource -> ret.add(obj.getResource(name))
                is Literal -> ret.add(obj.getLiteral(name))
                else -> System.out.println("NULL SAFETY::getVariableFromResultSet(): " +
                        "Variable \"$name\" not present in this ResultSet.")
            }
        }
        return ret
    }

    // old Queries do not delete!
    /** @Info: This function is no reasoning! Tested with protege.*/
    fun hasCharacteristicValuePositionMovement(): String {
        return "SELECT * " + // just return the ?Action variable; using * would return the ?Restriction and ?Value as well
                "WHERE { " +
                "?Action rdfs:subClassOf :Action . " +
                "?Action (owl:equivalentClass|^owl:equivalentClass)* ?Restriction . " + // because of possible symmetry. else: owl:equivalentClass
                "?Restriction (rdfs:subClassOf|(owl:intersectionOf/rdf:rest*/rdf:first))* ?Value . " + // get all values of restrictions throug itersection
                "?Value owl:onProperty :hasCharacteristic . " + // ?Value has to be the same in all lines, there returns exactly the one Restriction which is exactly the equ class to our Action
                "?Value owl:hasValue :positionMovement . " +
                "}"
    }

    // new Queries
    fun fromToPositionActionQuery(): String {
        return "SELECT ?Action " +
                "WHERE { " +
                "?Action rdfs:subClassOf* :Action . " +
                "?Action rdfs:subClassOf ?RestrictionFrom . " +
                "?RestrictionFrom owl:onProperty :fromPosition . " +
                "?Action rdfs:subClassOf ?RestrictionTo . " +
                "?RestrictionTo owl:onProperty :toPosition . " +
                "}"
    }

    fun getAllSubInstancesQuery(`class`: String): String {
        return "SELECT ?$`class` " +
                "WHERE { ?$`class` a/rdfs:subClassOf*  :$`class`" +
                "}"
    }

    fun eventsActedOnThingQuery(thing: String): String {
        return "SELECT ?Event " +
                "WHERE { ?Event a/rdfs:subClassOf* :Event . " +
                "?Event :actedOnThing :$thing . " +
                "}"
    }

}