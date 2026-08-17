plugins {
    id("printscript.java-library")
    id("printscript.quality")
}

dependencies {
    api(project(":common"))
    api(project(":ast"))
    api(project(":runtime-api"))
}
