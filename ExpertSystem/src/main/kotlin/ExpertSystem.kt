import org.apache.jena.rdf.model.Resource

//alltime todo: what is this warning?
// // RDFDefaultErrorHandler - unknown-source: {W136} Relative URIs are not permitted in RDF: specifically <ont-policy.rdf>

fun main(args: Array<String>) {
    val robot = AutonomousRobot()
    robot.run()

    try {
        // ontology definitions
        val ontName = "GeometricShape"
        val ontPrefix = "http://www.semanticweb.org/autonomous_robot/ontologies/2019/5/" + ontName + "#"
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

        // main
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

    } catch (e: Exception) {
        println("ExpertSystem:: ${e.printStackTrace()}")
    }

}