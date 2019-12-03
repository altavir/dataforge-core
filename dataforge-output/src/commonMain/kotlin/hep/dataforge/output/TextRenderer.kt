package hep.dataforge.output

import hep.dataforge.context.Context
import hep.dataforge.meta.Meta
import hep.dataforge.output.TextFormat.Companion.TEXT_RENDERER_TYPE
import hep.dataforge.provider.Type
import hep.dataforge.provider.top
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.io.Output
import kotlinx.io.text.writeUtf8String
import kotlin.reflect.KClass

class TextRenderer(override val context: Context, private val output: Output) : Renderer<Any> {
    private val cache = HashMap<KClass<*>, TextFormat>()

    /**
     * Find the first [TextFormat] matching the given object type.
     */
    override fun render(obj: Any, meta: Meta) {
        val format: TextFormat = if (obj is CharSequence) {
            DefaultTextFormat
        } else {
            val value = cache[obj::class]
            if (value == null) {
                val answer =
                    context.top<TextFormat>(TEXT_RENDERER_TYPE).values.firstOrNull { it.type.isInstance(obj) }
                if (answer != null) {
                    cache[obj::class] = answer
                    answer
                } else {
                    DefaultTextFormat
                }
            } else {
                value
            }
        }
        context.launch(Dispatchers.Output) {
            format.run { output.render(obj) }
        }
    }
}

/**
 * A text or binary renderer based on [Output]
 */
@Type(TEXT_RENDERER_TYPE)
interface TextFormat {
    /**
     * The priority of this renderer compared to other renderers
     */
    val priority: Int
    /**
     * The type of the content served by this renderer
     */
    val type: KClass<*>

    suspend fun Output.render(obj: Any)

    companion object {
        const val TEXT_RENDERER_TYPE = "dataforge.textRenderer"
    }
}

object DefaultTextFormat : TextFormat {
    override val priority: Int = Int.MAX_VALUE
    override val type: KClass<*> = Any::class

    override suspend fun Output.render(obj: Any) {
        writeUtf8String(obj.toString() + "\n")
    }
}