"use client";

import React from "react";
import { Droplets, Thermometer, AlertTriangle, Radio } from "lucide-react";

interface OverviewProps {
  isTelugu: boolean;
  moisture: number;
  peakTemp: number;
  activeAlertsCount: number;
  activeProbesCount: number;
}

export default function GodownOverview({
  isTelugu,
  moisture,
  peakTemp,
  activeAlertsCount,
  activeProbesCount
}: OverviewProps) {
  const isMoistureSafe = moisture <= 14.0;
  const isTempSafe = peakTemp < 35.0;

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
      {/* Moisture Card */}
      <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
        <div className="flex items-center justify-between">
          <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
            {isTelugu ? "ధాన్యపు తేమ శాతం" : "Paddy Moisture"}
          </span>
          <div className={`p-2 rounded-xl ${isMoistureSafe ? "bg-emerald-50 text-emerald-600" : "bg-amber-50 text-amber-600"}`}>
            <Droplets className="w-5 h-5" />
          </div>
        </div>
        <div className="mt-3 flex items-baseline space-x-2">
          <span className="text-3xl font-extrabold text-slate-900">{moisture > 0 ? `${moisture.toFixed(1)}%` : "--"}</span>
          <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${isMoistureSafe ? "bg-emerald-100 text-emerald-800" : "bg-amber-100 text-amber-800"}`}>
            {isMoistureSafe ? (isTelugu ? "సురక్షితం" : "Safe (<14%)") : (isTelugu ? "ఎక్కువ" : "Elevated")}
          </span>
        </div>
        <p className="mt-2 text-xs text-slate-500">
          {isTelugu ? "ఆప్టిమల్ పరిమితి: 12.0% - 13.5%" : "Optimal limit: 12.0% - 13.5%"}
        </p>
      </div>

      {/* Peak Temperature */}
      <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
        <div className="flex items-center justify-between">
          <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
            {isTelugu ? "గరిష్ట ఉష్ణోగ్రత" : "Core Peak Temp"}
          </span>
          <div className={`p-2 rounded-xl ${isTempSafe ? "bg-emerald-50 text-emerald-600" : "bg-red-50 text-red-600"}`}>
            <Thermometer className="w-5 h-5" />
          </div>
        </div>
        <div className="mt-3 flex items-baseline space-x-2">
          <span className="text-3xl font-extrabold text-slate-900">{peakTemp > 0 ? `${peakTemp.toFixed(1)}°C` : "--"}</span>
          <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${isTempSafe ? "bg-emerald-100 text-emerald-800" : "bg-red-100 text-red-800"}`}>
            {isTempSafe ? (isTelugu ? "సాధారణం" : "Normal") : (isTelugu ? "హాట్‌స్పాట్" : "Hotspot")}
          </span>
        </div>
        <p className="mt-2 text-xs text-slate-500">
          {isTelugu ? "క్రిటికల్ పరిమితి: > 35.0°C" : "Critical boundary: > 35.0°C"}
        </p>
      </div>

      {/* Active Alerts */}
      <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
        <div className="flex items-center justify-between">
          <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
            {isTelugu ? "యాక్టివ్ ప్రమాదాలు" : "Active Hotspots"}
          </span>
          <div className={`p-2 rounded-xl ${activeAlertsCount === 0 ? "bg-emerald-50 text-emerald-600" : "bg-red-50 text-red-600"}`}>
            <AlertTriangle className="w-5 h-5" />
          </div>
        </div>
        <div className="mt-3 flex items-baseline space-x-2">
          <span className="text-3xl font-extrabold text-slate-900">{activeAlertsCount}</span>
          <span className={`text-xs font-semibold px-2 py-0.5 rounded-full ${activeAlertsCount === 0 ? "bg-emerald-100 text-emerald-800" : "bg-red-100 text-red-800"}`}>
            {activeAlertsCount === 0 ? (isTelugu ? "అన్నీ క్లియర్" : "All Clear") : (isTelugu ? "చర్య అవసరం" : "Requires Action")}
          </span>
        </div>
        <p className="mt-2 text-xs text-slate-500">
          {isTelugu ? "థర్మల్ గ్రేడియంట్ & తేమ వార్నింగ్‌లు" : "Thermal stratification warnings"}
        </p>
      </div>

      {/* Fleet Probe Count */}
      <div className="bg-white p-5 rounded-2xl border border-slate-200 shadow-sm">
        <div className="flex items-center justify-between">
          <span className="text-xs font-semibold text-slate-500 uppercase tracking-wider">
            {isTelugu ? "కనెక్ట్ అయిన ప్రోబ్స్" : "Connected Probes"}
          </span>
          <div className="p-2 rounded-xl bg-blue-50 text-blue-600">
            <Radio className="w-5 h-5" />
          </div>
        </div>
        <div className="mt-3 flex items-baseline space-x-2">
          <span className="text-3xl font-extrabold text-slate-900">{activeProbesCount}</span>
          <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-blue-100 text-blue-800">
            {isTelugu ? "ఆన్‌లైన్ ఫ్లీట్" : "Online Fleet"}
          </span>
        </div>
        <p className="mt-2 text-xs text-slate-500">
          {isTelugu ? "ESP32 డ్యూయల్-కోర్ హార్డ్‌వేర్ ప్రోబ్స్" : "ESP32 multi-depth telemetry units"}
        </p>
      </div>
    </div>
  );
}
