import org.apache.jena.rdf.model.Resource
import kotlin.collections.List

abstract class RobotCommand(val name: String,
                            val usage: String,
                            val description: String,
                            val robot: AutonomousRobot) {
    abstract fun executeCommand(args: List<String>)
}

class HelpCommand(val tmpRobot: AutonomousRobot) : RobotCommand("help","help", "show this description", tmpRobot) {
    override fun executeCommand(args: List<String>) {
        println("You can enter commands which will be forwarded to the robot.")
        println("The following commands can be used:")
        for (cmd in robot.commands) {
            println("       ${cmd.usage} - ${cmd.description}")
        }
        println("")
    }
}

class ThingsCommand(val tmpRobot: AutonomousRobot) : RobotCommand("things", "things","show all things", tmpRobot) {
    override fun executeCommand(args: List<String>) {
        if(args.size != 1) {
            println("No further arguments are expected here!")
            return
        }

        val thingInstances = robot.queryMaster.thingInstancesQuery()

        println("The following things are within the kitchen:")
        thingInstances.map {
            if(it is Resource) println(it.localName)
            else println("NOT KNOWN - TAKE A LOOK")
        }

        println("")
    }
}

class StateCommand(val tmpRobot: AutonomousRobot) : RobotCommand("state","state thing_name", "shows the state of the thing with the specified name", tmpRobot) {
    override fun executeCommand(args: List<String>) {

    }
}

class ExitCommand(val tmpRobot: AutonomousRobot) : RobotCommand("exit", "exit", "exit the program", tmpRobot) {
    override fun executeCommand(args: List<String>) {
        println("Bye")
        robot.shouldContinue = false;
    }
}