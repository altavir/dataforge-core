package hep.dataforge.data

import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaRepr
import kotlinx.coroutines.CoroutineScope
import kotlin.reflect.KClass

/**
 * A data element characterized by its meta
 */
interface Data<out T : Any> : MetaRepr {
    /**
     * Type marker for the data. The type is known before the calculation takes place so it could be checked.
     */
    val type: KClass<out T>
    /**
     * Meta for the data
     */
    val meta: Meta

    /**
     * Lazy data value
     */
    val goal: Goal<T>

    override fun toMeta(): Meta = meta

    companion object {
        const val TYPE = "data"

        fun <T : Any> of(type: KClass<out T>, goal: Goal<T>, meta: Meta): Data<T> = DataImpl(type, goal, meta)

        inline fun <reified T : Any> of(goal: Goal<T>, meta: Meta): Data<T> = of(T::class, goal, meta)

        fun <T : Any> of(name: String, type: KClass<out T>, goal: Goal<T>, meta: Meta): Data<T> =
            NamedData(name, of(type, goal, meta))

        inline fun <reified T : Any> of(name: String, goal: Goal<T>, meta: Meta): Data<T> =
            of(name, T::class, goal, meta)

        fun <T : Any> static(scope: CoroutineScope, value: T, meta: Meta): Data<T> =
            DataImpl(value::class, Goal.static(scope, value), meta)
    }
}

/**
 * Upcast a [Data] to a supertype
 */
inline fun <reified R : Any, reified T : R> Data<T>.cast(): Data<R> {
    return Data.of(R::class, goal, meta)
}

fun <R : Any, T : R> Data<T>.cast(type: KClass<R>): Data<R> {
    return Data.of(type, goal, meta)
}

suspend fun <T : Any> Data<T>.await(): T = goal.await()

/**
 * Generic Data implementation
 */
private class DataImpl<out T : Any>(
    override val type: KClass<out T>,
    override val goal: Goal<T>,
    override val meta: Meta
) : Data<T>

class NamedData<out T : Any>(val name: String, data: Data<T>) : Data<T> by data
