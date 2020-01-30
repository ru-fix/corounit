package ru.fix.corounit.engine

import org.junit.platform.engine.TestDescriptor
import org.junit.platform.engine.UniqueId
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor
import org.junit.platform.engine.support.descriptor.ClassSource
import org.junit.platform.engine.support.descriptor.MethodSource
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.jvm.jvmName

class CorounitExecutionDescriptor(
        uniqueId: UniqueId
) : AbstractTestDescriptor(uniqueId, "Corounit") {
    override fun getType(): TestDescriptor.Type = TestDescriptor.Type.CONTAINER

    val classDescriptors: List<CorounitClassDescriptior>
        get() = children.map { it as CorounitClassDescriptior }

    fun addClassDescriptor(classDescr: CorounitClassDescriptior) = children.add(classDescr)
}

class CorounitClassDescriptior(
        parentUniqueId: UniqueId,
        val clazz: KClass<*>) : AbstractTestDescriptor(
        parentUniqueId.append("class", clazz.qualifiedName ?: clazz.jvmName),
        clazz.qualifiedName ?: clazz.jvmName,
        ClassSource.from(clazz.java)) {

    override fun getType(): TestDescriptor.Type = TestDescriptor.Type.CONTAINER

    val methodDescriptors: List<CorounitMethodDescriptior>
        get() = children.map { it as CorounitMethodDescriptior }

    fun addMethodDescriptor(methodDescr: CorounitMethodDescriptior) = children.add(methodDescr)
}


class CorounitMethodDescriptior(
        parentUniqueId: UniqueId,
        val method: KFunction<*>
) : AbstractTestDescriptor(
        parentUniqueId.append("method", method.name),
        method.name,
        MethodSource.from(method.javaMethod)) {

    override fun getType(): TestDescriptor.Type = TestDescriptor.Type.TEST
}

