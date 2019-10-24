package jcinterpret.core.descriptors

import jcinterpret.signature.Signature

class UnresolvableDescriptorException(val sig: Signature): Exception()
