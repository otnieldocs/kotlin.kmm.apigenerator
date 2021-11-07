import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.5.10"
    kotlin("plugin.serialization") version "1.5.31"
}

group = "me.ootniel"
version = "1.0-SNAPSHOT"

buildscript {
    repositories { mavenCentral() }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-serialization:1.5.31")
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-client-core:1.6.4")
    implementation("io.ktor:ktor-client-cio:1.6.4")
    implementation("io.ktor:ktor-client-serialization:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0")
    implementation("com.google.code.gson:gson:2.8.8")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "1.8"
}

abstract class GenerateRestApiTask: DefaultTask() {
    @get:org.gradle.api.tasks.InputFile
    abstract val destination: RegularFileProperty

    private val gson = com.google.gson.GsonBuilder().setLenient().create()
    private val camelRegex = "(?<=[a-zA-Z])[A-Z]".toRegex()
    private val snakeRegex = "_[a-zA-Z]".toRegex()

    // String extensions
    fun camelToSnakeCase(str: String): String {
        return camelRegex.replace(str) {
            "_${it.value}"
        }.toLowerCase()
    }

    fun snakeToLowerCamelCase(str: String): String {
        return snakeRegex.replace(str) {
            it.value.replace("_","")
                .toUpperCase()
        }
    }

    fun snakeToUpperCamelCase(str: String): String {
        return this.snakeToLowerCamelCase(str).capitalize()
    }

    @org.gradle.api.tasks.TaskAction
    fun read() {
        val inputFile = destination.get().asFile

        parse(inputFile)
    }

    private fun parse(inputFile: File) {
        val json = inputFile.readText()
        val jsonElmt = gson.fromJson(json, com.google.gson.JsonElement::class.java)
        val apiCollectionCode = mutableListOf<FunctionDef>()

        if (jsonElmt.isJsonObject) {
            val jsonObj = jsonElmt.asJsonObject
            val endpoints = jsonObj.getAsJsonArray("endpoints")
            println("endpoint size: ${endpoints.size()}")

            for (endpoint in endpoints) {
                val endpointObj = endpoint.asJsonObject
                val name = endpointObj.get("name").asString
                val path = endpointObj.get("path").asString
                val method = endpointObj.get("method").asString
                val responseModel = endpointObj.getAsJsonObject("response").getAsJsonObject("model")

                // construct class
                val requestClassName = name.plus("Request")
                val isRequestNull = endpointObj.getAsJsonObject("request").get("model").isJsonNull

                if (isRequestNull.not()) {
                    val requestModel = endpointObj.getAsJsonObject("request").getAsJsonObject("model")
                    val requestClass = getPojoClassString(requestClassName, requestModel)
                    writePojoClass(requestClassName, requestClass)
                }

                val responseClassName = name.plus("Response")
                val responseClass = getPojoClassString(responseClassName, responseModel)
                writePojoClass(responseClassName, responseClass)

                apiCollectionCode.add(
                    FunctionDef(
                        apiName = name,
                        endpoint = path,
                        method = method,
                        withRequest = isRequestNull.not()
                ))
            }

            writeApiCollectionClass(apiCollectionCode)
        }
    }

    private fun writePojoClass(name: String, code: String) {
        val pathApi = "src/main/kotlin/${project.group}/api"
        val dirName = (pathApi)
        val fileDir = File(dirName)
        if(!fileDir.exists()) fileDir.mkdirs()

        val outputFile = File("$pathApi/$name.kt")
        outputFile.writeText(code)
    }

    private fun getPojoClassString(clsName: String, model: com.google.gson.JsonObject): String {
        var props = ""
        val properties = model.getAsJsonArray("properties")

        for (i in 0 until properties.size()) {
            val property = properties.get(i)
            val propertyObj = property.asJsonObject
            val propName = propertyObj.get("name").asString
            val propType = propertyObj.get("type").asString
            val propStr = getProperty(propName, propType)
            props += propStr
        }

        props = props.substring(0, props.length-1).plus("\n")

        val packageLine = "package ${project.group}.api"
        val importLine = "import kotlinx.serialization.Serializable\nimport kotlinx.serialization.SerialName"
        val serializeLine = "@Serializable"
        val classLine = "data class $clsName($props)"

        return "$packageLine\n\n$importLine\n\n$serializeLine\n$classLine"
    }

    private fun getProperty(propName: String, propType: String): String {
        val type = when(propType.toLowerCase()) {
            "string" -> "String"
            "boolean" -> "Boolean"
            "int" -> "Int"
            "double" -> "Double"
            "float" -> "Float"
            else -> propType
        }

        val camelCasedProp = snakeToLowerCamelCase(propName)

        return "\n\t@SerialName(\"$propName\")\n\tval ${camelCasedProp}: $type,"
    }

    private fun getFile(fileName: String): File {
        val pathApi = "src/main/resources"
        val fileDir = File(pathApi)
        if (!fileDir.exists()) fileDir.mkdirs()

        return File("$pathApi/$fileName")
    }

    private fun createApiPostWithRequestFunction(apiName: String, endpoint: String): String {
        val template = getFile("fun_api_post_w_request_template.txt")

        return template.readText()
            .replace("%apiName%", snakeToUpperCamelCase(apiName))
            .replace("%endPoint%", endpoint)
    }

    private fun createApiPostWithoutRequestFunction(apiName: String, endpoint: String): String {
        val template = getFile("fun_api_post_wo_request_template.txt")

        return template.readText()
            .replace("%apiName%", snakeToUpperCamelCase(apiName))
            .replace("%endPoint%", endpoint)
    }

    private fun writeApiCollectionClass(functionDefs: List<FunctionDef>) {
        val path = "src/main/kotlin/${project.group}/api/RestApiCollection.kt"
        val template = getFile("class_api_collection_template.txt")

        var apiFunctions = ""

        for(def in functionDefs) {
            val apiFunction = when {
                def.method == "post" && def.withRequest -> {
                    createApiPostWithRequestFunction(def.apiName, def.endpoint)
                }
                def.method == "post" && def.withRequest.not() -> {
                    createApiPostWithoutRequestFunction(def.apiName, def.endpoint)
                }
                else -> ""
            }

            apiFunctions +=  apiFunction + "\n\t"
        }

        val code = template.readText()
            .replace("%packageName%", "${project.group}.api")
            .replace("%apiFunctions%",apiFunctions)

        val generatedCode = File(path)
        generatedCode.writeText(code)
    }

    private data class FunctionDef(val apiName: String, val endpoint: String, val method: String, val withRequest: Boolean)
}

val targetFile = objects.fileProperty()
val generatedFile = objects.fileProperty()

tasks.register<GenerateRestApiTask>("apiGenerator") {
    destination.set(targetFile)
}

tasks.register("generateApi") {
    dependsOn("apiGenerator")
    doLast {
        targetFile.get().asFile
    }
}

targetFile.set(layout.projectDirectory.file("src/main/resources/webservice.json"))