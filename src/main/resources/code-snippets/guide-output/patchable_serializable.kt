/**
 * Used by generated serializable PATCH request code to mark fields as updated or unchanged.
 * Use Set(value) to update a field, or Unchanged to leave it as-is.
 */
@Serializable
sealed class SerializablePatchable<out T> {
    @Serializable
    data class Set<out T>(val value: T) : SerializablePatchable<T>()

    @Serializable
    data object Unchanged : SerializablePatchable<Nothing>()
}