/*
 * This file is part of HyperCraft.
 *
 * HyperCraft is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2023-2026 HyperCraft Contributions
 */

package com.sevtinge.hypercraft.libhook.rules.systemui.statusbar.battery

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.BatteryManager
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.sevtinge.hypercraft.common.log.XposedLog
import com.sevtinge.hypercraft.common.utils.PrefsBridge
import com.sevtinge.hypercraft.libhook.base.BaseHook
import io.github.lingqiqi5211.ezhooktool.core.callMethod
import io.github.lingqiqi5211.ezhooktool.core.callStaticMethod
import io.github.lingqiqi5211.ezhooktool.core.java.Constructors
import io.github.lingqiqi5211.ezhooktool.core.loadClassOrNull
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createAfterHooks
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.createBeforeHooks
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getIntField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectField
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldOrNull
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.getObjectFieldOrNullAs
import io.github.lingqiqi5211.ezhooktool.xposed.dsl.setObjectField
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Displays the battery percentage as a text slot in the status bar.
 *
 * The text is injected through the SystemUI status bar icon slot system
 * (same mechanism as [BatteryDetailIndicator]) so it follows the icon area
 * on both the left side (next to the clock) and the right side (next to the
 * battery icons). The level is polled on a worker thread and pushed to the
 * main thread; the view itself is a [com.android.systemui.statusbar.views.NetworkSpeedView]
 * instance, so light/dark theming of the slot is handled by SystemUI.
 */
object BatteryPercentageIndicator : BaseHook() {

    private const val HOOK_TAG = "BatteryPercentageIndicator"
    private const val SLOT_NAME = "battery_percent"
    private const val SLOT_NETWORK_SPEED = "network_speed"

    private const val ICON_TYPE = 92
    private const val MSG_DATA_UPDATE = 100121
    private const val MSG_WORKER_TICK = 200121
    private const val TICK_INTERVAL_MS = 5_000L

    private const val TAG_SLOT_TEXT_ICON = "battery_percent_text_icon"
    private const val TAG_NUMBER = "battery_percent_number"

    private const val FIELD_CONTAINER = "mContainer"
    private const val FIELD_NETWORK_SPEED_NUMBER_TEXT = "mNetworkSpeedNumberText"
    private const val FIELD_VISIBLE_BY_CONTROLLER = "mVisibleByController"
    private const val FIELD_TYPE = "type"
    private const val FIELD_M_TYPE = "mType"
    private const val FIELD_M_CONTEXT = "mContext"
    private const val FIELD_M_GROUP = "mGroup"

    private const val METHOD_SET_VISIBILITY_BY_CONTROLLER = "setVisibilityByController"
    private const val METHOD_SET_ICON = "setIcon"
    private const val METHOD_GET_SLOT_INDEX = "getSlotIndex"
    private const val METHOD_ON_CREATE_LAYOUT_PARAMS = "onCreateLayoutParams"
    private const val METHOD_SET_BLOCKED = "setBlocked"
    private const val METHOD_ADD_DARK_RECEIVER = "addDarkReceiver"

    private const val STYLE_NETWORK_SPEED_NUMBER = "TextAppearance.StatusBar.NetWorkSpeedNumber"
    private const val STYLE_CLOCK = "TextAppearance.StatusBar.Clock"
    private const val PKG_SYSTEMUI = "com.android.systemui"

    private const val LOW_BATTERY_THRESHOLD = 20

    private val atRight by lazy {
        PrefsBridge.getBoolean("system_ui_statusbar_battery_percent_at_right")
    }
    private val showSign by lazy {
        PrefsBridge.getBoolean("system_ui_statusbar_battery_percent_show_sign", true)
    }
    private val showChargingIcon by lazy {
        PrefsBridge.getBoolean("system_ui_statusbar_battery_percent_charging_icon", true)
    }
    private val lowBatteryWarning by lazy {
        PrefsBridge.getBoolean("system_ui_statusbar_battery_percent_low_warn", true)
    }
    private val fontSize by lazy {
        PrefsBridge.getInt("system_ui_statusbar_battery_percent_font_size", 16)
    }

    private val textIconTagId = getFakeResId("battery_percent_text_icon_tag")
    private val mTextIcons = CopyOnWriteArrayList<View>()

    private var workerThread: HandlerThread? = null
    private var workerHandler: Handler? = null
    private var mainHandler: Handler? = null

    private data class BatteryState(
        val visible: Boolean,
        val text: String,
        val colorOverride: Int // -1 = no override (follow system theme)
    )

    override fun init() {
        val nsvCls = loadClassOrNull("com.android.systemui.statusbar.views.NetworkSpeedView", lpparam.classLoader)
        if (nsvCls == null) {
            XposedLog.e(HOOK_TAG, lpparam.packageName, "NetworkSpeedView class not found")
            return
        }

        if (atRight) {
            RightSideHookHelper.setup(nsvCls)
        } else {
            LeftSideHookHelper.setup(nsvCls)
        }

        startWorker()
        setupHotReloadCleanup()
    }

    private fun setupHotReloadCleanup() {
        registerHotReloadCleanup {
            workerHandler?.removeCallbacksAndMessages(null)
            workerThread?.quitSafely()
            workerThread = null
            workerHandler = null
            mainHandler?.removeCallbacksAndMessages(null)
            mainHandler = null
            mTextIcons.clear()
        }
    }

    private fun startWorker() {
        mainHandler = object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                if (msg.what == MSG_DATA_UPDATE) {
                    applyState(msg.obj as? BatteryState)
                }
            }
        }

        val thread = HandlerThread("BatteryPercentWorker").apply { start() }
        workerThread = thread
        val handler = object : Handler(thread.looper) {
            override fun handleMessage(msg: Message) {
                if (msg.what == MSG_WORKER_TICK) {
                    mainHandler?.obtainMessage(MSG_DATA_UPDATE, computeBatteryState())?.sendToTarget()
                    removeMessages(MSG_WORKER_TICK)
                    sendEmptyMessageDelayed(MSG_WORKER_TICK, TICK_INTERVAL_MS)
                }
            }
        }
        workerHandler = handler
        handler.sendEmptyMessage(MSG_WORKER_TICK)
    }

    private fun computeBatteryState(): BatteryState {
        val context = mTextIcons.firstOrNull()?.context
        if (context == null) return BatteryState(false, "", -1)

        val intent = runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull() ?: return BatteryState(false, "", -1)

        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        if (level < 0 || scale <= 0) return BatteryState(false, "", -1)
        val percent = level * 100 / scale

        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL

        val text = StringBuilder(8).apply {
            if (charging && showChargingIcon) append('⚡')
            append(percent)
            if (showSign) append('%')
        }.toString()

        val colorOverride =
            if (lowBatteryWarning && !charging && percent <= LOW_BATTERY_THRESHOLD) Color.RED else -1

        return BatteryState(true, text, colorOverride)
    }

    private fun applyState(state: BatteryState?) {
        if (state == null) return
        for (view in mTextIcons) {
            runCatching { view.callMethod(METHOD_SET_VISIBILITY_BY_CONTROLLER, state.visible) }
                .onFailure { view.visibility = if (state.visible) View.VISIBLE else View.GONE }
            if (state.visible) {
                val number = view.getObjectFieldOrNullAs<TextView>(FIELD_NETWORK_SPEED_NUMBER_TEXT)
                    ?: (view as? TextView)
                if (number != null) {
                    number.text = state.text
                    if (state.colorOverride >= 0) {
                        number.setTextColor(state.colorOverride)
                    }
                }
            }
        }
    }

    /**
     * Right-side injection: registers the [SLOT_NAME] slot in the SystemUI icon
     * list so the text appears in the icon area next to the battery icon.
     */
    private object RightSideHookHelper {
        fun setup(nsvCls: Class<*>) {
            setupStatusBarIconList()
            setupNetworkSpeedController()
            setupStatusBarIconControllerImpl()
            setupIconManager(nsvCls)
        }

        private fun setupStatusBarIconList() {
            val sbiListCls = loadClassOrNull(
                "com.android.systemui.statusbar.phone.ui.StatusBarIconList",
                lpparam.classLoader
            ) ?: loadClassOrNull(
                "com.android.systemui.statusbar.phone.StatusBarIconList",
                lpparam.classLoader
            ) ?: return

            runCatching {
                Constructors.find(sbiListCls)
                    .filter { it.parameterTypes.size == 1 && it.parameterTypes[0] == Array<String>::class.java }
                    .toList().createBeforeHooks { param ->
                        @Suppress("UNCHECKED_CAST")
                        val slots = param.args[0] as? Array<String>
                        if (slots != null) {
                            val slotList = ArrayList(slots.toList())
                            if (!slotList.contains(SLOT_NAME)) {
                                val netSpeedIndex = slotList.indexOf(SLOT_NETWORK_SPEED)
                                if (netSpeedIndex >= 0) {
                                    slotList.add(netSpeedIndex + 1, SLOT_NAME)
                                } else {
                                    slotList.add(SLOT_NAME)
                                }
                                param.args[0] = slotList.toTypedArray()
                            }
                        }
                    }
            }.onFailure {
                XposedLog.e(HOOK_TAG, lpparam.packageName, "Failed to hook StatusBarIconList constructor: ${it.message}")
            }
        }

        private fun setupNetworkSpeedController() {
            val nscCls = loadClassOrNull(
                "com.android.systemui.statusbar.policy.NetworkSpeedController",
                lpparam.classLoader
            ) ?: return

            runCatching {
                Constructors.find(nscCls).toList().createAfterHooks { param ->
                    val iconController = param.thisObject.getObjectFieldOrNull("mStatusBarIconController")
                    if (iconController != null) {
                        registerIconToController(iconController)
                    }
                }
            }.onFailure {
                XposedLog.e(HOOK_TAG, lpparam.packageName, "Failed to hook NetworkSpeedController constructor: ${it.message}")
            }
        }

        private fun registerIconToController(iconController: Any) {
            runCatching {
                iconController.callMethod(METHOD_SET_ICON, null, SLOT_NAME, 0)
            }.onFailure {
                runCatching {
                    val slotIndex = iconController.callMethod(METHOD_GET_SLOT_INDEX, SLOT_NAME) as? Int ?: 0
                    val sbHolderCls = loadClassOrNull(
                        "com.android.systemui.statusbar.phone.StatusBarIconHolder",
                        lpparam.classLoader
                    )
                    if (sbHolderCls != null) {
                        val holder = Constructors.find(sbHolderCls).toList().firstOrNull()?.newInstance()
                        if (holder != null) {
                            runCatching { holder.setObjectField(FIELD_TYPE, ICON_TYPE) }
                            runCatching { holder.setObjectField(FIELD_M_TYPE, ICON_TYPE) }
                            iconController.callMethod(METHOD_SET_ICON, slotIndex, holder)
                        }
                    }
                }
            }
        }

        private fun setupStatusBarIconControllerImpl() {
            val sbicImplCls = loadClassOrNull(
                "com.android.systemui.statusbar.phone.ui.StatusBarIconControllerImpl",
                lpparam.classLoader
            ) ?: loadClassOrNull(
                "com.android.systemui.statusbar.phone.StatusBarIconControllerImpl",
                lpparam.classLoader
            ) ?: return

            runCatching {
                sbicImplCls.declaredMethods
                    .filter { it.name == METHOD_SET_ICON && it.parameterCount == 2 }
                    .createBeforeHooks { param ->
                        val slotName = param.args[0] as? String
                        if (slotName == SLOT_NAME) {
                            val iconHolder = param.args[1]
                            if (iconHolder != null) {
                                runCatching { iconHolder.setObjectField(FIELD_TYPE, ICON_TYPE) }
                                runCatching { iconHolder.setObjectField(FIELD_M_TYPE, ICON_TYPE) }
                            }
                        }
                    }
            }.onFailure {
                XposedLog.e(HOOK_TAG, lpparam.packageName, "Failed to hook StatusBarIconControllerImpl.setIcon: ${it.message}")
            }
        }

        private fun setupIconManager(nsvCls: Class<*>) {
            val iconManagerCls = loadClassOrNull(
                "com.android.systemui.statusbar.phone.ui.IconManager",
                lpparam.classLoader
            ) ?: loadClassOrNull(
                "com.android.systemui.statusbar.phone.StatusBarIconController\$IconManager",
                lpparam.classLoader
            ) ?: return

            runCatching {
                iconManagerCls.declaredMethods
                    .filter { it.name == "addHolder" && it.parameterCount == 4 }
                    .createBeforeHooks { param ->
                        val iconHolder = param.args[3]
                        if (iconHolder != null) {
                            val type = runCatching { iconHolder.getIntField(FIELD_TYPE) }
                                .getOrElse { runCatching { iconHolder.getIntField(FIELD_M_TYPE) }.getOrDefault(-1) }
                            if (type == ICON_TYPE) {
                                handleIconManagerAddHolder(nsvCls, param)
                            }
                        }
                    }
            }.onFailure {
                XposedLog.e(HOOK_TAG, lpparam.packageName, "Failed to hook IconManager.addHolder: ${it.message}")
            }
        }

        private fun handleIconManagerAddHolder(
            nsvCls: Class<*>,
            param: io.github.lingqiqi5211.ezhooktool.xposed.common.HookParam
        ) {
            val mContext = param.thisObject.getObjectField(FIELD_M_CONTEXT) as Context
            val lp = runCatching {
                param.thisObject.callMethod(METHOD_ON_CREATE_LAYOUT_PARAMS) as LinearLayout.LayoutParams
            }.getOrElse {
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
            }
            val mGroup = param.thisObject.getObjectField(FIELD_M_GROUP) as ViewGroup
            val existing = mGroup.findViewWithTag<View>(TAG_SLOT_TEXT_ICON)
            if (existing != null) {
                if (!mTextIcons.contains(existing)) {
                    mTextIcons.add(existing)
                }
                param.result = existing
            } else {
                val iconView = ViewHelper.createTextIcon(nsvCls, mContext, lp, fromController = true)
                val index = (param.args[0] as? Int ?: 0).coerceAtLeast(0).coerceAtMost(mGroup.childCount)
                mGroup.addView(iconView, index)
                mTextIcons.add(iconView)
                param.result = iconView
            }
        }
    }

    /**
     * Left-side injection: places the text in the clock container so it shows
     * on the left side of the status bar, like [BatteryDetailIndicator].
     */
    private object LeftSideHookHelper {
        fun setup(nsvCls: Class<*>) {
            setupCollapsedStatusBar(nsvCls)
            setupStatusBarViewController(nsvCls)
            setupSystemIconAreaVisibility()
        }

        private fun setupCollapsedStatusBar(nsvCls: Class<*>) {
            val mcsbFragmentCls = loadClassOrNull(
                "com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment",
                lpparam.classLoader
            ) ?: loadClassOrNull(
                "com.android.systemui.statusbar.phone.CollapsedStatusBarFragment",
                lpparam.classLoader
            ) ?: return

            runCatching {
                mcsbFragmentCls.declaredMethods.filter {
                    (it.name == "initMiuiViewsOnViewCreated" || it.name == "onViewCreated") && it.parameterCount in 1..2
                }.createAfterHooks { param ->
                    val mContext = runCatching {
                        param.thisObject.callMethod("getContext") as? Context
                    }.getOrNull() ?: (param.args[0] as? View)?.context
                    if (mContext != null) {
                        inject(nsvCls, param.thisObject, param.args[0] as? View, mContext)
                    }
                }
            }.onFailure {
                XposedLog.e(HOOK_TAG, lpparam.packageName, "Failed to hook CollapsedStatusBarFragment: ${it.message}")
            }
        }

        private fun setupStatusBarViewController(nsvCls: Class<*>) {
            val controllerCls = loadClassOrNull(
                "com.android.systemui.statusbar.phone.PhoneStatusBarViewController",
                lpparam.classLoader
            )
            if (controllerCls != null) {
                runCatching {
                    controllerCls.declaredMethods
                        .filter { it.name == "onViewAttached" && it.parameterCount == 0 }
                        .createAfterHooks { param ->
                            val controller = param.thisObject
                            val startSideContainer = controller.getObjectFieldOrNullAs<ViewGroup>("startSideContainer")
                            val context = startSideContainer?.context ?: return@createAfterHooks
                            injectAfterClock(nsvCls, context, startSideContainer, null)
                        }
                }.onFailure {
                    XposedLog.e(HOOK_TAG, lpparam.packageName, "Failed to hook PhoneStatusBarViewController: ${it.message}")
                }
            }

            val statusBarViewCls = loadClassOrNull(
                "com.android.systemui.statusbar.phone.MiuiPhoneStatusBarView",
                lpparam.classLoader
            ) ?: loadClassOrNull(
                "com.android.systemui.statusbar.phone.PhoneStatusBarView",
                lpparam.classLoader
            )
            if (statusBarViewCls != null) {
                runCatching {
                    statusBarViewCls.declaredMethods.filter {
                        (it.name == "onFinishInflate" || it.name == "onAttachedToWindow") && it.parameterCount == 0
                    }.createAfterHooks { param ->
                        val view = param.thisObject as? ViewGroup ?: return@createAfterHooks
                        val clockId = view.resources.getIdentifier("clock", "id", PKG_SYSTEMUI)
                        val clockView = if (clockId != 0) view.findViewById<View>(clockId) else null
                        val container = clockView?.parent as? ViewGroup
                            ?: view.findViewById<ViewGroup>(
                                view.resources.getIdentifier("icons", "id", PKG_SYSTEMUI)
                            ) ?: view
                        injectAfterClock(nsvCls, view.context, container, clockView)
                    }
                }.onFailure {
                    XposedLog.e(HOOK_TAG, lpparam.packageName, "Failed to hook PhoneStatusBarView: ${it.message}")
                }
            }
        }

        private fun setupSystemIconAreaVisibility() {
            val mcsbFragmentCls = loadClassOrNull(
                "com.android.systemui.statusbar.phone.MiuiCollapsedStatusBarFragment",
                lpparam.classLoader
            ) ?: loadClassOrNull(
                "com.android.systemui.statusbar.phone.CollapsedStatusBarFragment",
                lpparam.classLoader
            ) ?: return

            runCatching {
                mcsbFragmentCls.declaredMethods
                    .filter { it.name == "showSystemIconArea" && it.parameterCount == 1 }
                    .createAfterHooks {
                        for (v in mTextIcons) {
                            runCatching { v.callMethod(METHOD_SET_VISIBILITY_BY_CONTROLLER, true) }
                                .onFailure { v.visibility = View.VISIBLE }
                        }
                    }
            }
            runCatching {
                mcsbFragmentCls.declaredMethods
                    .filter { it.name == "hideSystemIconArea" && it.parameterCount == 1 }
                    .createAfterHooks {
                        for (v in mTextIcons) {
                            runCatching { v.callMethod(METHOD_SET_VISIBILITY_BY_CONTROLLER, false) }
                                .onFailure { v.visibility = View.GONE }
                        }
                    }
            }
        }

        private fun inject(nsvCls: Class<*>, fragment: Any, rootView: View?, context: Context) {
            // Prefer the clock view's container (matches BatteryDetailIndicator's left-side placement)
            val clockView = fragment.getObjectFieldOrNullAs<View>("mClockView")
                ?: rootView?.let { root ->
                    val clockId = root.resources.getIdentifier("clock", "id", PKG_SYSTEMUI)
                    if (clockId != 0) root.findViewById(clockId) else null
                }
            val container = clockView?.parent as? ViewGroup ?: (rootView as? ViewGroup) ?: return
            injectAfterClock(nsvCls, context, container, clockView)
        }

        private fun injectAfterClock(
            nsvCls: Class<*>,
            context: Context,
            container: ViewGroup,
            clockView: View?
        ) {
            val existing = container.findViewWithTag<View>(TAG_SLOT_TEXT_ICON)
            if (existing != null) {
                if (!mTextIcons.contains(existing)) {
                    mTextIcons.add(existing)
                }
                return
            }

            val darkDispatcher =
                loadClassOrNull("com.android.systemui.plugins.DarkIconDispatcher", lpparam.classLoader)
                    ?.let { darkCls ->
                        loadClassOrNull("com.android.systemui.Dependency", lpparam.classLoader)?.let { depCls ->
                            runCatching { depCls.callStaticMethod("get", darkCls) }.getOrNull()
                        }
                    }

            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            val iconView = ViewHelper.createTextIcon(nsvCls, context, lp, fromController = false)
            val index = if (clockView != null) {
                (container.indexOfChild(clockView) + 1).coerceAtLeast(0).coerceAtMost(container.childCount)
            } else {
                container.childCount
            }
            container.addView(iconView, index)
            mTextIcons.add(iconView)
            if (darkDispatcher != null) {
                runCatching { darkDispatcher.callMethod(METHOD_ADD_DARK_RECEIVER, iconView) }
            }
        }
    }

    private object ViewHelper {
        fun createTextIcon(
            nsvCls: Class<*>,
            mContext: Context,
            lp: ViewGroup.LayoutParams,
            fromController: Boolean
        ): View {
            val constructors = Constructors.find(nsvCls).toList()
            val constructor = constructors.firstOrNull {
                it.parameterCount == 1 && it.parameterTypes[0] == Context::class.java
            } ?: constructors.firstOrNull { it.parameterCount == 2 }
                ?: constructors.first()

            val iconView = if (constructor.parameterCount == 1) {
                constructor.newInstance(mContext) as ViewGroup
            } else {
                constructor.newInstance(mContext, null) as ViewGroup
            }

            iconView.tag = TAG_SLOT_TEXT_ICON
            iconView.setTag(textIconTagId, ICON_TYPE)
            iconView.layoutParams = lp

            val number = TextView(mContext).apply {
                tag = TAG_NUMBER
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                gravity = Gravity.CENTER
                isSingleLine = true
            }
            iconView.addView(number)

            runCatching { iconView.setObjectField(FIELD_CONTAINER, number) }
            runCatching { iconView.setObjectField(FIELD_NETWORK_SPEED_NUMBER_TEXT, number) }
            runCatching { iconView.setObjectField(FIELD_VISIBLE_BY_CONTROLLER, true) }
            runCatching {
                iconView.javaClass.declaredMethods.filter { it.name.startsWith("updateResources") }.forEach {
                    it.isAccessible = true
                    it.invoke(iconView)
                }
            }

            val res = mContext.resources
            val styleId = res.getIdentifier(STYLE_NETWORK_SPEED_NUMBER, "style", PKG_SYSTEMUI)
                .takeIf { it != 0 }
                ?: res.getIdentifier(STYLE_CLOCK, "style", PKG_SYSTEMUI)
            if (styleId != 0) {
                number.setTextAppearance(styleId)
            }
            number.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fontSize * 0.5f)

            if (!fromController) {
                runCatching { iconView.callMethod(METHOD_SET_BLOCKED, false) }
            }

            return iconView
        }
    }
}
