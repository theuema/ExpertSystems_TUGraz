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
        println("${robot.robiName}Found the following Capabilities:")
        println()
        for (cap in capabilities) {
            println(" " + cap.localName)
        }
        println()
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
        println("${robot.robiName}Found configuration for capability ${capResource.localName}: " +
                "(hierarchically consisting of capabilities and components)")
        println()
        val compCapSet = mutableSetOf<String>()
        printConfigurationOfCompCap(cap, 0, compCapSet)
        println()
        println("${robot.robiName}Simple list of all capabilities and components needed: (no duplications)")
        println()
        for (name in compCapSet) {
            println("  $name")
        }
        println()
    }

    fun printConfigurationOfCompCap(compCap: ComponentCapability, numBanks: Int, compCapSet: MutableSet<String>) {
        if (numBanks != 0) {
            print("  ")
            for (blankIndex in 2..(numBanks - 1))
                print("-")
            var resourceName = compCap.resource.localName + " ("

            when (compCap) {
                is Capability -> resourceName += "Capability"
                is Component -> resourceName += "Component"
                else -> throw java.lang.Exception("The chosen class is not supported!!")
            }
            resourceName += ")"
            println(resourceName)
            compCapSet.add(resourceName)
        }

        for (cap in compCap.capabilities)
            printConfigurationOfCompCap(cap, numBanks + 2, compCapSet)
        for (comp in compCap.components)
            printConfigurationOfCompCap(comp, numBanks + 2, compCapSet)
    }

    // TODO maybe loop checking - add parent and look for same name occurrence
    fun fillCapCompWithRequiredComponentsAndCapabilities(compCapBase: ComponentCapabilityBase) {
        // get all the capabilities and components the compCapBase

        when (compCapBase) {
            is Capability -> {
                compCapBase.capabilities.clear()
                compCapBase.components.clear()
                var capabilityResourcesCompCapDependsOn = getAllCapabilityResourcesOfCapability(compCapBase)
                var componentResourcesCompCapDependsOn = getAllComponentResourcesOfCapability(compCapBase)

                var numNeededCompsCaps : Int = 0

                for (capResource in capabilityResourcesCompCapDependsOn) {
                    val cap = Capability(capResource)
                    fillCapCompWithRequiredComponentsAndCapabilities(cap)
                    compCapBase.capabilities.add(cap)
                    numNeededCompsCaps += cap.minNumberComponentsCapabilites
                }

                for (compResource in componentResourcesCompCapDependsOn) {
                    val comp = Component(compResource)
                    fillCapCompWithRequiredComponentsAndCapabilities(comp)
                    compCapBase.components.add(comp)
                    numNeededCompsCaps += comp.minNumberComponentsCapabilites
                }

                compCapBase.minNumberComponentsCapabilites = numNeededCompsCaps
            }
            is Component -> {
                compCapBase.capabilities.clear()
                compCapBase.components.clear()
                var capabilityResourcesCompCapDependsOn = getAllCapabilityResourcesOfComponent(compCapBase)
                var componentResourcesCompCapDependsOn = getAllComponentResourcesOfComponent(compCapBase)

                var numNeededCompsCaps : Int = 0

                // TODO make possible with alternative interpretations
                for (capResource in capabilityResourcesCompCapDependsOn) {
                    val cap = Capability(capResource)
                    fillCapCompWithRequiredComponentsAndCapabilities(cap)
                    compCapBase.capabilities.add(cap)
                    numNeededCompsCaps += cap.minNumberComponentsCapabilites
                }

                for (compResource in componentResourcesCompCapDependsOn) {
                    val comp = Component(compResource)
                    fillCapCompWithRequiredComponentsAndCapabilities(comp)
                    compCapBase.components.add(comp)
                    numNeededCompsCaps += comp.minNumberComponentsCapabilites
                }

                compCapBase.minNumberComponentsCapabilites = numNeededCompsCaps
            }
            is AlternativeCapabilities -> {
                // fill all the alternative capabilites with its components and capabilites
                for(cap in compCapBase.capabilities)
                    fillCapCompWithRequiredComponentsAndCapabilities(cap)

                // assign cap with least minNumberComponentsCapabilites to compCapBase
                var curMinNumberComponentsCapabilites : Int = Int.MAX_VALUE
                var curCapability : Capability? = null

                for (cap in compCapBase.capabilities) {
                    if(cap.minNumberComponentsCapabilites < curMinNumberComponentsCapabilites) {
                        curMinNumberComponentsCapabilites = cap.minNumberComponentsCapabilites
                        curCapability = cap
                    }
                }

                if(curCapability ==  null)
                    throw Exception("Capability must not be null!!")

                compCapBase.minNumberComponentsCapabilites = curMinNumberComponentsCapabilites
                compCapBase.capWithMinNumCompsCaps = curCapability
            }
            is AlternativeComponents -> {
                // fill all the alternative components with its components and capabilites
                for(comp in compCapBase.components)
                    fillCapCompWithRequiredComponentsAndCapabilities(comp)

                // assign comp with least minNumberComponentsCapabilites to compCapBase
                var curMinNumberComponentsCapabilites : Int = Int.MAX_VALUE
                var curComponent : Component? = null

                for (comp in compCapBase.components) {
                    if(comp.minNumberComponentsCapabilites < curMinNumberComponentsCapabilites) {
                        curMinNumberComponentsCapabilites = comp.minNumberComponentsCapabilites
                        curComponent = comp
                    }
                }

                if(curComponent ==  null)
                    throw Exception("Component must not be null!!")

                compCapBase.minNumberComponentsCapabilites = curMinNumberComponentsCapabilites
                compCapBase.compWithMinNumCompsCaps = curComponent
            }
            else -> throw java.lang.Exception("The chosen class is not supported!!")
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

