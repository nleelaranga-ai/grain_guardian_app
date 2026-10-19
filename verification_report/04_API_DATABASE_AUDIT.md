# 04 — API & DATABASE AUDIT REPORT

**Scope:** Inspection of networking interfaces, external APIs, cloud integrations, and local database persistence schemas.

---

## 1. Network API & Cloud Backend Audit

### Finding: Zero External HTTP/REST APIs Implemented
A comprehensive search across all application packages, configuration files, and Gradle build definitions confirms that **no external HTTP client, cloud backend, or remote REST API is connected or implemented in the Android application**.

### Specific Evidence
1. **Network Dependencies:**
   - In [`app/build.gradle.kts`](file:///C:/Users/LEELA%20RANGA%20PRASAD/.gemini/antigravity/scratch/GrainGuardian/app/build.gradle.kts), dependencies were audited. There is **no inclusion** of:
     - Square Retrofit (`com.squareup.retrofit2`)
     - Square OkHttp (`com.squareup.okhttp3`)
     - Ktor Client (`io.ktor:ktor-client`)
     - Volley / Apache HTTP
     - Google Firebase (Auth, Firestore, Realtime DB, Cloud Messaging)
     - Supabase Client (`io.github.jan-tennert.supabase`)
2. **Manifest Permissions:**
   - In [`AndroidManifest.xml`](file:///C:/Users/LEELA%20RANGA%20PRASAD/.gemini/antigravity/scratch/GrainGuardian/app/src/main/AndroidManifest.xml), the standard Android network permission:
     ```xml
     <uses-permission android:name="android.permission.INTERNET" />
     ```
     is **NOT PRESENT**. The app cannot make internet connections even if code attempted to do so.
3. **Repository Analysis:**
   - `InspectionRepository.kt` connects strictly to:
     - Local `GrainInspectionDao` (Room Database)
     - Local `BleManager` (Bluetooth Low Energy)
   - There are zero network calls, API token headers, or remote sync queues.

### Discrepancy Note with Proposal Slides
The original presentation slide deck outlined a Python FastAPI server and Supabase PostgreSQL instance. **These components are NOT present in the active mobile repository.** The project is an **Offline Edge System**.

---

## 2. Local Database Schema & Persistence Audit

### Engine & Framework
- **Database Engine:** SQLite (Embedded on Android Device)
- **Object Relational Mapping (ORM):** Android Room 2.5.1
- **Annotation Processing Tool:** Google Kotlin Symbol Processing (KSP 1.8.20-1.0.11)
- **Database File Name:** `grainguardian_database`
- **Schema Version:** 1 (`exportSchema = false`)

### Entity Definition: `inspections` Table
The single persistent entity is defined in [`GrainInspectionEntity.kt`](file:///C:/Users/LEELA%20RANGA%20PRASAD/.gemini/antigravity/scratch/GrainGuardian/app/src/main/java/com/vrsec/grainguardian/data/model/GrainInspectionEntity.kt):

| Column Name | SQLite Data Type | Kotlin Type | Default Value | Agronomic Role & Constraints |
| :--- | :---: | :---: | :---: | :--- |
| `id` | `INTEGER` | `Long` | `0` (Auto) | Primary Key (`autoGenerate = true`). Unique record identifier. |
| `cropName` | `TEXT` | `String` | `"Paddy"` | Target commodity (Paddy, Maize, Wheat, Pulses). |
| `mode` | `TEXT` | `String` | `"Storage Check"` | Inspection workflow: `"Drying Check"` or `"Storage Check"`. |
| `moisture` | `REAL` | `Float` | `13.2f` | Measured moisture content (% wet basis). |
| `t1Top` | `REAL` | `Float` | `29.0f` | Top sensor temperature at 10 cm depth (°C). |
| `t2Middle` | `REAL` | `Float` | `31.0f` | Middle sensor core temperature at 40 cm depth (°C). |
| `t3Bottom` | `REAL` | `Float` | `32.0f` | Bottom sensor temperature at 80 cm depth (°C). |
| `humidity` | `REAL` | `Float` | `68.0f` | Ambient storage air relative humidity (% RH). |
| `status` | `TEXT` | `String` | `"SAFE"` | Agronomic classification: `"SAFE"`, `"WARNING"`, `"HIGH_RISK"`. |
| `riskScore` | `INTEGER` | `Int` | `18` | Biological Degradation Risk Score (BDRS, 0–100 scale). |
| `dominantRisk` | `TEXT` | `String` | `"Moisture Optimal"` | Primary risk driver identified by Decision Engine. |
| `recommendationsJson` | `TEXT` | `String` | Pipe-delimited | Actionable advice strings separated by `\|` delimiter. |
| `timestamp` | `INTEGER` | `Long` | Current time ms | Unix epoch timestamp for historical sorting. |
| `formattedDate` | `TEXT` | `String` | Localized string | Human-readable timestamp (e.g., `"19 Oct 2026, 12:04 AM"`). |

---

## 3. Data Access Object (DAO) Operations

All CRUD queries are defined in [`GrainInspectionDao.kt`](file:///C:/Users/LEELA%20RANGA%20PRASAD/.gemini/antigravity/scratch/GrainGuardian/app/src/main/java/com/vrsec/grainguardian/data/database/GrainInspectionDao.kt):

```kotlin
@Dao
interface GrainInspectionDao {
    // 1. Reactive stream of all inspections for History Screen
    @Query("SELECT * FROM inspections ORDER BY timestamp DESC")
    fun getAllInspections(): Flow<List<GrainInspectionEntity>>

    // 2. Fetch specific inspection record for Detail Screen
    @Query("SELECT * FROM inspections WHERE id = :id LIMIT 1")
    suspend fun getInspectionById(id: Long): GrainInspectionEntity?

    // 3. Reactive stream of most recent inspection for Dashboard badge
    @Query("SELECT * FROM inspections ORDER BY timestamp DESC LIMIT 1")
    fun getLatestInspection(): Flow<GrainInspectionEntity?>

    // 4. Insert or overwrite inspection record
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInspection(inspection: GrainInspectionEntity): Long

    // 5. Delete specific record
    @Delete
    suspend fun deleteInspection(inspection: GrainInspectionEntity)

    // 6. Bulk delete all records (Clear History action in Settings)
    @Query("DELETE FROM inspections")
    suspend fun clearAll()
}
```

### Verification Findings
- **Persistence Verification:** Verified via KSP compile output during `./gradlew assembleDebug`. The Room compiler successfully generated `GrainGuardianDatabase_Impl.java` and `GrainInspectionDao_Impl.java`.
- **Reactive UI Binding:** The Jetpack Compose UI observes the DAO via `StateFlow` in `GrainViewModel.kt` (`allInspections` and `latestInspection`). When a record is inserted, the Dashboard and History screens update automatically.
- **Data Integrity:** Primary key auto-generation prevents ID collisions. Deleting a record triggers reactive recomposition without requiring manual page reloads.

---

## 4. Missing Database & API Features
1. **Database Migrations:** No migration strategy (`exportSchema = false`). If schema changes in V2 (e.g., adding GPS coordinates or farmer field IDs), destructive migration or explicit `Migration(1, 2)` objects will be required.
2. **Export Functionality:** There is currently no option to export the Room SQLite database to CSV, JSON, or PDF for agricultural extension officers or bank warehouse receipts.
3. **Multi-User Partitioning:** All inspections reside in a single flat table without farmer user IDs or tenant separation.
