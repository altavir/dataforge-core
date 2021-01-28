package hep.dataforge.actions

import hep.dataforge.data.Data
import hep.dataforge.data.StaticData
import hep.dataforge.meta.isEmpty
import hep.dataforge.misc.Named
import hep.dataforge.names.Name

public interface NamedData<out T : Any> : Named, Data<T> {
    override val name: Name
    public val data: Data<T>
}

private class NamedDataImpl<out T : Any>(
    override val name: Name,
    override val data: Data<T>,
) : Data<T> by data, NamedData<T> {
    override fun toString(): String = buildString {
        append("NamedData(name=\"$name\"")
        if (data is StaticData) {
            append(", value=${data.value}")
        }
        if (!data.meta.isEmpty()) {
            append(", meta=${data.meta}")
        }
        append(")")
    }
}

public fun <T : Any> Data<T>.named(name: Name): NamedData<T> = if (this is NamedData) {
    NamedDataImpl(name, this.data)
} else {
    NamedDataImpl(name, this)
}