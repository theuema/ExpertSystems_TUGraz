import org.apache.jena.rdf.model.Resource

abstract class ComponentCapabilityBase() {
    var minNumberComponentsCapabilites : Int = 0
}

abstract class ComponentBase() : ComponentCapabilityBase()
abstract class CapabilityBase() : ComponentCapabilityBase()

class AlternativeCapabilities() : CapabilityBase() {
    val capabilities = mutableListOf<Capability>()
    var capWithMinNumCompsCaps : Capability? = null
}

class AlternativeComponents() : ComponentBase() {
    val components = mutableListOf<Component>()
    var compWithMinNumCompsCaps : Component? = null
}

class Component(val resource: Resource) : ComponentBase() {
    val capabilities = mutableListOf<CapabilityBase>()
    val components = mutableListOf<ComponentBase>()
}

class Capability(val resource: Resource) : CapabilityBase() {
    val capabilities = mutableListOf<CapabilityBase>()
    val components = mutableListOf<ComponentBase>()
}
