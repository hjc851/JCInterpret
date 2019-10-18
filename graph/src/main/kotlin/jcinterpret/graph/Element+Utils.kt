package jcinterpret.graph

import org.graphstream.graph.Element

fun Element.copyAttributesFrom(other: Element): Element {
    for (key in other.attributeKeySet) {
        this.setAttribute(key, other.getAttribute(key))
    }

    return this
}