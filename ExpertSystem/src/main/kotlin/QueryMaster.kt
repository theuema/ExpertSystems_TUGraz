import org.apache.jena.ontology.OntModel
import org.apache.jena.query.*
import org.apache.jena.rdf.model.Literal
import org.apache.jena.rdf.model.Resource

class QueryMaster(private val ontModel: OntModel, private val ontPrefix: String) {
    private val srdl2 = "http://ias.cs.tum.edu/kb/srdl2.owl#"
    private val owl2xml = "http://www.w3.org/2006/12/owl2-xml#"
    private val knowrob = "http://ias.cs.tum.edu/kb/knowrob.owl#"

    private val computable = "http://ias.cs.tum.edu/kb/computable.owl#"
    private val dc = "http://purl.org/dc/elements/1.1/"
    // todo: remove prefixe at the end da wsl nie gebraucht

    private val rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    private val owl = "http://www.w3.org/2002/07/owl#"
    private val rdfs = "http://www.w3.org/2000/01/rdf-schema#"
    private val xsd = "http://www.w3.org/2001/XMLSchema#"
    private val comp = "http://ias.cs.tum.edu/kb/srdl2-comp.owl#"
    private val queryPrefix: String
    private val control = "http://ias.cs.tum.edu/kb/srdl2-cap.owl#"


    init {
        if (ontPrefix != control) throw Exception("QueryMaster::init(): $ontPrefix != $control")
        queryPrefix = buildQueryPrefix()
    }

    /** @Section: Functions */

    private fun buildQueryPrefix(): String {
        return "PREFIX rdf: <$rdf> \n" +
                "PREFIX owl: <$owl> \n" +
                "PREFIX rdfs: <$rdfs> \n" +
                "PREFIX xsd: <$xsd> \n" +
                "PREFIX comp: <$comp> \n" +
                "PREFIX : <$ontPrefix> "
    }

    /** Look at Protege on how to create this dataclass.
     *  Go to your Class (e.g.: puttingThingsFromAtoB) you want to find the superclasses to it with Restrictions as you see on the right.
     *  Just write it down as you see it in Protege: "subAction exactly 1 Grabbing/Moving/Reaching/Releasing" (subActions is the part we want to get out of the Query)
     *  SpecifiedObjectPropertiesFromCategoryDo("puttingThingsFromAtoB", "Action", "subAction", "exactly", "subActions")
     **/
    data class SpecifiedObjectPropertiesFromCategoryDo(
            val category: String,
            val subClassOf: String,
            val objectProperty: String,
            val quantifier: String,
            val queryVariable: String,
            val objectPropertyPrefix: String = "",
            val superClassPrefix: String = "",
            val restriction1Predicate: String = "onProperty"
    ) {
        val queryString: String

        init {
            queryString = getSpecifiedObjectPropertiesFromCategory(category, subClassOf, restriction1Predicate, objectProperty,
                    quantifier, queryVariable, objectPropertyPrefix, superClassPrefix)
        }

        private fun getSpecifiedObjectPropertiesFromCategory(category: String,
                                                             superClass: String,
                                                             restriction1Predicate: String,
                                                             objectProperty: String,
                                                             quantifier: String,
                                                             queryVariable: String,
                                                             objectPropertyPrefix: String,
                                                             superClassPrefix: String): String {

            val resolvedRestriction2Predicate = when (quantifier) {
                "exactly" -> "onClass"
                "value" -> "hasValue"
                "some" -> "someValuesFrom"
                else -> throw Exception("QueryMaster::getSpecifiedObjectPropertiesFromCategory() $quantifier not handled yet!")
            }

            return "SELECT ?$queryVariable \n" +
                    "WHERE { \n" +
                    ":$category rdfs:subClassOf* $superClassPrefix:$superClass . \n" +
                    ":$category rdfs:subClassOf* ?Restriction . \n" +
                    "?Restriction owl:$restriction1Predicate $objectPropertyPrefix:$objectProperty . \n" +
                    "?Restriction owl:$resolvedRestriction2Predicate ?$queryVariable . \n" +
                    "}\n"
        }
    }

    /** @Section: Private Generic Query Helpers */

    /** Takes a SPARQL SELECT-query-string, adds the OWL-prefixes and executes the query against a model.
     *  @param selectQueryString SELECT-query we want to execute w/o prefixes
     *  @param useOntModel specifies if we need reasoning or not
     *  @return copy of the resulting ResultSet object
     */
    private fun executeSelectQuery(selectQueryString: String): ResultSet {
        val query = QueryFactory.create(queryPrefix + selectQueryString)

        // .use{} takes advantage of java's auto-close and closes the QueryExecution
        //        takes care of exception handling
        QueryExecutionFactory.create(query, ontModel).use {
            return ResultSetFactory.copyResults(it.execSelect())
        }
    }

    /** Takes a ResultSet and a variable 'name' to generate a list of objects matching that name in the given ResultSet (QuerySolution)
     *  @param results is the outcome of a previous query as ResultSet (containing multiple QuerySolution objects)
     *  @param name of the variable we are looking for (e.g.: the "Action" we need to do next..)
     *  @return List of values of the specific variable, contained in the ResultSet
     */
    private fun getVariableFromResultSet(results: ResultSet, name: String): MutableList<Resource>? {
        if (!results.hasNext()) return null
        val ret: MutableList<Resource> = mutableListOf()

        while (results.hasNext()) {
            val obj = results.next()
            when (obj.get(name)) {
                is Resource -> ret.add(obj.getResource(name))
                is Literal -> throw Exception("QueryMaster::getVariableFromResultSet(): result for $name was Literal.")
                // maybe we need to change else -> to optional: ret.add(null)
                // if we need to check for other object we actually don't want or just 'continue' if we find other objects..
                else -> throw Exception("QueryMaster::getVariableFromResultSet(): no variable $name in actual QuerySolution.")
            }
        }
        return ret
    }

    /** Takes a ResultSet and a variable 'name' to return the Resource if it is present & the only Resource or
     *  null if 'name' is not found in the ResultSet. Does not allow Literals.
     *  @param results is the outcome of a previous query as ResultSet (containing just 1 QuerySolution object)
     *  @param name of the variable we are looking for (e.g.: the "initialState" of an GeometricShape..)
     *  @return Resource if the only solution returned by the SPARQL-Query, null if we don't got a QuerySolution
     */
    private fun getOnlyObjectVariableFromResultSet(results: ResultSet, name: String): Resource? {
        val obj: QuerySolution
        if (results.hasNext()) obj = results.next() else return null

        val ret = when (obj.get(name)) {
            is Resource -> obj.getResource(name)
            is Literal -> throw Exception("QueryMaster::getOnlyObjectVariableFromResultSet(): result for $name was Literal.")
            else -> null
        }

        if (results.hasNext())
            throw Exception("QueryMaster::getOnlyObjectVariableFromResultSet(): variable $name returned more than one solution.")

        return ret
    }

    /** Gives a tuple generated by a query with two different variables e.g.: postConditionOfActionQuery() **/
    private fun getTupleVariablesFromResultSet(results: ResultSet, name1: String, name2: String): MutableList<Pair<Resource, Resource>>? {
        if (!results.hasNext()) return null
        val ret: MutableList<Pair<Resource, Resource>> = mutableListOf()

        while (results.hasNext()) {
            val obj = results.next()
            val res1: Resource
            val res2: Resource

            when (obj.get(name1)) {
                is Resource -> res1 = obj.getResource(name1)
                is Literal -> throw Exception("QueryMaster::getVariableFromResultSet(): result for $name1 was Literal.")
                else -> throw Exception("QueryMaster::getVariableFromResultSet(): no variable $name2 in actual QuerySolution.")
            }
            when (obj.get(name2)) {
                is Resource -> res2 = obj.getResource(name2)
                is Literal -> throw Exception("QueryMaster::getVariableFromResultSet(): result for $name2 was Literal.")
                else -> throw Exception("QueryMaster::getVariableFromResultSet(): no variable $name2 in actual QuerySolution.")
            }
            ret.add(Pair(res1, res2))
        }
        return ret
    }

    /** Gives a query String that can query all Instances of a certain `class` **/
    private fun classInstancesQueryS(`class`: String): String {
        return "SELECT ?$`class` \n" +
                "WHERE { ?$`class` a/rdfs:subClassOf*  :$`class` \n" +
                "}\n"
    }

    /** @Section: Public Specific Queries */

    /** Gives the initialState of any Instance of the class Thing e.g.: pyramid1 **/
    // written with optional Resource? return value,
    // because we maybe need to check if we even find such an object!?
    fun initialStateOfThingQuery(thing: String): Resource? {
        val s =
                "SELECT ?InitialState \n" +
                        "WHERE { ?InitialState a/rdfs:subClassOf* :InitialState . \n" +
                        "?InitialState :actedOnThing :$thing . \n" +
                        "}\n"

        return getOnlyObjectVariableFromResultSet(executeSelectQuery(s), "InitialState")
    }

    /** Gives the Instance of an Event that follows the Instance of the actual Event
     * @param event is actual Event
     **/
    fun eventNextfromEvent(event: String): Resource? {
        val s = "SELECT ?nextEvent \n" +
                "WHERE { :${event} :isPreviousEventOf ?nextEvent . \n" +
                "}\n"

        return getOnlyObjectVariableFromResultSet(executeSelectQuery(s), "nextEvent")
    }

    /** Gives you the Action-Class that can carry a thing from a Position to a Position **/
    fun actionFromToPositionQuery(): Resource? {
        val s =
                "SELECT ?Action \n" +
                        "WHERE { \n" +
                        "?Action rdfs:subClassOf* :Action . \n" +
                        "?Action rdfs:subClassOf ?RestrictionFrom . \n" +
                        "?RestrictionFrom owl:onProperty :fromPosition . \n" +
                        "?Action rdfs:subClassOf ?RestrictionTo . \n" +
                        "?RestrictionTo owl:onProperty :toPosition . \n" +
                        "}\n"

        return getOnlyObjectVariableFromResultSet(executeSelectQuery(s), "Action")
    }

    /** @param actionInstance example: PuttingPyramidFromPos1ToPos2
     *  @param condition example: fromPosition
     *  @param queryVariable: any variable Name you need to specify, example: "Position"
     *  @return Resource if the only solution returned by the SPARQL-Query, null if we don't got a QuerySolution
     */
    fun getConditionOfActionInstanceQuery(actionInstance: String, condition: String, queryVariable: String): Resource? {
        val s =
                "SELECT ?$queryVariable \n" +
                        "WHERE { :$actionInstance a/rdfs:subClassOf*  :Action . \n" +
                        ":$actionInstance :$condition ?$queryVariable . \n" +
                        ":$condition rdfs:subPropertyOf* :condition . \n" +
                        "}\n"

        return getOnlyObjectVariableFromResultSet(executeSelectQuery(s), queryVariable)
    }

    /** @param actionList is an ordered List of Actions of instances of Things
     *  @param condition example: fromPosition
     *  @param queryVariable: any variable Name you need to specify, example: "Position"
     *  @return the last Condition specified in this chain of Actions
     */
    fun getLastConditionInActionListQuery(actionList: MutableList<Resource>, condition: String, queryVariable: String): Resource? {
        val actionListReversed = actionList.asReversed()

        var foundCondition: Resource? = null
        while (actionListReversed.isNotEmpty() && foundCondition == null) {
            foundCondition = getConditionOfActionInstanceQuery(actionListReversed.first().localName, condition, queryVariable)
                    ?: actionListReversed.removeAt(0)
        }
        return foundCondition
    }

    /** Gives all Instances of Class Thing **/
    fun thingInstancesQuery(): MutableList<Resource> {
        val s = classInstancesQueryS("Thing")
        return getVariableFromResultSet(executeSelectQuery(s), "Thing")
                ?: throw Exception("QueryMaster::thingInstancesQuery(): no Instances of class \"Thing\" found. Query: \n $s \n")
    }

    // TODO change when can have more classes
    fun getSuperclassesOfThing(thing: String): MutableList<String> {
        val thingClasses: MutableList<String> = mutableListOf()

        var tmpClass = getClassOfObjectQuery(thing)[0]
        thingClasses.add(tmpClass.localName)

        while (true) {
            try {
                val tmpList = getNextSuperclassQuery(tmpClass.localName)
                if (tmpList.size != 1) throw Exception("More than one superclass. Need to adapt code.")
                tmpClass = tmpList[0]
                thingClasses.add(tmpClass.localName)
            } catch (e: Exception) {
                if (e.message != null && e.message!!.startsWith("ExpertSystem::getNextSuperclassQuery(): no Superclass"))
                    break
                else
                    throw e
            }
        }

        return thingClasses
    }

    fun getNextSuperclassQuery(subclass: String): MutableList<Resource> {
        val s =
                "SELECT ?Superclass \n" +
                        "WHERE { :$subclass rdfs:subClassOf ?Superclass \n" +
                        "}\n"

        return getVariableFromResultSet(executeSelectQuery(s), "Superclass")
                ?: throw Exception("ExpertSystem::getNextSuperclassQuery(): no Superclass of class $subclass found. Query: \n $s \n")
    }

    fun getObjectsWithClass(className: String): MutableList<Resource> {
        val s = "SELECT ?Objects \n" +
                "WHERE { ?Objects a :$className. }"

        return getVariableFromResultSet(executeSelectQuery(s), "Objects")
                ?: throw Exception("ExpertSystem::getObjectsWithClass(): no object of $className found. Query: \n $s \n")
    }

    fun getClassOfObjectQuery(`object`: String): MutableList<Resource> {
        val s =
                "SELECT ?Class \n" +
                        "WHERE { :$`object` a ?Class . \n" +
                        "FILTER (strstarts(str(?Class), \"$ontPrefix\")) \n" +
                        "} \n"

        return getVariableFromResultSet(executeSelectQuery(s), "Class")
                ?: throw Exception("ExpertSystem::getClassOfObjectQuery(): no Class of $`object` found. Query: \n $s \n")
    }

    /** Gives all instances of class Events that acted on a certain Thing-Instance
     *  @param thing must be an instance of a thing
     **/
    fun eventsActedOnThingQuery(thing: String): MutableList<Resource> {
        val s =
                "SELECT ?Event \n" +
                        "WHERE { ?Event a/rdfs:subClassOf* :Event . \n" +
                        "?Event :actedOnThing :$thing . \n" +
                        "} \n"

        return getVariableFromResultSet(executeSelectQuery(s), "Event")
                ?: throw Exception("ExpertSystem::eventsActedOnThingQuery(): no Events that acted on $s found. Query: \n $s \n")
    }

    /** Gives a tuple of the post Condition (e.g.: toPosition, pos2)
     *  @param action must be an Instance of an Action
     **/
    fun postConditionOfActionQuery(action: String): MutableList<Pair<Resource, Resource>> {
        val s =
                "SELECT ?Condition ?Post \n" +
                        "WHERE { ?Condition rdfs:subPropertyOf :postCondition . \n" +
                        ":$action ?Condition  ?Post . \n" +
                        "} \n"

        return getTupleVariablesFromResultSet(executeSelectQuery(s), "Condition", "Post")
                ?: throw Exception("QueryMaster::postConditionOfActionQuery(): no tuple for \n $s \n found.")
    }

    /** Returns the direct subProperties of the passed property
     *  @param propertyName the property whose subproperties should be returned
     **/
    fun getSubPropertiesOf(propertyName: String): MutableList<Resource> {
        val s = "SELECT ?Condition\n" +
                "WHERE { ?Condition rdfs:subPropertyOf $propertyName . }"
        print(s)

        return getVariableFromResultSet(executeSelectQuery(s), "Condition")
                ?: throw Exception("ExpertSystem::getSubProperties(): no sub properties for the property that $propertyName. Query: \n $s \n")
    }

    /** Gives all Instances of Actions that are currently available for a certain Instance of Thing incl. initialAction (e.g.: initialPosPyr1)
     *  @param thing must be an Instance of an Thing
     **/
    fun resolveAvailableActionsOnThing(thing: String): MutableList<Resource> {
        val ret: MutableList<Resource> = mutableListOf()

        val initialState = initialStateOfThingQuery(thing)
                ?: throw Exception("QueryMaster::resolveAvailableActionsOnThing(): no initialState for $thing found.")

        ret.add(initialState)
        while (eventNextfromEvent(ret.last().localName) != null) {
            ret.add(eventNextfromEvent(ret.last().localName)!!)
        }

        return ret
    }

    /** Gives all possible instances of Actions that can currently follow after a certain instance of Action.
     *  @param action must be an Instance of an Action
     **/
    fun resolveAvailableActionsFromAction(action: Resource): MutableList<Resource> {
        val ret: MutableList<Resource> = mutableListOf()

        ret.add(action)
        while (eventNextfromEvent(ret.last().localName) != null) {
            ret.add(eventNextfromEvent(ret.last().localName)!!)
        }

        return ret
    }

    // gets the task description of a composed action in the right order
    fun getTaskDescription(composedActionClass: String): MutableList<String> {
        val subActionsDo = SpecifiedObjectPropertiesFromCategoryDo(composedActionClass, "Action", "subAction", "some", "SubActions")
        val subOrderingsDo = SpecifiedObjectPropertiesFromCategoryDo(composedActionClass, "Action", "orderingConstraints", "value", "Orderings")

        val subActionResources = getVariableFromResultSet(executeSelectQuery(subActionsDo.queryString), "SubActions")
                ?: throw Exception("QueryMaster::getTaskDescription(): " +
                        "$composedActionClass is SubClass of \"subAction some SubActions\" not found. Query: \n ${subActionsDo.queryString} \n ")
        val orderingResources = getVariableFromResultSet(executeSelectQuery(subOrderingsDo.queryString), "Orderings")
                ?: throw Exception("QueryMaster::getTaskDescription(): " +
                        "$composedActionClass is SubClass of \"orderingConstraints value Orderings\" not found. Query: \\n ${subOrderingsDo.queryString} \\n \")")

        val orderedActions = mergeOrderings(orderingResources)
        if (orderedActions.size != subActionResources.size)
            throw Exception("QueryMaster::getTaskDescription(): " +
                    "Sizes !equal: orderedActions.size: ${orderedActions.size}, subActionResources.size: ${subActionResources.size}. Check Ontology!")

        // check if the orderedActions contains all actions that where specified within the class as subActions
        for (subActionRes in subActionResources) {
            if (!orderedActions.contains(subActionRes.localName))
                throw Exception("QueryMaster::getTaskDescription(): " +
                        "The action ${subActionRes.localName} is not in orderedActions. Check Ontology!")
        }

        return orderedActions
    }

    fun mergeOrderings(orderingResources: MutableList<Resource>): MutableList<String> {
        val orderedSubActions: MutableList<String> = mutableListOf<String>()
        val tupleOrderings: MutableList<Pair<String, String>> = mutableListOf<Pair<String, String>>()

        // get all ordering tuples from ontology to tupleOrderings
        for (orderingRes in orderingResources) {
            val beforeAfterActionsQuery = "SELECT ?ActionBefore ?ActionAfter\n" +
                    "WHERE { \n" +
                    ":${orderingRes.localName} :happensBeforeInOrdering ?ActionBefore.\n" +
                    ":${orderingRes.localName} :happensAfterInOrdering ?ActionAfter.\n" +
                    "}"
            val actionBeforeAfterTuple = getTupleVariablesFromResultSet(
                    executeSelectQuery(beforeAfterActionsQuery), "ActionBefore", "ActionAfter")
                    ?: throw Exception("QueryMaster::mergeOrderings(): no tuple for \n ${beforeAfterActionsQuery} \n found.")
            tupleOrderings.add(Pair(actionBeforeAfterTuple[0].first.localName, actionBeforeAfterTuple[0].second.localName))
        }

        // put tupleOrderings in the right order and put it into orderedSubActions
        orderedSubActions.add(tupleOrderings[0].first)
        orderedSubActions.add(tupleOrderings[0].second)
        tupleOrderings.removeAt(0)

        // add actions that come after the current within the tuple
        var lastActionName = orderedSubActions[1]
        var found = true

        while (found) {
            found = false
            for (tuple in tupleOrderings) {
                if (tuple.first.equals(lastActionName)) {
                    orderedSubActions.add(tuple.second)
                    lastActionName = tuple.second
                    tupleOrderings.remove(tuple)
                    found = true
                    break
                }
            }
        }

        // add actions that come before the first action in orderedSubActions
        var firstActionName = orderedSubActions.get(0)
        found = true

        while (found) {
            found = false
            for (tuple in tupleOrderings) {
                if (tuple.second.equals(firstActionName)) {
                    orderedSubActions.add(tuple.first)
                    firstActionName = tuple.first
                    tupleOrderings.remove(tuple)
                    found = true
                    break
                }
            }
        }

        if (tupleOrderings.size != 0) throw Exception("List must be empty. Problem within ontology")
        return orderedSubActions
    }

    // new knowrob queries
    fun getSubClassQuery(superclass: String): MutableList<Resource> {
        val s =
                "SELECT ?Subclass \n" +
                        "WHERE {?Subclass rdfs:subClassOf* :$superclass . \n" +
                        "FILTER (?Subclass != :$superclass) \n" +
                        "}\n"
        return getVariableFromResultSet(executeSelectQuery(s), "Subclass")
                ?: throw Exception("ExpertSystem::getNextSuperclassQuery(): no subclass of class $superclass found. Query: \n $s \n")
    }

    fun getObjectFromDataClass(c: SpecifiedObjectPropertiesFromCategoryDo, queryVariable: String): MutableList<Resource> {
        return getVariableFromResultSet(executeSelectQuery(c.queryString), queryVariable) ?: mutableListOf()
    }
}