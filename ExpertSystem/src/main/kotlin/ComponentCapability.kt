import org.apache.jena.rdf.model.Resource

abstract class ComponentCapabilityBase(val parent: ComponentCapabilityBase?)

abstract class ComponentBase(parent: ComponentCapabilityBase?) : ComponentCapabilityBase(parent)
abstract class CapabilityBase(parent: ComponentCapabilityBase?) : ComponentCapabilityBase(parent)

class AlternativeCapabilities(val capabilities: MutableList<Capability>, parent: ComponentCapabilityBase?) : CapabilityBase(parent)

class AlternativeComponents(val components: MutableList<Component>, parent: ComponentCapabilityBase?) : ComponentBase(parent)

class Component(val resource: Resource, parent: ComponentCapabilityBase?) : ComponentBase(parent) {
    val capabilities = mutableListOf<CapabilityBase>()
    val components = mutableListOf<ComponentBase>()
}

class Capability(val resource: Resource, parent: ComponentCapabilityBase?) : CapabilityBase(parent) {
    val capabilities = mutableListOf<CapabilityBase>()
    val components = mutableListOf<ComponentBase>()
}
