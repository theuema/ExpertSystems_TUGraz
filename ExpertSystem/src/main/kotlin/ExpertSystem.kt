//alltime todo: what is this warning?
// // RDFDefaultErrorHandler - unknown-source: {W136} Relative URIs are not permitted in RDF: specifically <ont-policy.rdf>

fun main(args: Array<String>) {
    try {
        // ontology definitions
        val ontName = "GeometricShape"
        val ontPrefix = "http://www.semanticweb.org/thomashoedl/ontologies/2019/5/" + ontName + "#"
        val ontModelManager = OntologyModelManager("file:ontology/" + ontName + ".owl")

        // inspect model and/or test
        if (false) {
            ontModelManager.showNumberOfStatementsFromActualModel(20)
            val actionClass = ontModelManager.ontModel.getOntClass(ontPrefix + "Action")
            println("RESULT::getOntClass() returned $actionClass \n\n")

            // It's generally easier to figure out the right SPARQL queries to write if the data is viewed in the Turtle or
            // N3 serializations, because those serializations are much closer to the syntax of SPARQL queries.
            // do not really use normal representation: ontModel.write(System.out)
            if (true) ontModelManager.ontModel.write(System.out, "TURTLE")
            else ontModelManager.ontModel.write(System.out, "N-TRIPLES")
            println("\n\n")
        }

        val queryMaster = QueryMaster(ontModelManager.baseModel, ontModelManager.ontModel, ontPrefix)
        val results = queryMaster.executeModelQuery(queryMaster.hasCharacteristicValuePositionMovement(), "Action")

        println("RESULT::executeModelQuery() returned MutalbleList<Any>: \n" +
                "$results")

    } catch (e: Exception) {
        println("MAIN CATCH:: ${e.printStackTrace()}")
    }

}