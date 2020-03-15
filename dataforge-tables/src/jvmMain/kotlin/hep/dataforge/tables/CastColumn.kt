package hep.dataforge.tables

import hep.dataforge.meta.Meta
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.full.cast
import kotlin.reflect.full.isSubclassOf

@Suppress("UNCHECKED_CAST")
fun <T : Any> Column<*>.cast(type: KClass<out T>): Column<T> {
    return if (type.isSubclassOf(this.type)) {
        this as Column<T>
    } else {
        CastColumn(this, type)
    }
}

class CastColumn<T : Any>(val origin: Column<*>, override val type: KClass<out T>) : Column<T> {
    override val name: String get() = origin.name
    override val meta: Meta get() = origin.meta
    override val size: Int get() = origin.size


    override fun get(index: Int): T? = type.cast(origin[index])
}

class ColumnProperty<C: Any, T : C>(val table: Table<C>, val type: KClass<T>) : ReadOnlyProperty<Any?, Column<T>> {
    override fun getValue(thisRef: Any?, property: KProperty<*>): Column<T> {
        val name = property.name
        return (table.columns[name] ?: error("Column with name $name not found in the table")).cast(type)
    }
}

operator fun <C: Any, T : C> Collection<Column<C>>.get(header: ColumnHeader<T>): Column<T>? =
    find { it.name == header.name }?.cast(header.type)