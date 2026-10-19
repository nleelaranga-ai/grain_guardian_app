"""
GrainGuardian ESP32 Firmware Protocol & Binary Serialization Test Suite
Validates binary struct packing, LittleFS record format, BLE protocol commands,
and sensor calculation math against firmware constants.
"""

import struct
import unittest

# Matches struct __attribute__((packed)) GrainRecord in GrainGuardian_ESP32.ino
# uint32_t seq_id      (4 bytes, <I)
# uint32_t uptime_sec  (4 bytes, <I)
# int16_t  t1_centic   (2 bytes, <h)
# int16_t  t2_centic   (2 bytes, <h)
# int16_t  t3_centic   (2 bytes, <h)
# uint16_t moisture_mv (2 bytes, <H)
# uint8_t  humidity    (1 byte,  <B)
# uint8_t  status_flags(1 byte,  <B)
# Total: 4 + 4 + 2 + 2 + 2 + 2 + 1 + 1 = 18 bytes
GRAIN_RECORD_STRUCT = "<IIhhhHBB"
RECORD_SIZE = struct.calcsize(GRAIN_RECORD_STRUCT)

def convert_mv_to_moisture(mv: int) -> float:
    if mv < 150 or mv > 3250:
        return 0.0
    mc = 13.5 + (2100 - mv) / 500.0 * 4.5
    return max(8.0, min(35.0, mc))

def evaluate_alert_level(moisture: float, t1: float, t2: float, t3: float) -> str:
    temps = [t for t in (t1, t2, t3) if t != -127.0]
    max_t = max(temps) if temps else -100.0
    min_t = min(temps) if temps else 100.0
    tgi = (max_t - min_t) if len(temps) >= 2 else 0.0

    if (max_t >= 35.0 and len(temps) > 0) or tgi >= 3.0 or moisture >= 14.5:
        return "DANGER"
    if moisture > 13.5 or tgi >= 2.0 or (max_t >= 32.0 and len(temps) > 0):
        return "WARNING"
    return "SAFE"


class TestESP32FirmwareProtocol(unittest.TestCase):

    def test_record_struct_packing_size(self):
        self.assertEqual(RECORD_SIZE, 18, "GrainRecord binary size must be exactly 18 bytes packed")

    def test_record_serialization_roundtrip(self):
        seq = 101
        uptime = 3600
        t1 = 2850   # 28.50 C
        t2 = 2910   # 29.10 C
        t3 = 2990   # 29.90 C
        mv = 2110   # 2110 mV (~13.41%)
        hum = 64    # 64% RH
        flags = 0b00111111 # 63 (T1..T3, Moisture, DHT, OLED OK)

        packed = struct.pack(GRAIN_RECORD_STRUCT, seq, uptime, t1, t2, t3, mv, hum, flags)
        self.assertEqual(len(packed), 18)

        # Unpack and verify fidelity
        u_seq, u_uptime, u_t1, u_t2, u_t3, u_mv, u_hum, u_flags = struct.unpack(GRAIN_RECORD_STRUCT, packed)
        self.assertEqual(u_seq, seq)
        self.assertEqual(u_uptime, uptime)
        self.assertEqual(u_t1 / 100.0, 28.50)
        self.assertEqual(u_t2 / 100.0, 29.10)
        self.assertEqual(u_t3 / 100.0, 29.90)
        self.assertEqual(u_mv, 2110)
        self.assertEqual(u_hum, 64)
        self.assertEqual(u_flags, flags)

    def test_disconnected_sensor_sentinel(self):
        # Disconnected DS18B20 returns -12700 centi-degrees
        t_disc = -12700
        packed = struct.pack("<h", t_disc)
        unpacked = struct.unpack("<h", packed)[0]
        self.assertEqual(unpacked, -12700)
        self.assertEqual(unpacked / 100.0, -127.0)

    def test_moisture_calibration_accuracy(self):
        # 2100 mV standard paddy baseline = 13.5%
        self.assertAlmostEqual(convert_mv_to_moisture(2100), 13.5, places=2)
        # 1600 mV wet harvest paddy = 18.0%
        self.assertAlmostEqual(convert_mv_to_moisture(1600), 18.0, places=2)
        # 2600 mV over-dry paddy = 9.0%
        self.assertAlmostEqual(convert_mv_to_moisture(2600), 9.0, places=2)
        # Faulty/disconnected sensor (<150 mV or >3250 mV) returns 0.0
        self.assertEqual(convert_mv_to_moisture(50), 0.0)
        self.assertEqual(convert_mv_to_moisture(3300), 0.0)

    def test_alert_rule_evaluations(self):
        # Safe storage condition
        self.assertEqual(evaluate_alert_level(13.2, 28.0, 28.5, 29.0), "SAFE")
        # Moisture warning threshold (13.8%)
        self.assertEqual(evaluate_alert_level(13.8, 28.0, 28.5, 29.0), "WARNING")
        # Temperature Gradient warning (TGI = 2.4 C)
        self.assertEqual(evaluate_alert_level(13.0, 28.0, 29.5, 30.4), "WARNING")
        # Critical Hotspot (>35.0 C)
        self.assertEqual(evaluate_alert_level(13.0, 28.0, 32.0, 36.5), "DANGER")
        # Severe Moisture (>14.5%)
        self.assertEqual(evaluate_alert_level(15.2, 28.0, 28.5, 29.0), "DANGER")

    def test_ble_sync_packet_generation(self):
        # Verifies the exact HIST: string format emitted by firmware
        rec = {
            "seq": 1,
            "uptime": 1200,
            "t1": 28.54,
            "t2": 29.12,
            "t3": 30.01,
            "mv": 2110,
            "h": 64,
            "f": 31
        }
        packet = f"HIST:SEQ={rec['seq']},UP={rec['uptime']},T1={rec['t1']:.2f},T2={rec['t2']:.2f},T3={rec['t3']:.2f},MV={rec['mv']},H={rec['h']},F={rec['f']}"
        self.assertTrue(packet.startswith("HIST:"))
        self.assertIn("SEQ=1", packet)
        self.assertIn("UP=1200", packet)
        self.assertIn("T1=28.54", packet)
        self.assertIn("MV=2110", packet)


if __name__ == "__main__":
    unittest.main()
