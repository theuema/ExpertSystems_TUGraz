import org.apache.jena.ontology.Individual
import org.apache.jena.rdf.model.ResourceFactory
import sun.tools.jstat.Literal
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
        if (args.size != 1) {
            println("No argument needed!")
            return
        }

        println("You can enter commands which will be forwarded to the robot.")
        println("The following commands can be used:")
        for (cmd in robot.commands) {
            println("       ${cmd.usage} - ${cmd.description}")
        }
        println("")
    }
}

class ExitCommand(val tmpRobot: AutonomousRobot) : RobotCommand("exit", "exit", "exit the program", tmpRobot) {
    override fun executeCommand(args: List<String>) {
        if (args.size != 1) {
            println("No argument needed!")
            return
        }

        println("Bye")
        robot.shouldContinue = false;
    }
}

class ListSkillCommand(val tmpRobot: AutonomousRobot) : RobotCommand("skills", "skills", "lists all available skills", tmpRobot) {
    override fun executeCommand(args: List<String>) {
        if (args.size != 1) {
            println("No argument needed!")
            return
        }

        // TODO
    }
}

class SkillRequireCommand(val tmpRobot: AutonomousRobot) : RobotCommand("require", "require skill_name", "lists all requirements for the skill", tmpRobot) {
    override fun executeCommand(args: List<String>) {
        if (args.size != 2) {
            println("Exactly one needed!")
            return
        }

        // TODO check if action exists

        val taskDescription = robot.queryMaster.getTaskDescription(args[1])
        print("Task Description:")
        for (task in taskDescription) {
            print(task)
        }

        // TODO
    }
}

class PuttingThingToDifferentPlaceCommand(val tmpRobot: AutonomousRobot) : RobotCommand("put", "put thing pos", "the robot puts the chosen thing from its current position to pos2", tmpRobot) {
    override fun executeCommand(args: List<String>) {
        if (args.size != 2) {
            println("Need only one argumetn!")
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

        // get actionCounter
        val actionCounterInd = robot.ontModelManager.ontModel.getIndividual( robot.ontPrefix + "actionCounter" )
        val integerProp = robot.ontModelManager.ontModel.getDatatypeProperty(robot.ontPrefix + "integer") ?: throw Exception("Must have integer")
        val value = actionCounterInd.getPropertyValue(integerProp)
        val newVal = value.asLiteral().int + 1

        actionCounterInd.setPropertyValue(integerProp, ResourceFactory.createTypedLiteral(newVal))
        // add relcoation event as individual and add necessary properties/roles to event
        val relocationIndividual = robot.ontModelManager.ontModel.createIndividual( robot.ontPrefix + "ThingMovementAction" + value.asLiteral().value, relocationAction )

        val hasSubActionProp = robot.ontModelManager.ontModel.getObjectProperty(robot.ontPrefix + "hasSubAction") ?: throw Exception("Must have hasSubAction")

        // add actedOnThing property
        val actedOnThingProp = robot.ontModelManager.ontModel.getObjectProperty(robot.ontPrefix + "actedOnThing") ?: throw Exception("Must have actedOnThing")
        relocationIndividual.addProperty(actedOnThingProp, thingInstance)

        // add toPositoin property
        val toPositionProp = robot.ontModelManager.ontModel.getObjectProperty(robot.ontPrefix + "toPosition") ?: throw Exception("Must have toPosition")
        relocationIndividual.addProperty(toPositionProp, posInstance)

        // add fromPositoin property
        val fromPosition = robot.ontModelManager.ontModel.getObjectProperty(robot.ontPrefix + "fromPosition") ?: throw Exception("Must have fromPosition")
        val actionsAvailableForThing = robot.queryMaster.resolveAvailableActionsOnThing((thingInstance.localName))
        val lastToPosition = robot.queryMaster.getLastConditionInActionListQuery(actionsAvailableForThing, "toPosition", "Position")
                ?: throw Exception("ExpertSystem::getLastConditionInActionListQuery(): the Condition \"toPosition\" not found in given List in any Action.")
        val lastToPositionInstance = robot.ontModelManager.ontModel.getIndividual(lastToPosition.uri)
        relocationIndividual.addProperty(fromPosition, lastToPositionInstance)

        // add to previous event the previous event property
        val lastEventOfThing = robot.ontModelManager.ontModel.getIndividual(actionsAvailableForThing.last().uri)
        val isPrevEventOfProp = robot.ontModelManager.ontModel.getObjectProperty(robot.ontPrefix + "isPreviousEventOf") ?: throw Exception("Must have isPreviousEventOf")
        lastEventOfThing.addProperty(isPrevEventOfProp, relocationIndividual)

        val actionArray = Array<String?>(robot.ACTION_ARRAY_SIZE){null}
        actionArray[robot.THING_IDX] = thingInstance.localName
        actionArray[robot.FROM_LOCATION_IDX] = lastToPositionInstance.localName
        actionArray[robot.TO_LOCATION_IDX] = posInstance.localName

        // add all the subactions and "execute" them
        for (task in taskDescription) {
            val actionInd = robot.ontModelManager.ontModel.getIndividual(robot.ontPrefix + task)
            actionArray[robot.ACTION_IDX] = actionInd.localName
            robot.executeAction(actionArray)
            relocationIndividual.addProperty(hasSubActionProp, actionInd)
        }

        robot.ontModelManager.baseModel.write(File("test.owl").outputStream());

        // TODO how generate naming!!! - ask for last relcoation instance, define counter...
    }
}

