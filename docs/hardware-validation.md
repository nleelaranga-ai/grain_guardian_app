# GrainGuardian — Hardware Validation & Calibration Protocols v2.0

**Scope:** Laboratory calibration procedures, sensor validation standards, electrical wiring constraints, and physical limitations.

---

## 1. Multi-Sensor Electrical Wiring & Pin Constraints

```
ESP32 DevKit V1 (30-Pin)
├── 3V3 Rail ──> SEN0193 VCC, 3x DS18B20 VDD, 4.7kΩ Pull-Up Resistor
├── GND Rail ──> Common System Ground (Sensors, Battery Protection Ground)
├── GPIO 4   ──> DS18B20 1-Wire Digital Bus (with 4.7kΩ Pull-Up to 3.3V)
├── GPIO 34  ──> SEN0193 Analog Output (ADC1_CH6 - Hardware Isolated from BLE)
└── GPIO 27  ──> DHT11 Single-Bus Data Pin (with internal/external 10kΩ Pull-Up)
```

### Critical Silicon Design Rule: ADC1 Exclusivity
- **The Issue:** The ESP32 contains two analog-to-digital converters: ADC1 and ADC2. ADC2 pins (`GPIO 0, 2, 4, 12, 13, 14, 15, 25, 26, 27`) share analog hardware with the Wi-Fi and Bluetooth baseband radio. Whenever BLE is transmitting or scanning, ADC2 returns corrupted or null readings.
- **The Solution:** The SEN0193 moisture sensor is assigned strictly to **`GPIO 34`**, which belongs to **ADC1** (`ADC1_CH6`). ADC1 operates completely independently of the Bluetooth radio.

---

## 2. Capacitive Paddy Moisture Calibration Protocol

The SEN0193 capacitive sensor measures fringe-field capacitance rather than DC resistance. Because dielectric permittivity $\epsilon_r$ of dry paddy is $\sim 3.5$ and water is $\sim 80.4$, higher moisture content increases sensor capacitance and decreases output voltage.

### Calibration Formula (Implemented in BleManager.kt)
$$M = 13.5 + \left( \frac{2100 - V_{\text{out}}}{500} \right) \times 4.5$$

- $2100\text{ mV} \rightarrow 13.5\%$ (Bureau of Indian Standards safe paddy storage limit).
- $2600\text{ mV} \rightarrow 10.0\%$ (Over-dry grain / milling fracture hazard).
- $1600\text{ mV} \rightarrow 18.0\%$ (Wet harvest grain / aflatoxin hazard).

### Step-by-Step ISO 712 Air-Oven Verification Protocol
For formal laboratory verification:
1. Prepare 5 paddy batches at 11%, 13.5%, 15%, 17%, and 20% moisture.
2. Determine true moisture using the **Standard Air-Oven Method (IS 4333 Part II / ISO 712)**:
   - Weigh 10 g sample ($W_1$).
   - Dry at $130^\circ\text{C} \pm 2^\circ\text{C}$ for 16 hours.
   - Cool in desiccator and re-weigh ($W_2$):
     $$M_{\text{oven}} = \frac{W_1 - W_2}{W_1} \times 100\%$$
3. Standardize insertion density by filling a 1-liter graduated cylinder with paddy and dropping from 5 cm height 3 times before inserting the SEN0193 probe.
4. Record ADC voltage over 10 repeated insertions and fit polynomial regression coefficients.

---

## 3. DS18B20 Multi-Depth Temperature Validation

All three waterproof DS18B20 stainless steel probes share `GPIO 4`:
1. **Ice Bath Test:** Submerge all three probes in stirred ice water at $0.0^\circ\text{C} \pm 0.5^\circ\text{C}$. All three probes must read within $\pm 0.5^\circ\text{C}$.
2. **Warm Bath Test:** Submerge in heated water at $40.0^\circ\text{C} \pm 0.5^\circ\text{C}$. Verify consistency across all three sensors.
3. **Thermal Gradient Verification:** Warm the bottom probe ($T_3$) with a heating pad while keeping $T_1$ at room temperature. Verify that $\text{TGI} = T_3 - T_1$ triggers the hotspot warning in the application when $\text{TGI} > 3.0^\circ\text{C}$.
