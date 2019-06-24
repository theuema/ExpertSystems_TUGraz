fun main(args: Array<String>) {
    // Settings
    val robotActive = true
    val showModel = false

    // Ontology definitions
    val ontName = "srdl2-cap"
    val ontPrefixUrl = "http://ias.cs.tum.edu/kb/" + ontName + ".owl#"
    val ontModelManager = OntologyModelManager(ontPrefixUrl)

    try {
        val robot = AutonomousRobot(ontPrefixUrl, ontModelManager)
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

/* Demo 01
* Show components of the capability move_arm
* */
            val q = QueryMaster(ontModel = ontModelManager.ontModel, ontPrefix = ontPrefixUrl)
            val componentsForMoveArmDo =
                    QueryMaster.SpecifiedObjectPropertiesFromCategoryDo("move_arm", "Capability",
                            "dependsOnComponent", "some", "components", "comp")
            val components = q.getObjectFromDataClass(componentsForMoveArmDo, "components")
            print("Components of move_arm: \n")
            for (comp in components) {
                print(comp.localName + "\n")
            }
            print("\n")

        }
    } catch (e: Exception) {
        println("ExpertSystem:: ${e.printStackTrace()}")
    }

}