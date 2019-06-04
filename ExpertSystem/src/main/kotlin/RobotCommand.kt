abstract class RobotCommand(val name: String,
                            val usage: String,
                            val description: String) {
    abstract fun executeCommand()
}

class HelpCommand : RobotCommand("help","help", "show this description") {
    override fun executeCommand() {

    }
}

class ThingsCommand : RobotCommand("things", "things","show all things") {
    override fun executeCommand() {

    }
}

class StateCommand : RobotCommand("state","state thing_name", "shows the state of the thing with the specified name") {
    override fun executeCommand() {

    }
}

class ExitCommand : RobotCommand("exit", "exit", "exit the program") {
    override fun executeCommand() {

    }
}