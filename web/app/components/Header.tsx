"use client";

import React from "react";
import { ShieldCheck, Activity, Globe, User } from "lucide-react";

interface HeaderProps {
  isTelugu: boolean;
  onToggleLanguage: () => void;
  systemHealthy: boolean;
}

export default function Header({ isTelugu, onToggleLanguage, systemHealthy }: HeaderProps) {
  return (
    <header className="bg-white border-b border-slate-200 sticky top-0 z-30">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 h-16 flex items-center justify-between">
        {/* Brand */}
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-grain-green to-grain-darkGreen flex items-center justify-center text-white shadow-sm">
            <ShieldCheck className="w-6 h-6" />
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <span className="text-xl font-bold tracking-tight text-grain-darkGreen">GrainGuardian</span>
              <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-grain-lightGreen text-grain-darkGreen">
                v2.0
              </span>
            </div>
            <p className="text-xs text-slate-500">
              {isTelugu ? "స్మార్ట్ ధాన్య నిల్వ ఆరోగ్య పర్యవేక్షణ" : "Smart Paddy Storage Health Intelligence"}
            </p>
          </div>
        </div>

        {/* Right Actions */}
        <div className="flex items-center space-x-4">
          {/* Health Indicator */}
          <div className="hidden sm:flex items-center space-x-2 px-3 py-1.5 rounded-lg border border-slate-200 bg-slate-50 text-xs font-medium">
            <span className={`w-2 h-2 rounded-full ${systemHealthy ? "bg-emerald-500 animate-pulse" : "bg-amber-500"}`} />
            <span className="text-slate-600">
              {isTelugu 
                ? (systemHealthy ? "క్లౌడ్ సర్వర్‌ కనెక్ట్ అయింది" : "సర్వర్‌ ఆఫ్‌లైన్") 
                : (systemHealthy ? "Backend Connected" : "Local Mode")}
            </span>
          </div>

          {/* Language Switcher */}
          <button
            onClick={onToggleLanguage}
            className="flex items-center space-x-1.5 px-3 py-1.5 rounded-lg border border-slate-200 hover:bg-slate-50 text-xs font-semibold transition"
          >
            <Globe className="w-4 h-4 text-grain-green" />
            <span>{isTelugu ? "English" : "తెలుగు"}</span>
          </button>

          {/* Farmer Account */}
          <div className="flex items-center space-x-2 pl-2 border-l border-slate-200">
            <div className="w-8 h-8 rounded-full bg-emerald-100 text-grain-darkGreen flex items-center justify-center text-sm font-bold">
              R
            </div>
            <div className="hidden md:block text-left text-xs">
              <div className="font-bold text-slate-800">farmer_ramu</div>
              <div className="text-slate-500">Bapatla Godown #2</div>
            </div>
          </div>
        </div>
      </div>
    </header>
  );
}
