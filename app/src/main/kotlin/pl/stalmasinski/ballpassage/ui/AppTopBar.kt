package pl.stalmasinski.ballpassage.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable

/**
 * Top bar shared by all tabs. It stays pinned and changes its color once the content scrolls
 * underneath; pass [scrollBehavior] to the screen's Modifier.nestedScroll as well.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    scrollBehavior: TopAppBarScrollBehavior,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(title = { Text(title) }, actions = actions, scrollBehavior = scrollBehavior)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberAppTopBarScrollBehavior(): TopAppBarScrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
