import org.apache.jena.rdf.model.Resource

fun main(args: Array<String>) {
    // Settings
    val robotActive = true
    val showModel = false

    // Ontology definitions
    val ontName = "srdl2-cap"
    val ontPrefixUrl = "http://ias.cs.tum.edu/kb/" + ontName + ".owl#"
    val ontModelManager = OntologyModelManager(ontPrefixUrl)

    try {
        val robot = AutonomousRobot(ontName, ontPrefixUrl, ontModelManager)
        if (robotActive) robot.run() else {

            // inspect model and/or test
            if (showModel) {
                /*
                It's generally easier to figure out the right SPARQL queries to write if the data is viewed in the Turtle or
                N3 serializations, because those serializations are much closer to the syntax of SPARQL queries.
                do not really use normal representation: ontModel.write(System.out)
                */
                if (true) ontModelManager.ontModel.write(System.out, "TURTLE")
                else ontModelManager.ontModel.write(System.out, "N-TRIPLES")
                println("\n\n")
            }

            /* Demo of how to see Statements and get a certain class:*/
//             ontModelManager.showNumberOfStatementsFromActualModel(20)
//             val actionClass = ontModelManager.ontModel.getOntClass(ontPrefix + "Action")
//             println("RESULT::getOntClass() returned $actionClass \n\n")

        }
    } catch (e: Exception) {
        println("ExpertSystem:: ${e.printStackTrace()}")
    }

}