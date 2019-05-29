
//todo: what is this warning?
// // RDFDefaultErrorHandler - unknown-source: {W136} Relative URIs are not permitted in RDF: specifically <ont-policy.rdf>


fun main(args : Array<String>) {
    try {
        val ontologyModelManager = OntologyModelManager("file:ontology/pizza.owl")
        ontologyModelManager.showNumberOfStatementsFromActualModel(10)



    }catch (e: Exception){
        println(e.toString())
    }

}