# GrainGuardian — Database Schema & Data Dictionary v2.0

**ORM Framework:** Android Room 2.5.1 / KSP  
**Target Engine:** SQLite 3 (Embedded on Android Device)  
**Database Name:** `grainguardian_database`  
**Current Version:** 1  

---

## 1. Table: `inspections`

The primary local table storing post-harvest inspections, spot drying measurements, and continuous storage logs.

### Schema Definition (DDL)
```sql
CREATE TABLE IF NOT EXISTS `inspections` (
    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    `cropName` TEXT NOT NULL,
    `mode` TEXT NOT NULL,
    `moisture` REAL NOT NULL,
    `t1Top` REAL NOT NULL,
    `t2Middle` REAL NOT NULL,
    `t3Bottom` REAL NOT NULL,
    `humidity` REAL NOT NULL,
    `status` TEXT NOT NULL,
    `riskScore` INTEGER NOT NULL,
    `dominantRisk` TEXT NOT NULL,
    `recommendationsJson` TEXT NOT NULL,
    `timestamp` INTEGER NOT NULL,
    `formattedDate` TEXT NOT NULL
);
```

### Data Dictionary

| Column Name | SQLite Type | Kotlin Type | Nullable | Description & Domain Rules |
| :--- | :---: | :---: | :---: | :--- |
| `id` | `INTEGER` | `Long` | NO | Auto-incrementing primary key. |
| `cropName` | `TEXT` | `String` | NO | Target grain: `"Paddy"`, `"Maize"`, `"Wheat"`, `"Pulses"`. |
| `mode` | `TEXT` | `String` | NO | Operating mode: `"Drying Check"` (Mode A) or `"Storage Check"` (Mode B). |
| `moisture` | `REAL` | `Float` | NO | Calibrated moisture percentage (% wet basis, range: 8.0 to 32.0). |
| `t1Top` | `REAL` | `Float` | NO | Top layer probe temperature at 10 cm depth (°C). |
| `t2Middle` | `REAL` | `Float` | NO | Middle layer core probe temperature at 40 cm depth (°C). |
| `t3Bottom` | `REAL` | `Float` | NO | Bottom layer base probe temperature at 80 cm depth (°C). |
| `humidity` | `REAL` | `Float` | NO | Ambient warehouse air relative humidity (range: 0 to 100% RH). |
| `status` | `TEXT` | `String` | NO | Agronomic risk state: `"SAFE"`, `"WARNING"`, `"HIGH_RISK"`. |
| `riskScore` | `INTEGER` | `Int` | NO | Biological Degradation Risk Score (BDRS, range: 0 to 100). |
| `dominantRisk` | `TEXT` | `String` | NO | Natural language explanation of primary limiting factor. |
| `recommendationsJson` | `TEXT` | `String` | NO | Actionable advisory steps separated by pipe (`\|`) delimiter. |
| `timestamp` | `INTEGER` | `Long` | NO | Epoch milliseconds when the reading was taken. |
| `formattedDate` | `TEXT` | `String` | NO | Human-readable timestamp (e.g. `"19 Oct 2026, 12:04 AM"`). |

---

## 2. Queries & Data Access Contract (`GrainInspectionDao.kt`)

```kotlin
@Dao
interface GrainInspectionDao {
    @Query("SELECT * FROM inspections ORDER BY timestamp DESC")
    fun getAllInspections(): Flow<List<GrainInspectionEntity>>

    @Query("SELECT * FROM inspections WHERE id = :id LIMIT 1")
    suspend fun getInspectionById(id: Long): GrainInspectionEntity?

    @Query("SELECT * FROM inspections ORDER BY timestamp DESC LIMIT 1")
    fun getLatestInspection(): Flow<GrainInspectionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInspection(inspection: GrainInspectionEntity): Long

    @Delete
    suspend fun deleteInspection(inspection: GrainInspectionEntity)

    @Query("DELETE FROM inspections")
    suspend fun clearAll()
}
```

---

## 3. Deduplication Strategy for Historical Batch Sync

When synchronizing historical batches from the ESP32's LittleFS partition:
1. The record uptime $U_{\text{record}}$ is converted to wall-clock epoch timestamp $T_{\text{record}}$.
2. Before insertion, the repository executes a temporal deduplication check:
   ```sql
   SELECT COUNT(*) FROM inspections WHERE timestamp BETWEEN (:t - 30000) AND (:t + 30000);
   ```
3. If a record already exists within the 30-second window, the import is skipped, preventing duplicate records from multiple sync events.
