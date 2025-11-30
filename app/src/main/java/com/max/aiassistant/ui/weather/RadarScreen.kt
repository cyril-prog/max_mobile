package com.max.aiassistant.ui.weather

import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import com.max.aiassistant.ui.theme.DarkBackground

/**
 * Écran affichant le radar de précipitations RainViewer sur une carte interactive
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadarScreen(
    cityName: String,
    latitude: Double,
    longitude: Double,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Radar météo - $cityName",
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // WebView avec la carte RainViewer
            RadarWebView(
                latitude = latitude,
                longitude = longitude
            )
        }
    }
}

/**
 * WebView affichant la carte interactive avec RainViewer
 */
@Composable
private fun RadarWebView(
    latitude: Double,
    longitude: Double
) {
    Log.d("RadarScreen", "Création du WebView avec coordonnées: lat=$latitude, lon=$longitude")

    AndroidView(
        factory = { context ->
            WebView(context).apply {
                // Configuration des paramètres du WebView
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowContentAccess = true
                    allowFileAccess = true
                    // Permet le chargement de contenu mixte (HTTP/HTTPS)
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    // Active le cache pour de meilleures performances
                    cacheMode = WebSettings.LOAD_DEFAULT
                    databaseEnabled = true
                    // Active le zoom (optionnel)
                    builtInZoomControls = false
                    displayZoomControls = false
                }

                // Client pour intercepter les erreurs de chargement
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        Log.d("RadarScreen", "✅ Page chargée avec succès: $url")
                    }

                    @Deprecated("Deprecated in API level 23")
                    override fun onReceivedError(
                        view: WebView?,
                        errorCode: Int,
                        description: String?,
                        failingUrl: String?
                    ) {
                        super.onReceivedError(view, errorCode, description, failingUrl)
                        Log.e("RadarScreen", "❌ Erreur de chargement: $description (code: $errorCode, url: $failingUrl)")
                    }
                }

                // Client pour capturer les logs JavaScript
                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                        consoleMessage?.let {
                            val level = when (it.messageLevel()) {
                                ConsoleMessage.MessageLevel.ERROR -> "❌ ERROR"
                                ConsoleMessage.MessageLevel.WARNING -> "⚠️ WARNING"
                                else -> "ℹ️ INFO"
                            }
                            Log.d("RadarScreen-JS", "$level: ${it.message()} (ligne ${it.lineNumber()} de ${it.sourceId()})")
                        }
                        return true
                    }
                }

                val html = getRadarHtml(latitude, longitude)
                Log.d("RadarScreen", "📄 Chargement du HTML (${html.length} caractères)")
                Log.d("RadarScreen", "🌍 Coordonnées: lat=$latitude, lon=$longitude")

                // Utilise une base URL HTTPS pour éviter les restrictions de sécurité
                loadDataWithBaseURL(
                    "https://localhost/",
                    html,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

/**
 * Génère le HTML pour la carte avec RainViewer
 */
private fun getRadarHtml(latitude: Double, longitude: Double): String {
    return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Radar Météo</title>
    <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" crossorigin="" />
    <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js" crossorigin=""></script>
    <style>
        body, html {
            margin: 0;
            padding: 0;
            height: 100vh;
            width: 100vw;
            overflow: hidden;
            position: fixed;
        }
        #map {
            height: 100vh;
            width: 100vw;
            background-color: #1a1a1a;
            position: fixed;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
        }
        .radar-controls {
            position: absolute;
            bottom: 20px;
            left: 50%;
            transform: translateX(-50%);
            z-index: 1000;
            background: rgba(0, 0, 0, 0.7);
            padding: 15px 20px;
            border-radius: 12px;
            display: flex;
            align-items: center;
            gap: 15px;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.3);
        }
        .radar-controls button {
            background: #0A84FF;
            color: white;
            border: none;
            padding: 8px 16px;
            border-radius: 8px;
            cursor: pointer;
            font-size: 14px;
            font-weight: bold;
        }
        .radar-controls button:active {
            background: #0066CC;
        }
        .radar-controls input[type="range"] {
            width: 200px;
            cursor: pointer;
        }
        .radar-controls .time-display {
            color: white;
            font-size: 14px;
            font-weight: bold;
            min-width: 100px;
            text-align: center;
        }
        .legend {
            position: absolute;
            top: 80px;
            right: 10px;
            z-index: 1000;
            background: rgba(0, 0, 0, 0.7);
            padding: 10px;
            border-radius: 8px;
        }
        .legend-title {
            color: white;
            font-size: 12px;
            font-weight: bold;
            margin-bottom: 5px;
        }
        .legend-item {
            display: flex;
            align-items: center;
            margin: 3px 0;
        }
        .legend-color {
            width: 20px;
            height: 15px;
            margin-right: 5px;
            border-radius: 3px;
        }
        .legend-label {
            color: white;
            font-size: 11px;
        }
    </style>
</head>
<body>
    <div id="map"></div>

    <div class="legend">
        <div class="legend-title">Précipitations</div>
        <div class="legend-item">
            <div class="legend-color" style="background: #8080ff;"></div>
            <div class="legend-label">Faible</div>
        </div>
        <div class="legend-item">
            <div class="legend-color" style="background: #00ff00;"></div>
            <div class="legend-label">Modérée</div>
        </div>
        <div class="legend-item">
            <div class="legend-color" style="background: #ffff00;"></div>
            <div class="legend-label">Forte</div>
        </div>
        <div class="legend-item">
            <div class="legend-color" style="background: #ff0000;"></div>
            <div class="legend-label">Très forte</div>
        </div>
    </div>

    <div class="radar-controls">
        <button onclick="playAnimation()">▶ Lecture</button>
        <button onclick="stopAnimation()">⏸ Pause</button>
        <input type="range" id="timeSlider" min="0" max="0" value="0" oninput="updateRadarFrame(this.value)">
        <div class="time-display" id="timeDisplay">Maintenant</div>
    </div>

    <script>
        console.log('🚀 Script démarré!');
        console.log('📍 Coordonnées: $latitude, $longitude');

        // Attendre que Leaflet soit chargé
        window.addEventListener('load', function() {
            console.log('✅ Page chargée, Leaflet disponible:', typeof L !== 'undefined');
            console.log('🖥️ Window inner size:', window.innerWidth, 'x', window.innerHeight);
            console.log('📱 Viewport size:', window.visualViewport ? window.visualViewport.width + 'x' + window.visualViewport.height : 'N/A');

            try {
                // Vérifier la taille du conteneur
                const mapDiv = document.getElementById('map');
                console.log('📏 Taille du conteneur AVANT fix:', mapDiv.offsetWidth, 'x', mapDiv.offsetHeight);

                // FORCER une hauteur en pixels si elle est à 0
                if (mapDiv.offsetHeight === 0) {
                    console.log('⚠️ Hauteur à 0, forçage à 800px');
                    mapDiv.style.height = '800px';
                    mapDiv.style.minHeight = '800px';
                }

                console.log('📏 Taille du conteneur APRÈS fix:', mapDiv.offsetWidth, 'x', mapDiv.offsetHeight);

                // Initialisation de la carte centrée sur les coordonnées
                console.log('📍 Création de la carte...');
                const map = L.map('map').setView([$latitude, $longitude], 9);
                console.log('✅ Carte créée');

                // Forcer la mise à jour de la taille de la carte
                setTimeout(() => {
                    console.log('🔄 Invalidation de la taille de la carte...');
                    map.invalidateSize();
                    console.log('✅ Taille invalidée');
                }, 100);

                // Couche de carte de base (OpenStreetMap)
                const osmUrl = 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png';
                console.log('🗺️ Ajout de la couche OpenStreetMap...');
                console.log('🔗 URL OSM (template):', osmUrl);
                console.log('🔗 URL OSM (exemple):', 'https://a.tile.openstreetmap.org/9/256/172.png');
                L.tileLayer(osmUrl, {
                    attribution: '© OpenStreetMap contributors',
                    maxZoom: 18
                }).addTo(map);
                console.log('✅ Couche OSM ajoutée');

                // Marqueur pour la position
                console.log('📌 Ajout du marqueur...');
                L.marker([$latitude, $longitude])
                    .addTo(map)
                    .bindPopup('Position actuelle')
                    .openPopup();
                console.log('✅ Marqueur ajouté');

                // Variables pour RainViewer
                let radarLayers = [];
                let animationPosition = 0;
                let animationTimer = null;
                let timestamps = [];

                // Charger les données RainViewer
                async function loadRainViewer() {
                    try {
                        const rainviewerApiUrl = 'https://api.rainviewer.com/public/weather-maps.json';
                        console.log('🌧️ Chargement RainViewer...');
                        console.log('🔗 URL RainViewer API:', rainviewerApiUrl);
                        const response = await fetch(rainviewerApiUrl);
                        console.log('📡 Réponse RainViewer status:', response.status, response.statusText);
                        const data = await response.json();
                        console.log('✅ Données RainViewer chargées');
                        console.log('📊 Nombre de frames passées:', data.radar.past.length);
                        console.log('📊 Nombre de frames futures:', data.radar.nowcast.length);
                        if (data.radar.past.length > 0) {
                            console.log('🔗 URL exemple tuile radar:', 'https://tilecache.rainviewer.com' + data.radar.past[0].path + '/256/9/256/172/2/1_1.png');
                        }

                // Récupérer les timestamps (passé et futur)
                timestamps = data.radar.past.concat(data.radar.nowcast);

                // Mettre à jour le slider
                const slider = document.getElementById('timeSlider');
                slider.max = timestamps.length - 1;
                slider.value = data.radar.past.length - 1; // Position actuelle
                animationPosition = data.radar.past.length - 1;

                // Créer les couches radar pour chaque timestamp
                timestamps.forEach((timestamp, index) => {
                    const tileUrl = 'https://tilecache.rainviewer.com' + timestamp.path + '/256/{z}/{x}/{y}/2/1_1.png';
                    const layer = L.tileLayer(tileUrl, {
                        opacity: 0.6,
                        zIndex: 10
                    });
                    radarLayers.push(layer);
                });

                // Afficher la frame actuelle
                updateRadarFrame(animationPosition);

                    } catch (error) {
                        console.error('❌ Erreur lors du chargement de RainViewer:', error);
                    }
                }

                // Mettre à jour la frame du radar
                function updateRadarFrame(position) {
            position = parseInt(position);
            animationPosition = position;

            // Cacher toutes les couches
            radarLayers.forEach(layer => {
                if (map.hasLayer(layer)) {
                    map.removeLayer(layer);
                }
            });

            // Afficher la couche sélectionnée
            if (radarLayers[position]) {
                radarLayers[position].addTo(map);
            }

            // Mettre à jour l'affichage du temps
            updateTimeDisplay(position);
        }

        // Mettre à jour l'affichage du temps
        function updateTimeDisplay(position) {
            const timeDisplay = document.getElementById('timeDisplay');
            const timestamp = timestamps[position];

            if (!timestamp) {
                timeDisplay.textContent = 'Chargement...';
                return;
            }

            const date = new Date(timestamp.time * 1000);
            const now = new Date();
            const diff = Math.round((date - now) / 60000); // Différence en minutes

            if (diff === 0) {
                timeDisplay.textContent = 'Maintenant';
            } else if (diff > 0) {
                timeDisplay.textContent = '+' + diff + ' min';
            } else {
                timeDisplay.textContent = diff + ' min';
            }
        }

        // Lancer l'animation
        function playAnimation() {
            if (animationTimer) return;

            animationTimer = setInterval(() => {
                animationPosition = (animationPosition + 1) % timestamps.length;
                document.getElementById('timeSlider').value = animationPosition;
                updateRadarFrame(animationPosition);
            }, 500); // Changement toutes les 500ms
        }

        // Arrêter l'animation
        function stopAnimation() {
            if (animationTimer) {
                clearInterval(animationTimer);
                animationTimer = null;
            }
        }

                // Charger RainViewer au démarrage
                loadRainViewer();

            } catch (error) {
                console.error('❌ ERREUR GLOBALE:', error);
                console.error('Stack:', error.stack);
            }
        });
    </script>
</body>
</html>
    """.trimIndent()
}
