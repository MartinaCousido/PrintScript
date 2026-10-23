plugins {
    id("printscript.java-library")
    id("printscript.quality")
    id("printscript.format")
}

dependencies {
    implementation(platform("com.fasterxml.jackson:jackson-bom:2.22.4"))
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
}
