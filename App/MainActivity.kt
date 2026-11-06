package it.kodiauto.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Night = Color(0xFF10131B)
private val Panel = Color(0xFF1B2130)
private val Accent = Color(0xFF25B8D2)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { KodiAutoApp() }
    }
}

@Composable
fun KodiAutoApp() {
    var selected by remember { mutableStateOf("Home") }
    val sections = listOf("Home", "Musica", "Video", "Rete", "Impostazioni")
    MaterialTheme(colorScheme = darkColorScheme(
        background = Night, surface = Panel, primary = Accent,
        onBackground = Color.White, onSurface = Color.White
    )) {
        Row(Modifier.fillMaxSize().background(Night)) {
            Column(
                Modifier.width(112.dp).fillMaxHeight().background(Panel).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("KODI", color = Accent, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("AUTO", color = Color.LightGray, fontSize = 11.sp, letterSpacing = 4.sp)
                Spacer(Modifier.height(12.dp))
                sections.forEach { item ->
                    NavigationRailItem(
                        selected = selected == item,
                        onClick = { selected = item },
                        icon = {
                            Icon(
                                when(item) {
                                    "Home" -> Icons.Default.Home
                                    "Musica" -> Icons.Default.MusicNote
                                    "Video" -> Icons.Default.Movie
                                    "Rete" -> Icons.Default.Cloud
                                    else -> Icons.Default.Settings
                                }, contentDescription = item
                            )
                        },
                        label = { Text(item, fontSize = 10.sp) },
                        alwaysShowLabel = true
                    )
                }
            }
            Column(Modifier.fillMaxSize().padding(22.dp)) {
                Text("CENTRO MULTIMEDIALE", color = Accent, fontSize = 11.sp, letterSpacing = 2.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    when(selected) {
                        "Home" -> "Tutto il tuo intrattenimento."
                        "Musica" -> "La tua musica"
                        "Video" -> "Film e serie"
                        "Rete" -> "Sorgenti di rete"
                        else -> "Preferenze"
                    },
                    fontSize = 27.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(18.dp))
                when(selected) {
                    "Home" -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            FeatureCard("MUSICA", "Libreria audio", Icons.Default.MusicNote, Modifier.weight(1f)) { selected = "Musica" }
                            FeatureCard("VIDEO", "Libreria video", Icons.Default.Movie, Modifier.weight(1f)) { selected = "Video" }
                        }
                        Spacer(Modifier.height(14.dp))
                        FeatureCard("SORGENTI", "Telefono, USB e NAS", Icons.Default.FolderOpen, Modifier.fillMaxWidth()) { selected = "Rete" }
                        Spacer(Modifier.height(20.dp))
                        Text("PRONTO PER LA TUA LIBRERIA", color = Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Questa è la base grafica. Nel prossimo passaggio collegheremo selezione file, database e motore di riproduzione.", color = Color.LightGray)
                    }
                    "Musica" -> PlaceholderPage("Audio locale", "Selezione cartelle e riproduzione Media3 saranno implementate nel prossimo modulo.")
                    "Video" -> PlaceholderPage("Video", "Riproduzione sul telefono. L'eventuale visualizzazione sul display dell'auto dipende dal supporto ufficiale disponibile sul veicolo.")
                    "Rete" -> PlaceholderPage("Sorgenti", "Prossimi connettori: cartelle locali, USB, SMB/NAS e URL HTTP.")
                    else -> PlaceholderPage("Impostazioni", "Nome app, tema, librerie e preferenze di riproduzione.")
                }
            }
        }
    }
}

@Composable
private fun FeatureCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(modifier.clickable { onClick() }, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, null, tint = Accent, modifier = Modifier.size(30.dp))
            Text(title, color = Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PlaceholderPage(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, color = Accent, fontWeight = FontWeight.Bold)
            Text(body, color = Color.LightGray)
            Text("MODULO IN SVILUPPO", color = Color.Gray, fontSize = 10.sp, letterSpacing = 2.sp)
        }
    }
}
