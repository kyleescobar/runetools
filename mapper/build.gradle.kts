dependencies {
    implementation(project(":asm"))
    implementation("org.tinylog:tinylog-api-kotlin:_")
    implementation("org.tinylog:tinylog-impl:_")
    implementation("org.jgrapht:jgrapht-core:_")
    implementation("com.google.guava:guava:_")
}

tasks {
    register<JavaExec>("run") {
        dependsOn(build.get())
        group = "application"
        mainClass.set("dev.runetools.mapper.Mapper")
        workingDir = rootProject.projectDir
        classpath = sourceSets["main"].runtimeClasspath
    }

    register<Jar>("shadowJar") {
        dependsOn(build.get())
        group = "build"
        archiveClassifier.set("shaded")
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest {
            attributes["Main-Class"] = "dev.runetools.mapper.Mapper"
        }
        from(configurations.runtimeClasspath.get().map {
            if(it.isDirectory) it
            else zipTree(it)
        })
        with(jar.get())
    }

}