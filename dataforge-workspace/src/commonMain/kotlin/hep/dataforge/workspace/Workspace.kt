package hep.dataforge.workspace

import hep.dataforge.context.ContextAware
import hep.dataforge.data.Data
import hep.dataforge.data.DataNode
import hep.dataforge.meta.Meta
import hep.dataforge.meta.MetaBuilder
import hep.dataforge.meta.buildMeta
import hep.dataforge.names.Name
import hep.dataforge.names.toName
import hep.dataforge.provider.Provider
import hep.dataforge.provider.Type


@Type(Workspace.TYPE)
interface Workspace : ContextAware, Provider {
    /**
     * The whole data node for current workspace
     */
    val data: DataNode<Any>

    /**
     * All targets associated with the workspace
     */
    val targets: Map<String, Meta>

    /**
     * All tasks associated with the workspace
     */
    val tasks: Map<String, Task<*>>

    override fun provideTop(target: String, name: Name): Any? {
        return when (target) {
            "target", Meta.TYPE -> targets[name.toString()]
            Task.TYPE -> tasks[name.toString()]
            Data.TYPE -> data[name]
            DataNode.TYPE -> data.getNode(name)
            else -> null
        }
    }

    override fun listTop(target: String): Sequence<Name> {
        return when (target) {
            "target", Meta.TYPE -> targets.keys.asSequence().map { it.toName() }
            Task.TYPE -> tasks.keys.asSequence().map { it.toName() }
            Data.TYPE -> data.data().map { it.first }
            DataNode.TYPE -> data.nodes().map { it.first }
            else -> emptySequence()
        }
    }


    /**
     * Invoke a task in the workspace utilizing caching if possible
     */
    fun <R : Any> run(task: Task<R>, config: Meta): DataNode<R> {
        context.activate(this)
        try {
            val model = task.build(this, config)
            task.validate(model)
            return task.run(this, model)
        } finally {
            context.deactivate(this)
        }
    }

//    /**
//     * Invoke a task in the workspace utilizing caching if possible
//     */
//    operator fun <R : Any> Task<R>.invoke(targetName: String): DataNode<R> {
//        val target = targets[targetName] ?: error("A target with name $targetName not found in ${this@Workspace}")
//        context.logger.info { "Running ${this.name} on $target" }
//        return invoke(target)
//    }

    companion object {
        const val TYPE = "workspace"
    }
}

fun Workspace.run(task: Task<*>, target: String): DataNode<Any> {
    val meta = targets[target] ?: error("A target with name $target not found in ${this}")
    return run(task, meta)
}


fun Workspace.run(task: String, target: String) =
    tasks[task]?.let { run(it, target) } ?: error("Task with name $task not found")

fun Workspace.run(task: String, meta: Meta) =
    tasks[task]?.let { run(it, meta) } ?: error("Task with name $task not found")

fun Workspace.run(task: String, block: MetaBuilder.() -> Unit = {}) =
    run(task, buildMeta(block))
