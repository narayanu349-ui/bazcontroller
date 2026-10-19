package com.baz.controller.repository

import android.content.Context
import com.baz.controller.model.ControlType
import com.baz.controller.model.LayoutElement
import com.baz.controller.model.TemplateLayout
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TemplateRepository(context: Context) {

    private val prefs = context.getSharedPreferences("baz_templates", Context.MODE_PRIVATE)
    private val gson = Gson()

    private val _templates = MutableStateFlow<List<TemplateLayout>>(emptyList())
    val templates: StateFlow<List<TemplateLayout>> = _templates

    private val _activeTemplate = MutableStateFlow<TemplateLayout>(createEts2DefaultTemplate())
    val activeTemplate: StateFlow<TemplateLayout> = _activeTemplate

    init {
        loadTemplates()
    }

    private fun loadTemplates() {
        val json = prefs.getString("user_templates_json", null)
        val loaded = if (json != null) {
            try {
                val type = object : TypeToken<List<TemplateLayout>>() {}.type
                gson.fromJson<List<TemplateLayout>>(json, type)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        val all = mutableListOf<TemplateLayout>()
        all.add(createEts2DefaultTemplate())
        all.add(createAtsDefaultTemplate())
        all.add(createGtRacingDefaultTemplate())
        all.addAll(loaded)

        _templates.value = all
        _activeTemplate.value = all.first()
    }

    fun setActiveTemplate(template: TemplateLayout) {
        _activeTemplate.value = template
    }

    fun saveTemplate(template: TemplateLayout) {
        val current = _templates.value.toMutableList()
        val index = current.indexOfFirst { it.id == template.id }
        if (index >= 0) {
            current[index] = template
        } else {
            current.add(template)
        }
        _templates.value = current
        _activeTemplate.value = template
        persistUserTemplates()
    }

    fun deleteTemplate(id: String) {
        val current = _templates.value.toMutableList()
        val toRemove = current.find { it.id == id && !it.isDefault }
        if (toRemove != null) {
            current.remove(toRemove)
            _templates.value = current
            if (_activeTemplate.value.id == id) {
                _activeTemplate.value = current.first()
            }
            persistUserTemplates()
        }
    }

    fun exportTemplateJson(template: TemplateLayout): String {
        return gson.toJson(template)
    }

    fun importTemplateJson(jsonString: String): TemplateLayout? {
        return try {
            val template = gson.fromJson(jsonString, TemplateLayout::class.java)
            saveTemplate(template)
            template
        } catch (e: Exception) {
            null
        }
    }

    private fun persistUserTemplates() {
        val customOnly = _templates.value.filter { !it.isDefault }
        val json = gson.toJson(customOnly)
        prefs.edit().putString("user_templates_json", json).apply()
    }

    companion object {
        fun createEts2DefaultTemplate(): TemplateLayout {
            return TemplateLayout(
                id = "ets2_rig_default",
                name = "Euro Truck Simulator 2 (Full Rig)",
                description = "Optimized for heavy haulage with 360° wheel, H-shifter, retarder, hazards and cruise control",
                isDefault = true,
                elements = listOf(
                    LayoutElement(type = ControlType.STEERING_WHEEL, x = 0.05f, y = 0.25f, width = 0.38f, height = 0.65f),
                    LayoutElement(type = ControlType.DASHBOARD_CLUSTER, x = 0.08f, y = 0.04f, width = 0.32f, height = 0.18f),
                    LayoutElement(type = ControlType.INDICATOR_PANEL, x = 0.44f, y = 0.05f, width = 0.20f, height = 0.16f),
                    LayoutElement(type = ControlType.HORN_BUTTON, x = 0.18f, y = 0.48f, width = 0.12f, height = 0.18f),
                    LayoutElement(type = ControlType.LIGHTS_PANEL, x = 0.44f, y = 0.24f, width = 0.18f, height = 0.18f),
                    LayoutElement(type = ControlType.WIPERS_PANEL, x = 0.44f, y = 0.45f, width = 0.18f, height = 0.18f),
                    LayoutElement(type = ControlType.CRUISE_CONTROL, x = 0.44f, y = 0.66f, width = 0.18f, height = 0.22f),
                    LayoutElement(type = ControlType.CAMERA_CLUSTER, x = 0.65f, y = 0.05f, width = 0.14f, height = 0.16f),
                    LayoutElement(type = ControlType.TOGGLE_SWITCHES, x = 0.65f, y = 0.24f, width = 0.14f, height = 0.38f),
                    LayoutElement(type = ControlType.GEAR_SELECTOR, x = 0.65f, y = 0.65f, width = 0.15f, height = 0.28f),
                    LayoutElement(type = ControlType.CLUTCH_PEDAL, x = 0.81f, y = 0.38f, width = 0.055f, height = 0.55f),
                    LayoutElement(type = ControlType.BRAKE_PEDAL, x = 0.875f, y = 0.32f, width = 0.055f, height = 0.61f),
                    LayoutElement(type = ControlType.ACCELERATOR_PEDAL, x = 0.94f, y = 0.25f, width = 0.055f, height = 0.68f)
                )
            )
        }

        fun createAtsDefaultTemplate(): TemplateLayout {
            return TemplateLayout(
                id = "ats_highway_default",
                name = "American Truck Simulator (Highway Cruiser)",
                description = "Long haul layout featuring Jake brake retarder, diff-lock, CB radio horn, and dual cruise toggles",
                isDefault = true,
                elements = listOf(
                    LayoutElement(type = ControlType.STEERING_WHEEL, x = 0.06f, y = 0.22f, width = 0.40f, height = 0.70f),
                    LayoutElement(type = ControlType.DASHBOARD_CLUSTER, x = 0.10f, y = 0.03f, width = 0.32f, height = 0.16f),
                    LayoutElement(type = ControlType.INDICATOR_PANEL, x = 0.48f, y = 0.04f, width = 0.18f, height = 0.16f),
                    LayoutElement(type = ControlType.CRUISE_CONTROL, x = 0.48f, y = 0.24f, width = 0.18f, height = 0.24f),
                    LayoutElement(type = ControlType.GEAR_SELECTOR, x = 0.48f, y = 0.52f, width = 0.18f, height = 0.42f),
                    LayoutElement(type = ControlType.CAMERA_CLUSTER, x = 0.68f, y = 0.04f, width = 0.15f, height = 0.16f),
                    LayoutElement(type = ControlType.TOGGLE_SWITCHES, x = 0.68f, y = 0.24f, width = 0.15f, height = 0.35f),
                    LayoutElement(type = ControlType.HANDBRAKE, x = 0.68f, y = 0.62f, width = 0.12f, height = 0.32f),
                    LayoutElement(type = ControlType.BRAKE_PEDAL, x = 0.84f, y = 0.28f, width = 0.065f, height = 0.65f),
                    LayoutElement(type = ControlType.ACCELERATOR_PEDAL, x = 0.92f, y = 0.22f, width = 0.065f, height = 0.71f)
                )
            )
        }

        fun createGtRacingDefaultTemplate(): TemplateLayout {
            return TemplateLayout(
                id = "gt_racing_default",
                name = "GT Racing & Assetto Corsa",
                description = "Sequential paddle shifters, direct analog pedals, D-pad for pitstops, and camera angle shortcuts",
                isDefault = true,
                elements = listOf(
                    LayoutElement(type = ControlType.STEERING_WHEEL, x = 0.12f, y = 0.15f, width = 0.46f, height = 0.76f),
                    LayoutElement(type = ControlType.DPAD, x = 0.02f, y = 0.20f, width = 0.12f, height = 0.25f),
                    LayoutElement(type = ControlType.DASHBOARD_CLUSTER, x = 0.18f, y = 0.02f, width = 0.34f, height = 0.14f),
                    LayoutElement(type = ControlType.CUSTOM_BUTTON_GROUP, x = 0.60f, y = 0.08f, width = 0.18f, height = 0.35f),
                    LayoutElement(type = ControlType.GEAR_SELECTOR, x = 0.60f, y = 0.48f, width = 0.16f, height = 0.45f),
                    LayoutElement(type = ControlType.BRAKE_PEDAL, x = 0.80f, y = 0.20f, width = 0.08f, height = 0.72f),
                    LayoutElement(type = ControlType.ACCELERATOR_PEDAL, x = 0.90f, y = 0.15f, width = 0.08f, height = 0.77f)
                )
            )
        }
    }
}
