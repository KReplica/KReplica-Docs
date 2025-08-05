sealed class Patchable<out T> {
    data class Set<out T>(val value: T) : Patchable<T>()
    data object Unchanged : Patchable<Nothing>()
}