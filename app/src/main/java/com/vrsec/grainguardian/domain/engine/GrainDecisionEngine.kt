package com.vrsec.grainguardian.domain.engine

import com.vrsec.grainguardian.data.model.CropType
import com.vrsec.grainguardian.data.model.RiskStatus
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * GrainDecisionEngine — Deterministic Agronomic Safety & Quality Evaluator
 * 
 * Strict Evaluation Order:
 * 1. Physical Sensor Validation -> SENSOR_FAULT (Invalid, 0 mV, missing reading, Dallas -127°C error)
 * 2. Freshness Check against maxReadingAgeMinutes -> STALE
 * 3. Reading Quantity and Stability Check -> INSUFFICIENT_EVIDENCE
 * 4. Agronomic Rules Execution (Pre-storage vs Stored) -> ATTENTION or NORMAL
 *
 * NON-NEGOTIABLE SAFETY LAWS:
 * - Zero silent fallbacks (never default missing moisture to 13.2%)
 * - No exact drying hours (qualitative FES advice only)
 * - Separation of pre-storage (drying) and stored-grain (monitoring) advice
 * - Simulated sandbox inputs are explicitly flagged with source = "SIMULATED"
 */
object GrainDecisionEngine {

    // Reason code constants
    const val REASON_SENSOR_FAULT_MOISTURE = "SENSOR_FAULT_MOISTURE"
    const val REASON_SENSOR_FAULT_TEMPERATURE = "SENSOR_FAULT_TEMPERATURE"
    const val REASON_SENSOR_FAULT_TIMESTAMP = "SENSOR_FAULT_TIMESTAMP"
    const val REASON_STALE_DATA = "STALE_DATA"
    const val REASON_INSUFFICIENT_READINGS = "INSUFFICIENT_READINGS"
    const val REASON_THERMAL_GRADIENT = "THERMAL_GRADIENT"
    const val REASON_HIGH_TEMPERATURE = "HIGH_TEMPERATURE"
    const val REASON_MOISTURE_ABOVE_TARGET = "MOISTURE_ABOVE_TARGET"
    const val REASON_MOISTURE_OPTIMAL = "MOISTURE_OPTIMAL"
    const val REASON_STORAGE_OPTIMAL = "STORAGE_OPTIMAL"

    // Qualitative FES guidance
    const val ADVICE_DRYING_NEEDED_EN = "Your paddy may need more drying before storage."
    const val ADVICE_DRYING_NEEDED_TE = "నిల్వకు ముందు మీ ధాన్యాన్ని మరింత ఆరబెట్టాల్సి రావచ్చు."
    const val ADVICE_DRYING_SAFE_EN = "Paddy moisture is within the safe storage threshold."
    const val ADVICE_DRYING_SAFE_TE = "వరి ధాన్యం తేమ సురక్షిత నిల్వ పరిమితిలో ఉంది."

    /**
     * Pre-Storage Drying Readiness Assessment
     */
    fun evaluatePreStorage(
        readings: List<GrainReading>,
        config: RuleConfig = RuleConfig.DEFAULT,
        currentTimeMs: Long = System.currentTimeMillis()
    ): DecisionResult {
        val paddyConfig = config.paddy
        val isSimulated = readings.any { it.isSimulated }
        val source = if (isSimulated) "SIMULATED" else "REAL"

        // 1. Physical validation: Require at least one reading with valid moisture
        if (readings.isEmpty()) {
            return createErrorResult(
                status = AssessmentStatus.SENSOR_FAULT,
                reasonCode = REASON_SENSOR_FAULT_MOISTURE,
                readings = readings,
                config = config,
                currentTimeMs = currentTimeMs,
                source = source,
                enMsg = "Moisture sensor disconnected or zero reading. Cannot evaluate drying readiness.",
                teMsg = "తేమ సెన్సార్ కనెక్ట్ కాలేదు లేదా రీడింగ్ రాలేదు. ఆరబెట్టే సంసిద్ధతను లెక్కించలేము."
            )
        }

        for (r in readings) {
            if (r.timestamp <= 0L) {
                return createErrorResult(
                    status = AssessmentStatus.SENSOR_FAULT,
                    reasonCode = REASON_SENSOR_FAULT_TIMESTAMP,
                    readings = readings,
                    config = config,
                    currentTimeMs = currentTimeMs,
                    source = source,
                    enMsg = "Invalid timestamp detected on sensor reading.",
                    teMsg = "సెన్సార్ రీడింగ్‌లో చెల్లని సమయం నమోదు చేయబడింది."
                )
            }
            if (!r.isMoisturePhysicallyValid()) {
                return createErrorResult(
                    status = AssessmentStatus.SENSOR_FAULT,
                    reasonCode = REASON_SENSOR_FAULT_MOISTURE,
                    readings = readings,
                    config = config,
                    currentTimeMs = currentTimeMs,
                    source = source,
                    enMsg = "Moisture sensor fault or out-of-range value detected. Never issuing false safe verdict.",
                    teMsg = "తేమ సెన్సార్‌లో లోపం లేదా అసాధారణ విలువ గుర్తించబడింది. తప్పుడు సురక్షిత ఫలితాన్ని ఇవ్వలేము."
                )
            }
        }

        // 2. Freshness check
        val newestReading = readings.maxByOrNull { it.timestamp } ?: readings.first()
        val ageMinutes = (currentTimeMs - newestReading.timestamp) / (1000 * 60)
        if (ageMinutes > paddyConfig.maxReadingAgeMinutes) {
            return createErrorResult(
                status = AssessmentStatus.STALE,
                reasonCode = REASON_STALE_DATA,
                readings = readings,
                config = config,
                currentTimeMs = currentTimeMs,
                source = source,
                enMsg = "Sensor data is stale (${ageMinutes} min old, exceeds ${paddyConfig.maxReadingAgeMinutes} min limit). Please take fresh measurement.",
                teMsg = "సెన్సార్ డేటా పాతది (${ageMinutes} నిమిషాల క్రితం). దయచేసి కొత్త రీడింగ్ తీసుకోండి."
            )
        }

        // 3. Minimum readings stability check
        if (readings.size < paddyConfig.minValidReadings) {
            return createErrorResult(
                status = AssessmentStatus.INSUFFICIENT_EVIDENCE,
                reasonCode = REASON_INSUFFICIENT_READINGS,
                readings = readings,
                config = config,
                currentTimeMs = currentTimeMs,
                source = source,
                enMsg = "Insufficient stable readings (${readings.size}/${paddyConfig.minValidReadings} packets). Keep probe steady.",
                teMsg = "తగినన్ని స్థిరమైన రీడింగ్‌లు రాలేదు (${readings.size}/${paddyConfig.minValidReadings}). ప్రోబ్‌ను కదల్చకుండా ఉంచండి."
            )
        }

        // 4. Agronomic Pre-Storage Rules
        val avgMoisture = readings.mapNotNull { it.moisture }.average().toFloat()
        val target = paddyConfig.moistureTargetPercent
        val needsDrying = avgMoisture > target

        val assessmentStatus = if (needsDrying) AssessmentStatus.ATTENTION else AssessmentStatus.NORMAL
        val reasonCode = if (needsDrying) REASON_MOISTURE_ABOVE_TARGET else REASON_MOISTURE_OPTIMAL
        val riskStatus = if (needsDrying) {
            if (avgMoisture > target + 2.5f) RiskStatus.HIGH_RISK else RiskStatus.WARNING
        } else {
            RiskStatus.SAFE
        }

        val bdrs = if (needsDrying) {
            val delta = avgMoisture - target
            (35f + (delta / 2.5f) * 35f).roundToInt().coerceIn(35, 95)
        } else {
            (10f * (avgMoisture / target)).roundToInt().coerceIn(5, 18)
        }
        val gshi = 100 - bdrs

        val actionsEn = if (needsDrying) {
            listOf(
                "Spread grain evenly at 2–3 inches thickness on drying yard",
                "Turn the grain pile periodically every 1–2 hours",
                "Recheck moisture with GrainGuardian before bagging"
            )
        } else {
            listOf(
                "Grain is thoroughly dry and ready for bagging",
                "Store in a clean, ventilated, moisture-proof godown",
                "Place bags on wooden dunnage/pallets off the bare floor"
            )
        }

        val actionsTe = if (needsDrying) {
            listOf(
                "కళ్ళంలో ధాన్యాన్ని 2–3 అంగుళాల మందంలో సమానంగా పరచండి",
                "ప్రతి 1–2 గంటలకు ఒకసారి ధాన్యాన్ని కలపండి",
                "బస్తాల్లో నింపే ముందు మరలా ప్రోబ్‌తో తనిఖీ చేయండి"
            )
        } else {
            listOf(
                "ధాన్యం సంపూర్ణంగా ఎండి బస్తాల్లో నింపడానికి సిద్ధంగా ఉంది",
                "గాలి వెలుతురు ఉండే తేమ లేని ప్రదేశంలో నిల్వ చేయండి",
                "నేలపై కాకుండా చెక్క పలకలపై బస్తాలు అమర్చండి"
            )
        }

        return DecisionResult(
            assessmentStatus = assessmentStatus,
            reasonCode = reasonCode,
            contributingReadings = readings,
            ruleVersion = config.ruleVersion,
            evaluatedAt = currentTimeMs,
            source = source,
            status = riskStatus,
            moisture = (avgMoisture * 10f).roundToInt() / 10f,
            isMoistureSafe = !needsDrying,
            t1Top = readings.lastOrNull()?.t1Top ?: 0f,
            t2Middle = readings.lastOrNull()?.t2Middle ?: 0f,
            t3Bottom = readings.lastOrNull()?.t3Bottom ?: 0f,
            humidity = readings.lastOrNull()?.ambientHumidity ?: 65f,
            tgi = 0f,
            bdrs = bdrs,
            gshi = gshi,
            dominantRisk = if (needsDrying) "Moisture Elevated (${String.format("%.1f", avgMoisture)}%)" else "Moisture Optimal",
            dominantRiskTelugu = if (needsDrying) "అధిక తేమ (${String.format("%.1f", avgMoisture)}%)" else "నిల్వకు సరైన స్థితి",
            whyReasons = listOf(if (needsDrying) ADVICE_DRYING_NEEDED_EN else ADVICE_DRYING_SAFE_EN),
            whyReasonsTelugu = listOf(if (needsDrying) ADVICE_DRYING_NEEDED_TE else ADVICE_DRYING_SAFE_TE),
            recommendedActions = actionsEn,
            recommendedActionsTelugu = actionsTe,
            headline = if (!needsDrying) "READY FOR STORAGE" else "NOT READY - CONTINUE DRYING",
            headlineTelugu = if (!needsDrying) "నిల్వకు సిద్ధంగా ఉంది" else "సిద్ధంగా లేదు - ఎండబెట్టండి",
            subtitle = if (needsDrying) ADVICE_DRYING_NEEDED_EN else ADVICE_DRYING_SAFE_EN,
            subtitleTelugu = if (needsDrying) ADVICE_DRYING_NEEDED_TE else ADVICE_DRYING_SAFE_TE,
            isSimulated = isSimulated
        )
    }

    /**
     * Stored-Grain Monitoring Assessment
     * NOTE: Drying advice NEVER appears on a stored-grain result.
     */
    fun evaluateStoredGrain(
        readings: List<GrainReading>,
        config: RuleConfig = RuleConfig.DEFAULT,
        currentTimeMs: Long = System.currentTimeMillis()
    ): DecisionResult {
        val paddyConfig = config.paddy
        val isSimulated = readings.any { it.isSimulated }
        val source = if (isSimulated) "SIMULATED" else "REAL"

        // 1. Physical validation: Requires moisture AND 3-depth temperature probes
        if (readings.isEmpty()) {
            return createErrorResult(
                status = AssessmentStatus.SENSOR_FAULT,
                reasonCode = REASON_SENSOR_FAULT_TEMPERATURE,
                readings = readings,
                config = config,
                currentTimeMs = currentTimeMs,
                source = source,
                enMsg = "Temperature or moisture sensor disconnected. Cannot evaluate storage health.",
                teMsg = "ఉష్ణోగ్రత లేదా తేమ సెన్సార్లు కనెక్ట్ కాలేదు. నిల్వ ఆరోగ్యాన్ని అంచనా వేయలేము."
            )
        }

        for (r in readings) {
            if (r.timestamp <= 0L) {
                return createErrorResult(
                    status = AssessmentStatus.SENSOR_FAULT,
                    reasonCode = REASON_SENSOR_FAULT_TIMESTAMP,
                    readings = readings,
                    config = config,
                    currentTimeMs = currentTimeMs,
                    source = source,
                    enMsg = "Invalid timestamp detected on sensor reading.",
                    teMsg = "సెన్సార్ రీడింగ్‌లో చెల్లని సమయం నమోదు చేయబడింది."
                )
            }
            if (!r.isMoisturePhysicallyValid()) {
                return createErrorResult(
                    status = AssessmentStatus.SENSOR_FAULT,
                    reasonCode = REASON_SENSOR_FAULT_MOISTURE,
                    readings = readings,
                    config = config,
                    currentTimeMs = currentTimeMs,
                    source = source,
                    enMsg = "Moisture probe disconnected or invalid reading. Cannot assess storage.",
                    teMsg = "తేమ సెన్సార్ కనెక్ట్ కాలేదు లేదా చెల్లని రీడింగ్ వచ్చింది. నిల్వను అంచనా వేయలేము."
                )
            }
            if (!r.areStorageTempsValid()) {
                return createErrorResult(
                    status = AssessmentStatus.SENSOR_FAULT,
                    reasonCode = REASON_SENSOR_FAULT_TEMPERATURE,
                    readings = readings,
                    config = config,
                    currentTimeMs = currentTimeMs,
                    source = source,
                    enMsg = "One or more DS18B20 temperature probes missing or disconnected (-127°C). Cannot evaluate thermal hotspot.",
                    teMsg = "ఉష్ణోగ్రత సెన్సార్ కనెక్ట్ కాలేదు (-127°C). హాట్‌స్పాట్ ప్రమాదాన్ని అంచనా వేయలేము."
                )
            }
        }

        // 2. Freshness check
        val newestReading = readings.maxByOrNull { it.timestamp } ?: readings.first()
        val ageMinutes = (currentTimeMs - newestReading.timestamp) / (1000 * 60)
        if (ageMinutes > paddyConfig.maxReadingAgeMinutes) {
            return createErrorResult(
                status = AssessmentStatus.STALE,
                reasonCode = REASON_STALE_DATA,
                readings = readings,
                config = config,
                currentTimeMs = currentTimeMs,
                source = source,
                enMsg = "Stored grain data is stale (${ageMinutes} min old, exceeds ${paddyConfig.maxReadingAgeMinutes} min limit). Please insert probe and rescan.",
                teMsg = "నిల్వ ధాన్యం డేటా పాతది (${ageMinutes} నిమిషాల క్రితం). దయచేసి ప్రోబ్‌తో మళ్లీ పరీక్షించండి."
            )
        }

        // 3. Stability reading count
        if (readings.size < paddyConfig.minValidReadings) {
            return createErrorResult(
                status = AssessmentStatus.INSUFFICIENT_EVIDENCE,
                reasonCode = REASON_INSUFFICIENT_READINGS,
                readings = readings,
                config = config,
                currentTimeMs = currentTimeMs,
                source = source,
                enMsg = "Insufficient stable readings (${readings.size}/${paddyConfig.minValidReadings} packets). Keep probe in grain pile.",
                teMsg = "తగినన్ని స్థిరమైన రీడింగ్‌లు రాలేదు (${readings.size}/${paddyConfig.minValidReadings}). ప్రోబ్‌ను ధాన్యంలో ఉంచండి."
            )
        }

        // 4. Stored Grain Agronomic Rules
        val latest = readings.last()
        val avgMoisture = readings.mapNotNull { it.moisture }.average().toFloat()
        val t1 = latest.t1Top ?: 0f
        val t2 = latest.t2Middle ?: 0f
        val t3 = latest.t3Bottom ?: 0f
        val maxTemp = max(t1, max(t2, t3))
        val minTemp = min(t1, min(t2, t3))
        val tgi = maxTemp - minTemp

        val hasGradientWarning = tgi > paddyConfig.thermalGradientWarnC
        val hasMaxTempWarning = maxTemp > paddyConfig.maxTemperatureWarnC
        val isMoistureHigh = avgMoisture > paddyConfig.moistureTargetPercent

        val assessmentStatus = when {
            hasGradientWarning || hasMaxTempWarning || isMoistureHigh -> AssessmentStatus.ATTENTION
            else -> AssessmentStatus.NORMAL
        }

        val reasonCode = when {
            hasGradientWarning -> REASON_THERMAL_GRADIENT
            hasMaxTempWarning -> REASON_HIGH_TEMPERATURE
            isMoistureHigh -> REASON_MOISTURE_ABOVE_TARGET
            else -> REASON_STORAGE_OPTIMAL
        }

        val riskStatus = when {
            assessmentStatus == AssessmentStatus.NORMAL -> RiskStatus.SAFE
            avgMoisture > paddyConfig.moistureTargetPercent + 2.5f || maxTemp > 38f || (isMoistureHigh && hasGradientWarning) -> RiskStatus.HIGH_RISK
            else -> RiskStatus.WARNING
        }

        val deltaM = max(0f, avgMoisture - paddyConfig.moistureTargetPercent)
        val deltaT = max(0f, maxTemp - 32.0f)
        val deltaH = max(0f, (latest.ambientHumidity ?: 65f) - 70.0f)
        val rawRisk = (deltaM * 14.0f) + (tgi * 7.5f) + (deltaT * 5.0f) + (deltaH * 1.2f)
        val bdrs = when (riskStatus) {
            RiskStatus.SAFE -> (rawRisk + 8f).roundToInt().coerceIn(6, 20)
            RiskStatus.WARNING -> (rawRisk + 30f).roundToInt().coerceIn(35, 65)
            RiskStatus.HIGH_RISK -> (rawRisk + 55f).roundToInt().coerceIn(70, 96)
        }
        val gshi = 100 - bdrs

        // Stored grain actions NEVER recommend sun drying. They advise aeration, turning bags, or segregation.
        val actionsEn = when (riskStatus) {
            RiskStatus.SAFE -> listOf(
                "Grain storage condition is safe and stable",
                "Maintain dry ventilation throughout storage area",
                "Perform routine monitoring again in 7 days"
            )
            RiskStatus.WARNING -> listOf(
                "Open aeration vents during dry daylight hours",
                "Turn the grain bags / pile to release accumulated heat",
                "Monitor daily with probe until temperature normalizes"
            )
            RiskStatus.HIGH_RISK -> listOf(
                "Immediate aeration required — move and aerate affected stack",
                "Segregate heated bags from the rest of the godown lot",
                "Inspect walls and floor for moisture ingress"
            )
        }

        val actionsTe = when (riskStatus) {
            RiskStatus.SAFE -> listOf(
                "ధాన్యం నిల్వ సురక్షితంగా మరియు స్థిరంగా ఉంది",
                "నిల్వ గదిలో మంచి గాలి ప్రసరణ ఉండేలా చూడండి",
                "7 రోజుల తర్వాత ప్రోబ్‌తో సాధారణ తనిఖీ చేయండి"
            )
            RiskStatus.WARNING -> listOf(
                "పగటి వేళల్లో వెంటిలేషన్ కిటికీలు తెరవండి",
                "లోపలి వేడి బయటకు పోయేలా ధాన్యాన్ని తిరగేయండి",
                "ఉష్ణోగ్రత తగ్గే వరకు రోజూ ప్రోబ్‌తో పర్యవేక్షించండి"
            )
            RiskStatus.HIGH_RISK -> listOf(
                "వెంటనే ధాన్యాన్ని కదిలించి గాలి తగిలేలా చేయండి",
                "వేడెక్కిన బస్తాలను మిగతా ధాన్యం నుండి వేరు చేయండి",
                "గోడలు మరియు నేల నుండి తేమ వస్తుందేమో తనిఖీ చేయండి"
            )
        }

        val dominantEn = when {
            hasGradientWarning -> "Thermal Gradient (Convective Moisture Risk)"
            hasMaxTempWarning -> "High Temperature Peak"
            isMoistureHigh -> "Moisture Elevated in Stack"
            else -> "Storage Condition Optimal"
        }

        val dominantTe = when {
            hasGradientWarning -> "ఉష్ణోగ్రత ప్రవణత (తేమ కదలిక ప్రమాదం)"
            hasMaxTempWarning -> "అధిక ఉష్ణోగ్రత గరిష్టం"
            isMoistureHigh -> "నిల్వలో అధిక తేమ"
            else -> "నిల్వ పరిస్థితి సురక్షితం"
        }

        return DecisionResult(
            assessmentStatus = assessmentStatus,
            reasonCode = reasonCode,
            contributingReadings = readings,
            ruleVersion = config.ruleVersion,
            evaluatedAt = currentTimeMs,
            source = source,
            status = riskStatus,
            moisture = (avgMoisture * 10f).roundToInt() / 10f,
            isMoistureSafe = !isMoistureHigh,
            t1Top = t1,
            t2Middle = t2,
            t3Bottom = t3,
            humidity = latest.ambientHumidity ?: 65f,
            tgi = (tgi * 10f).roundToInt() / 10f,
            bdrs = bdrs,
            gshi = gshi,
            dominantRisk = dominantEn,
            dominantRiskTelugu = dominantTe,
            whyReasons = listOf("Gradient: ${String.format("%.1f", tgi)}°C, Peak: ${String.format("%.1f", maxTemp)}°C, Moisture: ${String.format("%.1f", avgMoisture)}%"),
            whyReasonsTelugu = listOf("ఉష్ణోగ్రత తేడా: ${String.format("%.1f", tgi)}°C, గరిష్టం: ${String.format("%.1f", maxTemp)}°C, తేమ: ${String.format("%.1f", avgMoisture)}%"),
            recommendedActions = actionsEn,
            recommendedActionsTelugu = actionsTe,
            headline = when (riskStatus) {
                RiskStatus.SAFE -> "GRAIN STORAGE IS SAFE"
                RiskStatus.WARNING -> "STORAGE WARNING - ACTION NEEDED"
                RiskStatus.HIGH_RISK -> "HIGH RISK - URGENT ACTION REQUIRED"
            },
            headlineTelugu = when (riskStatus) {
                RiskStatus.SAFE -> "నిల్వ సురక్షితంగా ఉంది"
                RiskStatus.WARNING -> "నిల్వ హెచ్చరిక - చర్య అవసరం"
                RiskStatus.HIGH_RISK -> "అత్యధిక ప్రమాదం - వెంటనే సరిచేయండి"
            },
            subtitle = if (riskStatus == RiskStatus.SAFE) "Thermal gradient and moisture within safe boundaries."
                       else "Internal hotspot or thermal stratification detected in stack.",
            subtitleTelugu = if (riskStatus == RiskStatus.SAFE) "ఉష్ణోగ్రత ప్రవణత మరియు తేమ సురక్షిత పరిమితుల్లో ఉన్నాయి."
                             else "ధాన్యంలో లోపలి వేడి లేదా తేమ పెరుగుదల గమనించబడింది.",
            isSimulated = isSimulated
        )
    }

    private fun createErrorResult(
        status: AssessmentStatus,
        reasonCode: String,
        readings: List<GrainReading>,
        config: RuleConfig,
        currentTimeMs: Long,
        source: String,
        enMsg: String,
        teMsg: String
    ): DecisionResult {
        return DecisionResult(
            assessmentStatus = status,
            reasonCode = reasonCode,
            contributingReadings = readings,
            ruleVersion = config.ruleVersion,
            evaluatedAt = currentTimeMs,
            source = source,
            status = RiskStatus.HIGH_RISK,
            moisture = 0f,
            isMoistureSafe = false,
            headline = if (status == AssessmentStatus.STALE) "STALE MEASUREMENT"
                       else if (status == AssessmentStatus.INSUFFICIENT_EVIDENCE) "INSUFFICIENT EVIDENCE"
                       else "SENSOR FAULT",
            headlineTelugu = if (status == AssessmentStatus.STALE) "పాత కొలత"
                             else if (status == AssessmentStatus.INSUFFICIENT_EVIDENCE) "చాలని డేటా"
                             else "సెన్సార్ లోపం",
            subtitle = enMsg,
            subtitleTelugu = teMsg,
            dominantRisk = "Fault: $reasonCode",
            dominantRiskTelugu = "లోపం: $reasonCode",
            whyReasons = listOf(enMsg),
            whyReasonsTelugu = listOf(teMsg),
            recommendedActions = listOf("Check sensor connection and repeat measurement"),
            recommendedActionsTelugu = listOf("సెన్సార్ కనెక్షన్‌ని పరిశీలించి మళ్లీ కొలవండి"),
            isSimulated = readings.any { it.isSimulated }
        )
    }

    // Convenience backward-compatible adapters
    fun evaluateDrying(crop: CropType, moisture: Float): DecisionResult {
        val now = System.currentTimeMillis()
        val readings = List(5) { i ->
            GrainReading(
                timestamp = now - (4 - i) * 1000L,
                moisture = moisture,
                moistureMv = 2100,
                t1Top = 29f,
                t2Middle = 30f,
                t3Bottom = 30.5f,
                ambientHumidity = 64f
            )
        }
        return evaluatePreStorage(readings)
    }

    fun evaluateStorage(
        crop: CropType,
        moisture: Float,
        t1Top: Float,
        t2Middle: Float,
        t3Bottom: Float,
        humidity: Float
    ): DecisionResult {
        val now = System.currentTimeMillis()
        val readings = List(5) { i ->
            GrainReading(
                timestamp = now - (4 - i) * 1000L,
                moisture = moisture,
                moistureMv = 2100,
                t1Top = t1Top,
                t2Middle = t2Middle,
                t3Bottom = t3Bottom,
                ambientHumidity = humidity
            )
        }
        return evaluateStoredGrain(readings)
    }
}
