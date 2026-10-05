package com.ivy.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.google.testing.junit.testparameterinjector.TestParameter
import com.google.testing.junit.testparameterinjector.TestParameterInjector
import com.ivy.ui.PaparazziScreenshotTest
import com.ivy.ui.PaparazziTheme
import org.junit.Test
import org.junit.runner.RunWith

/** Snapshots of the shared Material 3 component kit in both themes. */
@RunWith(TestParameterInjector::class)
class IvyComponentsPaparazziTest(
    @TestParameter
    private val theme: PaparazziTheme,
) : PaparazziScreenshotTest() {

    @Test
    fun `screen title with actions`() {
        snapshot(theme) {
            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                IvyScreenTitle(
                    text = "Loans",
                    subtitle = "2 active",
                    actions = {
                        IvyCloseButton(onClick = {})
                    },
                )
            }
        }
    }

    @Test
    fun `top bar`() {
        snapshot(theme) {
            IvyTopBar(title = "Cloud Sync setup", onBack = {})
        }
    }

    @Test
    fun `search field empty and filled`() {
        snapshot(theme) {
            Column(modifier = Modifier.padding(16.dp)) {
                IvySearchField(value = TextFieldValue(""), onValueChange = {}, placeholder = "Search")
                IvySearchField(
                    value = TextFieldValue("Groceries"),
                    onValueChange = {},
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }

    @Test
    fun `list card`() {
        snapshot(theme) {
            Box(modifier = Modifier.padding(16.dp)) {
                IvyListCard(onClick = {}, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Cash", modifier = Modifier.padding(16.dp))
                }
            }
        }
    }

    @Test
    fun `bottom action bar`() {
        snapshot(theme) {
            Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                IvyBottomActionBar(onNavigateBack = {}) {
                    Button(onClick = {}, modifier = Modifier.weight(1f)) {
                        Text(text = "Add category")
                    }
                }
            }
        }
    }

    @Test
    fun `close buttons`() {
        snapshot(theme) {
            Row(modifier = Modifier.padding(16.dp)) {
                IvyCloseButton(onClick = {})
                IvyCloseButton(onClick = {}, tonal = true, modifier = Modifier.padding(start = 12.dp))
                BackButton(onClick = {}, modifier = Modifier.padding(start = 12.dp))
            }
        }
    }

    @Test
    fun `confirm dialog destructive`() {
        snapshot(theme) {
            Box(modifier = Modifier.fillMaxSize()) {
                IvyConfirmDialog(
                    visible = true,
                    title = "Remove this database?",
                    text = "Sync stops and the saved URL and token are forgotten.",
                    confirmText = "Remove",
                    destructive = true,
                    onConfirm = {},
                    onDismiss = {},
                )
            }
        }
    }
}
