class AutonomousRobot(val ontName: String, val ontPrefix: String, val ontModelManager: OntologyModelManager) {
    val commands = listOf(HelpCommand(this), ThingsCommand(this), StateCommand(this), ExitCommand(this),
            PuttingThingToDifferentPlaceCommand(this))
    val queryMaster = QueryMaster(ontModelManager.baseModel, ontModelManager.ontModel, ontPrefix)
    var shouldContinue = true

    var ACTION_IDX = 0
    var THING_IDX = 1
    var FROM_LOCATION_IDX = 2
    var TO_LOCATION_IDX = 3

    var ACTION_ARRAY_SIZE = 4

    fun run() {
        findAndExecuteCommand(listOf("help"))

        while (shouldContinue) {
            print("~\$Robi : ")
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

    fun executeAction(args: Array<String?>) {
        if(args.size != ACTION_ARRAY_SIZE) throw Exception("Array must be ACTION_ARRAY_SIZE!")

        when (args[ACTION_IDX]) {
            "Reaching" -> println("Reach thing ${args[THING_IDX]} at position ${args[FROM_LOCATION_IDX]}")
            "Grabbing" -> println("Grab thing ${args[THING_IDX]}")
            "Moving" -> println("Move to position ${args[TO_LOCATION_IDX]}")
            "Releasing" -> println("Release thing ${args[THING_IDX]}")
            else -> throw Exception("The action ${args[ACTION_IDX]} is not supported")
        }
    }


}