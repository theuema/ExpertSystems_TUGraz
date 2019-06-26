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
        val cap = Capability(capResource, null)
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

    class AlternativeCapabilityComponentNoDuplicateOutput(val alternativeName: String = "", val alternativesCompCapOutputSet : MutableSet<CapabilityComponentNoDuplicateOutput>)

    class CapabilityComponentNoDuplicateOutput(val name: String = "", val alternatives: MutableList<AlternativeCapabilityComponentNoDuplicateOutput>? = null, val alternativeTypeName: String = "") {
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
                println("Alternatives of Type " + compCapOutput.alternativeTypeName)
                for(alt in compCapOutput.alternatives) {
                    print("  ")
                    for (blankIndex in 0..(numBlanks + 2 - 1))
                        print("-")
                    println("Alternative ${alt.alternativeName}")
                    printConfigurationOfCompCapSimpleWithoutDuplicate(alt.alternativesCompCapOutputSet, numBlanks + 4)
                }
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
                val alternativeCapabilityList = mutableListOf<AlternativeCapabilityComponentNoDuplicateOutput>()
                val alternativeCapabilitesOutput = CapabilityComponentNoDuplicateOutput("", alternativeCapabilityList, "Capabilities")
                compCapOutputSet.add(alternativeCapabilitesOutput)

                for (cap in compCapBase.capabilities) {
                    val alternativesCompCapOutputSet = mutableSetOf<CapabilityComponentNoDuplicateOutput>()
                    val alternativeCapabilitySet = AlternativeCapabilityComponentNoDuplicateOutput(cap.resource.localName, alternativesCompCapOutputSet)
                    alternativeCapabilityList.add(alternativeCapabilitySet)
                    printConfigurationOfCompCapHierarchical(cap, numBlanks + 2, alternativesCompCapOutputSet)
                }
            }
            is AlternativeComponents -> {
                println("Alternative Components")
                val alternativeComponentList = mutableListOf<AlternativeCapabilityComponentNoDuplicateOutput>()
                val alternativeComponentsOutput = CapabilityComponentNoDuplicateOutput("", alternativeComponentList, "Components")
                compCapOutputSet.add(alternativeComponentsOutput)

                for (comp in compCapBase.components) {
                    val alternativesCompCapOutputSet = mutableSetOf<CapabilityComponentNoDuplicateOutput>()
                    val alternativeComponentSet = AlternativeCapabilityComponentNoDuplicateOutput(comp.resource.localName, alternativesCompCapOutputSet)
                    alternativeComponentList.add(alternativeComponentSet)
                    printConfigurationOfCompCapHierarchical(comp, numBlanks + 2, alternativesCompCapOutputSet)
                }

            }
            else -> throw java.lang.Exception("The chosen class is not supported!!")
        }
    }

    fun fillCapCompWithRequiredComponentsAndCapabilities(compCapBase: ComponentCapabilityBase) {
        // get all the capabilities and components the compCapBase

        when (compCapBase) {
            is Capability -> {
                compCapBase.capabilities.clear()
                compCapBase.components.clear()
                var capabilityResourcesCompCapDependsOn = getAllCapabilityResourcesOfCapability(compCapBase)
                var componentResourcesCompCapDependsOn = getAllComponentResourcesOfCapability(compCapBase)

                for (capResource in capabilityResourcesCompCapDependsOn) {
                    var capBase: CapabilityBase?
                    if (capResource.resource != null) {
                        capBase = Capability(capResource.resource, compCapBase)
                        checkIfLoopExists(capBase)
                    } else if (capResource.alternativeResources != null) {
                        val capabilities = mutableListOf<Capability>()
                        capBase = AlternativeCapabilities(capabilities, compCapBase)
                        for (altResource in capResource.alternativeResources) {
                            val cap = Capability(altResource, capBase)
                            capabilities.add(cap)
                            checkIfLoopExists(cap)
                        }
                    } else {
                        throw java.lang.Exception("At the QueryResult resource and alternativeResources must not be null!")
                    }

                    fillCapCompWithRequiredComponentsAndCapabilities(capBase)
                    compCapBase.capabilities.add(capBase)
                }

                for (compResource in componentResourcesCompCapDependsOn) {
                    var compBase: ComponentBase?
                    if (compResource.resource != null) {
                        compBase = Component(compResource.resource, compCapBase)
                        checkIfLoopExists(compBase)
                    } else if (compResource.alternativeResources != null) {
                        val components = mutableListOf<Component>()
                        compBase = AlternativeComponents(components, compCapBase)
                        for (altResource in compResource.alternativeResources) {
                            val comp = Component(altResource, compBase)
                            components.add(comp)
                            checkIfLoopExists(comp)
                        }
                    } else {
                        throw java.lang.Exception("At the QueryResult resource and alternativeResources must not be null!")
                    }

                    fillCapCompWithRequiredComponentsAndCapabilities(compBase)
                    compCapBase.components.add(compBase)
                }
            }
            is Component -> {
                compCapBase.capabilities.clear()
                compCapBase.components.clear()
                var capabilityResourcesCompCapDependsOn = getAllCapabilityResourcesOfComponent(compCapBase)
                var componentResourcesCompCapDependsOn = getAllComponentResourcesOfComponent(compCapBase)

                for (capResource in capabilityResourcesCompCapDependsOn) {
                    var capBase: CapabilityBase?
                    if (capResource.resource != null) {
                        capBase = Capability(capResource.resource, compCapBase)
                        checkIfLoopExists(capBase)
                    } else if (capResource.alternativeResources != null) {
                        val capabilities = mutableListOf<Capability>()
                        capBase = AlternativeCapabilities(capabilities, compCapBase)
                        for (altResource in capResource.alternativeResources) {
                            val cap = Capability(altResource, capBase)
                            capabilities.add(cap)
                            checkIfLoopExists(cap)
                        }
                    } else {
                        throw java.lang.Exception("At the QueryResult resource and alternativeResources must not be null!")
                    }

                    fillCapCompWithRequiredComponentsAndCapabilities(capBase)
                    compCapBase.capabilities.add(capBase)
                }

                for (compResource in componentResourcesCompCapDependsOn) {
                    var compBase: ComponentBase?
                    if (compResource.resource != null) {
                        compBase = Component(compResource.resource, compCapBase)
                        checkIfLoopExists(compBase)
                    } else if (compResource.alternativeResources != null) {
                        val components = mutableListOf<Component>()
                        compBase = AlternativeComponents(components, compCapBase)
                        for (altResource in compResource.alternativeResources) {
                            val comp = Component(altResource, compBase)
                            components.add(comp)
                            checkIfLoopExists(comp)
                        }
                    } else {
                        throw java.lang.Exception("At the QueryResult resource and alternativeResources must not be null!")
                    }

                    fillCapCompWithRequiredComponentsAndCapabilities(compBase)
                    compCapBase.components.add(compBase)
                }
            }
            is AlternativeCapabilities -> {
                // fill all the alternative capabilites with its components and capabilites
                for (cap in compCapBase.capabilities)
                    fillCapCompWithRequiredComponentsAndCapabilities(cap)
            }
            is AlternativeComponents -> {
                // fill all the alternative components with its components and capabilites
                for (comp in compCapBase.components)
                    fillCapCompWithRequiredComponentsAndCapabilities(comp)
            }
            else -> throw java.lang.Exception("The chosen class is not supported!!")
        }
    }

    fun checkIfLoopExists(compCapBase: ComponentCapabilityBase) {
        var compCapName : String

        when(compCapBase) {
            is Capability -> compCapName = compCapBase.resource.localName
            is Component -> compCapName = compCapBase.resource.localName
            else -> return
        }

        var parent = compCapBase.parent
        var loopExists = false

        while(parent != null && !loopExists) {
            when (parent) {
                is Capability -> if (compCapName.equals(parent.resource.localName)) loopExists = true
                is Component -> if (compCapName.equals(parent.resource.localName)) loopExists = true
            }
            parent = parent.parent
        }

        if(loopExists)
            throw Exception("There exists a loop in the ontology when $compCapName was used. Check Ontology!")
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

