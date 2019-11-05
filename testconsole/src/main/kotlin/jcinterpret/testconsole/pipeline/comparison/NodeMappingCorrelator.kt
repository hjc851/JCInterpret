package jcinterpret.testconsole.pipeline.comparison

import jcinterpret.graph.isData
import org.graphstream.graph.Node

object NodeMappingCorrelator {
    fun correlate(vararg mappings: List<Pair<Node, Node>>): Pair<Map<String, String>, Map<String, String>> {
        val lmap = mutableMapOf<String, String>()
        val rmap = mutableMapOf<String, String>()

        val lblacklist = mutableSetOf<String>()
        val rblacklist = mutableSetOf<String>()

        for (result in mappings) {
            for ((l, r) in result) {

                if (lblacklist.contains(l.id) || rblacklist.contains(r.id))
                    continue

                if (l.isData() && r.isData()) {
                    if (!lmap.containsKey(l.id) && !rmap.containsKey(r.id)) {
                        lmap[l.id] = r.id
                        rmap[r.id] = l.id
                    } else {
                        if (lmap.containsKey(l.id) && rmap.containsKey(r.id)) {
                            val lexistingmap = lmap[l.id]!!
                            val rexistingmap = rmap[r.id]!!

                            if (lexistingmap == r.id && rexistingmap == l.id) {
                                continue
                            } else {
                                lmap.remove(l.id)
                                rmap.remove(r.id)
                                lblacklist.add(l.id)
                                rblacklist.add(r.id)
                            }

                        } else if (lmap.containsKey(l.id)) {
                            val mapped = lmap[l.id]!!

                            if (mapped == r.id) {
                                continue
                            } else {
                                lmap.remove(l.id)
                                rmap.remove(r.id)
                                lblacklist.add(l.id)
                                rblacklist.add(r.id)
                            }

                        } else if (rmap.containsKey(r.id)) {
                            val mapped = rmap[r.id]!!

                            if (mapped == l.id) {
                                continue
                            } else {
                                lmap.remove(l.id)
                                rmap.remove(r.id)
                                lblacklist.add(l.id)
                                rblacklist.add(r.id)
                            }

                        } else {
                            TODO()
                        }
                    }
                }
            }
        }

        return lmap to rmap
    }
}