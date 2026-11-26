/* SPDX-License-Identifier: AGPL-3.0-or-later */
package io.github.lingqiqi5211.ezhooktool.xposed.dsl
import io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam
object Hooks { var before: ((HookParam) -> Unit)? = null; var registrations = 0 }
class Builder { fun before(callback: (HookParam) -> Unit) { Hooks.before = callback } }
fun Class<*>.hookAllMethods(name: String, builder: Builder.() -> Unit) {
    check(name == "createOverlays")
    Hooks.registrations++
    Builder().builder()
}
