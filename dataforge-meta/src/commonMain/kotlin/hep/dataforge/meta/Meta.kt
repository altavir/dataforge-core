package hep.dataforge.meta

import hep.dataforge.meta.Meta.Companion.VALUE_KEY
import hep.dataforge.meta.MetaItem.NodeItem
import hep.dataforge.meta.MetaItem.ValueItem
import hep.dataforge.names.*
import hep.dataforge.values.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder


/**
 * A member of the meta tree. Could be represented as one of following:
 * * a [ValueItem] (leaf)
 * * a [NodeItem] (node)
 */
@Serializable
public sealed class MetaItem<out M : Meta> {

    abstract override fun equals(other: Any?): Boolean

    abstract override fun hashCode(): Int

    @Serializable
    public class ValueItem(public val value: Value) : MetaItem<Nothing>() {
        override fun toString(): String = value.toString()

        @OptIn(ExperimentalSerializationApi::class)
        @Serializer(ValueItem::class)
        public companion object : KSerializer<ValueItem> {
            override val descriptor: SerialDescriptor get() = ValueSerializer.descriptor

            override fun deserialize(decoder: Decoder): ValueItem = ValueItem(ValueSerializer.deserialize(decoder))

            override fun serialize(encoder: Encoder, value: ValueItem) {
                ValueSerializer.serialize(encoder, value.value)
            }
        }

        override fun equals(other: Any?): Boolean {
            return this.value == (other as? ValueItem)?.value
        }

        override fun hashCode(): Int {
            return value.hashCode()
        }
    }

    @Serializable
    public class NodeItem<M : Meta>(@Serializable(MetaSerializer::class) public val node: M) : MetaItem<M>() {
        //Fixing serializer for node could cause class cast problems, but it should not since Meta descendants are not serializeable
        override fun toString(): String = node.toString()

        @OptIn(ExperimentalSerializationApi::class)
        @Serializer(NodeItem::class)
        public companion object : KSerializer<NodeItem<out Meta>> {
            override val descriptor: SerialDescriptor get() = MetaSerializer.descriptor

            override fun deserialize(decoder: Decoder): NodeItem<*> = NodeItem(MetaSerializer.deserialize(decoder))

            override fun serialize(encoder: Encoder, value: NodeItem<*>) {
                MetaSerializer.serialize(encoder, value.node)
            }
        }

        override fun equals(other: Any?): Boolean = node == (other as? NodeItem<*>)?.node

        override fun hashCode(): Int = node.hashCode()
    }

    public companion object {
        public fun of(arg: Any?): MetaItem<*> {
            return when (arg) {
                null -> ValueItem(Null)
                is MetaItem<*> -> arg
                is Meta -> NodeItem(arg)
                else -> ValueItem(Value.of(arg))
            }
        }
    }
}

public fun Value.asMetaItem(): ValueItem = ValueItem(this)
public fun <M:Meta> M.asMetaItem(): NodeItem<M> = NodeItem(this)

/**
 * The object that could be represented as [Meta]. Meta provided by [toMeta] method should fully represent object state.
 * Meaning that two states with the same meta are equal.
 */
public interface MetaRepr {
    public fun toMeta(): Meta
}

public interface ItemProvider{
    public fun getItem(name: Name): MetaItem<*>?
}

/**
 * Generic meta tree representation. Elements are [MetaItem] objects that could be represented by three different entities:
 *  * [MetaItem.ValueItem] (leaf)
 *  * [MetaItem.NodeItem] single node
 *
 *   * Same name siblings are supported via elements with the same [Name] but different queries
 */
public interface Meta : MetaRepr, ItemProvider {
    /**
     * Top level items of meta tree
     */
    public val items: Map<NameToken, MetaItem<*>>

    override fun getItem(name: Name): MetaItem<*>? {
        if (name.isEmpty()) return NodeItem(this)
        return name.firstOrNull()?.let { token ->
            val tail = name.cutFirst()
            when (tail.length) {
                0 -> items[token]
                else -> items[token]?.node?.get(tail)
            }
        }
    }

    override fun toMeta(): Meta = seal()

    override fun equals(other: Any?): Boolean

    override fun hashCode(): Int

    override fun toString(): String

    public companion object {
        public const val TYPE: String = "meta"

        /**
         * A key for single value node
         */
        public const val VALUE_KEY: String = "@value"

        public val EMPTY: Meta = object: MetaBase() {
            override val items: Map<NameToken, MetaItem<*>> = emptyMap()
        }
    }
}

/* Get operations*/

/**
 * Perform recursive item search using given [name]. Each [NameToken] is treated as a name in [Meta.items] of a parent node.
 *
 * If [name] is empty return current [Meta] as a [NodeItem]
 */
public operator fun Meta?.get(name: Name): MetaItem<*>? = this?.getItem(name)

public operator fun Meta?.get(token: NameToken): MetaItem<*>? = this?.items?.get(token)

/**
 * Parse [Name] from [key] using full name notation and pass it to [Meta.get]
 */
public operator fun Meta?.get(key: String): MetaItem<*>? = get(key.toName())

/**
 * Get a sequence of [Name]-[Value] pairs
 */
public fun Meta.values(): Sequence<Pair<Name, Value>> {
    return items.asSequence().flatMap { (key, item) ->
        when (item) {
            is ValueItem -> sequenceOf(key.asName() to item.value)
            is NodeItem -> item.node.values().map { pair -> (key.asName() + pair.first) to pair.second }
        }
    }
}

/**
 * Get a sequence of all [Name]-[MetaItem] pairs for all items including nodes
 */
public fun Meta.sequence(): Sequence<Pair<Name, MetaItem<*>>> {
    return sequence {
        items.forEach { (key, item) ->
            yield(key.asName() to item)
            if (item is NodeItem<*>) {
                yieldAll(item.node.sequence().map { (innerKey, innerItem) ->
                    (key + innerKey) to innerItem
                })
            }
        }
    }
}

public operator fun Meta.iterator(): Iterator<Pair<Name, MetaItem<*>>> = sequence().iterator()

/**
 * A meta node that ensures that all of its descendants has at least the same type
 */
public interface MetaNode<out M : MetaNode<M>> : Meta {
    override val items: Map<NameToken, MetaItem<M>>
}

/**
 * The same as [Meta.get], but with specific node type
 */
public operator fun <M : MetaNode<M>> M?.get(name: Name): MetaItem<M>? = if( this == null) {
    null
} else {
    @Suppress("UNCHECKED_CAST", "ReplaceGetOrSet")
    (this as Meta).get(name) as MetaItem<M>? // Do not change
}

public operator fun <M : MetaNode<M>> M?.get(key: String): MetaItem<M>? = this[key.toName()]

public operator fun <M : MetaNode<M>> M?.get(key: NameToken): MetaItem<M>? = this[key.asName()]

/**
 * Equals, hashcode and to string for any meta
 */
public abstract class MetaBase : Meta {

    override fun equals(other: Any?): Boolean = if (other is Meta) {
        this.items == other.items
    } else {
        false
    }

    override fun hashCode(): Int = items.hashCode()

    override fun toString(): String = JSON_PRETTY.encodeToString(MetaSerializer, this)
}

/**
 * Equals and hash code implementation for meta node
 */
public abstract class AbstractMetaNode<M : MetaNode<M>> : MetaNode<M>, MetaBase()

/**
 * The meta implementation which is guaranteed to be immutable.
 *
 * If the argument is possibly mutable node, it is copied on creation
 */
public class SealedMeta internal constructor(
    override val items: Map<NameToken, MetaItem<SealedMeta>>
) : AbstractMetaNode<SealedMeta>()

/**
 * Generate sealed node from [this]. If it is already sealed return it as is
 */
public fun Meta.seal(): SealedMeta = this as? SealedMeta ?: SealedMeta(items.mapValues { entry -> entry.value.seal() })

@Suppress("UNCHECKED_CAST")
public fun MetaItem<*>.seal(): MetaItem<SealedMeta> = when (this) {
    is ValueItem -> this
    is NodeItem -> NodeItem(node.seal())
}

/**
 * Unsafe methods to access values and nodes directly from [MetaItem]
 */
public val MetaItem<*>?.value: Value?
    get() = (this as? ValueItem)?.value
        ?: (this?.node?.get(VALUE_KEY) as? ValueItem)?.value

public val MetaItem<*>?.string: String? get() = value?.string
public val MetaItem<*>?.boolean: Boolean? get() = value?.boolean
public val MetaItem<*>?.number: Number? get() = value?.number
public val MetaItem<*>?.double: Double? get() = number?.toDouble()
public val MetaItem<*>?.float: Float? get() = number?.toFloat()
public val MetaItem<*>?.int: Int? get() = number?.toInt()
public val MetaItem<*>?.long: Long? get() = number?.toLong()
public val MetaItem<*>?.short: Short? get() = number?.toShort()

public inline fun <reified E : Enum<E>> MetaItem<*>?.enum(): E? = if (this is ValueItem && this.value is EnumValue<*>) {
    this.value.value as E
} else {
    string?.let { enumValueOf<E>(it) }
}

public val MetaItem<*>.stringList: List<String>? get() = value?.list?.map { it.string }

public val <M : Meta> MetaItem<M>?.node: M?
    get() = when (this) {
        null -> null
        is ValueItem -> null//error("Trying to interpret value meta item as node item")
        is NodeItem -> node
    }

public fun Meta.isEmpty(): Boolean = this === Meta.EMPTY || this.items.isEmpty()