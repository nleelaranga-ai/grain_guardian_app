"use client";

import React from "react";
import { Layers, Flame, CheckCircle2 } from "lucide-react";

interface StratificationProps {
  isTelugu: boolean;
  t1: number;
  t2: number;
  t3: number;
  ambientHumidity: number;
}

export default function StratificationChart({
  isTelugu,
  t1,
  t2,
  t3,
  ambientHumidity
}: StratificationProps) {
  const tMax = Math.max(t1, t2, t3);
  const tMin = Math.min(t1, t2, t3);
  const tgi = Number((tMax - tMin).toFixed(2));
  const isHotspot = tgi >= 3.0 || tMax >= 35.0;

  return (
    <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-sm">
      <div className="flex items-center justify-between pb-4 border-b border-slate-100">
        <div>
          <h3 className="text-base font-bold text-slate-900 flex items-center space-x-2">
            <Layers className="w-5 h-5 text-grain-green" />
            <span>{isTelugu ? "ధాన్యపు కుప్ప లోతు & ఉష్ణోగ్రత పొరలు (Stratification)" : "Multi-Depth Thermal Stratification"}</span>
          </h3>
          <p className="text-xs text-slate-500 mt-0.5">
            {isTelugu ? "3-పాయింట్ డిజిటల్ DS18B20 ప్రోబ్ గ్రేడియంట్ విశ్లేషణ" : "3-point digital DS18B20 probe gradient profiling"}
          </p>
        </div>

        {/* TGI Indicator */}
        <div className={`px-3 py-1 rounded-xl flex items-center space-x-2 text-xs font-bold ${
          isHotspot ? "bg-red-50 text-red-700 border border-red-200" : "bg-emerald-50 text-emerald-700 border border-emerald-200"
        }`}>
          {isHotspot ? <Flame className="w-4 h-4 text-red-600 animate-bounce" /> : <CheckCircle2 className="w-4 h-4 text-emerald-600" />}
          <span>TGI: {tgi > 0 ? `${tgi}°C` : "--"}</span>
          <span className="text-[10px] font-normal hidden sm:inline">
            ({isHotspot ? (isTelugu ? "ప్రమాదకర గ్రేడియంట్" : "Hotspot Alert") : (isTelugu ? "సహజ పంపిణీ" : "Equilibrium")})
          </span>
        </div>
      </div>

      {/* Cross-section visualization */}
      <div className="mt-6 space-y-4">
        {/* Layer 1: Top (10cm) */}
        <div className="p-4 rounded-xl border border-slate-100 bg-gradient-to-r from-amber-50/50 to-emerald-50/50 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="w-8 h-8 rounded-lg bg-amber-100 text-amber-800 flex items-center justify-center font-bold text-xs">
              T1
            </div>
            <div>
              <div className="text-xs font-bold text-slate-800">
                {isTelugu ? "పై పొర (10 cm లోతు)" : "Top Layer (10 cm depth)"}
              </div>
              <div className="text-[11px] text-slate-500">
                {isTelugu ? "వాతావరణ ఉష్ణోగ్రత ప్రభావం" : "Surface & aeration boundary"}
              </div>
            </div>
          </div>
          <div className="text-right">
            <div className="text-lg font-extrabold text-slate-900">{t1 > 0 ? `${t1.toFixed(1)}°C` : "--"}</div>
            <div className="text-[10px] text-slate-400">DS18B20 Top</div>
          </div>
        </div>

        {/* Layer 2: Core Middle (40cm) */}
        <div className="p-4 rounded-xl border border-slate-100 bg-gradient-to-r from-emerald-50/50 to-blue-50/50 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="w-8 h-8 rounded-lg bg-emerald-100 text-emerald-800 flex items-center justify-center font-bold text-xs">
              T2
            </div>
            <div>
              <div className="text-xs font-bold text-slate-800">
                {isTelugu ? "మధ్య భాగం (40 cm కోర్ లోతు)" : "Core Middle (40 cm depth)"}
              </div>
              <div className="text-[11px] text-slate-500">
                {isTelugu ? "ధాన్యం కేంద్ర భాగం" : "Primary heat retention core"}
              </div>
            </div>
          </div>
          <div className="text-right">
            <div className="text-lg font-extrabold text-slate-900">{t2 > 0 ? `${t2.toFixed(1)}°C` : "--"}</div>
            <div className="text-[10px] text-slate-400">DS18B20 Core</div>
          </div>
        </div>

        {/* Layer 3: Base Bottom (80cm) */}
        <div className="p-4 rounded-xl border border-slate-100 bg-gradient-to-r from-blue-50/50 to-indigo-50/50 flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="w-8 h-8 rounded-lg bg-indigo-100 text-indigo-800 flex items-center justify-center font-bold text-xs">
              T3
            </div>
            <div>
              <div className="text-xs font-bold text-slate-800">
                {isTelugu ? "దిగువ పొర (80 cm నేల మట్టం)" : "Bottom Base (80 cm depth)"}
              </div>
              <div className="text-[11px] text-slate-500">
                {isTelugu ? "తేమ ఘనీభవనం & శిలీంధ్ర ముప్పు" : "Floor condensation & fungus zone"}
              </div>
            </div>
          </div>
          <div className="text-right">
            <div className="text-lg font-extrabold text-slate-900">{t3 > 0 ? `${t3.toFixed(1)}°C` : "--"}</div>
            <div className="text-[10px] text-slate-400">DS18B20 Base</div>
          </div>
        </div>
      </div>

      {/* Ambient Conditions footer */}
      <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
        <span>{isTelugu ? "గాలి సాపేక్ష తేమ (RH):" : "Ambient Godown Humidity (RH):"}</span>
        <span className="font-bold text-slate-700">{ambientHumidity > 0 ? `${ambientHumidity.toFixed(1)}% RH` : "64.0% RH"}</span>
      </div>
    </div>
  );
}
