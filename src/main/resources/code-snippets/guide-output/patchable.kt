/**
 * Used by generated PATCH request code to mark fields as updated or unchanged.
 * Use Set(value) to update a field, or Unchanged to leave it as-is.
 */
sealed class Patchable<out T> {
    data class Set<out T>(val value: T) : Patchable<T>()
    data object Unchanged : Patchable<Nothing>()
}