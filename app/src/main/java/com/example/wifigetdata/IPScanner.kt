package com.example.wifigetdata

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Bundle
import android.text.format.Formatter
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress

class IPScanner : MainActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent{
            IPScannerContent()
        }
    }

    @Composable
    fun IPScannerContent() {
        val coroutineScope = rememberCoroutineScope()
        val ipAddresses = remember { mutableStateOf(listOf<String>()) }
        val isScanning = remember { mutableStateOf(false) }
        val scanComplete = remember { mutableStateOf(false) }

        suspend fun checkHosts() = coroutineScope {
            isScanning.value = true

            val timeout = 1000
            // Get Wifi service
            val wifiManager =
                applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            // Get the IP address of the device and format it as a string
            val ipAddress = Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
            // Extract the subnet from the IP address
            val subnet = ipAddress.substringBeforeLast(".")
            println(" Scanning subnet $subnet")
            ipAddresses.value = listOf()
            val jobs = List(254) { i ->
                async {
                    val host = "$subnet.$i"
                    println("Checking host $host")
                    val inetAddress = InetAddress.getByName(host)
                    if (inetAddress.isReachable(timeout)) {
                        val hostName = inetAddress.hostName
                        println("$host is reachable with host name $hostName")
                        ipAddresses.value += "$host ($hostName)"
                        println(ipAddresses.value.toString())
                    }
                }
            }
            if (isScanning.value) {
                jobs.forEach { it.await() }
            }
            println("All hosts checked. ")
            isScanning.value = false
            scanComplete.value = true
            delay(5000)
            scanComplete.value = false
        }

        val intent = Intent(this@IPScanner, CalibrationActivity::class.java)

        Column (modifier = Modifier.fillMaxSize().padding(20.dp)){
            Text(text = "Scan for Devices")
            Row (horizontalArrangement = Arrangement.Center){
                Button(enabled = (!isScanning.value), onClick = { coroutineScope.launch { withContext(Dispatchers.IO) { checkHosts() } } }, modifier= Modifier.padding(10.dp)) {
                    Text(text = "Scan")
                }
                if (isScanning.value) {
                    CircularProgressIndicator()
                } else if (scanComplete.value) {
                    Icon(Icons.Filled.Check, contentDescription = "Scan complete", tint = Color.Green)
                }
            }
            LazyColumn(userScrollEnabled = true) {
                items(ipAddresses.value.size) { index ->
                    Card(modifier = Modifier.padding(10.dp), onClick = {
                        isScanning.value = false
                        println("Clicked on ${ipAddresses.value[index]}")
                        BasicValues.setURL("http://${ipAddresses.value[index].substringBefore("(").trim()}")
                        sharedViewModel.fetchData()
                        println("fetch data function called")
                        startActivity(intent)
                    }){
                        Text(
                            text = ipAddresses.value[index],
                            )
                    }
                }
            }
        }
    }
}