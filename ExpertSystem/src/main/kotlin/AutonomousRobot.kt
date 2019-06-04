class AutonomousRobot(private val filename: String) {
    // Commands
    val commands = listOf(HelpCommand(this), ThingsCommand(this), StateCommand(this), ExitCommand(this))
    var shouldContinue = true

    // Ontology
    val ontName = "GeometricShape"
    val ontPrefix = "http://www.semanticweb.org/autonomous_robot/ontologies/2019/5/" + ontName + "#"
    val ontModelManager = OntologyModelManager("file:ontology/" + ontName + ".owl")

    // Query Master
    val queryMaster = QueryMaster(ontModelManager.baseModel, ontModelManager.ontModel, ontPrefix)

    fun run() {
        findAndExecuteCommand(listOf("help"))

        while(shouldContinue) {
            val args = readLine()!!.split(' ')

            if(!findAndExecuteCommand(args))
                println("The command you entered was not found.")
        }
    }

    fun findAndExecuteCommand(args: List<String>) : Boolean {
        for(arg in args)
            arg.trim()
        val typedCmdName = args[0]

        for (cmd in commands) {
            if(cmd.name.equals(typedCmdName)) {
                cmd.executeCommand(args)
                return true
            }
        }

        return false
    }
}