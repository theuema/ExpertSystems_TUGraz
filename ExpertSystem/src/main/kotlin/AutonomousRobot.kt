class AutonomousRobot(val ontPrefix: String, val ontModelManager: OntologyModelManager) {

    val commands =
            listOf(HelpCommand(this), ListCapabilityCommand(this),
                    CapabilityRequireCommand(this), ExitCommand(this))
    val queryMaster = QueryMaster(ontModelManager.ontModel, ontPrefix)
    var shouldContinue = true
    val robiName = "~\$Robi : "

    fun run() {
        findAndExecuteCommand(listOf("help"))

        while (shouldContinue) {
            print(robiName)
            val args = readLine()!!.split(' ')

            if (!findAndExecuteCommand(args))
                println("The command you entered was not found.")
        }
    }

    fun findAndExecuteCommand(args: List<String>): Boolean {
        for (arg in args)
            arg.trim()
        val typedCmdName = args[0]

        for (cmd in commands) {
            if (cmd.name.equals(typedCmdName)) {
                cmd.executeCommand(args)
                return true
            }
        }

        return false
    }
}