package hep.dataforge.data

import hep.dataforge.meta.Meta
import hep.dataforge.names.Name
import hep.dataforge.names.startsWith
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlin.reflect.KClass

/**
 * Remove all values with keys starting with [name]
 */
internal fun MutableMap<Name, *>.removeWhatStartsWith(name: Name) {
    val toRemove = keys.filter { it.startsWith(name) }
    toRemove.forEach(::remove)
}

/**
 * An action that caches results on-demand and recalculates them on source push
 */
public abstract class CachingAction<in T : Any, out R : Any>(
    public val outputType: KClass<out R>,
) : Action<T, R> {

    protected abstract fun CoroutineScope.transform(
        set: DataSet<T>,
        meta: Meta,
        key: Name = Name.EMPTY,
    ): Flow<NamedData<R>>

    override suspend fun execute(
        dataSet: DataSet<T>,
        meta: Meta,
        scope: CoroutineScope?,
    ): DataSet<R> = DataTree.dynamic(outputType) {
        coroutineScope {
            collectFrom(transform(dataSet, meta))
        }
        scope?.let {
            dataSet.updates.collect {
                //clear old nodes
                remove(it)
                //collect new items
                collectFrom(scope.transform(dataSet, meta, it))
                //FIXME if the target is data, updates are fired twice
            }
        }
    }
}
