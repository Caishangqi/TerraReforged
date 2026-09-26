import org.gradle.api.Action
import org.gradle.api.Project


var isPrerelease = false


/**
 * Gradle 9 removed `Project.exec`. `providers.exec` is the replacement, and it fails lazily: the
 * non-zero exit of a working copy without Git only surfaces when the result is read.
 */
fun Project.getGitHash(): String {
    val git = providers.exec {
        commandLine("git", "rev-parse", "--short", "HEAD")
        isIgnoreExitValue = true
    }
    if (git.result.get().exitValue != 0) {
        logger.warn("Git revision is unavailable. Using dev as the prerelease version.")
        return "dev"
    }
    return git.standardOutput.asText.get().trim()
}

fun Project.forSubProjects(project: String, action: Action<Project>) {
    project(project).subprojects.forEach {
        action.execute(it)
    }
}

fun Project.forImmediateSubProjects(project: String, action: Action<Project>) {
    project(project).childProjects.forEach {
        action.execute(it.value)
    }
}

fun preRelease(preRelease: Boolean) {
    isPrerelease = preRelease
}

fun Project.versionProjects(project: String, version: String) {
    forSubProjects(project) {
        this.version = version
        logger.info("Setting version of $path to $version")
    }
    project(project).version = version
    logger.info("Setting version of $project to $version")
}

fun Project.version(version: String): String {
    return if (!isPrerelease)
        version
    else //Only use git hash if it's a prerelease.
        "$version-BETA+${getGitHash()}"
}