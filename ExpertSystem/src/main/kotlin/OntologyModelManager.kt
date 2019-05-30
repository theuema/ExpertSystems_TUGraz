import org.apache.jena.ontology.OntModel
import org.apache.jena.ontology.OntModelSpec
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.util.FileManager
import org.apache.jena.rdf.model.Resource

class OntologyModelManager(private val filename: String) {
    private val ontologyModel: OntModel

    init {
        val model = FileManager.get().loadModel(filename)
        ontologyModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM, model)
        //ontologyModel.read(filename)  // seems like it is not needed?
        println("Model: " + filename + " loaded!")
    }

    // Functions

    fun getOntologyModel(): OntModel {
        return ontologyModel
    }

    fun showNumberOfStatementsFromActualModel(number: Int) {
        val iter = ontologyModel.listStatements()

        // print out the predicate, subject and object of each statement
        var cnt = 0
        while (iter.hasNext() && cnt < number) {
            val stmt = iter.nextStatement()         // get next statement
            val subject = stmt.getSubject()         // get the subject
            val predicate = stmt.getPredicate()     // get the predicate
            val obj = stmt.getObject()              // get the object

            System.out.print("subject: " + subject.toString() + "  ")
            System.out.print("predicate: " + predicate.toString() + "  ")

            if (obj is Resource) System.out.print("resource: " + obj.toString() + "  ")
            else System.out.print("literal: " + obj.toString() + "  ")

            cnt++
            System.out.print("  -> end of statement\n")
        }
    }
}