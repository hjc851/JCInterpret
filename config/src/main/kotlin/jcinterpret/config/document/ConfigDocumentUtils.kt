package jcinterpret.config.document

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.nio.file.Path

object ConfigDocumentUtils {
    val mapper = ObjectMapper().registerModule(KotlinModule())

    fun read(path: Path): ConfigDocument {
        return mapper.readValue(path.toFile(), ConfigDocument::class.java)
    }

    fun write(path: Path, document: ConfigDocument) {
        mapper.writeValue(path.toFile(), document)
    }
}