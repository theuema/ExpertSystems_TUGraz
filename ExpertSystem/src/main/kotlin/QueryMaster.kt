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

    /** Takes a SPARQL SELECT-query String, adds the OWL-prefixes and executes the query against the model.
     *  @Info: This is no Reasoning! Tested with protege.
     *  @param queryString SELECT-query we want to execute w/o prefixes
     *  @param name of the variable we are looking for (e.g.: what "Action" we need to do next..)
     *  @return List of found objects that match the query
     */
    fun executeModelSelectQuery(queryString: String, name: String): MutableList<Any> {
        val query = QueryFactory.create(queryPrefix + queryString)
        val ret: MutableList<Any> = mutableListOf()

        QueryExecutionFactory.create(query, model).use {
            val results = it.execSelect() // .use{} takes advantage of java's auto-close and closes the QueryExecution; also takes care of exception handlin;

            while (results.hasNext()) {
                val obj = results.next()
                when (obj.get(name)) {
                    is Resource -> ret.add(obj.getResource(name))
                    is Literal -> ret.add(obj.getLiteral(name))
                    else -> System.out.println("NULL SAFETY::printResult(): RDFNode $name not present in this solution.")
                }
            }
        }
        return ret
    }

    // Queries
    fun hasCharacteristicValuePositionMovement(): String {
        return "SELECT ?Action " + // just return the ?Action variable; using * would return the ?Restriction and ?Value as well
                "WHERE { " +
                "?Action rdfs:subClassOf :Action . " +
                "?Action (owl:equivalentClass|^owl:equivalentClass)* ?Restriction . " + // because of possible symmetry. else: owl:equivalentClass
                "?Restriction (rdfs:subClassOf|(owl:intersectionOf/rdf:rest*/rdf:first))* ?Value . " + // get all values of restrictions throug itersection
                "?Value owl:onProperty :hasCharacteristic . " + //?Value has to be the same in all lines, there returns exactly the one Restriction which is exactly the equ class to our Action
                "?Value owl:hasValue :positionMovement . " +
                "}"
    }
}