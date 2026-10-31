package dev.puzzlecollection.data

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

/** One consumer, latest wins: concurrent writers could land an older state last. */
class SaveWriter(scope: CoroutineScope, private val file: SaveFile, dispatcher: CoroutineDispatcher) {
    private sealed interface Command {
        data class Write(val text: String) : Command
        data object Delete : Command
    }

    private val commands = Channel<Command>(Channel.CONFLATED)

    init {
        scope.launch(dispatcher) {
            for (command in commands) {
                when (command) {
                    is Command.Write -> file.write(command.text)
                    Command.Delete -> file.delete()
                }
            }
        }
    }

    fun write(text: String) {
        commands.trySend(Command.Write(text))
    }

    fun delete() {
        commands.trySend(Command.Delete)
    }
}
