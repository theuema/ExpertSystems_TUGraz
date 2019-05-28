import org.apache.jena.ontology.OntModel
import org.apache.jena.ontology.OntModelSpec
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.util.FileManager

class OntologyModelManager(private val filename: String) {
    private var ontologyModel: OntModel? = null // todo can we work with this model?
    private var model: Model? = null // todo what is this model?

    init {
        try {
            ontologyModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null)
            model = FileManager.get().loadModel(filename)
            println("Ontology Model: " + filename + " loaded!")
        } catch (e: Exception) {
            println("Something went wrong" + e.toString())
        }
    }

    fun getOntologyModel(): OntModel? {
        return ontologyModel
    }
}