package jcinterpret.entry

import org.eclipse.jdt.core.dom.IMethodBinding

data class EntryPoint (
    val binding: IMethodBinding
)