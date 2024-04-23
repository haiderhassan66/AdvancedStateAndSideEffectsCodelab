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

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.AlertDialog
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.samples.crane.R
import androidx.compose.samples.crane.base.CraneEditableUserInput
import androidx.compose.samples.crane.base.CraneUserInput
import androidx.compose.samples.crane.base.rememberEditableUserInputState
import androidx.compose.samples.crane.data.ExploreModel
import androidx.compose.samples.crane.home.PeopleUserInputAnimationState.Invalid
import androidx.compose.samples.crane.home.PeopleUserInputAnimationState.Valid
import androidx.compose.samples.crane.ui.BottomSheetShape
import androidx.compose.samples.crane.ui.CraneTheme
import androidx.compose.samples.crane.ui.DialogShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import java.util.Locale

enum class PeopleUserInputAnimationState { Valid, Invalid }

class PeopleUserInputState {
    var people by mutableStateOf(1)
        private set

    val animationState: MutableTransitionState<PeopleUserInputAnimationState> =
        MutableTransitionState(Valid)

    fun addPerson() {
        people = (people % (MAX_PEOPLE + 1)) + 1
        updateAnimationState()
    }

    private fun updateAnimationState() {
        val newState =
            if (people > MAX_PEOPLE) Invalid
            else Valid

        if (animationState.currentState != newState) animationState.targetState = newState
    }
}

@Composable
fun PeopleUserInput(
    titleSuffix: String? = "",
    onPeopleChanged: (Int) -> Unit,
    peopleState: PeopleUserInputState = remember { PeopleUserInputState() }
) {
    Column {
        val transitionState = remember { peopleState.animationState }
        val tint = tintPeopleUserInput(transitionState)

        val people = peopleState.people
        CraneUserInput(
            text = if (people == 1) "$people Adult$titleSuffix" else "$people Adults$titleSuffix",
            vectorImageId = R.drawable.ic_person,
            tint = tint.value,
            onClick = {
                peopleState.addPerson()
                onPeopleChanged(peopleState.people)
            }
        )
        if (transitionState.targetState == Invalid) {
            Text(
                text = "Error: We don't support more than $MAX_PEOPLE people",
                style = MaterialTheme.typography.body1.copy(color = MaterialTheme.colors.secondary)
            )
        }
    }
}

@Composable
fun FromDestination(imageId: Int, viewModel: MainViewModel = viewModel(), screen: CraneScreen) {
    when (screen) {
        CraneScreen.Fly -> {
            viewModel.getDestinationLocation()
        }

        CraneScreen.Sleep -> {
            viewModel.getSleepLocation()
        }

        CraneScreen.Eat -> {
            viewModel.getEatLocation()
        }
    }
    val locationList by viewModel.location.collectAsStateWithLifecycle()
    val sleepLocationList by viewModel.sleepLocation.collectAsStateWithLifecycle()
    val eatLocationList by viewModel.eatLocation.collectAsStateWithLifecycle()
//    val locationText = rememberSaveable {
//        mutableStateOf("City, Country")
//    }
    val flyLocText by viewModel.flyLocState.collectAsStateWithLifecycle()
    val sleepLocText by viewModel.sleepLocState.collectAsStateWithLifecycle()


    val showPopup = remember {
        mutableStateOf(false)
    }
    CraneUserInput(
        text = if (screen == CraneScreen.Fly) flyLocText else sleepLocText,
        vectorImageId = imageId,
        onClick = {
            showPopup.value = true
        })

    if (showPopup.value) {
        ScrollableListPopup(
            locationList = when (screen) {
                CraneScreen.Fly -> {
                    locationList
                }

                CraneScreen.Sleep -> {
                    sleepLocationList
                }

                CraneScreen.Eat -> {
                    eatLocationList
                }
            },
            onLocationClicked = {
                showPopup.value = false
                when (screen) {
                    CraneScreen.Fly -> {
                        viewModel.toDestinationChanged(it)
                        viewModel.setFlyLocState(it)
                    }

                    CraneScreen.Sleep -> {
                        viewModel.sleepLocationChanged(it)
                        viewModel.setSleepLocState(it)
                    }

                    CraneScreen.Eat -> {
                        viewModel.eatLocationChanged(it)
                    }
                }
            },
            onDismissRequest = {
                showPopup.value = false
            })
    }
}

@Composable
fun ScrollableListPopup(
    locationList: List<String>,
    onDismissRequest: () -> Unit,
    onLocationClicked: (String) -> Unit
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth(.8f)
                .fillMaxHeight(.6f)
                .padding(vertical = 24.dp)
                .background(color = Color.White, shape = DialogShape)
        ) {
            items(locationList) {
                PopUpItem(it, onLocationClicked)
            }
        }
    }
}

@Composable
fun PopUpItem(item: String, onLocationClicked: (String) -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = item,
            style = MaterialTheme.typography.body1,
            color = Color.Black,
            modifier = Modifier
                .padding(vertical = 4.dp)
                .clickable {
                    onLocationClicked(item)
                }
        )
    }
}


@Composable
fun ToDestinationUserInput(onToDestinationChanged: (String) -> Unit) {
    val editableUserInputState = rememberEditableUserInputState(hint = "Choose Destination")
    CraneEditableUserInput(
        state = editableUserInputState,
        caption = "To",
        vectorImageId = R.drawable.ic_plane
    )

    val currentOnDestinationChanged by rememberUpdatedState(onToDestinationChanged)
    LaunchedEffect(editableUserInputState) {
        snapshotFlow { editableUserInputState.text }
            .filter { !editableUserInputState.isHint }
            .collect {
                currentOnDestinationChanged(editableUserInputState.text)
            }
    }
}

@Composable
fun DatesUserInput() {
    CraneUserInput(
        caption = "Select Dates",
        text = "",
        vectorImageId = R.drawable.ic_calendar
    )
}

@Composable
private fun tintPeopleUserInput(
    transitionState: MutableTransitionState<PeopleUserInputAnimationState>
): State<Color> {
    val validColor = MaterialTheme.colors.onSurface
    val invalidColor = MaterialTheme.colors.secondary

    val transition = updateTransition(transitionState, label = "")
    return transition.animateColor(
        transitionSpec = { tween(durationMillis = 300) }, label = ""
    ) {
        if (it == Valid) validColor else invalidColor
    }
}

@Preview
@Composable
fun PeopleUserInputPreview() {
    CraneTheme {
        PeopleUserInput(onPeopleChanged = {})
    }
}
