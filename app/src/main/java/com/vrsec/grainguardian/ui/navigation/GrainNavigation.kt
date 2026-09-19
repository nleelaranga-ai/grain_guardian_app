package com.vrsec.grainguardian.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.vrsec.grainguardian.ui.screens.*
import com.vrsec.grainguardian.viewmodel.GrainViewModel

@Composable
fun GrainNavigation(
    navController: NavHostController,
    viewModel: GrainViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateNext = {
                    navController.navigate(Screen.LanguageSelection.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.LanguageSelection.route) {
            LanguageSelectionScreen(
                viewModel = viewModel,
                onContinue = {
                    navController.navigate(Screen.Welcome.route)
                }
            )
        }

        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onGetStarted = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                viewModel = viewModel,
                onNavigate = { route ->
                    navController.navigate(route)
                },
                onStartDryingCheck = {
                    viewModel.startDryingCheckFlow()
                    navController.navigate(Screen.CropSelection.route)
                },
                onStartStorageCheck = {
                    viewModel.startStorageCheckFlow()
                    navController.navigate(Screen.CropSelection.route)
                }
            )
        }

        composable(Screen.CropSelection.route) {
            CropSelectionScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onContinue = {
                    navController.navigate(Screen.AssessmentSelection.route)
                }
            )
        }

        composable(Screen.AssessmentSelection.route) {
            AssessmentSelectionScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSelectDrying = {
                    viewModel.selectAssessmentMode(com.vrsec.grainguardian.data.model.AssessmentMode.DRYING_READINESS)
                    navController.navigate(Screen.ConnectProbe.route)
                },
                onSelectStorage = {
                    viewModel.selectAssessmentMode(com.vrsec.grainguardian.data.model.AssessmentMode.STORAGE_HEALTH)
                    navController.navigate(Screen.ConnectProbe.route)
                }
            )
        }

        composable(Screen.ConnectProbe.route) {
            ConnectProbeScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onConnected = {
                    navController.navigate(Screen.PrepareMeasurement.route)
                }
            )
        }

        composable(Screen.PrepareMeasurement.route) {
            PrepareMeasurementScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onConnectProbeClick = {
                    navController.navigate(Screen.ConnectProbe.route)
                },
                onStartMeasurement = {
                    navController.navigate(Screen.LiveMeasurement.route)
                }
            )
        }

        composable(Screen.LiveMeasurement.route) {
            LiveMeasurementScreen(
                viewModel = viewModel,
                onMeasurementComplete = { mode ->
                    if (mode == com.vrsec.grainguardian.data.model.AssessmentMode.DRYING_READINESS) {
                        navController.navigate(Screen.DryingResult.route) {
                            popUpTo(Screen.LiveMeasurement.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.StorageResult.route) {
                            popUpTo(Screen.LiveMeasurement.route) { inclusive = true }
                        }
                    }
                },
                onConnectProbeClick = {
                    navController.navigate(Screen.ConnectProbe.route)
                }
            )
        }

        composable(Screen.DryingResult.route) {
            DryingResultScreen(
                viewModel = viewModel,
                onBack = { navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Dashboard.route) { inclusive = true } } },
                onViewRecommendation = {
                    navController.navigate(Screen.Recommendation.route)
                },
                onSaveAndHome = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.StorageResult.route) {
            StorageResultScreen(
                viewModel = viewModel,
                onBack = { navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Dashboard.route) { inclusive = true } } },
                onViewAnalysis = {
                    navController.navigate(Screen.RiskAnalysis.route)
                },
                onSaveAndHome = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.RiskAnalysis.route) {
            RiskAnalysisScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onViewRecommendation = {
                    navController.navigate(Screen.Recommendation.route)
                }
            )
        }

        composable(Screen.Recommendation.route) {
            RecommendationScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onCheckAgain = {
                    navController.navigate(Screen.CropSelection.route) {
                        popUpTo(Screen.Dashboard.route)
                    }
                },
                onSaveInspection = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.InspectionHistory.route) {
            InspectionHistoryScreen(
                viewModel = viewModel,
                onNavigate = { route -> navController.navigate(route) },
                onInspectionClick = { id ->
                    navController.navigate(Screen.InspectionDetails.createRoute(id))
                }
            )
        }

        composable(
            route = Screen.InspectionDetails.route,
            arguments = listOf(navArgument("inspectionId") { type = NavType.LongType })
        ) { backStackEntry ->
            val inspectionId = backStackEntry.arguments?.getLong("inspectionId") ?: 0L
            InspectionDetailsScreen(
                inspectionId = inspectionId,
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Help.route) {
            HelpScreen(
                viewModel = viewModel,
                onNavigate = { route -> navController.navigate(route) }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                viewModel = viewModel,
                onNavigate = { route -> navController.navigate(route) },
                onOpenDeviceInfo = {
                    navController.navigate(Screen.DeviceInfo.route)
                }
            )
        }

        composable(Screen.DeviceInfo.route) {
            DeviceInfoScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
