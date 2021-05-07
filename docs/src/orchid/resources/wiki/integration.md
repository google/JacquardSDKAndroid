It's easy to integrate the Jacquard SDK using Maven.

You need to include GMaven in your repositories section of your gradle file:

```
  repositories {
    maven {
        url "https://maven.google.com/"
    }
  }
```

Then include Jacquard in the `dependencies` section:

```
  implementation "com.google.jacquard:jacquard-sdk:0.2.0"
```
