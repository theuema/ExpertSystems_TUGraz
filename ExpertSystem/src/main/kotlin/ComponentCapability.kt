import org.apache.jena.rdf.model.Resource

abstract class ComponentCapabilityBase() {
    var minNumberComponentsCapabilites: Int = 0
}

abstract class ComponentBase() : ComponentCapabilityBase()
abstract class CapabilityBase() : ComponentCapabilityBase()

class AlternativeCapabilities(val capabilities: MutableList<Capability>) : CapabilityBase() {
    var capWithMinNumCompsCaps: Capability? = null
}

class AlternativeComponents(val components: MutableList<Component>) : ComponentBase() {
    var compWithMinNumCompsCaps: Component? = null
}

class Component(val resource: Resource) : ComponentBase() {
    val capabilities = mutableListOf<CapabilityBase>()
    val components = mutableListOf<ComponentBase>()
}

class Capability(val resource: Resource) : CapabilityBase() {
    val capabilities = mutableListOf<CapabilityBase>()
    val components = mutableListOf<ComponentBase>()
}
