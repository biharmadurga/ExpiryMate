package com.expirymate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.expirymate.app.data.AppDatabase
import com.expirymate.app.data.ExpiryItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.launch

private val Emerald = Color(0xFF059669)
private val Amber = Color(0xFFD97706)
private val ExpiredRed = Color(0xFFDC2626)
private val Charcoal = Color(0xFF1F2933)
private val OffWhite = Color(0xFFFAFAF7)

private const val DAY = 86_400_000L
private val CATEGORIES = listOf("Food", "Document", "Warranty", "Vehicle", "Other")

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

fun todayUtcMillis(): Long {
    val local = Calendar.getInstance()
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    utc.clear()
    utc.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    return utc.timeInMillis
}

fun formatDate(millis: Long): String {
    val f = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    f.timeZone = TimeZone.getTimeZone("UTC")
    return f.format(Date(millis))
}

data class NavItem(val label: Int, val icon: ImageVector)

@Composable
fun AppRoot() {
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).expiryDao() }
    val itemsFlow = remember { dao.getAll() }
    val list by itemsFlow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    val navItems = listOf(
        NavItem(R.string.nav_home, Icons.Filled.Home),
        NavItem(R.string.nav_locker, Icons.Filled.Lock),
        NavItem(R.string.nav_add, Icons.Filled.Add),
        NavItem(R.string.nav_calendar, Icons.Filled.DateRange),
        NavItem(R.string.nav_settings, Icons.Filled.Settings)
    )
    var selected by remember { mutableIntStateOf(0) }
    var showAddMenu by remember { mutableStateOf(false) }
    var showForm by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                navItems.forEachIndexed { index, item ->
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
                DashboardScreen(
                    list = list,
                    onAdd = { showForm = true },
                    onDelete = { item -> scope.launch { dao.delete(item) } }
                )
            } else {
                PlaceholderScreen(stringResource(navItems[selected].label))
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
                    TextButton(onClick = { showAddMenu = false; showForm = true }) { Text(stringResource(R.string.action_add_expiry)) }
                    TextButton(onClick = { showAddMenu = false }) { Text(stringResource(R.string.action_scan)) }
                    TextButton(onClick = { showAddMenu = false }) { Text(stringResource(R.string.action_reminder)) }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddMenu = false }) { Text(stringResource(R.string.close)) }
            }
        )
    }

    if (showForm) {
        AddItemDialog(
            onDismiss = { showForm = false },
            onSave = { name, category, millis ->
                scope.launch {
                    dao.insert(ExpiryItem(name = name, category = category, expiryMillis = millis))
                }
                showForm = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemDialog(onDismiss: () -> Unit, onSave: (String, String, Long) -> Unit) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(CATEGORIES[0]) }
    var dateMillis by remember { mutableStateOf<Long?>(null) }
    var showPicker by remember { mutableStateOf(false) }
    val pickerState = rememberDatePickerState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.action_add_expiry)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item name") },
                    singleLine = true
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CATEGORIES.forEach { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = { category = c },
                            label = { Text(c) }
                        )
                    }
                }
                OutlinedButton(onClick = { showPicker = true }) {
                    Text(dateMillis?.let { formatDate(it) } ?: "Select expiry date")
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && dateMillis != null,
                onClick = { dateMillis?.let { onSave(name.trim(), category, it) } }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateMillis = pickerState.selectedDateMillis
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
fun DashboardScreen(
    list: List<ExpiryItem>,
    onAdd: () -> Unit,
    onDelete: (ExpiryItem) -> Unit
) {
    val today = todayUtcMillis()
    val limit = today + 30 * DAY
    val soonItems = list.filter { it.expiryMillis in today..limit }
    val expiredCount = list.count { it.expiryMillis < today }
    val warrantyCount = list.count { it.category == "Warranty" && it.expiryMillis >= today }

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
            Text(
                text = "Developed by Shubham Mishra",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), stringResource(R.string.stat_documents), "0", Emerald)
                StatCard(Modifier.weight(1f), stringResource(R.string.stat_tracked), list.size.toString(), Emerald)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), stringResource(R.string.stat_soon), soonItems.size.toString(), Amber)
                StatCard(Modifier.weight(1f), stringResource(R.string.stat_expired), expiredCount.toString(), ExpiredRed)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Modifier.weight(1f), stringResource(R.string.stat_warranties), warrantyCount.toString(), Emerald)
                StatCard(Modifier.weight(1f), stringResource(R.string.stat_reminders), "0", Emerald)
            }
        }
        item {
            SectionCard(
                title = stringResource(R.string.section_expiring_soon),
                emptyText = stringResource(R.string.empty_expiring),
                rows = soonItems,
                today = today,
                onDelete = onDelete
            )
        }
        item {
            SectionCard(
                title = stringResource(R.string.section_today),
                emptyText = stringResource(R.string.empty_today)
            )
        }
        item {
            SectionCard(
                title = "All Items",
                emptyText = "No items yet. Tap Add Expiry Item to create one.",
                rows = list,
                today = today,
                onDelete = onDelete
            )
        }
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
                FilledTonalButton(onClick = onAdd, modifier = Modifier.weight(1f)) {
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
fun SectionCard(
    title: String,
    emptyText: String,
    rows: List<ExpiryItem> = emptyList(),
    today: Long = 0L,
    onDelete: (ExpiryItem) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            if (rows.isEmpty()) {
                Text(
                    text = emptyText,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } else {
                rows.forEach { item ->
                    ItemRow(item = item, today = today, onDelete = { onDelete(item) })
                }
            }
        }
    }
}

@Composable
fun ItemRow(item: ExpiryItem, today: Long, onDelete: () -> Unit) {
    val days = ((item.expiryMillis - today) / DAY).toInt()
    val status = when {
        days < 0 -> "Expired ${-days} days ago"
        days == 0 -> "Expires today"
        else -> "$days days left"
    }
    val statusColor = when {
        days < 0 -> ExpiredRed
        days <= 30 -> Amber
        else -> Emerald
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.name, fontWeight = FontWeight.SemiBold)
            Text(
                text = item.category + " • " + formatDate(item.expiryMillis),
                fontSize = 12.sp
            )
            Text(text = status, fontSize = 12.sp, color = statusColor)
        }
        TextButton(onClick = onDelete) { Text("Delete") }
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
