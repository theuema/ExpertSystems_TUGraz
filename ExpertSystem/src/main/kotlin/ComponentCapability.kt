import org.apache.jena.rdf.model.Resource

abstract class ComponentCapability(val resource: Resource) {
    val capabilities = mutableListOf<Capability>()
    val components = mutableListOf<Component>()
}

class Component(resource: Resource) : ComponentCapability(resource)

class Capability(resource: Resource) : ComponentCapability(resource)
