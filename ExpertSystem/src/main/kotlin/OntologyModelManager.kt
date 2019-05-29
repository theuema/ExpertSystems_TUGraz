import org.apache.jena.ontology.OntModel
import org.apache.jena.ontology.OntModelSpec
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.util.FileManager
import org.apache.jena.rdf.model.Resource

class OntologyModelManager(private val filename: String) {
    private val ontologyModel: OntModel
    private val model: Model

    init {
        ontologyModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, null)
        model = FileManager.get().loadModel(filename)
        println("Ontology Model: " + filename + " loaded!")
    }

    // Functions

    fun getModel(): Model {
        return model
    }

    fun getOntologyModel(): OntModel {
        return ontologyModel
    }

    fun showNumberStatementsFromActualModel(number: Int){
        val iter = model.listStatements()

        // print out the predicate, subject and object of each statement
        var cnt = 0

        println("-> first statement:")
        while (iter.hasNext() && cnt < number) {
            val stmt = iter.nextStatement()         // get next statement
            val subject = stmt.getSubject()         // get the subject
            val predicate = stmt.getPredicate()     // get the predicate
            val obj = stmt.getObject()              // get the object

            println("this is the subject: " + subject.toString())
            println("this is the predicate: " + predicate.toString() + " !")

            if (obj is Resource) println("obj is a resource: " + obj.toString())
            else println("obj is a literal: \"" + obj.toString() + "\"")

            cnt++
            if (cnt < number)println("-> next statement:")
        }
    }
}