"use client";

import React from "react";
import { AlertCircle, AlertTriangle, CheckCircle, Clock } from "lucide-react";

export interface RiskEventItem {
  event_id: number;
  device_mac: string;
  crop_name: string;
  tgi: number;
  peak_temp: number;
  status: string;
  headline_en: string;
  headline_te: string;
  created_at: string;
  acknowledged: boolean;
}

interface AlertsProps {
  isTelugu: boolean;
  events: RiskEventItem[];
  onAcknowledge: (eventId: number) => void;
}

export default function RiskAlertsTable({
  isTelugu,
  events,
  onAcknowledge
}: AlertsProps) {
  return (
    <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
      <div className="p-6 pb-4 border-b border-slate-100 flex items-center justify-between">
        <div>
          <h3 className="text-base font-bold text-slate-900 flex items-center space-x-2">
            <AlertCircle className="w-5 h-5 text-amber-500" />
            <span>{isTelugu ? "గోడౌన్ ప్రమాద హెచ్చరికల లాగ్" : "Storage Risk & Hotspot Incident Log"}</span>
          </h3>
          <p className="text-xs text-slate-500 mt-0.5">
            {isTelugu ? "లైవ్ టెలిమెట్రీ నుండి గుర్తించబడిన థర్మల్ మరియు తేమ ఈవెంట్లు" : "Real-time automated incident detection from IoT probe telemetry"}
          </p>
        </div>
        <span className="text-xs font-semibold px-2.5 py-1 rounded-full bg-slate-100 text-slate-700">
          {events.length} {isTelugu ? "ఈవెంట్లు" : "Incidents"}
        </span>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full text-left text-xs">
          <thead className="bg-slate-50/80 text-slate-500 uppercase tracking-wider font-semibold border-b border-slate-100">
            <tr>
              <th className="py-3 px-4">{isTelugu ? "స్థితి" : "Severity"}</th>
              <th className="py-3 px-4">{isTelugu ? "హెచ్చరిక వివరాలు" : "Incident Headline"}</th>
              <th className="py-3 px-4">{isTelugu ? "గరిష్ట ఉష్ణోగ్రత / TGI" : "Peak Temp / TGI"}</th>
              <th className="py-3 px-4">{isTelugu ? "పరికరం ID" : "Device MAC"}</th>
              <th className="py-3 px-4">{isTelugu ? "సమయం" : "Timestamp"}</th>
              <th className="py-3 px-4 text-right">{isTelugu ? "చర్య" : "Action"}</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100 text-slate-700">
            {events.length === 0 ? (
              <tr>
                <td colSpan={6} className="py-8 text-center text-slate-400">
                  <CheckCircle className="w-8 h-8 mx-auto text-emerald-500 mb-2" />
                  <p className="text-sm font-medium text-slate-600">
                    {isTelugu ? "ఎటువంటి ప్రమాద హెచ్చరికలు లేవు. ధాన్యం సురక్షితంగా ఉంది!" : "No active storage hotspots detected. All grain piles safe!"}
                  </p>
                </td>
              </tr>
            ) : (
              events.map((e) => {
                const isHigh = e.status === "HIGH_RISK";
                return (
                  <tr key={e.event_id} className={`hover:bg-slate-50/60 transition ${e.acknowledged ? "opacity-60" : ""}`}>
                    {/* Status Badge */}
                    <td className="py-3 px-4">
                      <span className={`inline-flex items-center px-2 py-0.5 rounded-full font-bold text-[11px] ${
                        isHigh ? "bg-red-100 text-red-800" : "bg-amber-100 text-amber-800"
                      }`}>
                        {isHigh ? <AlertCircle className="w-3 h-3 mr-1" /> : <AlertTriangle className="w-3 h-3 mr-1" />}
                        {isHigh ? (isTelugu ? "అత్యధిక ప్రమాదం" : "HIGH RISK") : (isTelugu ? "హెచ్చరిక" : "WARNING")}
                      </span>
                    </td>

                    {/* Headline */}
                    <td className="py-3 px-4 font-semibold text-slate-900">
                      <div>{isTelugu ? e.headline_te : e.headline_en}</div>
                      <div className="text-[10px] text-slate-400 font-normal">Paddy Storage Lot • Bapatla Godown</div>
                    </td>

                    {/* Metrics */}
                    <td className="py-3 px-4">
                      <div className="font-bold text-slate-800">{e.peak_temp.toFixed(1)}°C</div>
                      <div className="text-[10px] text-slate-500">TGI: +{e.tgi.toFixed(1)}°C</div>
                    </td>

                    {/* MAC */}
                    <td className="py-3 px-4 font-mono text-[11px] text-slate-600">
                      {e.device_mac}
                    </td>

                    {/* Timestamp */}
                    <td className="py-3 px-4 text-slate-500">
                      <div className="flex items-center space-x-1">
                        <Clock className="w-3 h-3" />
                        <span>{new Date(e.created_at).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                      </div>
                    </td>

                    {/* Action */}
                    <td className="py-3 px-4 text-right">
                      {e.acknowledged ? (
                        <span className="text-[11px] text-emerald-600 font-bold flex items-center justify-end space-x-1">
                          <CheckCircle className="w-3.5 h-3.5" />
                          <span>{isTelugu ? "చూశారు" : "Resolved"}</span>
                        </span>
                      ) : (
                        <button
                          onClick={() => onAcknowledge(e.event_id)}
                          className="px-3 py-1 rounded-lg bg-slate-900 hover:bg-slate-800 text-white font-semibold text-[11px] transition"
                        >
                          {isTelugu ? "నిర్ధారించు" : "Acknowledge"}
                        </button>
                      )}
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
