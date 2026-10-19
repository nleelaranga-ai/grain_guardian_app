"use client";

import React, { useState, useEffect } from "react";
import Header from "./components/Header";
import GodownOverview from "./components/GodownOverview";
import StratificationChart from "./components/StratificationChart";
import RiskAlertsTable, { RiskEventItem } from "./components/RiskAlertsTable";
import FleetStatus, { DeviceItem } from "./components/FleetStatus";

export default function DashboardPage() {
  const [isTelugu, setIsTelugu] = useState<boolean>(false);
  const [systemHealthy, setSystemHealthy] = useState<boolean>(true);

  // Live telemetry state
  const [moisture, setMoisture] = useState<number>(13.4);
  const [t1, setT1] = useState<number>(25.38);
  const [t2, setT2] = useState<number>(25.56);
  const [t3, setT3] = useState<number>(25.94);
  const [humidity, setHumidity] = useState<number>(64.4);

  // Fleet and risks
  const [devices, setDevices] = useState<DeviceItem[]>([
    {
      id: 1,
      device_mac: "24:6F:28:AB:CD:EF",
      device_name: "GrainGuardian-01",
      farm_location: "Bapatla Godown Shed 2",
      hardware_version: "v2.0",
      registered_at: new Date().toISOString()
    }
  ]);

  const [riskEvents, setRiskEvents] = useState<RiskEventItem[]>([
    {
      event_id: 101,
      device_mac: "24:6F:28:AB:CD:EF",
      crop_name: "Paddy",
      tgi: 3.8,
      peak_temp: 36.2,
      status: "WARNING",
      headline_en: "WARNING - THERMAL STRATIFICATION DETECTED",
      headline_te: "హెచ్చరిక - ధాన్యంలో వేడి మరియు తేమ పెరుగుతోంది",
      created_at: new Date().toISOString(),
      acknowledged: false
    }
  ]);

  const apiUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8000";

  // Fetch real data from backend
  const fetchDashboardData = async () => {
    try {
      // 1. Health
      const healthRes = await fetch(`${apiUrl}/api/v1/health`, { cache: "no-store" });
      if (healthRes.ok) {
        setSystemHealthy(true);
      } else {
        setSystemHealthy(false);
      }

      // 2. Latest Telemetry
      const telRes = await fetch(`${apiUrl}/api/v1/telemetry/latest`, { cache: "no-store" });
      if (telRes.ok) {
        const telData = await telRes.json();
        setMoisture(telData.moisture || 13.4);
        setT1(telData.t1_top || 25.38);
        setT2(telData.t2_middle || 25.56);
        setT3(telData.t3_bottom || 25.94);
        setHumidity(telData.ambient_humidity || 64.4);
      }

      // 3. Risks
      const riskRes = await fetch(`${apiUrl}/api/v1/storage/risks?limit=20`, { cache: "no-store" });
      if (riskRes.ok) {
        const rData = await riskRes.json();
        if (rData.events && rData.events.length > 0) {
          setRiskEvents(rData.events);
        }
      }

      // 4. Devices
      const devRes = await fetch(`${apiUrl}/api/v1/devices`, { cache: "no-store" });
      if (devRes.ok) {
        const dData = await devRes.json();
        if (dData.length > 0) {
          setDevices(dData);
        }
      }
    } catch {
      // Keep running smoothly offline
      setSystemHealthy(false);
    }
  };

  useEffect(() => {
    fetchDashboardData();
    const interval = setInterval(fetchDashboardData, 10000);
    return () => clearInterval(interval);
  }, []);

  const handleAcknowledge = async (eventId: number) => {
    try {
      await fetch(`${apiUrl}/api/v1/storage/risks/${eventId}/acknowledge`, { method: "POST" });
    } catch {
      // Fallback local update
    }
    setRiskEvents((prev) =>
      prev.map((e) => (e.event_id === eventId ? { ...e, acknowledged: true } : e))
    );
  };

  const peakTemp = Math.max(t1, t2, t3);
  const unacknowledgedCount = riskEvents.filter((e) => !e.acknowledged).length;

  return (
    <div className="min-h-screen flex flex-col bg-slate-50/50">
      <Header
        isTelugu={isTelugu}
        onToggleLanguage={() => setIsTelugu(!isTelugu)}
        systemHealthy={systemHealthy}
      />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
        {/* Page Title & Cluster Info */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h1 className="text-2xl font-black text-slate-900 tracking-tight">
              {isTelugu ? "కేంద్ర ధాన్య నిల్వ ఇంటెలిజెన్స్ డ్యాష్‌బోర్డ్" : "Central Grain Storage Health Intelligence"}
            </h1>
            <p className="text-xs text-slate-500 mt-1">
              {isTelugu ? "బాపట్ల క్లస్టర్ గోడౌన్ #2 • వరి రకం BPT 5204 (సాంబ మసూరి)" : "Bapatla Cluster Warehouse #2 • Paddy Variety BPT 5204 (Samba Masuri)"}
            </p>
          </div>
          <button
            onClick={fetchDashboardData}
            className="self-start sm:self-auto px-4 py-2 rounded-xl bg-white border border-slate-200 hover:bg-slate-50 text-xs font-bold text-slate-700 shadow-sm transition"
          >
            {isTelugu ? "డేటా రిఫ్రెష్ చేయండి" : "Refresh Telemetry"}
          </button>
        </div>

        {/* 1. Overview KPI Cards */}
        <GodownOverview
          isTelugu={isTelugu}
          moisture={moisture}
          peakTemp={peakTemp}
          activeAlertsCount={unacknowledgedCount}
          activeProbesCount={devices.length}
        />

        {/* 2. Middle Row: Multi-Depth Stratification & Fleet */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          <div className="lg:col-span-2">
            <StratificationChart
              isTelugu={isTelugu}
              t1={t1}
              t2={t2}
              t3={t3}
              ambientHumidity={humidity}
            />
          </div>
          <div className="lg:col-span-1">
            <FleetStatus isTelugu={isTelugu} devices={devices} />
          </div>
        </div>

        {/* 3. Incident & Risk Alerts Table */}
        <RiskAlertsTable
          isTelugu={isTelugu}
          events={riskEvents}
          onAcknowledge={handleAcknowledge}
        />
      </main>

      {/* Footer */}
      <footer className="border-t border-slate-200 bg-white py-6 mt-12 text-center text-xs text-slate-500">
        <p>GrainGuardian 2.0 • Smart Paddy Post-Harvest Readiness & Storage Health Intelligence System</p>
        <p className="text-[11px] text-slate-400 mt-1">EPICS Technical Innovation & Edge IoT Agritech Architecture</p>
      </footer>
    </div>
  );
}
