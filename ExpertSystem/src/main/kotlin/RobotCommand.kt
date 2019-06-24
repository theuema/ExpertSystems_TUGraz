import org.apache.jena.rdf.model.Resource

abstract class RobotCommand(val name: String,
                            val usage: String,
                            val description: String,
                            val robot: AutonomousRobot) {
    abstract fun executeCommand(args: List<String>)
}

class HelpCommand(tmpRobot: AutonomousRobot) : RobotCommand("help", "help", "show this description", tmpRobot) {
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

class ExitCommand(tmpRobot: AutonomousRobot) : RobotCommand("exit", "exit", "exit the program", tmpRobot) {
    override fun executeCommand(args: List<String>) {
        if (args.size != 1) {
            println("No argument needed!")
            return
        }

        println("Bye")
        robot.shouldContinue = false
    }
}

class ListCapabilityCommand(tmpRobot: AutonomousRobot) : RobotCommand("caps", "caps", "lists all available capabilities", tmpRobot) {
    override fun executeCommand(args: List<String>) {
        if (args.size != 1) {
            println("No argument needed!")
            return
        }
        val capabilities = robot.queryMaster.getSubClassQuery("Capability")
        println("\n Capabilities:")
        for (cap in capabilities) {
            println(" " + cap.localName)
        }
        print("\n")
    }
}

class CapabilityRequireCommand(tmpRobot: AutonomousRobot) : RobotCommand("require", "require cap_name", "lists all requirements for the capabilities", tmpRobot) {
    override fun executeCommand(args: List<String>) {
        if (args.size != 2) {
            println("Exactly one needed!")
            return
        }

        var capResource: Resource? = null

        // check if entered capability exists
        val capabilities = robot.queryMaster.getSubClassQuery("Capability")
        for (cap in capabilities) {
            if (args[1].equals(cap.localName)) {
                capResource = cap
                break
            }
        }

        if (capResource == null) {
            println("The entered capability does not exist!")
            return
        }

        // get all direct and indirect capabilites and components of the entered capability
        val cap = Capability(capResource)
        fillCapCompWithRequiredComponentsAndCapabilities(cap)

        // print the configuration of the chosen capability
        println("The capability ${capResource.localName} needs the following configuration (consisting of capabilites and components):")
        printConfigurationOfCompCap(cap, 0)
    }

    fun printConfigurationOfCompCap(compCap: ComponentCapability, numBanks: Int) {
        for (blankIndex in 0..(numBanks - 1))
            print(" ")

        if (numBanks != 0) {
            print(compCap.resource.localName + " (")

            when (compCap) {
                is Capability -> print("Capability")
                is Component -> print("Component")
                else -> throw java.lang.Exception("The chosen class is not supported!!")
            }

            println(")")
        }

        for (cap in compCap.capabilities)
            printConfigurationOfCompCap(cap, numBanks + 2)
        for (comp in compCap.components)
            printConfigurationOfCompCap(comp, numBanks + 2)
    }

    // TODO maybe loop checking - add parent and look for same name occurrence
    fun fillCapCompWithRequiredComponentsAndCapabilities(compCap: ComponentCapability) {
        compCap.capabilities.clear()
        compCap.components.clear()

        // get all the capabilities and components the compCap

        var capabilityResourcesCompCapDependsOn: MutableList<Resource>?
        var componentResourcesCompCapDependsOn: MutableList<Resource>?

        when (compCap) {
            is Capability -> {
                capabilityResourcesCompCapDependsOn = getAllCapabilityResourcesOfCapability(compCap)
                componentResourcesCompCapDependsOn = getAllComponentResourcesOfCapability(compCap)
            }
            is Component -> {
                capabilityResourcesCompCapDependsOn = getAllCapabilityResourcesOfComponent(compCap)
                componentResourcesCompCapDependsOn = getAllComponentResourcesOfComponent(compCap)
            }
            else -> throw java.lang.Exception("The chosen class is not supported!!")
        }


        for (capResource in capabilityResourcesCompCapDependsOn) {
            val cap = Capability(capResource)
            fillCapCompWithRequiredComponentsAndCapabilities(cap)
            compCap.capabilities.add(cap)
        }

        for (compResource in componentResourcesCompCapDependsOn) {
            val comp = Component(compResource)
            fillCapCompWithRequiredComponentsAndCapabilities(comp)
            compCap.components.add(comp)
        }
    }

    fun getAllCapabilityResourcesOfCapability(capability: Capability): MutableList<Resource> {
        val queryObject = QueryMaster.SpecifiedObjectPropertiesFromCategoryDo(capability.resource.localName,
                "Capability", "dependsOnCapability", "some", "capabilities")
        val capabilityResources = robot.queryMaster.getObjectFromDataClass(queryObject, "capabilities")
        return capabilityResources
    }

    fun getAllComponentResourcesOfCapability(capability: Capability): MutableList<Resource> {
        val queryObject = QueryMaster.SpecifiedObjectPropertiesFromCategoryDo(capability.resource.localName,
                "Capability", "dependsOnComponent", "some", "components", "comp")
        val componentResources = robot.queryMaster.getObjectFromDataClass(queryObject, "components")
        return componentResources
    }

    fun getAllCapabilityResourcesOfComponent(component: Component): MutableList<Resource> {
        val queryObject = QueryMaster.SpecifiedObjectPropertiesFromCategoryDo(component.resource.localName,
                "Component", "dependsOnCapability", "some", "capabilities", "", "comp")
        val capabilityResources = robot.queryMaster.getObjectFromDataClass(queryObject, "capabilities")
        return capabilityResources
    }

    fun getAllComponentResourcesOfComponent(component: Component): MutableList<Resource> {
        val queryObject = QueryMaster.SpecifiedObjectPropertiesFromCategoryDo(component.resource.localName,
                "Component", "dependsOnComponent", "some", "components", "comp", "comp")
        val componentResources = robot.queryMaster.getObjectFromDataClass(queryObject, "components")
        return componentResources
    }
}

