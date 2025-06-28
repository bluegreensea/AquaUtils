package bluesea.aquautils.util

import java.lang.reflect.Field

object ReflectionUtil {
    fun <K, V> reflectionMapRemove(mapField: Field, map: Map<K, V>, keyProperty: (K) -> String, matchKey: String) {
        val mapClass = mapField.type
        val mapRemoveMethod = mapClass.getDeclaredMethod("remove", Object::class.java)
        var removed = false
        for (entry in map) {
            if (keyProperty.invoke(entry.key) == matchKey) {
                mapRemoveMethod.invoke(map, entry.key)
                removed = true
                break
            }
        }
        if (!removed) {
            Exception("Couldn't remove $matchKey in ${mapField.declaringClass.name}.${mapField.name}: not exists")
                .printStackTrace()
        }
    }
}
