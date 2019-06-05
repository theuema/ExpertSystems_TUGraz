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
            if(it is Resource) {
                val classList = robot.queryMaster.getSuperclassesOfThing(it.localName)
                print(it.localName + ": ")
                for (class_index in 0..(classList.size-1)) {
                    print(classList.get(class_index))
                    if(class_index != classList.size-1) print(", ")
                }
                println("")
            }
            else {
                throw Exception("Should not end up here!!")
            }
        }

        println("")
    }
}

class PuttingThingToDifferentPlaceCommand(val tmpRobot: AutonomousRobot) : RobotCommand("put","put thing pos2", "the robot puts the chosen thing from its current position to pos2", tmpRobot) {
    override fun executeCommand(args: List<String>) {
        if(args.size != 3) {
            println("Need at least two arguments!")
            return
        }

        // TODO
        // get current position of thing - if thing exists
        // get pos2 - if exists
        // get task description for putting something somewhere
        // carry out task with task description
        // add tasks to ontology as individuals
    }
}

class StateCommand(val tmpRobot: AutonomousRobot) : RobotCommand("state","state thing_name", "shows the state of the thing with the specified name", tmpRobot) {
    override fun executeCommand(args: List<String>) {
        // TODO
    }
}

class ExitCommand(val tmpRobot: AutonomousRobot) : RobotCommand("exit", "exit", "exit the program", tmpRobot) {
    override fun executeCommand(args: List<String>) {
        println("Bye")
        robot.shouldContinue = false;
    }
}