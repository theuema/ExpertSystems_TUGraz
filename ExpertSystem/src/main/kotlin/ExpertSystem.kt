import org.apache.jena.rdf.model.Resource

fun main(args: Array<String>) {
    // Settings
    val robotActive = true
    val showModel = false

    // Ontology definitions
    val ontName = "knowrob"
    val ontPrefix = "http://ias.cs.tum.edu/kb/" + ontName + "#"
    val ontModelManager = OntologyModelManager("file:ontology/" + ontName + ".owl")

    try {
        val robot = AutonomousRobot(ontName, ontPrefix, ontModelManager)
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

            // Demo Main
            val q = QueryMaster(ontModelManager.baseModel, ontModelManager.ontModel, ontPrefix)

            /** @DEMO1: actionFromToPositionQuery() */
            println("__DEMO1::get our action from one position to the other position:")
            val action = q.actionFromToPositionQuery()
                    ?: throw Exception("ExpertSystem::actionFromToPositionQuery(): no specific action for " +
                            "owl:onProperty :from/toPosition defined in ontology.")
            println("RESULT::actionFromToPositionQuery() " +
                    "returned ${action.javaClass}>: \n ${action.localName} \n\n")

            /** @DEMO2: thingInstancesQuery() && eventsActedOnThingQueryS() */
            println("__DEMO2::all instances from Thing and show what events acted on that _thing:")
            val thingInstances = q.thingInstancesQuery()
            val eventsActedOnThing: MutableList<MutableList<Resource>> = mutableListOf()

            thingInstances.map {
                eventsActedOnThing.add(q.eventsActedOnThingQuery(it.localName))
            }
            println("RESULT::thingInstancesQuery() " +
                    "returned MutableList<Any>: \n $thingInstances")

            println("RESULT::eventsActedOnThing() " +
                    "returned MutableList<Any>: \n $eventsActedOnThing \n\n")

            /** @DEMO3: initialStateOfThingQuery() */
            println("__DEMO3::get InitialState for ${thingInstances[1].localName}:")
            val initialState = q.initialStateOfThingQuery(thingInstances[1].localName)
                    ?: throw Exception("ExpertSystem::initialStateOfThingQuery(): no initialState for ${thingInstances[1].localName} found.")

            println("RESULT::initialStateOfThingQuery() " +
                    "returned ${initialState.javaClass}>: \n ${initialState.localName} \n\n")

            /** @DEMO4: eventNextfromEvent() */
            println("__DEMO4::get next Action for ${initialState.localName}:")
            val nextEvent = q.eventNextfromEvent(initialState.localName)
                    ?: throw Exception("ExpertSystem::eventNextfromEvent(): no nextEvent for ${initialState.localName} found.")
            println("RESULT::eventNextfromEvent() " +
                    "returned ${nextEvent.javaClass}>: \n ${nextEvent.localName} \n\n")


            /** @DEMO5: resolveAvailableActionsOnThing() */
            println("__DEMO5::get chain of available Actions beginning from InitialState for Thing Individual: ${thingInstances[1].localName}:")
            val actionsAvailableForThing = q.resolveAvailableActionsOnThing((thingInstances[1].localName))
            println("RESULT::actionsAvailableForThing() " +
                    "returned MutableList<Resource>: \n $actionsAvailableForThing \n\n")

            /** @DEMO6: resolveAvailableActionsFromAction() */
            println("__DEMO6::get chain of available 'next' Actions for Action: ${eventsActedOnThing[1][2].localName}:")
            val actionsAvailableAfterAction = q.resolveAvailableActionsFromAction((eventsActedOnThing[1][2]))
            println("RESULT::actionsAvailableForThing() " +
                    "returned MutableList<Resource>: \n $actionsAvailableAfterAction \n\n")

            /** @DEMO7: Print properties from an individual */
            println("__DEMO7::Print properties from following individuals.")
            println("Individuals are: $actionsAvailableForThing")
            actionsAvailableForThing.map {
                println("--> properties of individual ${it.localName} :")

                val stmt = it.listProperties()
                while (stmt.hasNext()) {
                    val prop = stmt.next()
                    println("property: ${prop.`object`.toString()}")
                }
            }
            println("\n\n")

            /** @DEMO8: Get Post Conditions for an action  */
            println("__DEMO8::Get Post Condition for ${nextEvent.localName}.")
            val postConditionTuple = q.postConditionOfActionQuery(nextEvent.localName)
            println("RESULT::postConditionOfActionQuery() " +
                    "returned MutableList<Pair(Resource, Resource)>: \n $postConditionTuple \n\n")


            /** @DEMO9: Get the current Condition for an Instace of an Action  */
            println("__DEMO9::Get the current toPosition condition for the Instance Action: ${eventsActedOnThing[1][2].localName}:")
            val toPosition = q.getConditionOfActionInstanceQuery(eventsActedOnThing[1][2].localName, "toPosition", "Position")
                    ?: throw Exception("ExpertSystem::getConditionOfActionInstanceQuery(): no Condition \"toPosition\" found within ${eventsActedOnThing[1][2].localName}.")
            println("RESULT::getConditionOfActionInstanceQuery() " +
                    "returned ${toPosition.javaClass}: \n ${toPosition.localName} \n\n")


            /** @DEM10: Get the last Condition Value for toPosition in an actionList  */
            println("__DEM10::Get the last Condition Value for \"toPosition\" in the following actionList: \n $actionsAvailableForThing")
            val lastToPosition = q.getLastConditionInActionListQuery(actionsAvailableForThing, "toPosition", "Position")
                    ?: throw Exception("ExpertSystem::getLastConditionInActionListQuery(): the Condition \"toPosition\" not found in given List in any Action.")
            println("RESULT::getLastConditionInActionListQuery() " +
                    "returned ${lastToPosition.javaClass}: \n ${lastToPosition.localName} \n\n")
        }
    } catch (e: Exception) {
        println("ExpertSystem:: ${e.printStackTrace()}")
    }

}