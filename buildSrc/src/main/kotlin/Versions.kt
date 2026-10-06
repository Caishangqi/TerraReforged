object Versions {
    object Build {
        // Purpur 26.2 runs on Java 25 and the 26.2 server jars are compiled to class file major 69.
        const val java = 25
    }

    object Terra {
        const val overworldConfig = "latest"
        const val reimagENDConfig = "latest"
        const val tartarusConfig = "latest"
        const val defaultConfig = "latest"
    }
    
    object Libraries {
        const val tectonic = "4.3.1"
        const val paralithic = "2.0.1"
        const val strata = "1.3.2"
        const val seismic = "2.5.7"
        
        const val cloud = "2.0.0"
        
        const val caffeine = "3.2.2"
        
        const val slf4j = "2.0.17"

        // Console text rendering. Paper 26.2 imports adventure-bom 5.2.0 and ships adventure-api,
        // minimessage and the legacy/plain serializers on the server classpath, so this is pinned to
        // the version the Dev Bundle resolves and declared compileOnly. It must never be shaded: a
        // second copy of Component cannot be sent to Paper's own Audience.
        const val adventure = "5.2.0"

        object Internal {
            const val shadow = "8.3.9"
            const val apacheText = "1.14.0"
            const val apacheIO = "2.20.0"
            const val guava = "33.5.0-jre"
            const val asm = "9.9"
            const val snakeYml = "2.5"
            const val jetBrainsAnnotations = "26.0.2-1"
            const val junit = "6.0.0"
            const val nbt = "6.1"
        }
    }
    
    object Fabric {
        const val fabricAPI = "0.134.1+${Mod.minecraft}"
        const val cloud = "2.0.0-beta.13"
    }
//
//    object Quilt {
//        const val quiltLoader = "0.20.2"
//        const val fabricApi = "7.3.1+0.89.3-1.20.1"
//    }
    
    object Mod {
        const val mixin = "0.16.4+mixin.0.8.7"
        const val mixinExtras = "0.5.0"
        
        const val minecraft = "1.21.10"
        const val yarn = "$minecraft+build.1"
        const val fabricLoader = "0.18.2"
        
        const val architecuryLoom = "1.11.451"
        const val architecturyPlugin = "3.4.162"

    }
//
//    object Forge {
//        const val forge = "${Mod.minecraft}-48.0.13"
//        const val burningwave = "12.63.0"
//    }
    
    object Bukkit {
        // Paper abandoned the "<mc>-R0.1-SNAPSHOT" coordinate after 1.21.11. A 26.2 artifact is a
        // release, not a snapshot, and carries its build number in the version string itself.
        // paper-api and dev-bundle share this coordinate, so one constant pins both.
        const val minecraft = "26.2"
        const val paperBuild = "129"
        const val paper = "$minecraft.build.$paperBuild-stable"
        const val paperDevBundle = paper
        const val paperLib = "1.0.8"
        const val reflectionRemapper = "0.1.3"
        const val runPaper = "3.1.0"

        // 2.0.1 is the first cloud release whose RegistryReflection falls back to
        // net.minecraft.resources.Identifier. Older builds only look for ResourceLocation and
        // MinecraftKey, so command registration throws on 26.2 and Terra never finishes enabling.
        const val cloud = "2.0.1"
        const val multiverse = "5.3.0"

        // Compile-only, and pinned to the build the test server runs rather than to the newest
        // release. Only OraxenBlocks is used, but an integration that compiles against a later API
        // than it is tested on cannot say which of the two it proved.
        const val oraxen = "1.219.0"

        const val craftEngine = "26.9.2"

        // The Paperweight plugin version is not declared here. buildSrc resolves its own plugin
        // classpath before this file compiles, so buildSrc/build.gradle.kts owns that version.
    }
    
//
//    object Sponge {
//        const val sponge = "9.0.0-SNAPSHOT"
//        const val mixin = "0.8.2"
//        const val minecraft = "1.17.1"
//    }
//
    object CLI {
        const val logback = "1.5.19"
        const val picocli = "4.7.7"
    }
    
    object Allay {
        const val api = "0.20.0"
        const val gson = "2.13.2"
        
        const val mappings = "366baa6"
        const val mappingsGenerator = "e957088"
        
        const val mcmeta = "c976eb3"
    }
    
    object Minestom {
        const val minestom = "2025.10.04-1.21.8"
    }
}
