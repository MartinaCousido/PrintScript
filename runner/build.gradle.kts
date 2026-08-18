plugins {
    id("printscript.java-library")
    id("printscript.quality")
}

dependencies {
    api(project(":common"))
    api(project(":runtime-api"))
    implementation(project(":lexer"))
    implementation(project(":parser"))
    implementation(project(":interpreter"))
}
