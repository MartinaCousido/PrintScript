plugins {
    id("printscript.java-library")
}

dependencies {
    api(project(":common"))
    api(project(":ast"))
    api(project(":runtime-api"))
}
