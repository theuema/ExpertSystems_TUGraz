import org.apache.jena.rdf.model.Resource

abstract class Configuration {
    val capabilities = mutableListOf<Resource>()
    val components = mutableListOf<Resource>()
}

class Component : Configuration()

class Capability : Configuration()
