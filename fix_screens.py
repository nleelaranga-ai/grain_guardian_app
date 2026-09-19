import os
import re

screens_dir = r"C:\Users\LEELA RANGA PRASAD\.gemini\antigravity\scratch\GrainGuardian\app\src\main\java\com\vrsec\grainguardian\ui\screens"

for fname in os.listdir(screens_dir):
    if not fname.endswith(".kt"):
        continue
    fpath = os.path.join(screens_dir, fname)
    with open(fpath, "r", encoding="utf-8") as f:
        content = f.read()

    # Replace AutoMirrored
    content = content.replace("Icons.AutoMirrored.Filled.ArrowForwardIos", "Icons.Default.ChevronRight")
    content = content.replace("Icons.AutoMirrored.Filled.ArrowBack", "Icons.Default.ArrowBack")
    content = content.replace("import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos", "import androidx.compose.material.icons.filled.ChevronRight")
    content = content.replace("import androidx.compose.material.icons.automirrored.filled.ArrowBack", "import androidx.compose.material.icons.filled.ArrowBack")

    # Add ExperimentalMaterial3Api import and annotation
    if "Scaffold" in content:
        if "import androidx.compose.material3.ExperimentalMaterial3Api" not in content:
            content = content.replace("import androidx.compose.material3.Scaffold", "import androidx.compose.material3.ExperimentalMaterial3Api\nimport androidx.compose.material3.Scaffold")
        
        # Add annotation before @Composable function
        lines = content.splitlines()
        new_lines = []
        for line in lines:
            if line.startswith("@Composable") and "fun " in content and "@OptIn(ExperimentalMaterial3Api::class)" not in new_lines:
                new_lines.append("@OptIn(ExperimentalMaterial3Api::class)")
            new_lines.append(line)
        content = "\n".join(new_lines)

    with open(fpath, "w", encoding="utf-8") as f:
        f.write(content)
    print("Fixed:", fname)
