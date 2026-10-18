version = version("1.0.0")

dependencies {
    compileOnlyApi(project(":common:addons:manifest-addon-loader"))

    testImplementation("com.dfsek.tectonic", "yaml", Versions.Libraries.tectonic)
}
