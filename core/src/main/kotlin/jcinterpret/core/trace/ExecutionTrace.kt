package jcinterpret.core.trace

import jcinterpret.core.ctx.meta.ClassArea
import jcinterpret.core.ctx.meta.HeapArea
import jcinterpret.core.descriptors.DescriptorLibrary
import jcinterpret.core.source.SourceLibrary

class ExecutionTrace (
    val records: MutableList<TracerRecord>,
    val descriptorLibrary: DescriptorLibrary,
    val sourceLibrary: SourceLibrary,
    val heapArea: HeapArea,
    val classArea: ClassArea
)