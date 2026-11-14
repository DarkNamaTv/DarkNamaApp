package com.dark.darknama.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import com.dark.darknama.utils.DeviceUtils

/** Restore the last actionable control on a destination, not a non-clickable container. */
class TvFocusRegistry {
    private val targets = mutableMapOf<String, MutableMap<String, FocusRequester>>()
    private val initialKeys = mutableMapOf<String, String>()
    private val lastKeys = mutableMapOf<String, String>()

    fun register(entry: String, key: String, requester: FocusRequester, initial: Boolean) {
        targets.getOrPut(entry) { mutableMapOf() }[key] = requester
        if (initial) initialKeys[entry] = key
    }

    fun unregister(entry: String, key: String, requester: FocusRequester) {
        val registered = targets[entry] ?: return
        if (registered[key] === requester) registered.remove(key)
        if (registered.isEmpty()) targets.remove(entry)
    }

    fun rememberFocus(entry: String, key: String) { lastKeys[entry] = key }

    fun restore(entry: String?): Boolean {
        val route = entry ?: return false
        val available = targets[route] ?: return false
        for (key in preferredFocusKeys(lastKeys[route], initialKeys[route], available.keys)) {
            val requester = available[key] ?: continue
            try {
                requester.requestFocus()
                return true
            } catch (_: IllegalStateException) {
                // A lazy item may have been detached between registration and restoration.
            }
        }
        return false
    }
}

/** Last focused wins; if it has scrolled away, use the entry action before other controls. */
internal fun preferredFocusKeys(last: String?, initial: String?, available: Collection<String>): List<String> =
    (listOfNotNull(last, initial) + available).distinct().filter { it in available }

val LocalTvFocusRegistry = staticCompositionLocalOf<TvFocusRegistry?> { null }
val LocalTvFocusEntry = staticCompositionLocalOf<String?> { null }
/** The selected sidebar item, for explicit TV edge-to-rail focus order. */
val LocalSidebarFocusRequester = staticCompositionLocalOf<FocusRequester?> { null }

/** An actionable dialog control, focused after its separate window is attached on TV only. */
@Composable
fun Modifier.tvDialogInitialFocus(): Modifier {
    if (!DeviceUtils.isTv(LocalContext.current)) return this
    val requester = remember { FocusRequester() }
    LaunchedEffect(requester) {
        withFrameNanos { }
        try { requester.requestFocus() } catch (_: IllegalStateException) { }
    }
    return this.focusRequester(requester)
}

/** Applied *before* a Button, Card, or clickable modifier's focus target. No-op on phones. */
@Composable
fun Modifier.tvFocusItem(key: String, initial: Boolean = false): Modifier {
    val registry = LocalTvFocusRegistry.current ?: return this
    val entry = LocalTvFocusEntry.current ?: return this
    val requester = remember(entry, key) { FocusRequester() }
    DisposableEffect(registry, entry, key, requester, initial) {
        registry.register(entry, key, requester, initial)
        onDispose { registry.unregister(entry, key, requester) }
    }
    return this.focusRequester(requester).onFocusChanged {
        if (it.isFocused) registry.rememberFocus(entry, key)
    }
}
