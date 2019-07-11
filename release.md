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

### Release Branch
1. Set root POM's version and parent.version to desired fixed version
  * The version MUST match the ${kmdp.impl.version} variable in the BOM
2. Set kmdp.archetype.version.resolved = ${kmdp.archetype.version}
3. Set org.omg.spec.API4KP.version = ${api4kp.version}
4. Ensure all properties/ files declare
  * version = (the value of ${kmdp.impl.version})
  * apiVersion = (the value of ${api4kp.version})

### Nex Dev Branch
1. Set parent and project to the next desired version
2. Set kmdp.archetype.version.resolved = ${edu.mayo.kmdp:kmdp-api-facade-generator:jar.version}
3. Set org.omg.spec.API4KP.version = ${org.omg.spec:API4KP:jar.version}
4. Update the properties/ files to match the next version of kmdp.impl.version