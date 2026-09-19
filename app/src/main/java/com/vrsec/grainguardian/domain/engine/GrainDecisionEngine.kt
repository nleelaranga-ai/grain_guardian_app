package com.vrsec.grainguardian.domain.engine

import com.vrsec.grainguardian.data.model.CropType
import com.vrsec.grainguardian.data.model.RiskStatus
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object GrainDecisionEngine {

    fun evaluateDrying(crop: CropType, moisture: Float): DecisionResult {
        val safeThreshold = crop.safeMoistureThreshold
        val isSafe = moisture <= safeThreshold
        val deltaM = max(0f, moisture - safeThreshold)

        val status = when {
            moisture <= safeThreshold -> RiskStatus.SAFE
            moisture <= safeThreshold + 2.5f -> RiskStatus.WARNING
            else -> RiskStatus.HIGH_RISK
        }

        val bdrs = when (status) {
            RiskStatus.SAFE -> {
                val ratio = if (safeThreshold > 0) moisture / safeThreshold else 1f
                (ratio * 15f).roundToInt().coerceIn(5, 18)
            }
            RiskStatus.WARNING -> {
                (35f + (deltaM / 2.5f) * 30f).roundToInt().coerceIn(35, 65)
            }
            RiskStatus.HIGH_RISK -> {
                (68f + (deltaM / 4.0f) * 27f).roundToInt().coerceIn(70, 95)
            }
        }
        val gshi = 100 - bdrs

        val whyList = mutableListOf<String>()
        val whyTeluguList = mutableListOf<String>()

        if (isSafe) {
            whyList.add("Moisture level (${String.format("%.1f", moisture)}%) is within the safe storage threshold (${safeThreshold}%).")
            whyTeluguList.add("తేమ శాతం (${String.format("%.1f", moisture)}%) సురక్షిత నిల్వ పరిమితి (${safeThreshold}%) లోపు ఉంది.")
        } else {
            whyList.add("Moisture level (${String.format("%.1f", moisture)}%) exceeds the safe limit (${safeThreshold}%).")
            whyTeluguList.add("తేమ శాతం (${String.format("%.1f", moisture)}%) సురక్షిత పరిమితి (${safeThreshold}%) కంటే ఎక్కువ ఉంది.")
            whyList.add("Grain retains excess moisture which may trigger mold growth if bagged immediately.")
            whyTeluguList.add("వెంటనే బస్తాల్లో నింపితే అధిక తేమ వల్ల బూజు, తెగుళ్లు పట్టే ప్రమాదం ఉంది.")
        }

        val actions = if (isSafe) {
            listOf(
                "Grain is thoroughly dry and ready for bagging",
                "Store in a clean, ventilated, moisture-proof godown",
                "Place bags on wooden dunnage/pallets off the bare floor"
            )
        } else {
            listOf(
                "Continue sun drying for 4–6 additional hours",
                "Spread grain evenly at 2–3 inches thickness on drying yard",
                "Turn the grain pile periodically every 1–2 hours",
                "Recheck moisture with GrainGuardian before final bagging"
            )
        }

        val actionsTelugu = if (isSafe) {
            listOf(
                "ధాన్యం సంపూర్ణంగా ఎండి బస్తాల్లో నింపడానికి సిద్ధంగా ఉంది",
                "గాలి వెలుతురు ఉండే తేమ లేని ప్రదేశంలో నిల్వ చేయండి",
                "నేలపై కాకుండా చెక్క పలకలపై బస్తాలు అమర్చండి"
            )
        } else {
            listOf(
                "మరో 4–6 గంటల పాటు ఎండబెట్టడం కొనసాగించండి",
                "కళ్ళంలో ధాన్యాన్ని 2–3 అంగుళాల మందంలో సమానంగా పరచండి",
                "ప్రతి 1–2 గంటలకు ఒకసారి ధాన్యాన్ని కలపండి",
                "బస్తాల్లో నింపే ముందు మరలా ప్రోబ్‌తో తేమను తనిఖీ చేయండి"
            )
        }

        return DecisionResult(
            status = status,
            moisture = moisture,
            isMoistureSafe = isSafe,
            t1Top = 29.0f,
            t2Middle = 30.0f,
            t3Bottom = 30.5f,
            humidity = 64.0f,
            tgi = 1.5f,
            bdrs = bdrs,
            gshi = gshi,
            dominantRisk = if (isSafe) "Optimal for Storage" else "Moisture Elevated (${String.format("%.1f", moisture)}%)",
            dominantRiskTelugu = if (isSafe) "నిల్వకు సరైన స్థితి" else "అధిక తేమ (${String.format("%.1f", moisture)}%)",
            whyReasons = whyList,
            whyReasonsTelugu = whyTeluguList,
            recommendedActions = actions,
            recommendedActionsTelugu = actionsTelugu,
            headline = if (isSafe) "SAFE TO STORE" else "NOT READY - CONTINUE DRYING",
            headlineTelugu = if (isSafe) "నిల్వకు సురక్షితం" else "సిద్ధంగా లేదు - ఎండబెట్టండి",
            subtitle = if (isSafe) "The measured moisture is within the scientific safe range for this crop."
            else "Moisture content exceeds the threshold. Additional drying required.",
            subtitleTelugu = if (isSafe) "కొలిచిన తేమ ఈ పంటకు సిఫార్సు చేసిన సురక్షిత పరిమితిలో ఉంది."
            else "తేమ సురక్షిత పరిమితి కంటే ఎక్కువ ఉంది. మరింత ఎండబెట్టడం అవసరం."
        )
    }

    fun evaluateStorage(
        crop: CropType,
        moisture: Float,
        t1Top: Float,
        t2Middle: Float,
        t3Bottom: Float,
        humidity: Float
    ): DecisionResult {
        val safeMoisture = crop.safeMoistureThreshold
        val maxTemp = max(t1Top, max(t2Middle, t3Bottom))
        val minTemp = min(t1Top, min(t2Middle, t3Bottom))
        val tgi = maxTemp - minTemp // Temperature Gradient Index

        val hasHotspot = maxTemp > 35.0f || tgi > 3.0f
        val isMoistureHigh = moisture > safeMoisture

        val status = when {
            !isMoistureHigh && !hasHotspot && humidity < 72f -> RiskStatus.SAFE
            moisture > safeMoisture + 2.5f || maxTemp > 38f || (isMoistureHigh && hasHotspot) -> RiskStatus.HIGH_RISK
            else -> RiskStatus.WARNING
        }

        val deltaM = max(0f, moisture - safeMoisture)
        val deltaT = max(0f, maxTemp - 32.0f)
        val deltaH = max(0f, humidity - 70.0f)

        val rawRisk = (deltaM * 14.0f) + (tgi * 7.5f) + (deltaT * 5.0f) + (deltaH * 1.2f)
        val bdrs = when (status) {
            RiskStatus.SAFE -> (rawRisk + 8f).roundToInt().coerceIn(6, 20)
            RiskStatus.WARNING -> (rawRisk + 30f).roundToInt().coerceIn(35, 65)
            RiskStatus.HIGH_RISK -> (rawRisk + 55f).roundToInt().coerceIn(70, 96)
        }
        val gshi = 100 - bdrs

        val whyList = mutableListOf<String>()
        val whyTeluguList = mutableListOf<String>()

        if (moisture > safeMoisture) {
            whyList.add("Moisture is elevated (${String.format("%.1f", moisture)}% vs safe limit ${safeMoisture}%).")
            whyTeluguList.add("తేమ శాతం ఎక్కువగా ఉంది (${String.format("%.1f", moisture)}% vs సురక్షిత పరిమితి ${safeMoisture}%).")
        } else {
            whyList.add("Moisture is within safe storage limit (${String.format("%.1f", moisture)}%).")
            whyTeluguList.add("తేమ శాతం సురక్షిత పరిమితిలో ఉంది (${String.format("%.1f", moisture)}%).")
        }

        if (hasHotspot) {
            whyList.add("Internal heat accumulation detected (Gradient: ${String.format("%.1f", tgi)}°C, Peak: ${String.format("%.1f", maxTemp)}°C).")
            whyTeluguList.add("అంతర్గత ఉష్ణోగ్రత పెరుగుదల గమనించబడింది (గ్రేడియంట్: ${String.format("%.1f", tgi)}°C, గరిష్టం: ${String.format("%.1f", maxTemp)}°C).")
        } else {
            whyList.add("Temperature gradient across top, middle, and bottom layers is normal.")
            whyTeluguList.add("అన్ని పొరల్లో ఉష్ణోగ్రత సాధారణంగా ఉంది.")
        }

        if (humidity > 72f) {
            whyList.add("Ambient relative humidity is high (${String.format("%.0f", humidity)}%).")
            whyTeluguList.add("వాతావరణంలో తేమ శాతం ఎక్కువగా ఉంది (${String.format("%.0f", humidity)}%).")
        }

        val dominant = when {
            moisture > safeMoisture + 2.5f -> "Critical High Moisture"
            hasHotspot -> "Internal Hotspot / Biological Respiration"
            humidity > 75f -> "High Ambient Humidity"
            status == RiskStatus.SAFE -> "Grain storage condition is optimal"
            else -> "Mild Moisture & Temperature Variation"
        }

        val dominantTelugu = when {
            moisture > safeMoisture + 2.5f -> "తీవ్రమైన అధిక తేమ"
            hasHotspot -> "అంతర్గత వేడి పెరుగుదల / హాట్‌స్పాట్"
            humidity > 75f -> "అధిక వాతావరణ తేమ"
            status == RiskStatus.SAFE -> "నిల్వ పరిస్థితి సురక్షితంగా ఉంది"
            else -> "స్వల్ప తేమ / ఉష్ణోగ్రత పెరుగుదల"
        }

        val actions = when (status) {
            RiskStatus.SAFE -> listOf(
                "Grain storage is in safe and stable condition",
                "Maintain dry ventilation throughout storage area",
                "Perform routine monitoring again in 7 days"
            )
            RiskStatus.WARNING -> listOf(
                "Open aeration vents during dry daylight hours",
                "Turn the grain bags / pile to release accumulated heat",
                "Monitor daily with probe until temperature normalizes",
                "Ensure no condensation forming under covers or roofs"
            )
            RiskStatus.HIGH_RISK -> listOf(
                "Immediate aeration required — unload and spread grain immediately",
                "Sun dry grain to eliminate active biological heating",
                "Segregate affected bags from the rest of the lot",
                "Inspect godown floor and walls for moisture seepage"
            )
        }

        val actionsTelugu = when (status) {
            RiskStatus.SAFE -> listOf(
                "ధాన్యం నిల్వ సురక్షితంగా మరియు స్థిరంగా ఉంది",
                "నిల్వ గదిలో మంచి గాలి ప్రసరణ ఉండేలా చూడండి",
                "7 రోజుల తర్వాత ప్రోబ్‌తో సాధారణ తనిఖీ చేయండి"
            )
            RiskStatus.WARNING -> listOf(
                "పగటి వేళల్లో వెంటిలేషన్ కిటికీలు తెరవండి",
                "లోపలి వేడి బయటకు పోయేలా ధాన్యాన్ని తిరగేయండి",
                "ఉష్ణోగ్రత తగ్గే వరకు రోజూ ప్రోబ్‌తో పర్యవేక్షించండి",
                "పైకప్పుల కింద తేమ బిందువులు ఏర్పడకుండా జాగ్రత్త పడండి"
            )
            RiskStatus.HIGH_RISK -> listOf(
                "వెంటనే ధాన్యాన్ని బయటకు తీసి సమానంగా పరచండి",
                "వేడి మరియు బూజు తగ్గడానికి వెంటనే ఎండలో ఆరబెట్టండి",
                "పాడవుతున్న బస్తాలను మిగతా ధాన్యం నుండి వేరు చేయండి",
                "నేల మరియు గోడల నుండి తేమ వస్తుందేమో పరిశీలించండి"
            )
        }

        return DecisionResult(
            status = status,
            moisture = moisture,
            isMoistureSafe = !isMoistureHigh,
            t1Top = t1Top,
            t2Middle = t2Middle,
            t3Bottom = t3Bottom,
            humidity = humidity,
            tgi = tgi,
            bdrs = bdrs,
            gshi = gshi,
            dominantRisk = dominant,
            dominantRiskTelugu = dominantTelugu,
            whyReasons = whyList,
            whyReasonsTelugu = whyTeluguList,
            recommendedActions = actions,
            recommendedActionsTelugu = actionsTelugu,
            headline = when (status) {
                RiskStatus.SAFE -> "GRAIN STORAGE IS SAFE"
                RiskStatus.WARNING -> "STORAGE WARNING - ACTION NEEDED"
                RiskStatus.HIGH_RISK -> "HIGH RISK - URGENT ACTION REQUIRED"
            },
            headlineTelugu = when (status) {
                RiskStatus.SAFE -> "నిల్వ సురక్షితంగా ఉంది"
                RiskStatus.WARNING -> "నిల్వ హెచ్చరిక - చర్య అవసరం"
                RiskStatus.HIGH_RISK -> "అత్యధిక ప్రమాదం - వెంటనే సరిచేయండి"
            },
            subtitle = when (status) {
                RiskStatus.SAFE -> "No active hotspot or moisture hazard detected."
                RiskStatus.WARNING -> "Internal heat or moisture elevation detected in the stack."
                RiskStatus.HIGH_RISK -> "Critical hotspot or high moisture threatening grain quality."
            },
            subtitleTelugu = when (status) {
                RiskStatus.SAFE -> "ఎలాంటి హాట్‌స్పాట్ లేదా అధిక తేమ ప్రమాదం లేదు."
                RiskStatus.WARNING -> "ధాన్యంలో లోపలి వేడి లేదా తేమ పెరుగుదల గమనించబడింది."
                RiskStatus.HIGH_RISK -> "ధాన్యం నాణ్యతను దెబ్బతీసే తీవ్రమైన వేడి లేదా తేమ ప్రమాదం ఉంది."
            }
        )
    }
}
