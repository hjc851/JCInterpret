package jcinterpret.graphstudio.util

import javafx.fxml.FXMLLoader
import kotlin.reflect.KClass

object FXMLResource {
    fun <T : Any> forClass(type: Class<*>, init: T.() -> Unit = { }): FXMLLoader {
        val fileName = type.name
                .replace('.', '/')
                .plus(".fxml")

        val loader = FXMLLoader(Resource.inBundle(javaClass).named(fileName))
        loader.setControllerFactory {
            val instance = it.newInstance() as T
            init(instance)
            return@setControllerFactory instance
        }
        return loader
    }

    fun <T : Any> forClass(type: KClass<T>, init: T.() -> Unit = { }): FXMLLoader {
        return forClass(type.java, init)
    }
}