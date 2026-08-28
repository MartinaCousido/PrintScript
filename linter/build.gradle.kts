plugins {
    id("printscript.java-library")
    id("printscript.quality")
    id("printscript.format")
}

dependencies {
    api(project(":common"))
    api(project(":ast"))
    api(project(":configuration"))
}
