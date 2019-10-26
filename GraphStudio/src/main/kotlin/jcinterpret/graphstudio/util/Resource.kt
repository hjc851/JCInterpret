package jcinterpret.graphstudio.util

import java.net.URL

object Resource {
    class Bundle internal constructor(klass: Class<Any>) {
        val loader: ClassLoader = klass.classLoader

        fun root(): URL {
            return loader.getResource(".")
        }

        fun named(name: String): URL {
            return loader.getResource(name)
        }
    }

    fun inBundle(klass: Class<Any>): Bundle {
        return Bundle(klass)
    }

    fun inDefaultBundle(): Bundle {
        return Bundle(this.javaClass)
    }
}