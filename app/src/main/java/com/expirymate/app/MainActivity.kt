package com.expirymate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Emerald = Color(0xFF059669)
private val Amber = Color(0xFFD97706)
private val ExpiredRed = Color(0xFFDC2626)
private val Charcoal = Color(0xFF1F2933)
private val OffWhite = Color(0xFFFAFAF7)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ExpiryMateTheme {
                AppRoot()
            }
        }
    }
}

@Composable
fun ExpiryMateTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) {
        darkColorScheme(primary = Color(0xFF34D399))
    } else {
        lightColorScheme(
            primary = Emerald,
            background = OffWhite,
            surface = Color.White,
            onBackground = Charcoal,
            onSurface = Charcoal
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}

data class NavItem(val label: Int, val icon: ImageVector)

@Composable
fun AppRoot() {
    val items = listOf(
        NavItem(R.string.nav_home, Icons.Filled.Home),
        NavItem(R.string.nav_locker, Icons.Filled.Lock),
        NavItem(R.string.nav_add, Icons.Filled.Add),
        NavItem(R.string.nav_calendar, Icons.Filled.DateRange),
        NavItem(R.string.nav_settings, Icons.Filled.Settings)
    )
    var selected by remember { mutableIntStateOf(0) }
    var showAddMenu by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selected == index,
                        onClick = {
                            if (index == 2) showAddMenu = true else selected = index
                        },
                        icon = { Icon(item.icon, contentDescription = null) },
                        label = { Text(stringResource(item.label)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            if (selected == 0) {
                DashboardScreen()
            } else {
                PlaceholderScreen(stringResource(items[selected].label))
            }
        }
    }

    if (showAddMenu) {
        AlertDialog(
            onDismissRequest = { showAddMenu = false },
            title = { Text(stringResource(R.string.nav_add)) },
            text = {
                Column {
                    TextButton(onClick = { showAddMenu = false }) { Text(stringResource(R.string.action_add_document)) }
                    TextButton(onClick = { showAddMenu = false }) { Text(stringResource(R.string.action_add_expiry)) }
                    TextButton(onClick = { showAddMenu = false }) { Text(stringResource(R.string.action_scan)) }
                    TextButton(onClick = { showAddMenu = false }) { Text(stringResource(R.string.action_reminder)) }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddMenu = false }) { Text(stringResource(R.string.close)) }
            }
        )
    }
}

@Composable
fun DashboardScreen() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.tagline),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), stringResource(R.string.stat_documents), "0", Emerald)
                StatCard(Modifier.weight(1f), stringResource(R.string.stat_tracked), "0", Emerald)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), stringResource(R.string.stat_soon), "0", Amber)
                StatCard(Modifier.weight(1f), stringResource(R.string.stat_expired), "0", ExpiredRed)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), stringResource(R.string.stat_warranties), "0", Emerald)
                StatCard(Modifier.weight(1f), stringResource(R.string.stat_reminders), "0", Emerald)
            }
        }
        item { SectionCard(stringResource(R.string.section_expiring_soon), stringResource(R.string.empty_expiring)) }
        item { SectionCard(stringResource(R.string.section_today), stringResource(R.string.empty_today)) }
        item {
            Text(
                text = stringResource(R.string.section_quick),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilledTonalButton(onClick = { }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_add_document), fontSize = 12.sp)
                }
                FilledTonalButton(onClick = { }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_add_expiry), fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilledTonalButton(onClick = { }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_scan), fontSize = 12.sp)
                }
                FilledTonalButton(onClick = { }, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_reminder), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun StatCard(modifier: Modifier, label: String, value: String, accent: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = value, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = accent)
            Text(text = label, fontSize = 13.sp)
        }
    }
}

@Composable
fun SectionCard(title: String, emptyText: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                text = emptyText,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(text = stringResource(R.string.coming_soon), fontSize = 14.sp)
    }
}
