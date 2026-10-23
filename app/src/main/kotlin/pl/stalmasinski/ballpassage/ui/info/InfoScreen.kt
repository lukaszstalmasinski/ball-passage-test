package pl.stalmasinski.ballpassage.ui.info

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import pl.stalmasinski.ballpassage.BuildConfig
import pl.stalmasinski.ballpassage.R
import pl.stalmasinski.ballpassage.ui.AppTopBar
import pl.stalmasinski.ballpassage.ui.rememberAppTopBarScrollBehavior

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreen() {
    val scrollBehavior = rememberAppTopBarScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = { AppTopBar(title = stringResource(R.string.info_title), scrollBehavior = scrollBehavior) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Section(R.string.info_section_standard, R.string.info_description, R.string.info_extrapolation)
                Section(R.string.info_section_method, R.string.info_method)
                Section(R.string.info_section_disclaimer, R.string.info_disclaimer)
                Section(R.string.info_section_privacy, R.string.info_privacy)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle(R.string.info_section_version)
                    Text(
                        text = stringResource(R.string.info_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun Section(@StringRes title: Int, @StringRes vararg paragraphs: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(title)
        paragraphs.forEach { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
private fun SectionTitle(@StringRes title: Int) {
    Text(
        text = stringResource(title),
        modifier = Modifier.semantics { heading() },
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
    )
}
