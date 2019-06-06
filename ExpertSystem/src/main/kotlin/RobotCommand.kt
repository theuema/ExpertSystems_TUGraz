import org.apache.jena.ontology.Individual
import java.io.File
import kotlin.collections.List

abstract class RobotCommand(val name: String,
                            val usage: String,
                            val description: String,
                            val robot: AutonomousRobot) {
    abstract fun executeCommand(args: List<String>)
}

class HelpCommand(val tmpRobot: AutonomousRobot) : RobotCommand("help", "help", "show this description", tmpRobot) {
    override fun executeCommand(args: List<String>) {
        println("You can enter commands which will be forwarded to the robot.")
        println("The following commands can be used:")
        for (cmd in robot.commands) {
            println("       ${cmd.usage} - ${cmd.description}")
        }
        println("")
    }
}

class ThingsCommand(val tmpRobot: AutonomousRobot) : RobotCommand("things", "things", "show all things", tmpRobot) {
    override fun executeCommand(args: List<String>) {
        if (args.size != 1) {
            println("No further arguments are expected here!")
            return
        }

        val thingInstances = robot.queryMaster.thingInstancesQuery()

        println("The following things are within the kitchen:")
        for (thing in thingInstances) {
            val classList = robot.queryMaster.getSuperclassesOfThing(thing.localName)
            print(thing.localName + ": ")
            for (class_index in 0..(classList.size - 1)) {
                print(classList.get(class_index))
                if (class_index != classList.size - 1) print(", ")
            }
            println("")
        }

        println("")
    }
}

class PuttingThingToDifferentPlaceCommand(val tmpRobot: AutonomousRobot) : RobotCommand("put", "put thing pos", "the robot puts the chosen thing from its current position to pos2", tmpRobot) {
    override fun executeCommand(args: List<String>) {
        if (args.size != 3) {
            println("Need at least two arguments!")
            return
        }

        // check if entered thing exists
        var thingInstance: Individual = robot.ontModelManager.ontModel.getIndividual(robot.ontPrefix + args[1])

        if(thingInstance == null) {
            val thingInstances = robot.queryMaster.thingInstancesQuery()
            println("The thing ${args[1]} was not found. Only the following things are available")
            for (thing in thingInstances) {
                println(thing.localName)
            }
            return
        }

        // check if entered position exists
        val posInstance: Individual = robot.ontModelManager.ontModel.getIndividual(robot.ontPrefix + args[2])

        if(posInstance == null) {
            val availablePositions = robot.queryMaster.getObjectsWithClass("Position")
            println("The position ${args[2]} was not found. Only the following positions are available")
            for (pos in availablePositions) {
                println(pos.localName)
            }
            return
        }

        // get task description for putting something somewhere
        val relocationAction = robot.queryMaster.actionFromToPositionQuery()
                ?: throw java.lang.Exception("RobotCommand::PuttingThingToDifferentPlaceCommand(): Should not be null")
        val taskDescription = robot.queryMaster.getTaskDescription(relocationAction.localName)

        // add relcoation event as individual and add necessary properties/roles to event
        val relocationIndividual = robot.ontModelManager.ontModel.createIndividual( robot.ontPrefix + "ind0", relocationAction )

        val hasSubActionProp = robot.ontModelManager.ontModel.getObjectProperty(robot.ontPrefix + "hasSubAction") ?: throw Exception("Must have hasSubAction")

        // add all the subactions
        for (task in taskDescription) {
            val ind = robot.ontModelManager.ontModel.getIndividual(robot.ontPrefix + task)
            relocationIndividual.addProperty(hasSubActionProp, ind)
        }

        // add actedOnThing property
        val actedOnThingProp = robot.ontModelManager.ontModel.getObjectProperty(robot.ontPrefix + "actedOnThing") ?: throw Exception("Must have actedOnThing")
        relocationIndividual.addProperty(actedOnThingProp, thingInstance)

        // add toPositoin property
        val toPositionProp = robot.ontModelManager.ontModel.getObjectProperty(robot.ontPrefix + "toPosition") ?: throw Exception("Must have toPosition")
        relocationIndividual.addProperty(toPositionProp, posInstance)

        robot.ontModelManager.baseModel.write(File("test.owl").outputStream());


        // TODO how generate naming!!! - ask for last relcoation instance, define counter...

        // TODO for each task in task description a "function" - just says that it is carried out

        // TODO
        // get current position of thing
    }
}

class StateCommand(val tmpRobot: AutonomousRobot) : RobotCommand("state", "state thing_name", "shows the state of the thing with the specified name", tmpRobot) {
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