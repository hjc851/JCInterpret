package jcinterpret.document

import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KClass
import com.esotericsoftware.kryo.Kryo
import com.esotericsoftware.kryo.io.Input
import com.esotericsoftware.kryo.io.Output

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

    fun <T: Any> readObject(path: Path, type: KClass<T>): T {
        val fin = Files.newInputStream(path)
        val oin = ObjectInputStream(fin)
        val obj = oin.readObject() as T
        oin.close()
        fin.close()
        return obj
    }

    fun <T: Any> writeObject(path: Path, document: T) {
        val fout = Files.newOutputStream(path)
        val oout = ObjectOutputStream(fout)
        oout.writeObject(document)
        oout.close()
        fout.close()
    }

    val kryo = Kryo().apply {
        isRegistrationRequired = false
        references = true
    }

    fun <T> readKObject(path: Path, type: Class<T>): T {
        return Files.newInputStream(path).use { fin ->
            kryo.readObject(Input(fin), type)
        }
    }

    fun <T> writeKObject(path: Path, obj: T) {
        Files.newOutputStream(path).use { fout ->
            kryo.writeObject(Output(fout), obj)
        }
    }
}