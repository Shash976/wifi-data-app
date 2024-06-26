package com.example.wifigetdata

import android.annotation.SuppressLint
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("CoroutineCreationDuringComposition")
@Composable
fun CalibrationScreen( sharedViewModel: MainViewModel, navController: NavController) {
    val counter = remember { mutableIntStateOf(0) }
    val r2score = remember { mutableDoubleStateOf(0.0) }
    val r2CoroutineScope = rememberCoroutineScope()
    val shownAdditionalCards = remember { mutableIntStateOf(0) }
    val startR2Math = remember{ mutableIntStateOf(0) }
    val allData = sharedViewModel.repository.dataValueDao.getAll().collectAsState(emptyList())
    val arraySize = allData.value.size+1
    val calibConcentration = sharedViewModel.calibrationConcentration


    LaunchedEffect(Unit) {
        r2CoroutineScope.launch{
            sharedViewModel.r2score.collect { r2 ->
                withContext(Dispatchers.Default) {
                    r2score.doubleValue = r2
                    val idealR2conditions = r2 >= 0.9 && counter.intValue in 3..<arraySize
                    println("\t is r2 (${r2score.doubleValue}) less? $idealR2conditions | Gradient: ${sharedViewModel.gradient.doubleValue}, Intercept: ${sharedViewModel.intercept.doubleValue} ")
                    when(idealR2conditions){
                        true -> {
                            println("R2score ($r2) is sufficient. Switching to main screen")
                            navController.navigate(Routes.HOME.toString())
                        }
                        false -> {
                            if (shownAdditionalCards.intValue < calibConcentration.size - 3) {
                                shownAdditionalCards.intValue++ // Show one additional card
                                println("showing additional card")
                            }
                            else if (counter.intValue+1 >= calibConcentration.size) {
                                println("All calibration values calibrated. Switching to main screen")
                                navController.navigate(Routes.HOME.toString())
                            }
                        }
                    }
                    //delay(sharedViewModel.updateTiming.value + 1000) // Adjust delay between showing cards
                }
            }
        }
    }

    @Composable
    fun CalibrateCard(index: Int) {
        val labelText = remember { mutableDoubleStateOf(0.0) }
        val concentration = calibConcentration[index]
        val textFixed by remember(counter) { derivedStateOf { counter.intValue != index } } // Is text fixed?
        val updateLabelCoroutineScope = rememberCoroutineScope() // Updates Label Text
        val mathCoroutineScope = rememberCoroutineScope() // Updates R2Score

        updateLabelCoroutineScope.launch {
            sharedViewModel.theValue.collect {
                if (!textFixed && it != labelText.doubleValue) {
                    labelText.doubleValue = it
                }
            }
        }

        val onButtonClick = {
            println("Counter ${counter.intValue}")
            counter.intValue = index+1
            sharedViewModel.updateData(newVoltage = labelText.doubleValue, newConcentration = concentration)
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.padding(10.dp)){
            Card(
                modifier = Modifier.padding(10.dp),
                shape = CardDefaults.elevatedShape,
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(10.dp)
                ) {
                    Text(text = "${labelText.doubleValue} V")
                    Text(text = "$concentration μm")
                    Button(shape = RoundedCornerShape(10.dp), enabled = !textFixed, onClick = {
                        onButtonClick()
                    }) {
                        Text(text = "Set Voltage")
                    }
                }
            }
        }

    }

    Box(modifier= Modifier.fillMaxSize()){
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text="Calibrate",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(15.dp)
            )
            Text(
                text = "R-Square score: ${r2score.doubleValue}",
                modifier = Modifier.padding(10.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.FixedSize(165.dp),
                modifier = Modifier.padding(1.dp),
                verticalArrangement = Arrangement.Center,
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(sharedViewModel.calibrationConcentration.size) {
                    val isVisible = it < (3 + shownAdditionalCards.intValue)
                    if (isVisible) {
                        if (it>=3) println("Showing Card number ${it + 1}")
                        item {
                            CalibrateCard(index = it)
                        }
                    }
                }
            }
        }
        val context = LocalContext.current
        FloatingActionButton(onClick = {
            sharedViewModel.resetValues()
            (context as ComponentActivity).recreate()
        },modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(16.dp)) {
            Icon(Icons.Filled.Refresh, contentDescription = "Reset")

        }
    }
}