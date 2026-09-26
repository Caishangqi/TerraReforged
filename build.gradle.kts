preRelease(true)

versionProjects(":common:api", version("7.0.0"))
versionProjects(":common:implementation", version("7.0.0"))
versionProjects(":platforms", version("7.0.0"))


allprojects {
    group = "com.dfsek.terra"

    configureCompilation()
    configureDependencies()
    configurePublishing()

    tasks.withType<JavaCompile>().configureEach {
        options.isFork = true
        options.isIncremental = true
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()

        maxHeapSize = "2G"
        ignoreFailures = false
        failFast = true
        maxParallelForks = (Runtime.getRuntime().availableProcessors() - 1).takeIf { it > 0 } ?: 1

        reports.html.required.set(false)
        reports.junitXml.required.set(false)
    }

    tasks.withType<Copy>().configureEach {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    tasks.withType<Jar>().configureEach {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}

afterEvaluate {
    forImmediateSubProjects(":platforms") {
        configureDistribution()
    }
    project(":platforms:bukkit:common").configureDistribution()
    forSubProjects(":common:addons") {
        // Shadow 9 wires shadowJar into assemble itself, so an addon's build already produces its
        // shaded jar. Adding `build finalizedBy shadowJar` on top of that made `./gradlew build` fail
        // with a circular dependency: build -> assemble -> shadowJar -> build.
        apply(plugin = "com.gradleup.shadow")

        dependencies {
            "compileOnly"(project(":common:api"))
            "testImplementation"(project(":common:api"))
        }
    }
}
