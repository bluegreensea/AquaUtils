package bluesea.aquautils.brigadier

import bluesea.aquautils.arguments.PlayersArgumentType
import bluesea.aquautils.util.ReflectionUtil.reflectionMapRemove
import com.mojang.brigadier.arguments.ArgumentType
import com.velocitypowered.api.network.ProtocolVersion
import com.velocitypowered.proxy.protocol.packet.brigadier.ArgumentIdentifier
import com.velocitypowered.proxy.protocol.packet.brigadier.ArgumentPropertyRegistry
import com.velocitypowered.proxy.protocol.packet.brigadier.ArgumentPropertySerializer
import org.slf4j.Logger

@Suppress("MemberVisibilityCanBePrivate")
object VelocityArgumentTypeRegistry {
    private val argumentPropertyRegistryClass = ArgumentPropertyRegistry::class.java

    val playersArgumentTypeClass = PlayersArgumentType::class.java

    fun replace(logger: Logger) {
        registerReplaceEmpty(
            ArgumentIdentifier.id("minecraft:entity", ArgumentIdentifier.mapSet(ProtocolVersion.MINECRAFT_1_19, 6)),
            playersArgumentTypeClass,
            PlayersArgumentPropertySerializer.PLAYERS,
            logger
        )
    }

    fun <T : ArgumentType<*>> registerReplaceEmpty(identifier: ArgumentIdentifier, klazz: Class<T>, serializer: ArgumentPropertySerializer<T>, logger: Logger) {
        unregisterEmpty(identifier.identifier)
        register(identifier, klazz, serializer)
        logger.warn("The argument type {} has been replaced by {}", identifier.identifier, playersArgumentTypeClass)
    }

    fun restore(logger: Logger) {
        val byteArgumentPropertySerializerClass = Class.forName("com.velocitypowered.proxy.protocol.packet.brigadier.ByteArgumentPropertySerializer")
        val byteField = byteArgumentPropertySerializerClass.getDeclaredField("BYTE")
        byteField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        unregisterRestoreEmpty(
            ArgumentIdentifier.id("minecraft:entity", ArgumentIdentifier.mapSet(ProtocolVersion.MINECRAFT_1_19, 6)),
            byteField.get(null) as ArgumentPropertySerializer<Byte>,
            logger
        )
    }

    fun <T> unregisterRestoreEmpty(identifier: ArgumentIdentifier, serializer: ArgumentPropertySerializer<T>, logger: Logger) {
        unregister(identifier, playersArgumentTypeClass)
        empty(identifier, serializer)
        logger.info("The argument type {} has been restored", identifier.identifier)
    }

    fun <T : ArgumentType<*>> register(identifier: ArgumentIdentifier, klazz: Class<T>, serializer: ArgumentPropertySerializer<T>) {
        val registerMethod = argumentPropertyRegistryClass.getDeclaredMethod(
            "register",
            ArgumentIdentifier::class.java,
            Class::class.java,
            ArgumentPropertySerializer::class.java
        )
        registerMethod.isAccessible = true
        registerMethod.invoke(null, identifier, klazz, serializer)
    }

    fun unregisterEmpty(identifier: String) {
        val byIdentifierField = argumentPropertyRegistryClass.getDeclaredField("byIdentifier")
        byIdentifierField.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val byIdentifier = byIdentifierField.get(null) as Map<ArgumentIdentifier, ArgumentPropertySerializer<*>>

        reflectionMapRemove(byIdentifierField, byIdentifier, { it.identifier }, identifier)
    }

    fun <T> empty(identifier: ArgumentIdentifier, serializer: ArgumentPropertySerializer<T>) {
        val registerMethod = argumentPropertyRegistryClass.getDeclaredMethod(
            "empty",
            ArgumentIdentifier::class.java,
            ArgumentPropertySerializer::class.java
        )
        registerMethod.isAccessible = true
        registerMethod.invoke(null, identifier, serializer)
    }

    fun <T : ArgumentType<*>> unregister(identifier: ArgumentIdentifier, klass: Class<T>) {
        val byIdentifierField = argumentPropertyRegistryClass.getDeclaredField("byIdentifier")
        byIdentifierField.isAccessible = true
        val byClassField = argumentPropertyRegistryClass.getDeclaredField("byClass")
        byClassField.isAccessible = true
        val classToIdField = argumentPropertyRegistryClass.getDeclaredField("classToId")
        classToIdField.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val byIdentifier = byIdentifierField.get(null) as Map<ArgumentIdentifier, ArgumentPropertySerializer<*>>

        @Suppress("UNCHECKED_CAST")
        val byClass = byClassField.get(null) as Map<Class<T>, ArgumentPropertySerializer<*>>

        @Suppress("UNCHECKED_CAST")
        val classToId = classToIdField.get(null) as Map<Class<T>, ArgumentIdentifier>

        reflectionMapRemove(byIdentifierField, byIdentifier, { it.identifier }, identifier.identifier)
        reflectionMapRemove(byClassField, byClass, { it.name }, klass.name)
        reflectionMapRemove(classToIdField, classToId, { it.name }, klass.name)
    }
}
