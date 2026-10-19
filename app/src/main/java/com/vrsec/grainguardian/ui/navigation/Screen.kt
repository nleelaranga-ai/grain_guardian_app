package com.vrsec.grainguardian.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object LanguageSelection : Screen("language_selection")
    object Welcome : Screen("welcome")
    object Dashboard : Screen("dashboard")
    object CropSelection : Screen("crop_selection")
    object AssessmentSelection : Screen("assessment_selection")
    object ConnectProbe : Screen("connect_probe")
    object PrepareMeasurement : Screen("prepare_measurement")
    object LiveMeasurement : Screen("live_measurement")
    object DryingResult : Screen("drying_result")
    object StorageResult : Screen("storage_result")
    object RiskAnalysis : Screen("risk_analysis")
    object Recommendation : Screen("recommendation")
    object InspectionHistory : Screen("inspection_history")
    object InspectionDetails : Screen("inspection_details/{inspectionId}") {
        fun createRoute(inspectionId: Long) = "inspection_details/$inspectionId"
    }
    object Help : Screen("help")
    object Settings : Screen("settings")
    object DeviceInfo : Screen("device_info")
    object Account : Screen("account")
    object StorageLocations : Screen("storage_locations")
    object SyncStatus : Screen("sync_status")
    object AlertsList : Screen("alerts_list")
    object TechnicianSignIn : Screen("technician_sign_in")
    object TechnicianDiagnostics : Screen("technician_diagnostics")
    object PrivacyData : Screen("privacy_data")
    object PermissionExplainer : Screen("permission_explainer")
}
