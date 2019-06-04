import org.apache.jena.rdf.model.Literal
import org.apache.jena.rdf.model.Resource

//alltime todo: what is this warning?
// // RDFDefaultErrorHandler - unknown-source: {W136} Relative URIs are not permitted in RDF: specifically <ont-policy.rdf>

fun main(args: Array<String>) {
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

        // actionFromToPositionQuery() Demo
        println("__DEMO1::get our action from one position to the other position:")
        val actions = q.actionFromToPositionQuery()
        println("RESULT::actionFromToPositionQuery()" +
                " returned MutableList<Any>: \n $actions")
        actions.map {
            if (it is Resource) println("RESOURCE::${it.toString()}")
            else if (it is Literal) println("LITERAL::in solution: ${it.getDatatypeURI()}")
        }
        println("\n\n")

        println("__DEMO2::all instances from Thing and show what events acted on that _thing:")
        // thingInstancesQuery() && eventsActedOnThingQueryS() Demo
        val thingInstances = q.thingInstancesQuery()
        val eventsActedOnThing: MutableList<MutableList<Any>> = mutableListOf()

        thingInstances.map {
            if (it is Resource) eventsActedOnThing.add(q.eventsActedOnThingQuery(it.localName))
            if (it is Literal) println("LITERAL::in solution: ${it.getDatatypeURI()}")
        }
        println("RESULT::thingInstancesQuery() " +
                "returned MutableList<Any>: \n $thingInstances")

        println("RESULT::eventsActedOnThing() " +
                "returned MutableList<Any>: \n $eventsActedOnThing \n\n")

        println("__DEMO3::get InitialState of ${(thingInstances[1] as Resource).localName}:")
        // initialStateOfThingQuery() Demo
        val initialState = q.initialStateOfThingQuery((thingInstances[1] as Resource).localName)
        println("RESULT::initialStateOfThingQuery() " +
                "returned ${initialState.javaClass.kotlin}>: \n ${initialState.localName} \n\n")

        println("__DEMO4::get next Event from ${initialState.localName}:")
        // eventNextfromEvent() Demo
        val nextEvent = q.eventNextfromEvent(initialState.localName)
                ?: throw Exception("QueryMaster::getOnlyObjectFromResultSet(): no nextEvent for ${initialState.localName} in ResultSet.")
        println("RESULT::eventNextfromEvent() " +
                "returned ${nextEvent.javaClass.kotlin}>: \n ${nextEvent.localName} \n\n")

    } catch (e: Exception) {
        println("ExpertSystem:: ${e.printStackTrace()}")
    }

}