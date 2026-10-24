package org.kmprtc.sample.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.CallMissed
import androidx.compose.material.icons.automirrored.rounded.CallReceived
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.kmprtc.sample.API_BASE_URL
import org.kmprtc.sample.AppViewModel
import org.kmprtc.sample.UiState
import org.kmprtc.sample.call.Registration
import org.kmprtc.sample.data.CallRecord
import org.kmprtc.sample.data.Contact
import org.kmprtc.sample.data.Room
import org.kmprtc.sample.ui.components.Avatar
import org.kmprtc.sample.ui.components.EmptyState
import org.kmprtc.sample.ui.components.RegistrationChip
import org.kmprtc.sample.ui.components.SectionLabel
import org.kmprtc.sample.ui.components.formatDuration
import org.kmprtc.sample.ui.theme.CallColors

private enum class Tab(val label: String, val icon: ImageVector, val selectedIcon: ImageVector) {
    Contacts("Contacts", Icons.Outlined.People, Icons.Rounded.People),
    Rooms("Salles", Icons.Outlined.Groups, Icons.Rounded.Groups),
    History("Historique", Icons.Outlined.History, Icons.Rounded.History),
    Profile("Profil", Icons.Outlined.Person, Icons.Rounded.Person),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(state: UiState, registration: Registration, vm: AppViewModel, snackbar: SnackbarHostState) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showCreateRoom by remember { mutableStateOf(false) }
    val current = Tab.entries[tab]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(current.label, fontWeight = FontWeight.SemiBold) },
                actions = {
                    RegistrationChip(registration, vm.engine.supportsCalls)
                    IconButton(onClick = { vm.refreshAll() }) { Icon(Icons.Rounded.Refresh, "Actualiser") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEachIndexed { i, t ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = { Icon(if (tab == i) t.selectedIcon else t.icon, null) },
                        label = { Text(t.label) },
                    )
                }
            }
        },
        floatingActionButton = {
            if (current == Tab.Rooms) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateRoom = true },
                    icon = { Icon(Icons.Rounded.Add, null) },
                    text = { Text("Nouvelle salle") },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.loading,
            onRefresh = { vm.refreshAll() },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            when (current) {
                Tab.Contacts -> ContactsTab(state, vm)
                Tab.Rooms -> RoomsTab(state, vm)
                Tab.History -> HistoryTab(state, vm)
                Tab.Profile -> ProfileTab(state, registration, vm)
            }
        }
    }

    if (showCreateRoom) {
        CreateRoomDialog(onDismiss = { showCreateRoom = false }) { name, pin, video ->
            vm.createRoom(name, pin, video)
            showCreateRoom = false
        }
    }
}

// ─── Contacts ─────────────────────────────────────────────────────────────────

@Composable
private fun ContactsTab(state: UiState, vm: AppViewModel) {
    var query by rememberSaveable { mutableStateOf("") }
    val list = state.contacts.filter {
        query.isBlank() || it.user.displayName.contains(query, true) || it.user.email.contains(query, true) ||
            it.sipUsername?.contains(query) == true
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Rechercher un nom, un email, un numero") },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                singleLine = true,
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        item { EchoTestCard(vm) }
        item { SectionLabel("Utilisateurs du serveur (${list.size})") }
        if (list.isEmpty()) {
            item {
                EmptyState(
                    Icons.Outlined.People,
                    "Personne a appeler pour l'instant",
                    "Installez l'application sur un 2e telephone avec le meme SERVER_HOST et creez un autre compte.",
                )
            }
        }
        items(list, key = { it.user.id }) { ContactRow(it, vm) }
    }
}

@Composable
private fun EchoTestCard(vm: AppViewModel) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.RecordVoiceOver, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Test d'echo (600)", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(
                    "Vous entendez et voyez votre propre retour",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                )
            }
            FilledTonalIconButton(onClick = { vm.echoTest(video = false) }) { Icon(Icons.Rounded.Call, "Echo audio") }
            FilledTonalIconButton(onClick = { vm.echoTest(video = true) }) { Icon(Icons.Rounded.Videocam, "Echo video") }
        }
    }
}

@Composable
private fun ContactRow(contact: Contact, vm: AppViewModel) {
    ListItem(
        leadingContent = { Avatar(contact.user.displayName) },
        headlineContent = { Text(contact.user.displayName, fontWeight = FontWeight.Medium) },
        supportingContent = {
            Text(
                contact.sipUsername?.let { "N° $it  ·  ${contact.user.email}" } ?: contact.user.email,
                maxLines = 1,
            )
        },
        trailingContent = {
            Row {
                IconButton(onClick = { vm.call(contact, video = false) }) {
                    Icon(Icons.Rounded.Call, "Appel audio", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { vm.call(contact, video = true) }) {
                    Icon(Icons.Rounded.Videocam, "Appel video", tint = MaterialTheme.colorScheme.primary)
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

// ─── Salles de conference ─────────────────────────────────────────────────────

@Composable
private fun RoomsTab(state: UiState, vm: AppViewModel) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 8.dp, 16.dp, 96.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (state.rooms.isEmpty()) {
            item {
                EmptyState(
                    Icons.Outlined.Groups,
                    "Aucune salle active",
                    "Creez une salle : tout le monde peut la rejoindre en audio ou en video avec son numero.",
                )
            }
        }
        items(state.rooms, key = { it.id }) { RoomCard(it, isOwner = it.ownerId == state.me?.id, vm) }
    }
}

@Composable
private fun RoomCard(room: Room, isOwner: Boolean, vm: AppViewModel) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.tertiaryContainer, modifier = Modifier.size(48.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Groups, null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    }
                }
                Spacer(Modifier.size(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(room.name, style = MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("N° ${room.roomNumber}", style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
                        if (room.hasPin) {
                            Spacer(Modifier.size(8.dp))
                            Icon(Icons.Outlined.Lock, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                room.pin?.let { " PIN $it" } ?: " PIN",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (isOwner) {
                    IconButton(onClick = { vm.deleteRoom(room) }) { Icon(Icons.Outlined.Delete, "Fermer la salle") }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { vm.joinRoom(room, video = false) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Call, null, Modifier.size(18.dp)); Spacer(Modifier.size(8.dp)); Text("Audio")
                }
                if (room.video) {
                    Button(onClick = { vm.joinRoom(room, video = true) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Videocam, null, Modifier.size(18.dp)); Spacer(Modifier.size(8.dp)); Text("Video")
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateRoomDialog(onDismiss: () -> Unit, onCreate: (String, String?, Boolean) -> Unit) {
    var name by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var video by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Groups, null) },
        title = { Text("Nouvelle salle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Nom de la salle") }, singleLine = true)
                OutlinedTextField(
                    pin, { v -> pin = v.filter { it.isDigit() }.take(8) },
                    label = { Text("PIN (optionnel)") },
                    supportingText = { Text("Demande a l'entree, saisi au clavier") },
                    singleLine = true,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Video autorisee", Modifier.weight(1f))
                    Switch(video, { video = it })
                }
            }
        },
        confirmButton = { Button(onClick = { onCreate(name, pin, video) }, enabled = name.isNotBlank()) { Text("Creer") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

// ─── Historique ───────────────────────────────────────────────────────────────

@Composable
private fun HistoryTab(state: UiState, vm: AppViewModel) {
    val mine = state.sip?.username
    LazyColumn(Modifier.fillMaxSize()) {
        if (state.history.isEmpty()) {
            item { EmptyState(Icons.Outlined.History, "Aucun appel", "Vos appels apparaissent ici des qu'ils sont termines.") }
        }
        items(state.history, key = { it.id }) { HistoryRow(it, mine, state.contacts, vm) }
    }
}

@Composable
private fun HistoryRow(r: CallRecord, mine: String?, contacts: List<Contact>, vm: AppViewModel) {
    val outgoing = r.src == mine
    val other = if (outgoing) r.dst else r.src
    val answered = r.disposition == "ANSWERED"
    val (icon, tint) = when {
        r.direction == "conference" -> Icons.Rounded.Groups to MaterialTheme.colorScheme.tertiary
        !answered -> Icons.AutoMirrored.Rounded.CallMissed to MaterialTheme.colorScheme.error
        outgoing -> Icons.AutoMirrored.Rounded.CallMade to CallColors.online
        else -> Icons.AutoMirrored.Rounded.CallReceived to MaterialTheme.colorScheme.primary
    }
    val name = vm.nameOf(other)
    ListItem(
        leadingContent = { Avatar(name, 40.dp) },
        headlineContent = { Text(name, fontWeight = FontWeight.Medium) },
        supportingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, Modifier.size(16.dp), tint = tint)
                Spacer(Modifier.size(6.dp))
                Text(r.calldate.take(16).replace('T', ' '))
            }
        },
        trailingContent = {
            Text(
                if (answered) formatDuration(r.billsec) else "Manque",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.clickable {
            contacts.firstOrNull { it.sipUsername == other }?.let { vm.call(it, video = false) }
        },
    )
}

// ─── Profil ───────────────────────────────────────────────────────────────────

@Composable
private fun ProfileTab(state: UiState, registration: Registration, vm: AppViewModel) {
    val me = state.me ?: return
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Avatar(me.displayName, 84.dp)
                Spacer(Modifier.height(12.dp))
                Text(me.displayName, style = MaterialTheme.typography.titleLarge)
                Text(me.email, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                RegistrationChip(registration, vm.engine.supportsCalls)
            }
        }
        SectionLabel("Compte SIP", Modifier.padding(start = 0.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            InfoRow("Mon numero", state.sip?.username ?: "...")
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            InfoRow("Domaine", state.sip?.domain ?: "...")
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            InfoRow("Transport", state.sip?.transport?.uppercase() ?: "...")
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            InfoRow("TURN", if (state.sip?.turnUsername != null) "Actif (24 h)" else "Non")
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            InfoRow("Serveur API", API_BASE_URL)
        }
        Spacer(Modifier.height(20.dp))
        FilledTonalButton(onClick = { vm.connectSip() }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Refresh, null); Spacer(Modifier.size(8.dp)); Text("Reconnecter le SIP")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { vm.logout() }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.AutoMirrored.Rounded.Logout, null); Spacer(Modifier.size(8.dp)); Text("Se deconnecter")
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium)
    }
}
