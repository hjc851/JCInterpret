package jcinterpret.core.trace

import jcinterpret.core.ctx.meta.ClassArea
import jcinterpret.core.ctx.meta.HeapArea
import jcinterpret.core.descriptors.DescriptorLibrary
import jcinterpret.core.source.SourceLibrary
import java.io.Serializable

class ExecutionTrace (
    val records: MutableList<TracerRecord>,
    val heapArea: HeapArea
): Serializable