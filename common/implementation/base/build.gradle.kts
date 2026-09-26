dependencies {
    api(project(":common:api"))
    api(project(":common:implementation:bootstrap-addon-loader"))

    testImplementation("org.slf4j", "slf4j-api", Versions.Libraries.slf4j)

    // Console rendering for com.dfsek.terra.text and com.dfsek.terra.log. Paper supplies these at
    // runtime; shading them would create a second Component type that its Audience cannot accept.
    compileOnly("net.kyori", "adventure-api", Versions.Libraries.adventure)
    compileOnly("net.kyori", "adventure-text-minimessage", Versions.Libraries.adventure)
    compileOnly("net.kyori", "adventure-text-serializer-legacy", Versions.Libraries.adventure)
    compileOnly("net.kyori", "adventure-text-serializer-plain", Versions.Libraries.adventure)
    testImplementation("net.kyori", "adventure-api", Versions.Libraries.adventure)
    testImplementation("net.kyori", "adventure-text-minimessage", Versions.Libraries.adventure)
    testImplementation("net.kyori", "adventure-text-serializer-legacy", Versions.Libraries.adventure)
    testImplementation("net.kyori", "adventure-text-serializer-plain", Versions.Libraries.adventure)

    implementation("commons-io", "commons-io", Versions.Libraries.Internal.apacheIO)

    implementation("org.apache.commons", "commons-text", Versions.Libraries.Internal.apacheText)
    implementation("com.dfsek.tectonic", "yaml", Versions.Libraries.tectonic)



    implementation("com.dfsek", "paralithic", Versions.Libraries.paralithic)
}
