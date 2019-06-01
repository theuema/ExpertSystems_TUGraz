import org.apache.jena.iri.IRI
import org.apache.jena.ontology.OntClass

//todo: what is this warning?
// // RDFDefaultErrorHandler - unknown-source: {W136} Relative URIs are not permitted in RDF: specifically <ont-policy.rdf>


fun main(args : Array<String>) {
    try {
        // ontology definitions
        val ontName = "knowrob"
        val ontNamespace = "http://ias.cs.tum.edu/kb/"+ ontName +".owl#"
        val ontModelManager = OntologyModelManager("file:ontology/"+ ontName + ".owl")

        // testing
        // ontModelManager.showNumberOfStatementsFromActualModel(10)
        val actionClass = ontModelManager.getOntologyModel().getOntClass( ontNamespace + "Action") // todo: from Protege: IRI needed?
        val ontModel = ontModelManager.getOntologyModel()

        // It's generally easier to figure out the right SPARQL queries to write if the data is viewed in the Turtle or
        // N3 serializations, because those serializations are much closer to the syntax of SPARQL queries.
        // not: ontModel.write(System.out)
        ontModel.write(System.out, "TURTLE") // || ontModel.write(System.out, "N-TRIPLES")

    }catch (e: Exception){
        println(e.toString())
    }

}