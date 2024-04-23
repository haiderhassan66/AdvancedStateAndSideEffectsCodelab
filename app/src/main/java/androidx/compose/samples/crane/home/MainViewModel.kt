/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.samples.crane.home

import androidx.compose.runtime.collectAsState
import androidx.compose.samples.crane.data.DestinationsRepository
import androidx.compose.samples.crane.data.ExploreModel
import androidx.compose.samples.crane.di.DefaultDispatcher
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.random.Random

const val MAX_PEOPLE = 4

@HiltViewModel
class MainViewModel @Inject constructor(
    private val destinationsRepository: DestinationsRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _hotels = MutableStateFlow<List<ExploreModel>>(emptyList())
    val hotels: StateFlow<List<ExploreModel>> = _hotels.asStateFlow()
    private val _restaurants = MutableStateFlow<List<ExploreModel>>(emptyList())
    val restaurants: StateFlow<List<ExploreModel>> = _restaurants.asStateFlow()
    private val _suggestedDestinations = MutableStateFlow<List<ExploreModel>>(emptyList())
    val suggestedDestinations: StateFlow<List<ExploreModel>> = _suggestedDestinations.asStateFlow()

    var location = MutableStateFlow<List<String>>(emptyList())
    var sleepLocation = MutableStateFlow<List<String>>(emptyList())
    var eatLocation = MutableStateFlow<List<String>>(emptyList())

    var flyLocState = MutableStateFlow("City, Country")
    var sleepLocState = MutableStateFlow("City, Country")

    init {
        _suggestedDestinations.value = destinationsRepository.destinations
        _hotels.value = destinationsRepository.hotels
        _restaurants.value = destinationsRepository.restaurants
    }

    fun setFlyLocState(loc:String){
        flyLocState.value = loc
    }

    fun setSleepLocState(loc:String){
        sleepLocState.value = loc
    }

    fun getDestinationLocation(){
        val locList:MutableList<String> = mutableListOf()
        viewModelScope.launch {
            withContext(defaultDispatcher){
                destinationsRepository.destinations.forEach {
                    locList.add(it.city.nameToDisplay)
                }
            }
            location.value = locList
        }
    }

    fun getSleepLocation(){
        val locList:MutableList<String> = mutableListOf()
        viewModelScope.launch {
            withContext(defaultDispatcher){
                destinationsRepository.hotels.forEach {
                    locList.add(it.city.nameToDisplay)
                }
            }
            sleepLocation.value = locList
        }
    }

    fun getEatLocation(){
        val locList:MutableList<String> = mutableListOf()
        viewModelScope.launch {
            withContext(defaultDispatcher){
                destinationsRepository.restaurants.forEach {
                    locList.add(it.city.nameToDisplay)
                }
            }
            eatLocation.value = locList
        }
    }

    fun updatePeople(people: Int) {
        viewModelScope.launch {
            if (people > MAX_PEOPLE) {
                _suggestedDestinations.value = emptyList()
                _hotels.value = emptyList()
                _restaurants.value = emptyList()
            } else {
                val newDestinations = withContext(defaultDispatcher) {
                    destinationsRepository.destinations
                        .shuffled(Random(people * (1..100).shuffled().first()))
                }
                _suggestedDestinations.value = newDestinations

                val newHotels = withContext(defaultDispatcher) {
                    destinationsRepository.hotels
                        .shuffled(Random(people * (1..100).shuffled().first()))
                }
                _hotels.value = newHotels

                val newRestaurants = withContext(defaultDispatcher) {
                    destinationsRepository.restaurants
                        .shuffled(Random(people * (1..100).shuffled().first()))
                }
                _restaurants.value = newRestaurants
            }
        }
    }

    fun toDestinationChanged(newDestination: String) {
        viewModelScope.launch {
            val newDestinations = withContext(defaultDispatcher) {
                destinationsRepository.destinations
                    .filter { it.city.nameToDisplay.contains(newDestination) }
            }
            _suggestedDestinations.value = newDestinations
        }
    }

    fun sleepLocationChanged(newLoc:String){
        viewModelScope.launch {
            val newLoc = withContext(defaultDispatcher) {
                destinationsRepository.hotels
                    .filter { it.city.nameToDisplay.contains(newLoc) }
            }
            _hotels.value = newLoc
        }
    }

    fun eatLocationChanged(newLoc:String){
        viewModelScope.launch {
            val newLoc = withContext(defaultDispatcher) {
                destinationsRepository.restaurants
                    .filter { it.city.nameToDisplay.contains(newLoc) }
            }
            _restaurants.value = newLoc
        }
    }
}
