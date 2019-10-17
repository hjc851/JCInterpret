package jcinterpret.document

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KClass

object DocumentUtils {

    val jsonMapper = ObjectMapper().apply {
        registerModule(KotlinModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    fun <T: Any> read(path: Path, type: KClass<T>): T {
        return jsonMapper.readValue(path.toFile(), type.java)
    }

    fun <T: Any> write(path: Path, document: T) {
        jsonMapper.writeValue(path.toFile(), document)
    }

    fun <T: Serializable> readObject(path: Path, type: KClass<T>): T {
        val fin = Files.newInputStream(path)
        val oin = ObjectInputStream(fin)
        val obj = oin.readObject() as T
        oin.close()
        fin.close()
        return obj
    }

    fun <T: Serializable> writeObject(path: Path, document: T) {
        val fout = Files.newOutputStream(path)
        val oout = ObjectOutputStream(fout)
        oout.writeObject(document)
        oout.close()
        fout.close()
    }
}