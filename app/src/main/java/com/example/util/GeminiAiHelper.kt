package com.example.util

import com.example.BuildConfig
import com.example.data.model.Car
import com.example.data.model.Refill
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Tip categories for CNG optimization and vehicle longevity.
 */
enum class CngTipCategory(val displayName: String, val iconName: String) {
    DRIVING_HABIT("Driving Habits", "speed"),
    PRESSURE_AND_FILLING("Pressure & Filling", "local_gas_station"),
    ENGINE_MAINTENANCE("Engine & Tuning", "build"),
    SPARK_PLUGS("Spark Plugs & Ignition", "flash_on"),
    AIR_FILTER_TUNING("Air Intake & Filter", "air"),
    CYLINDER_SAFETY("Cylinder & Safety", "security"),
    SEASONAL_WEATHER("Seasonal & AC Usage", "ac_unit"),
    TIRES_AND_LOAD("Tire Pressure & Weight", "tire_repair")
}

/**
 * Structured CNG Tip model.
 */
data class CngTip(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val category: CngTipCategory,
    val impactLevel: String, // "High Impact", "Medium Impact", "Essential Safety"
    val estimatedSavings: String, // e.g. "+10-15% km/kg", "Extends Engine Life"
    val actionableStep: String
)

/**
 * Recommended maintenance item for CNG vehicle health.
 */
data class VehicleMaintenanceItem(
    val component: String,
    val intervalKm: String,
    val urgency: String, // "Normal", "Attention Needed", "Critical"
    val tip: String
)

/**
 * Comprehensive Vehicle Health and Mileage Audit report.
 */
data class VehicleHealthAudit(
    val carName: String,
    val currentMileage: Double,
    val healthScore: Int, // 0 - 100
    val summary: String,
    val recommendations: List<String>,
    val maintenanceItems: List<VehicleMaintenanceItem>,
    val isAiGenerated: Boolean
)

/**
 * Helper class to interact with Gemini AI (gemini-3.5-flash) to provide
 * intelligent recommendations on CNG fuel efficiency, driving habits,
 * and vehicle health maintenance.
 */
class GeminiAiHelper(
    private val apiKey: String = BuildConfig.GEMINI_API_KEY
) {

    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        @Volatile
        private var INSTANCE: GeminiAiHelper? = null

        fun getInstance(): GeminiAiHelper {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: GeminiAiHelper().also { INSTANCE = it }
            }
        }
    }

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Checks whether a valid Gemini API key is configured.
     */
    fun isGeminiConfigured(): Boolean {
        return apiKey.isNotBlank() &&
                apiKey != "MY_GEMINI_API_KEY" &&
                apiKey != "DEFAULT_API_KEY" &&
                apiKey.length > 10
    }

    /**
     * Fetches personalized CNG mileage and health tips tailored to vehicle specs.
     */
    suspend fun getMileageAndHealthTips(
        car: Car? = null,
        currentMileage: Double? = null,
        drivingCondition: String = "City & Highway"
    ): Result<List<CngTip>> = withContext(Dispatchers.IO) {
        if (!isGeminiConfigured()) {
            return@withContext Result.success(getCuratedCngTips())
        }

        val prompt = buildString {
            append("You are an expert automotive engineer specializing in CNG (Compressed Natural Gas) vehicles. ")
            append("Generate 6 actionable, highly effective tips for a driver to improve CNG mileage (km/kg) and maintain engine longevity.\n")
            if (car != null) {
                append("Vehicle: ${car.name}, Odometer: ${car.currentOdometer} km, Tank Capacity: ${car.tankCapacityKg} kg.\n")
            }
            if (currentMileage != null && currentMileage > 0) {
                append("Current Recorded Mileage: ${String.format(java.util.Locale.US, "%.1f", currentMileage)} km/kg.\n")
            }
            append("Driving Conditions: $drivingCondition.\n\n")
            append("Return a clean JSON array with 6 items. Each item must have:\n")
            append("- title (short, catchy, 3-6 words)\n")
            append("- description (detailed explanation with numbers, 2 sentences)\n")
            append("- category (one of: DRIVING_HABIT, PRESSURE_AND_FILLING, ENGINE_MAINTENANCE, SPARK_PLUGS, AIR_FILTER_TUNING, CYLINDER_SAFETY, SEASONAL_WEATHER, TIRES_AND_LOAD)\n")
            append("- impactLevel ('High Impact', 'Medium Impact', or 'Essential Safety')\n")
            append("- estimatedSavings (e.g., '+8-12% km/kg' or 'Saves ~₹600/mo')\n")
            append("- actionableStep (immediate concrete action the driver should take)\n")
            append("Respond with ONLY valid JSON inside ```json``` or plain JSON.")
        }

        try {
            val responseText = callGeminiRaw(prompt)
            val tips = parseTipsFromJson(responseText)
            if (tips.isNotEmpty()) {
                Result.success(tips)
            } else {
                Result.success(getCuratedCngTips())
            }
        } catch (e: Exception) {
            // Graceful fallback to offline curated expert tips
            Result.success(getCuratedCngTips())
        }
    }

    /**
     * Conducts an in-depth Vehicle Health and Fuel Efficiency Audit based on actual refill records.
     */
    suspend fun conductVehicleHealthAudit(
        car: Car,
        refills: List<Refill>
    ): Result<VehicleHealthAudit> = withContext(Dispatchers.IO) {
        val carRefills = refills.filter { it.carId == car.id }
        val avgMileage = if (carRefills.isNotEmpty()) {
            carRefills.mapNotNull { if (it.mileageKmPerKg > 0) it.mileageKmPerKg else null }.average().let { if (it.isNaN()) 24.5 else it }
        } else 25.0

        if (!isGeminiConfigured()) {
            return@withContext Result.success(buildFallbackHealthAudit(car, avgMileage))
        }

        val prompt = buildString {
            append("You are an expert CNG automotive mechanical engineer and fleet diagnostic specialist. ")
            append("Evaluate the vehicle health and fuel efficiency performance for:\n")
            append("Car Model: ${car.name}, Registration: ${car.regNumber}, Current Odometer: ${car.currentOdometer} km, Tank: ${car.tankCapacityKg} kg.\n")
            append("Total Logged Refills: ${carRefills.size}, Average Mileage: ${String.format(java.util.Locale.US, "%.1f", avgMileage)} km/kg.\n\n")
            append("Generate a diagnostic evaluation JSON with:\n")
            append("- healthScore (integer between 60 and 98)\n")
            append("- summary (2-3 sentences diagnostic summary of vehicle performance and CNG system condition)\n")
            append("- recommendations (array of 3-4 string suggestions to improve economy or preserve engine components)\n")
            append("- maintenanceItems (array of 4 objects with: component, intervalKm, urgency ['Normal', 'Attention Needed', 'Critical'], tip)\n")
            append("Return ONLY JSON.")
        }

        try {
            val responseText = callGeminiRaw(prompt)
            val jsonClean = cleanJsonString(responseText)
            val jsonObj = JSONObject(jsonClean)

            val healthScore = jsonObj.optInt("healthScore", 88).coerceIn(40, 100)
            val summary = jsonObj.optString("summary", "Your vehicle is performing within optimal parameters with healthy CNG combustion efficiency.")
            
            val recArray = jsonObj.optJSONArray("recommendations")
            val recommendations = mutableListOf<String>()
            if (recArray != null) {
                for (i in 0 until recArray.length()) {
                    recommendations.add(recArray.getString(i))
                }
            } else {
                recommendations.addAll(listOf(
                    "Switch to petrol for the first 2-3 km on cold mornings to lubricate valve seats.",
                    "Inspect spark plug gaps every 10,000 km (calibrate to 0.7-0.8 mm for CNG).",
                    "Keep tire pressures 2 PSI higher for reduced rolling resistance."
                ))
            }

            val itemsArray = jsonObj.optJSONArray("maintenanceItems")
            val maintenanceItems = mutableListOf<VehicleMaintenanceItem>()
            if (itemsArray != null) {
                for (i in 0 until itemsArray.length()) {
                    val item = itemsArray.getJSONObject(i)
                    maintenanceItems.add(
                        VehicleMaintenanceItem(
                            component = item.optString("component", "CNG Spark Plugs"),
                            intervalKm = item.optString("intervalKm", "Every 15,000 km"),
                            urgency = item.optString("urgency", "Normal"),
                            tip = item.optString("tip", "Use iridium or pre-gapped CNG specific plugs.")
                        )
                    )
                }
            } else {
                maintenanceItems.addAll(getDefaultMaintenanceChecklist())
            }

            Result.success(
                VehicleHealthAudit(
                    carName = car.name,
                    currentMileage = avgMileage,
                    healthScore = healthScore,
                    summary = summary,
                    recommendations = recommendations,
                    maintenanceItems = maintenanceItems,
                    isAiGenerated = true
                )
            )
        } catch (e: Exception) {
            Result.success(buildFallbackHealthAudit(car, avgMileage))
        }
    }

    /**
     * Ask Gemini AI a direct conversational question about CNG vehicles, maintenance, hydro-testing, or mileage.
     */
    suspend fun askCngAdvisor(
        query: String,
        car: Car? = null,
        recentMileage: Double? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!isGeminiConfigured()) {
            return@withContext Result.success(getSmartLocalAnswer(query))
        }

        val prompt = buildString {
            append("You are the AI CNG Master Advisor for the CNG Tracker app. ")
            append("Answer the user's question clearly, concisely, and accurately with real automotive engineering facts.\n")
            if (car != null) {
                append("User Car: ${car.name}, Odometer: ${car.currentOdometer} km.\n")
            }
            if (recentMileage != null && recentMileage > 0) {
                append("Current Mileage: ${String.format(java.util.Locale.US, "%.1f", recentMileage)} km/kg.\n")
            }
            append("\nUser Question: $query\n\n")
            append("Formatting rules: Use bullet points for steps, bold key figures, and keep the answer under 160 words.")
        }

        try {
            val responseText = callGeminiRaw(prompt)
            Result.success(responseText.trim())
        } catch (e: Exception) {
            Result.success(getSmartLocalAnswer(query))
        }
    }

    /**
     * Performs a direct REST API call to Gemini 3.5 Flash endpoint.
     */
    private fun callGeminiRaw(prompt: String): String {
        val requestJson = JSONObject().apply {
            val contentsArray = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    }
                    put("parts", partsArray)
                }
                put(contentObj)
            }
            put("contents", contentsArray)

            val configObj = JSONObject().apply {
                put("temperature", 0.4)
                put("topP", 0.95)
                put("topK", 40)
            }
            put("generationConfig", configObj)

            val systemInstruction = JSONObject().apply {
                val parts = JSONArray().apply {
                    put(JSONObject().put("text", "You are an expert Indian CNG vehicle mechanic, fuel economy specialist, and safety compliance expert."))
                }
                put("parts", parts)
            }
            put("systemInstruction", systemInstruction)
        }

        val url = "$BASE_URL?key=$apiKey"
        val requestBody = requestJson.toString().toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = httpClient.newCall(request).execute()
        val responseBodyStr = response.body?.string().orEmpty()

        if (!response.isSuccessful) {
            throw IllegalStateException("Gemini API Error ${response.code}: $responseBodyStr")
        }

        val json = JSONObject(responseBodyStr)
        val candidates = json.optJSONArray("candidates")
        val firstCandidate = candidates?.optJSONObject(0)
        val content = firstCandidate?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val text = parts?.optJSONObject(0)?.optString("text")

        if (text.isNullOrBlank()) {
            throw IllegalStateException("No text candidate in Gemini response")
        }

        return text
    }

    /**
     * Parses the JSON array of tips generated by Gemini AI.
     */
    private fun parseTipsFromJson(rawResponse: String): List<CngTip> {
        val clean = cleanJsonString(rawResponse)
        val tips = mutableListOf<CngTip>()

        try {
            val array = if (clean.startsWith("[")) {
                JSONArray(clean)
            } else if (clean.startsWith("{")) {
                val obj = JSONObject(clean)
                obj.optJSONArray("tips") ?: JSONArray()
            } else {
                JSONArray()
            }

            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val catStr = obj.optString("category", "DRIVING_HABIT").uppercase()
                val category = try {
                    CngTipCategory.valueOf(catStr)
                } catch (_: Exception) {
                    CngTipCategory.DRIVING_HABIT
                }

                tips.add(
                    CngTip(
                        id = UUID.randomUUID().toString(),
                        title = obj.optString("title", "Optimize Driving RPM"),
                        description = obj.optString("description", "Maintain engine between 1800 to 2200 RPM for highest thermal efficiency in CNG mode."),
                        category = category,
                        impactLevel = obj.optString("impactLevel", "High Impact"),
                        estimatedSavings = obj.optString("estimatedSavings", "+10-15% km/kg"),
                        actionableStep = obj.optString("actionableStep", "Shift to higher gear early and avoid sudden hard accelerations.")
                    )
                )
            }
        } catch (_: Exception) {}

        return tips
    }

    /**
     * Cleans code fence blocks markdown (```json ... ```) from LLM output.
     */
    private fun cleanJsonString(raw: String): String {
        var trimmed = raw.trim()
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.removePrefix("```json")
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.removePrefix("```")
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.removeSuffix("```")
        }
        return trimmed.trim()
    }

    /**
     * Curated, engineering-grade offline tips for CNG vehicles.
     */
    fun getCuratedCngTips(): List<CngTip> {
        return listOf(
            CngTip(
                id = "tip_1",
                title = "Cruise in the 1800-2200 RPM Eco Band",
                description = "CNG burns slower than petrol. Keeping the engine in 1800-2200 RPM in top gear allows full stoichiometric combustion without fuel wastage.",
                category = CngTipCategory.DRIVING_HABIT,
                impactLevel = "High Impact",
                estimatedSavings = "+12-18% km/kg",
                actionableStep = "Shift up smoothly by 2000 RPM and maintain steady throttle on highways."
            ),
            CngTip(
                id = "tip_2",
                title = "Fill at 200-210 Bar Pressure Early Morning",
                description = "Gas expands with heat. Refilling early in the morning when ambient temperatures are cooler compresses denser gas into your cylinder.",
                category = CngTipCategory.PRESSURE_AND_FILLING,
                impactLevel = "High Impact",
                estimatedSavings = "+0.8 to 1.2 kg per refill",
                actionableStep = "Look for dispensers with >= 200 Bar pressure readings during off-peak morning hours."
            ),
            CngTip(
                id = "tip_3",
                title = "Calibrate Spark Plug Gap (0.7-0.8 mm)",
                description = "CNG requires higher ignition voltage than petrol. Reducing spark plug electrode gap to 0.7-0.8 mm prevents misfires and increases combustion efficiency.",
                category = CngTipCategory.SPARK_PLUGS,
                impactLevel = "High Impact",
                estimatedSavings = "+8-10% km/kg & smoother idle",
                actionableStep = "Have your mechanic check spark plug gaps every 10,000 km or replace with Iridium plugs."
            ),
            CngTip(
                id = "tip_4",
                title = "Clean Air Filter Every 2,500 KM",
                description = "CNG demands exact air-to-fuel ratios (approx 17.2:1). A clogged air filter starves the gas mixture of oxygen, causing sluggish pickup and poor mileage.",
                category = CngTipCategory.AIR_FILTER_TUNING,
                impactLevel = "Medium Impact",
                estimatedSavings = "+5-8% km/kg",
                actionableStep = "Blow compressed air through your filter cartridge every 2,500 km and replace every 10,000 km."
            ),
            CngTip(
                id = "tip_5",
                title = "PESO Cylinder Hydro-Testing Every 3 Years",
                description = "Mandatory safety compliance under Indian Gas Cylinder Rules 2016. Hydro-testing checks structural cylinder integrity at 330 bar test pressure.",
                category = CngTipCategory.CYLINDER_SAFETY,
                impactLevel = "Essential Safety",
                estimatedSavings = "100% Leak & Explosion Protection",
                actionableStep = "Verify your PESO compliance plate in the engine bay and renew hydro-test certificate on time."
            ),
            CngTip(
                id = "tip_6",
                title = "Cold Morning Petrol Warm-Up (2 km)",
                description = "Starting and driving 1-2 km on petrol warms the engine block and deposits lubrication on dry intake valve seats before switching to dry CNG.",
                category = CngTipCategory.ENGINE_MAINTENANCE,
                impactLevel = "High Impact",
                estimatedSavings = "Prevents premature valve seat recession",
                actionableStep = "Let engine start on petrol and auto-switch only after reaching 50°C coolant temperature."
            ),
            CngTip(
                id = "tip_7",
                title = "Maintain +2 PSI Over Spec Tire Pressure",
                description = "CNG cylinders add 60-80 kg weight to the rear axle. Running tires at +2 PSI above stock spec neutralizes rolling drag on the extra rear load.",
                category = CngTipCategory.TIRES_AND_LOAD,
                impactLevel = "Medium Impact",
                estimatedSavings = "+4-6% km/kg",
                actionableStep = "Inflate tires to 33-35 PSI cold and rotate tires every 8,000 km."
            ),
            CngTip(
                id = "tip_8",
                title = "Moderate AC Fan Speed in Stop-and-Go Traffic",
                description = "The air conditioner compressor puts an additional 15-20% load on smaller CNG engines (1.0L - 1.2L) during bumper-to-bumper city idling.",
                category = CngTipCategory.SEASONAL_WEATHER,
                impactLevel = "Medium Impact",
                estimatedSavings = "+1.5-2.5 km/kg in summer",
                actionableStep = "Use cabin recirculation and blower speed 2 once cabin cools down."
            )
        )
    }

    /**
     * Fallback health audit when network is offline.
     */
    private fun buildFallbackHealthAudit(car: Car, mileage: Double): VehicleHealthAudit {
        val score = when {
            mileage >= 26.0 -> 94
            mileage >= 22.0 -> 86
            mileage >= 18.0 -> 76
            else -> 68
        }
        return VehicleHealthAudit(
            carName = car.name,
            currentMileage = mileage,
            healthScore = score,
            summary = "Your ${car.name} is running efficiently with a solid recorded average of ${String.format(java.util.Locale.US, "%.1f", mileage)} km/kg. Maintaining clean air induction and high-pressure refills will maximize longevity.",
            recommendations = listOf(
                "Keep spark plug electrode gaps tuned between 0.75 mm and 0.80 mm.",
                "Ensure morning cold starts run on petrol for 1-2 km to protect engine valve seals.",
                "Maintain 33-35 PSI tire pressures to balance the ${car.tankCapacityKg} kg cylinder rear axle load."
            ),
            maintenanceItems = getDefaultMaintenanceChecklist(),
            isAiGenerated = false
        )
    }

    /**
     * Standard CNG periodic maintenance checklist.
     */
    private fun getDefaultMaintenanceChecklist(): List<VehicleMaintenanceItem> {
        return listOf(
            VehicleMaintenanceItem(
                component = "CNG Low-Pressure Gas Filter",
                intervalKm = "Every 20,000 km",
                urgency = "Normal",
                tip = "Filters micro compressor oil and particulates before reaching sequential gas injectors."
            ),
            VehicleMaintenanceItem(
                component = "High-Voltage Spark Plugs",
                intervalKm = "Every 15,000 km",
                urgency = "Normal",
                tip = "CNG has higher ignition temperature; inspect for carbon buildup and clean electrode tip."
            ),
            VehicleMaintenanceItem(
                component = "CNG Reducer/Vaporizer Drain",
                intervalKm = "Every 30,000 km",
                urgency = "Normal",
                tip = "Purge settled compressor oil residue from reducer chamber to prevent pressure drops."
            ),
            VehicleMaintenanceItem(
                component = "PESO Cylinder Hydro-Testing",
                intervalKm = "Every 3 Years",
                urgency = "Critical",
                tip = "Statutory testing by certified PESO testing center for burst and leak compliance."
            )
        )
    }

    /**
     * Smart local responses for common driver inquiries when offline.
     */
    private fun getSmartLocalAnswer(query: String): String {
        val q = query.lowercase()
        return when {
            "pressure" in q || "bar" in q ->
                "**CNG Pressure Guidance:**\n• Optimal filling pressure is **200 to 210 Bar**.\n• Below 180 Bar, you will get 15-25% less gas quantity in your cylinder.\n• Always prioritize stations with modern booster compressors during non-peak morning hours."

            "plug" in q || "spark" in q || "misfire" in q || "pickup" in q ->
                "**Spark Plug & Pickup Tip:**\n• CNG requires higher spark energy than petrol.\n• Set electrode gap to **0.75 mm** (narrower than petrol standard 1.0 mm).\n• Replace spark plugs every 15,000 to 20,000 km with Iridium/CNG-grade plugs for instant throttle response."

            "hydro" in q || "cylinder" in q || "test" in q || "safety" in q || "peso" in q ->
                "**Cylinder Hydro-Testing Law:**\n• In India, CNG cylinders must be hydro-tested **every 3 years** as per PESO rules.\n• Tests verify cylinder tensile strength at 330 Bar water pressure.\n• Never delay testing to avoid gas leakage risks and compliance penalties at fuel pumps."

            "petrol" in q || "switch" in q || "cold start" in q ->
                "**Petrol-to-CNG Switching:**\n• Run your car on **petrol for 1-2 km each morning**.\n• Petrol contains lubricating additives that protect valve seats from dry CNG combustion heat.\n• Keep at least 1/4 tank of petrol to prevent fuel pump dry-run burnout."

            "filter" in q || "air" in q || "mileage" in q ->
                "**Air Filter & Mileage:**\n• Clean your engine air filter every **2,500 km**.\n• CNG needs an exact 17.2:1 air-fuel ratio. A dusty filter causes rich mixture combustion and drops mileage by up to 3 km/kg."

            else ->
                "**CNG Efficiency Tips:**\n• Maintain smooth acceleration under 2,200 RPM.\n• Keep tires at 33-35 PSI.\n• Refill early in the morning when gas density is higher.\n• Clean air filter every 2,500 km."
        }
    }
}
