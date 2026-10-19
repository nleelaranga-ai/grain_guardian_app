"use client";

import React from "react";
import { Cpu, Wifi, MapPin } from "lucide-react";

export interface DeviceItem {
  id: number;
  device_mac: string;
  device_name: string;
  farm_location: string;
  hardware_version: string;
  registered_at: string;
}

interface FleetProps {
  isTelugu: boolean;
  devices: DeviceItem[];
}

export default function FleetStatus({ isTelugu, devices }: FleetProps) {
  return (
    <div className="bg-white rounded-2xl border border-slate-200 shadow-sm p-6">
      <div className="flex items-center justify-between pb-4 border-b border-slate-100">
        <div>
          <h3 className="text-base font-bold text-slate-900 flex items-center space-x-2">
            <Cpu className="w-5 h-5 text-grain-green" />
            <span>{isTelugu ? "హార్డ్‌వేర్ ప్రోబ్ ఫ్లీట్" : "Hardware Probe Fleet Status"}</span>
          </h3>
          <p className="text-xs text-slate-500 mt-0.5">
            {isTelugu ? "నమోదైన ESP32 వైర్‌లెస్ సెన్సింగ్ ప్రోబ్స్" : "Registered ESP32 wireless multi-depth units"}
          </p>
        </div>
        <span className="text-xs font-semibold px-2.5 py-1 rounded-full bg-slate-100 text-slate-700">
          {devices.length} {isTelugu ? "యూనిట్లు" : "Probes"}
        </span>
      </div>

      <div className="mt-4 grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {devices.length === 0 ? (
          <div className="col-span-full py-6 text-center text-slate-400 text-xs">
            {isTelugu ? "ప్రస్తుతం ఏ ప్రోబ్స్ నమోదు కాలేదు." : "No registered hardware probes detected."}
          </div>
        ) : (
          devices.map((dev) => (
            <div key={dev.id} className="p-4 rounded-xl border border-slate-100 bg-slate-50/50 hover:bg-slate-50 transition">
              <div className="flex items-center justify-between">
                <div className="font-bold text-slate-900 text-sm">{dev.device_name}</div>
                <span className="flex items-center space-x-1 text-[11px] font-semibold text-emerald-600 bg-emerald-50 px-2 py-0.5 rounded-md">
                  <Wifi className="w-3 h-3" />
                  <span>Online</span>
                </span>
              </div>
              <div className="mt-2 text-xs font-mono text-slate-500">{dev.device_mac}</div>
              <div className="mt-2 flex items-center text-xs text-slate-600 space-x-1">
                <MapPin className="w-3.5 h-3.5 text-slate-400" />
                <span>{dev.farm_location}</span>
              </div>
              <div className="mt-3 pt-2 border-t border-slate-200/60 flex items-center justify-between text-[11px] text-slate-400">
                <span>Firmware {dev.hardware_version}</span>
                <span>LittleFS Ready</span>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
