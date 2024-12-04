package com.example.lab12_gm

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.Polygon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import com.google.accompanist.permissions.rememberPermissionState

// 1. Primero, crear una clase para manejar la ubicación
class LocationService(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                continuation.resume(location)
            }.addOnFailureListener { e ->
                continuation.resume(null)
            }
        } catch (e: Exception) {
            continuation.resume(null)
        }
    }
}

// 2. Crear un ViewModel para manejar la lógica
class MapViewModel : ViewModel() {
    private val _currentLocation = MutableStateFlow<LatLng?>(null)
    val currentLocation: StateFlow<LatLng?> = _currentLocation.asStateFlow()

    fun updateLocation(location: LatLng) {
        _currentLocation.value = location
    }
}

// 3. Modificar tu MapScreen para incluir la ubicación actual
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel
) {
    val context = LocalContext.current
    val locations = listOf(
        LatLng(-16.433415,-71.5442652), // JLByR
        LatLng(-16.4205151,-71.4945209), // Paucarpata
        LatLng(-16.3524187,-71.5675994) // Zamacola
    )

    val currentLocation by viewModel.currentLocation.collectAsState()

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(locations[0], 12f)
    }

    var mapType by remember { mutableStateOf(MapType.NORMAL) }

    // 4. Manejar los permisos
    val permissionState = rememberPermissionState(
        android.Manifest.permission.ACCESS_FINE_LOCATION
    )

    LaunchedEffect(Unit) {
        if (permissionState.status.isGranted) {
            val locationService = LocationService(context)
            locationService.getCurrentLocation()?.let { location ->
                viewModel.updateLocation(LatLng(location.latitude, location.longitude))
            }
        } else {
            permissionState.launchPermissionRequest()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                mapType = mapType,
                isMyLocationEnabled = permissionState.status.isGranted
            )
        ) {
            val context = LocalContext.current
            val bitmap = BitmapFactory.decodeResource(context.resources, R.drawable.monicon)
            val smallMarkerIcon = Bitmap.createScaledBitmap(bitmap, 84, 84, false)
            val markerDescriptor = BitmapDescriptorFactory.fromBitmap(smallMarkerIcon)

            // 5. Mostrar el marcador de ubicación actual
            currentLocation?.let { location ->
                Marker(
                    state = rememberMarkerState(position = location),
                    title = "Mi ubicación",
                    snippet = "Estás aquí",
                    icon = markerDescriptor
                )

                LaunchedEffect(location) {
                    cameraPositionState.animate(
                        update = CameraUpdateFactory.newLatLngZoom(
                            location,
                            15f
                        )
                    )
                }
            }
        }

        MapTypeButtons(onMapTypeChange = { newType -> mapType = newType })
    }
}

@Composable
fun MapTypeButtons(onMapTypeChange: (MapType) -> Unit) {
    Column {
        Button(onClick = { onMapTypeChange(MapType.NORMAL) }) { Text("Normal") }
        Button(onClick = { onMapTypeChange(MapType.HYBRID) }) { Text("Híbrido") }
        Button(onClick = { onMapTypeChange(MapType.SATELLITE) }) { Text("Satélite") }
        Button(onClick = { onMapTypeChange(MapType.TERRAIN) }) { Text("Terreno") }
    }
}

