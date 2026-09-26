import com.dfsek.terra.tectonicdoc.TectonicDocPlugin
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.filter
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.plugins.ide.idea.model.IdeaModel

fun Project.configureCompilation() {
    apply(plugin = "maven-publish")
    apply(plugin = "java")
    apply(plugin = "java-library")
    apply(plugin = "idea")
    apply<TectonicDocPlugin>()
    
    configure<IdeaModel> {
        module {
            isDownloadJavadoc = true
            isDownloadSources = true
        }
    }
    
    // Minecraft 26.2 ships class file major 69. Javac rejects that on a lower --release, so the
    // whole tree targets Java 25 rather than splitting the Bukkit modules onto their own level.
    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(Versions.Build.java))
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(Versions.Build.java)
        doFirst {
            options.compilerArgs.add("-Xlint:all")
        }
    }
    
    tasks.withType<ProcessResources> {
        include("**/*.*")
        filter<ReplaceTokens>(
            "tokens" to mapOf(
                "DESCRIPTION" to properties["terra.description"],
                "WIKI" to properties["terra.wiki"],
                "SOURCE" to properties["terra.source"],
                "ISSUES" to properties["terra.issues"],
                "LICENSE" to properties["terra.license"]
                             )
                             )
    }
    
    afterEvaluate {
        tasks.withType<ProcessResources> {
            include("**/*.*")
            filter<ReplaceTokens>(
                "tokens" to mapOf(
                    "VERSION" to version.toString()
                                 )
                                 )
        }
    }
    
    tasks.withType<Javadoc> {
        options.encoding = "UTF-8"
    }
    
    tasks.withType<Jar> {
        archiveBaseName.set("TerraReforged-${archiveBaseName.get()}")
        from("../LICENSE", "../../LICENSE")
    }
    
    tasks.register<Jar>("sourcesJar") {
        archiveClassifier.set("sources")
    }
    
    tasks.register<Jar>("javadocJar") {
        dependsOn("javadoc")
        archiveClassifier.set("javadoc")
        from(tasks.getByName<Javadoc>("javadoc").destinationDir)
    }
}