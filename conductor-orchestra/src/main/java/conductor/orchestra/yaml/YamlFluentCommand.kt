/*
 *
 *  Copyright (c) 2022 mobile.dev inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */

package conductor.orchestra.yaml

import conductor.Point
import conductor.orchestra.AssertCommand
import conductor.orchestra.BackPressCommand
import conductor.orchestra.ConductorCommand
import conductor.orchestra.ElementSelector
import conductor.orchestra.InputTextCommand
import conductor.orchestra.LaunchAppCommand
import conductor.orchestra.ScrollCommand
import conductor.orchestra.SwipeCommand
import conductor.orchestra.TapOnElementCommand
import conductor.orchestra.TapOnPointCommand

data class YamlFluentCommand(
    val tapOn: YamlElementSelectorUnion? = null,
    val assertVisible: YamlElementSelector? = null,
    val action: String? = null,
    val inputText: String? = null,
    val launchApp: String? = null,
    val swipe: YamlElementSelectorUnion? = null
) {

    @SuppressWarnings("ComplexMethod")
    fun toCommand(): ConductorCommand {
        return when {
            launchApp != null -> ConductorCommand(
                launchAppCommand = LaunchAppCommand(launchApp)
            )
            tapOn != null -> tapCommand(tapOn)
            assertVisible != null -> ConductorCommand(
                assertCommand = AssertCommand(
                    visible = toElementSelector(assertVisible),
                )
            )
            inputText != null -> ConductorCommand(
                inputTextCommand = InputTextCommand(inputText)
            )
            swipe != null -> swipeCommand(swipe)
            action != null -> when (action) {
                "back" -> ConductorCommand(backPressCommand = BackPressCommand())
                "scroll" -> ConductorCommand(scrollCommand = ScrollCommand())
                else -> throw IllegalStateException("Unknown navigation target: $action")
            }
            else -> throw IllegalStateException("No mapping provided for $this")
        }
    }

    private fun tapCommand(tapOn: YamlElementSelectorUnion): ConductorCommand {
        val retryIfNoChange = (tapOn as? YamlElementSelector)?.retryTapIfNoChange ?: true
        val waitUntilVisible = (tapOn as? YamlElementSelector)?.waitUntilVisible ?: true
        val point = (tapOn as? YamlElementSelector)?.point

        return if (point != null) {
            val points = point.split(",")
                .map {
                    it.trim().toInt()
                }

            ConductorCommand(
                tapOnPoint = TapOnPointCommand(
                    x = points[0],
                    y = points[1],
                    retryIfNoChange = retryIfNoChange,
                    waitUntilVisible = waitUntilVisible,
                )
            )
        } else {
            ConductorCommand(
                tapOnElement = TapOnElementCommand(
                    selector = toElementSelector(tapOn),
                    retryIfNoChange = retryIfNoChange,
                    waitUntilVisible = waitUntilVisible,
                )
            )
        }
    }

    private fun swipeCommand(tapOn: YamlElementSelectorUnion): ConductorCommand {
        val start = (tapOn as? YamlElementSelector)?.start
        val end = (tapOn as? YamlElementSelector)?.end
        val startPoint: Point?
        val endPoint: Point?

        if (start != null) {
            val points = start.split(",")
                .map {
                    it.trim().toInt()
                }

            startPoint = Point(points[0], points[1])
        } else {
            throw IllegalStateException("No start point configured for swipe action")
        }

        if (end != null) {
            val points = end.split(",")
                .map {
                    it.trim().toInt()
                }

            endPoint = Point(points[0], points[1])
        } else {
            throw IllegalStateException("No end point configured for swipe action")
        }

        return ConductorCommand(
            swipeCommand = SwipeCommand(startPoint, endPoint)
        )
    }

    private fun toElementSelector(selectorUnion: YamlElementSelectorUnion): ElementSelector {
        return if (selectorUnion is StringElementSelector) {
            ElementSelector(
                textRegex = selectorUnion.value,
            )
        } else if (selectorUnion is YamlElementSelector) {
            toElementSelector(selectorUnion)
        } else {
            throw IllegalStateException("Unknown selector type: $selectorUnion")
        }
    }

    private fun toElementSelector(selector: YamlElementSelector): ElementSelector {
        val size = if (selector.width != null || selector.height != null) {
            ElementSelector.SizeSelector(
                width = selector.width,
                height = selector.height,
                tolerance = selector.tolerance,
            )
        } else {
            null
        }

        return ElementSelector(
            textRegex = selector.text,
            idRegex = selector.id,
            size = size,
            below = selector.below?.let { toElementSelector(it) },
            above = selector.above?.let { toElementSelector(it) },
            leftOf = selector.leftOf?.let { toElementSelector(it) },
            rightOf = selector.rightOf?.let { toElementSelector(it) },
            containsChild = selector.containsChild?.let { toElementSelector(it) },
            optional = selector.optional ?: false,
        )
    }
}
