import org.apache.jena.ontology.OntModel
import org.apache.jena.ontology.OntModelSpec
import org.apache.jena.rdf.model.ModelFactory

class OntologyModelManager(private val ontBaseUrl: String) {
    val ontModel: OntModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM) //OWL_MEM lt. code example from email

    init {
        ontModel.read(ontBaseUrl)
        println("OntologyModelManager::$ontBaseUrl loaded!\n\n")
    }
}