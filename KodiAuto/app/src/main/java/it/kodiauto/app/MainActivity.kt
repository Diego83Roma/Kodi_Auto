package it.kodiauto.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Night = Color(0xFF090D14)
private val Rail = Color(0xFF101722)
private val SurfaceCard = Color(0xFF151F2D)
private val SurfaceRaised = Color(0xFF1B2939)
private val Gold = Color(0xFFD7AE62)
private val GoldLight = Color(0xFFF0D69B)
private val Muted = Color(0xFF9AA8B8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { KodiAutoApp() }
    }
}

@Composable
fun KodiAutoApp() {
    var selected by remember { mutableStateOf("Home") }
    val sections = listOf(
        Triple("Home", Icons.Default.Home, "Panoramica"),
        Triple("Musica", Icons.Default.MusicNote, "Libreria audio"),
        Triple("Video", Icons.Default.Movie, "Film e serie"),
        Triple("Rete", Icons.Default.Cloud, "Sorgenti"),
        Triple("Impostazioni", Icons.Default.Settings, "Preferenze")
    )
    MaterialTheme(colorScheme = darkColorScheme(
        background = Night, surface = SurfaceCard, primary = Gold,
        onBackground = Color.White, onSurface = Color.White
    )) {
        Row(Modifier.fillMaxSize().background(Night)) {
            Column(
                Modifier.width(94.dp).fillMaxHeight().background(Rail).padding(vertical = 22.dp, horizontal = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.size(54.dp).background(
                        Brush.linearGradient(listOf(GoldLight, Gold, Color(0xFF78501D))),
                        RoundedCornerShape(17.dp)
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = Night, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.height(10.dp))
                Text("KODI", color = GoldLight, fontSize = 15.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Text("AUTO", color = Muted, fontSize = 9.sp, letterSpacing = 3.sp)
                Spacer(Modifier.height(30.dp))
                sections.forEach { (name, icon, _) ->
                    val active = selected == name
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            .background(if (active) Color(0xFF293445) else Color.Transparent, RoundedCornerShape(15.dp))
                            .clickable { selected = name }.padding(vertical = 11.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(icon, contentDescription = name, tint = if (active) GoldLight else Muted, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.height(5.dp))
                        Text(name, color = if (active) Color.White else Muted, fontSize = 9.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
                    }
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.GraphicEq, contentDescription = "Media center", tint = Gold.copy(alpha = .8f), modifier = Modifier.size(22.dp))
                Text("MEDIA", color = Muted, fontSize = 8.sp, letterSpacing = 2.sp)
            }

            Column(Modifier.fillMaxSize().padding(horizontal = 26.dp, vertical = 22.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("KODI AUTO  /  MEDIA CENTER", color = Gold, fontSize = 10.sp, letterSpacing = 2.2.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        Text(
                            when (selected) {
                                "Home" -> "Il tuo mondo, in un solo posto."
                                "Musica" -> "La colonna sonora è tua."
                                "Video" -> "Cinema, sempre con te."
                                "Rete" -> "Tutte le tue sorgenti."
                                else -> "Personalizza la tua esperienza."
                            },
                            fontSize = 25.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold, color = Color.White
                        )
                    }
                    Surface(color = SurfaceCard, shape = RoundedCornerShape(50), border = BorderStroke(1.dp, Color.White.copy(alpha = .07f))) {
                        Row(Modifier.padding(horizontal = 13.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(7.dp).background(Color(0xFF63C995), RoundedCornerShape(50)))
                            Spacer(Modifier.width(8.dp))
                            Text("KODI ENGINE", color = Muted, fontSize = 9.sp, letterSpacing = 1.sp)
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))
                when (selected) {
                    "Home" -> HomePage(onMusic = { selected = "Musica" }, onVideo = { selected = "Video" }, onSources = { selected = "Rete" })
                    "Musica" -> LibraryPage(
                        icon = Icons.Default.MusicNote, eyebrow = "LA TUA RACCOLTA",
                        title = "Musica", description = "Sfoglia album e brani presenti nella libreria Kodi. Seleziona un album per visualizzarne le tracce.",
                        info = "La riproduzione viene inoltrata al motore Kodi tramite il servizio multimediale."
                    )
                    "Video" -> LibraryPage(
                        icon = Icons.Default.Movie, eyebrow = "LA TUA VIDEOTECA",
                        title = "Film e serie", description = "Accedi ai film indicizzati nella libreria Kodi e ai relativi dettagli.",
                        info = "La riproduzione video sul display dell'auto è soggetta ai limiti e alle regole di Android Auto."
                    )
                    "Rete" -> LibraryPage(
                        icon = Icons.Default.Cloud, eyebrow = "SORGENTI MULTIMEDIALI",
                        title = "La tua rete", description = "Organizza l'accesso ai contenuti provenienti dai dispositivi e dalle risorse di rete.",
                        info = "Sono previste cartelle locali, USB, condivisioni SMB/NAS e indirizzi HTTP."
                    )
                    else -> LibraryPage(
                        icon = Icons.Default.Tune, eyebrow = "CONFIGURAZIONE",
                        title = "Impostazioni", description = "Qui raccoglieremo le preferenze dell'app, le librerie e le opzioni di riproduzione.",
                        info = "Le impostazioni già presenti nel motore Kodi restano separate e non vengono sovrascritte."
                    )
                }
            }
        }
    }
}

@Composable
private fun HomePage(onMusic: () -> Unit, onVideo: () -> Unit, onSources: () -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(17.dp)) {
        item {
            Box(
                Modifier.fillMaxWidth().height(205.dp)
                    .background(
                        Brush.linearGradient(listOf(Color(0xFF39404A), Color(0xFF202A38), Color(0xFF101722))),
                        RoundedCornerShape(24.dp)
                    ).padding(24.dp)
            ) {
                Column(Modifier.align(Alignment.CenterStart).fillMaxWidth(.72f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("LA TUA ESPERIENZA MULTIMEDIALE", color = GoldLight, fontSize = 9.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                    Text("Tutto parte da qui.", color = Color.White, fontSize = 29.sp, lineHeight = 33.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Musica, video e contenuti di rete raccolti in un'unica interfaccia.", color = Color(0xFFD0D7E0), fontSize = 12.sp, lineHeight = 17.sp)
                }
                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Gold.copy(alpha = .18f), modifier = Modifier.size(118.dp).align(Alignment.CenterEnd))
                Text("KODI  •  AUTO", color = Gold, fontSize = 9.sp, letterSpacing = 2.sp, modifier = Modifier.align(Alignment.BottomEnd))
            }
        }
        item {
            Text("ESPLORA LA LIBRERIA", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                HomeTile("MUSICA", "Album e brani", Icons.Default.MusicNote, Modifier.weight(1f), onMusic)
                HomeTile("VIDEO", "Film e raccolta", Icons.Default.Movie, Modifier.weight(1f), onVideo)
            }
        }
        item {
            HomeTile("SORGENTI DI RETE", "Telefono, USB e NAS", Icons.Default.FolderOpen, Modifier.fillMaxWidth(), onSources, wide = true)
        }
        item {
            Surface(color = SurfaceCard, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = .05f))) {
                Row(Modifier.fillMaxWidth().padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Memory, contentDescription = null, tint = Gold, modifier = Modifier.size(25.dp))
                    Spacer(Modifier.width(13.dp))
                    Column {
                        Text("MOTORE KODI", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Libreria e controllo locale", color = Muted, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTile(title: String, subtitle: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit, wide: Boolean = false) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = SurfaceCard, shape = RoundedCornerShape(19.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = .06f))
    ) {
        Row(
            Modifier.fillMaxWidth().padding(if (wide) 18.dp else 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(43.dp).background(Color(0xFF2B3542), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = GoldLight, modifier = Modifier.size(23.dp))
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = GoldLight, fontSize = 9.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = "Apri", tint = Muted)
        }
    }
}

@Composable
private fun LibraryPage(icon: ImageVector, eyebrow: String, title: String, description: String, info: String) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Surface(color = SurfaceCard, shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = .06f))) {
            Column(Modifier.fillMaxWidth().padding(23.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Box(Modifier.size(54.dp).background(Color(0xFF2B3542), RoundedCornerShape(17.dp)), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = GoldLight, modifier = Modifier.size(29.dp))
                }
                Text(eyebrow, color = Gold, fontSize = 9.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
                Text(title, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(description, color = Color(0xFFBCC7D3), fontSize = 13.sp, lineHeight = 19.sp)
            }
        }
        Surface(color = SurfaceRaised, shape = RoundedCornerShape(16.dp)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Gold, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(11.dp))
                Text(info, color = Muted, fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
        Text("Le funzioni disponibili restano accessibili dalle sezioni dedicate.", color = Muted, fontSize = 10.sp)
    }
}
