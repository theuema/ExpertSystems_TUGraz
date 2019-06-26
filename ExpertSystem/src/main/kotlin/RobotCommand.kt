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

        println("${robot.robiName}Bye")
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
            if (cap.resource == null) throw java.lang.Exception("The resource must not be null!")
            println(" " + cap.resource.localName)
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
            if (cap.resource == null) throw java.lang.Exception("The resource must not be null!")
            if (args[1].equals(cap.resource.localName)) {
                capResource = cap.resource
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
        println("${robot.robiName}Found configuration for capability ${capResource.localName} " +
                "(hierarchically consisting of capabilities and components):")
        println()
        val compCapOutputSet = mutableSetOf<CapabilityComponentNoDuplicateOutput>()
        printConfigurationOfCompCapHierarchical(cap, 0, compCapOutputSet)
        println()
        println("${robot.robiName}Simple list of all capabilities and components needed (no duplications):")
        println()
        printConfigurationOfCompCapSimpleWithoutDuplicate(compCapOutputSet, 0)
        println()
    }

    class CapabilityComponentNoDuplicateOutput(val name: String = "", val alternatives: MutableSet<CapabilityComponentNoDuplicateOutput>? = null, val alternativeTypeName: String = "") {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is CapabilityComponentNoDuplicateOutput) return false
            if (name.equals("") || other.name.equals("")) return super.equals(other)
            return name.equals(other.name)
        }

        override fun hashCode(): Int {
            if (!name.equals(""))
                return name.hashCode()

            return super.hashCode()
        }
    }

    fun printConfigurationOfCompCapSimpleWithoutDuplicate(compCapOutputSet: MutableSet<CapabilityComponentNoDuplicateOutput>, numBlanks: Int) {
        for (compCapOutput in compCapOutputSet) {
            print("  ")
            for (blankIndex in 0..(numBlanks - 1))
                print("-")

            if (!compCapOutput.name.equals("")) {
                println(compCapOutput.name)
            } else if (compCapOutput.alternatives != null) {
                println("Alternative " + compCapOutput.alternativeTypeName)
                printConfigurationOfCompCapSimpleWithoutDuplicate(compCapOutput.alternatives, numBlanks + 2)
            } else {
                throw Exception("Name nothing and alternatives null is not supported!")
            }
        }
    }

    fun printConfigurationOfCompCapHierarchical(compCapBase: ComponentCapabilityBase, numBlanks: Int, compCapOutputSet: MutableSet<CapabilityComponentNoDuplicateOutput>) {
        if (numBlanks == 0 && compCapBase !is Capability) throw Exception("First one must be a capability")

        // Don't want to print out first entry
        if (numBlanks != 0) {
            print("  ")
            for (blankIndex in 2..(numBlanks - 1))
                print("-")
        }

        when (compCapBase) {
            is Capability -> {
                // Don't want to print out first entry
                if (numBlanks != 0) {
                    var resourceName = compCapBase.resource.localName + " (Capability)"
                    println(resourceName)
                    compCapOutputSet.add(CapabilityComponentNoDuplicateOutput(resourceName))
                }

                for (cap in compCapBase.capabilities)
                    printConfigurationOfCompCapHierarchical(cap, numBlanks + 2, compCapOutputSet)
                for (comp in compCapBase.components)
                    printConfigurationOfCompCapHierarchical(comp, numBlanks + 2, compCapOutputSet)
            }
            is Component -> {
                var resourceName = compCapBase.resource.localName + " (Component)"
                println(resourceName)
                compCapOutputSet.add(CapabilityComponentNoDuplicateOutput(resourceName))

                for (cap in compCapBase.capabilities)
                    printConfigurationOfCompCapHierarchical(cap, numBlanks + 2, compCapOutputSet)
                for (comp in compCapBase.components)
                    printConfigurationOfCompCapHierarchical(comp, numBlanks + 2, compCapOutputSet)
            }
            is AlternativeCapabilities -> {
                println("Alternative Capabilities")
                val alternativeCapabilitesSet = mutableSetOf<CapabilityComponentNoDuplicateOutput>()
                val alternativeCapabilitesOutput = CapabilityComponentNoDuplicateOutput("", alternativeCapabilitesSet, "Capabilities")
                compCapOutputSet.add(alternativeCapabilitesOutput)

                for (cap in compCapBase.capabilities)
                    printConfigurationOfCompCapHierarchical(cap, numBlanks + 2, alternativeCapabilitesSet)
            }
            is AlternativeComponents -> {
                println("Alternative Components")
                val alternativeComponentsSet = mutableSetOf<CapabilityComponentNoDuplicateOutput>()
                val alternativeComponentsOutput = CapabilityComponentNoDuplicateOutput("", alternativeComponentsSet, "Components")
                compCapOutputSet.add(alternativeComponentsOutput)

                for (comp in compCapBase.components)
                    printConfigurationOfCompCapHierarchical(comp, numBlanks + 2, alternativeComponentsSet)

            }
            else -> throw java.lang.Exception("The chosen class is not supported!!")
        }
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

                var numNeededCompsCaps: Int = 0

                for (capResource in capabilityResourcesCompCapDependsOn) {
                    var capBase: CapabilityBase?
                    if (capResource.resource != null) {
                        capBase = Capability(capResource.resource)
                    } else if (capResource.alternativeResources != null) {
                        val capabilities = mutableListOf<Capability>()
                        for (altResource in capResource.alternativeResources)
                            capabilities.add(Capability(altResource))
                        capBase = AlternativeCapabilities(capabilities)
                    } else {
                        throw java.lang.Exception("At the QueryResult resource and alternativeResources must not be null!")
                    }

                    fillCapCompWithRequiredComponentsAndCapabilities(capBase)
                    compCapBase.capabilities.add(capBase)
                    numNeededCompsCaps += capBase.minNumberComponentsCapabilites
                }

                for (compResource in componentResourcesCompCapDependsOn) {
                    var compBase: ComponentBase?
                    if (compResource.resource != null) {
                        compBase = Component(compResource.resource)
                    } else if (compResource.alternativeResources != null) {
                        val components = mutableListOf<Component>()
                        for (altResource in compResource.alternativeResources)
                            components.add(Component(altResource))
                        compBase = AlternativeComponents(components)
                    } else {
                        throw java.lang.Exception("At the QueryResult resource and alternativeResources must not be null!")
                    }

                    fillCapCompWithRequiredComponentsAndCapabilities(compBase)
                    compCapBase.components.add(compBase)
                    numNeededCompsCaps += compBase.minNumberComponentsCapabilites
                }

                compCapBase.minNumberComponentsCapabilites = numNeededCompsCaps
            }
            is Component -> {
                compCapBase.capabilities.clear()
                compCapBase.components.clear()
                var capabilityResourcesCompCapDependsOn = getAllCapabilityResourcesOfComponent(compCapBase)
                var componentResourcesCompCapDependsOn = getAllComponentResourcesOfComponent(compCapBase)

                var numNeededCompsCaps: Int = 0

                for (capResource in capabilityResourcesCompCapDependsOn) {
                    var capBase: CapabilityBase?
                    if (capResource.resource != null) {
                        capBase = Capability(capResource.resource)
                    } else if (capResource.alternativeResources != null) {
                        val capabilities = mutableListOf<Capability>()
                        for (altResource in capResource.alternativeResources)
                            capabilities.add(Capability(altResource))
                        capBase = AlternativeCapabilities(capabilities)
                    } else {
                        throw java.lang.Exception("At the QueryResult resource and alternativeResources must not be null!")
                    }

                    fillCapCompWithRequiredComponentsAndCapabilities(capBase)
                    compCapBase.capabilities.add(capBase)
                    numNeededCompsCaps += capBase.minNumberComponentsCapabilites
                }

                for (compResource in componentResourcesCompCapDependsOn) {
                    var compBase: ComponentBase?
                    if (compResource.resource != null) {
                        compBase = Component(compResource.resource)
                    } else if (compResource.alternativeResources != null) {
                        val components = mutableListOf<Component>()
                        for (altResource in compResource.alternativeResources)
                            components.add(Component(altResource))
                        compBase = AlternativeComponents(components)
                    } else {
                        throw java.lang.Exception("At the QueryResult resource and alternativeResources must not be null!")
                    }

                    fillCapCompWithRequiredComponentsAndCapabilities(compBase)
                    compCapBase.components.add(compBase)
                    numNeededCompsCaps += compBase.minNumberComponentsCapabilites
                }

                compCapBase.minNumberComponentsCapabilites = numNeededCompsCaps
            }
            is AlternativeCapabilities -> {
                // fill all the alternative capabilites with its components and capabilites
                for (cap in compCapBase.capabilities)
                    fillCapCompWithRequiredComponentsAndCapabilities(cap)

                // assign cap with least minNumberComponentsCapabilites to compCapBase
                var curMinNumberComponentsCapabilites: Int = Int.MAX_VALUE
                var curCapability: Capability? = null

                for (cap in compCapBase.capabilities) {
                    if (cap.minNumberComponentsCapabilites < curMinNumberComponentsCapabilites) {
                        curMinNumberComponentsCapabilites = cap.minNumberComponentsCapabilites
                        curCapability = cap
                    }
                }

                if (curCapability == null)
                    throw Exception("Capability must not be null!!")

                compCapBase.minNumberComponentsCapabilites = curMinNumberComponentsCapabilites
                compCapBase.capWithMinNumCompsCaps = curCapability
            }
            is AlternativeComponents -> {
                // fill all the alternative components with its components and capabilites
                for (comp in compCapBase.components)
                    fillCapCompWithRequiredComponentsAndCapabilities(comp)

                // assign comp with least minNumberComponentsCapabilites to compCapBase
                var curMinNumberComponentsCapabilites: Int = Int.MAX_VALUE
                var curComponent: Component? = null

                for (comp in compCapBase.components) {
                    if (comp.minNumberComponentsCapabilites < curMinNumberComponentsCapabilites) {
                        curMinNumberComponentsCapabilites = comp.minNumberComponentsCapabilites
                        curComponent = comp
                    }
                }

                if (curComponent == null)
                    throw Exception("Component must not be null!!")

                compCapBase.minNumberComponentsCapabilites = curMinNumberComponentsCapabilites
                compCapBase.compWithMinNumCompsCaps = curComponent
            }
            else -> throw java.lang.Exception("The chosen class is not supported!!")
        }
    }

    fun getAllCapabilityResourcesOfCapability(capability: Capability): MutableList<QueryMaster.QueryResult> {
        val queryObject = QueryMaster.SpecifiedObjectPropertiesFromCategoryDo(capability.resource.localName,
                "Capability", "dependsOnCapability", "some", "capabilities")
        val capabilityResources = robot.queryMaster.getObjectFromDataClass(queryObject, "capabilities")
        return capabilityResources
    }

    fun getAllComponentResourcesOfCapability(capability: Capability): MutableList<QueryMaster.QueryResult> {
        val queryObject = QueryMaster.SpecifiedObjectPropertiesFromCategoryDo(capability.resource.localName,
                "Capability", "dependsOnComponent", "some", "components", "comp")
        val componentResources = robot.queryMaster.getObjectFromDataClass(queryObject, "components")
        return componentResources
    }

    fun getAllCapabilityResourcesOfComponent(component: Component): MutableList<QueryMaster.QueryResult> {
        val queryObject = QueryMaster.SpecifiedObjectPropertiesFromCategoryDo(component.resource.localName,
                "Component", "dependsOnCapability", "some", "capabilities", "", "comp")
        val capabilityResources = robot.queryMaster.getObjectFromDataClass(queryObject, "capabilities")
        return capabilityResources
    }

    fun getAllComponentResourcesOfComponent(component: Component): MutableList<QueryMaster.QueryResult> {
        val queryObject = QueryMaster.SpecifiedObjectPropertiesFromCategoryDo(component.resource.localName,
                "Component", "dependsOnComponent", "some", "components", "comp", "comp")
        val componentResources = robot.queryMaster.getObjectFromDataClass(queryObject, "components")
        return componentResources
    }
}

