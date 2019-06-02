import org.apache.jena.ontology.OntModel
import org.apache.jena.query.*
import org.apache.jena.rdf.model.Model

class QueryMaster(private val model: Model, private val ontModel: OntModel, private val ontPrefix: String) {
    private val rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    private val rdfs = "http://www.w3.org/2000/01/rdf-schema#"
    private val owl = "http://www.w3.org/2002/07/owl#"
    private val xsd = "http://www.w3.org/2001/XMLSchema#"
    private val queryPrefix: String

    init {
        queryPrefix = buildQueryPrefix()
    }

    fun buildQueryPrefix(): String {
        return "PREFIX rdf: <$rdf> " +
                "PREFIX rdfs: <$rdfs> " +
                "PREFIX owl: <$owl> " +
                "PREFIX xsd: <$xsd> " +
                "PREFIX : <$ontPrefix> "
    }

    fun executeModelQuery(queryString: String, name: String = "UNSET", print: Boolean = false): MutableList<Any> {
        val query = QueryFactory.create(queryPrefix + queryString)
        val ret: MutableList<Any> = mutableListOf()

        QueryExecutionFactory.create(query, model).use {
            val results = it.execSelect()

            while (results.hasNext()) {
                val obj = results.next()

                val found = obj.get(name)
                        ?: "NULL SAFETY::printResult(): RDFNode $name not present in this solution."
                if (print || found is String) {
                    System.out.println(found)
                }

                if (found !is String)
                    if (obj.get(name).isResource) ret.add(obj.getResource(name))
                    else ret.add(obj.getLiteral(name))
            }
        }
        return ret
    }

    fun hasCharacteristicValuePositionMovement(): String {
        return "SELECT ?Action " +
                "WHERE { " +
                "?Action rdfs:subClassOf :Action . " +
                "?Action (owl:equivalentClass|^owl:equivalentClass)* ?Restriction . " +
                "?Restriction (rdfs:subClassOf|(owl:intersectionOf/rdf:rest*/rdf:first))* ?Value . " +
                "?Value owl:onProperty :hasCharacteristic . " +
                "?Value owl:hasValue :positionMovement . " +
                "}"
    }
}