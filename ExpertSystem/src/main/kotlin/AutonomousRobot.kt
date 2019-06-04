class AutonomousRobot(private val filename: String) {
    val commands = listOf(HelpCommand(), ThingsCommand(), StateCommand(), ExitCommand())

    fun run() {
        println("You can enter commands in order to ")

        """
        do {
            val args = readLine()!!.split(' ')

            when(args[0]) {
                "",  -> println("Invalid number")
                1, 2 -> println("Number too low")
                3 -> println("Number correct")
                4 -> println("Number too high, but acceptable")
                else -> println("Number too high")
            }
        } while (!cli.exit)
        """
    }

    fun printHelp() {
        println("You can enter commands which will be forwarded to the robot.")
        println("The following commands can be used:")
        for (cmd in commands) {
            println("       ${cmd.usage} - ${cmd.description}")
        }
        println("")
    }

    fun getAllThings() : MutableList<String> {
        // return all things as string list
        return mutableListOf();
    }

    fun getStateOfThing(thingName: String) {

    }

}