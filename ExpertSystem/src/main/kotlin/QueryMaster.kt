import org.apache.jena.ontology.OntModel
import org.apache.jena.query.*
import org.apache.jena.rdf.model.Literal
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Resource

class QueryMaster(private val model: Model, private val ontModel: OntModel, private val ontPrefix: String) {
    private val rdf = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    private val rdfs = "http://www.w3.org/2000/01/rdf-schema#"
    private val owl = "http://www.w3.org/2002/07/owl#"
    private val xsd = "http://www.w3.org/2001/XMLSchema#"
    private val queryPrefix: String

    init {
        queryPrefix = buildQueryPrefix()
    }

    /** @Section: Functions */

    private fun buildQueryPrefix(): String {
        return "PREFIX rdf: <$rdf> " +
                "PREFIX rdfs: <$rdfs> " +
                "PREFIX owl: <$owl> " +
                "PREFIX xsd: <$xsd> " +
                "PREFIX : <$ontPrefix> "
    }

    /** @Section: Private Generic Query Helpers */

    /** Takes a SPARQL SELECT-query-string, adds the OWL-prefixes and executes the query against a model.
     *  @param selectQueryString SELECT-query we want to execute w/o prefixes
     *  @param useOntModel specifies if we need reasoning or not
     *  @return copy of the resulting ResultSet object
     */
    private fun executeSelectQuery(selectQueryString: String, useOntModel: Boolean): ResultSet {
        val m = if (useOntModel) ontModel else model
        val query = QueryFactory.create(queryPrefix + selectQueryString)

        // .use{} takes advantage of java's auto-close and closes the QueryExecution
        //        takes care of exception handling
        QueryExecutionFactory.create(query, m).use {
            return ResultSetFactory.copyResults(it.execSelect())
        }
    }

    /** Takes a ResultSet and a variable 'name' to generate a list of objects matching that name in the given ResultSet (QuerySolution)
     *  @param results is the outcome of a previous query as ResultSet (containing multiple QuerySolution objects)
     *  @param name of the variable we are looking for (e.g.: the "Action" we need to do next..)
     *  @return List of values of the specific variable, contained in the ResultSet
     */
    private fun getVariableFromResultSet(results: ResultSet, name: String): MutableList<Resource> {
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
     *  @return Resource if found & the only one, null if 'name' not present in the ResultSet or there are multiple QuerySolutions
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
            return null

        return ret
    }

    private fun classInstancesQueryS(`class`: String): String {
        return "SELECT ?$`class` " +
                "WHERE { ?$`class` a/rdfs:subClassOf*  :$`class`" +
                "}"
    }

    /** @Section: Public Specific Queries */

    // written with optional Resource? return value,
    // because we maybe need to check in ExpertSystem.kt if we even find such an object!?
    fun initialStateOfThingQuery(thing: String): Resource? {
        val s = "SELECT ?InitialState " +
                "WHERE { ?InitialState a/rdfs:subClassOf* :InitialState . " +
                "?InitialState :actedOnThing :$thing . " +
                "}"

        return getOnlyObjectVariableFromResultSet(executeSelectQuery(s, false), "InitialState")
    }

    fun eventNextfromEvent(event: String): Resource? {
        val s = "SELECT ?nextEvent " +
                "WHERE { :${event} :isPreviousEventOf ?nextEvent .}"

        return getOnlyObjectVariableFromResultSet(executeSelectQuery(s, false), "nextEvent")
    }

    fun actionFromToPositionQuery(): Resource? {
        val s = "SELECT ?Action " +
                "WHERE { " +
                "?Action rdfs:subClassOf* :Action . " +
                "?Action rdfs:subClassOf ?RestrictionFrom . " +
                "?RestrictionFrom owl:onProperty :fromPosition . " +
                "?Action rdfs:subClassOf ?RestrictionTo . " +
                "?RestrictionTo owl:onProperty :toPosition . " +
                "}"

        return getOnlyObjectVariableFromResultSet(executeSelectQuery(s, false), "Action")
    }

    fun thingInstancesQuery(): MutableList<Resource> {
        val s = classInstancesQueryS("Thing")
        return getVariableFromResultSet(executeSelectQuery(s, false), "Thing")
    }

    // TODO change when can have more classes
    fun getSuperclassesOfThing(thing: String): MutableList<String> {
        val thingClasses: MutableList<String> = mutableListOf<String>()

        var tmpClass = getClassOfThingQuery(thing).get(0) as Resource
        thingClasses.add(tmpClass.localName)

        while(true)
        {
            val tmpList = getNextSuperclassQuery(tmpClass.localName)
            if (tmpList.isEmpty()) break;
            tmpClass = tmpList.get(0) as Resource
            thingClasses.add(tmpClass.localName)
        }

        return thingClasses
    }

    fun getNextSuperclassQuery(subclass: String): MutableList<Resource> {
        val query = "SELECT ?Superclass " +
                    "WHERE { :$subclass rdfs:subClassOf ?Superclass}"

        return getVariableFromResultSet(executeSelectQuery(query, false), "Superclass")
    }

    fun getClassOfThingQuery(thing: String): MutableList<Resource> {
        val query = "SELECT ?Class " +
                "WHERE { :$thing a ?Class . " +
                "FILTER (strstarts(str(?Class), \"$ontPrefix\"))" +
                "}"

        return getVariableFromResultSet(executeSelectQuery(query, false), "Class")
    }

    fun eventsActedOnThingQuery(thing: String): MutableList<Resource> {
        val s = "SELECT ?Event " +
                "WHERE { ?Event a/rdfs:subClassOf* :Event . " +
                "?Event :actedOnThing :$thing . " +
                "}"

        return getVariableFromResultSet(executeSelectQuery(s, false), "Event")
    }

    /** @Section: old Queries do not delete! */

    fun hasCharacteristicValuePositionMovement(): String {
        return "SELECT * " + // just return the ?Action variable; using * would return the ?Restriction and ?Value as well
                "WHERE { " +
                "?Action rdfs:subClassOf :Action . " +
                "?Action (owl:equivalentClass|^owl:equivalentClass)* ?Restriction . " + // because of possible symmetry. else: owl:equivalentClass
                "?Restriction (rdfs:subClassOf|(owl:intersectionOf/rdf:rest*/rdf:first))* ?Value . " + // get all values of restrictions throug itersection
                "?Value owl:onProperty :hasCharacteristic . " + // ?Value has to be the same in all lines, there returns exactly the one Restriction which is exactly the equ class to our Action
                "?Value owl:hasValue :positionMovement . " +
                "}"
    }
}