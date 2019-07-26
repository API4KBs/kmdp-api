# kmdp-API
##Release Instructions

Affected variables:
* project.parent.version
* project.version (SELF)

Affected properties
* kmdp.archetype.version.resolved
* org.omg.spec.API4KP.version

Affected files
* properties/*.properties      

Affected plugin configuration:
* kmdp-configurable-archetype-plugin.archetypeVersion

### Release Branch
1. Set root POM's version and parent.version to desired fixed version
  * The version MUST match the ${kmdp.impl.version} variable in the BOM
2. Set kmdp.archetype.version.resolved = ${kmdp.archetype.version}
3. Set org.omg.spec.API4KP.version = ${api4kp.version}
4. Ensure all properties/ files declare
  * version = (the value of ${kmdp.impl.version})
  * apiVersion = (should stay whatever value has been used in the last dev cycle)
5. Set kmdp-configurable-archetype-plugin.archetypeVersion = ${kmdp.archetype.version}

### Nex Dev Branch
1. Set parent and project to the next desired version
2. Set kmdp.archetype.version.resolved = ${edu.mayo.kmdp:kmdp-api-facade-generator:jar.version}
3. Set org.omg.spec.API4KP.version = ${org.omg.spec:API4KP:jar.version}
4. Update the properties/ files to match the next version of kmdp.impl.version
5. Set kmdp-configurable-archetype-plugin.archetypeVersion = ${edu.mayo.kmdp:kmdp-openapi-spring-service-archetype:jar.version}