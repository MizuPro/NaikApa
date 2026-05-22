import re
import os

file_path = "app/src/main/java/com/example/naikapa/presentation/home/HomeFragment.kt"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Fix applyStatusBarTopPadding
content = content.replace(
    '        binding.cardHeader.apply {\n            com.example.naikapa.common.applyStatusBarTopPadding(this, 16)\n        }',
    '        binding.cardHeader.applyStatusBarTopPadding(16)'
)

# 2. Add import for applyStatusBarTopPadding
if 'import com.example.naikapa.common.applyStatusBarTopPadding' not in content:
    content = content.replace('import com.example.naikapa.R', 'import com.example.naikapa.R\nimport com.example.naikapa.common.applyStatusBarTopPadding')

# 3. Fix checkPendingHistoryReplay
old_mode = """        req.pendingMode?.let { mode ->
            val targetCard = when {
                mode.contains("Motor", ignoreCase = true) -> binding.modeMotor
                mode.contains("Mobil", ignoreCase = true) -> binding.modeMobil
                mode.contains("TransJakarta", ignoreCase = true) || mode.contains("TJ", ignoreCase = true) -> binding.modeTJ
                mode.contains("KRL", ignoreCase = true) -> binding.modeKRL
                mode.contains("MRT", ignoreCase = true) -> binding.modeMRT
                mode.contains("LRT", ignoreCase = true) -> binding.modeLRT
                else -> binding.modeCampur
            }
            selectTransitMode(targetCard)
        }"""
        
new_mode = """        req.pendingMode?.let { mode ->
            val targetCard = when {
                mode.contains("Motor", ignoreCase = true) -> binding.modeMotor
                mode.contains("Mobil", ignoreCase = true) -> binding.modeMobil
                mode.contains("TransJakarta", ignoreCase = true) || mode.contains("TJ", ignoreCase = true) -> binding.modeTJ
                mode.contains("KRL", ignoreCase = true) -> binding.modeKRL
                mode.contains("MRT", ignoreCase = true) -> binding.modeMRT
                mode.contains("LRT", ignoreCase = true) -> binding.modeLRT
                else -> binding.modeCampur
            }
            targetCard.isChecked = true
        }"""
content = content.replace(old_mode, new_mode)

old_priority = """        req.pendingPriority?.let { priority ->
            val chips = listOf(
                binding.chipTercepat,
                binding.chipTerhemat,
                binding.chipMinimJalanKaki,
                binding.chipMinimTransit
            )
            chips.forEach { it.isChecked = false }
            when {
                priority.contains("Hemat", ignoreCase = true) -> binding.chipTerhemat.isChecked = true
                priority.contains("Jalan", ignoreCase = true) -> binding.chipMinimJalanKaki.isChecked = true
                priority.contains("Transit", ignoreCase = true) -> binding.chipMinimTransit.isChecked = true
                else -> binding.chipTercepat.isChecked = true
            }
            updateChipStyles()
        }"""
new_priority = """        req.pendingPriority?.let { priority ->
            when {
                priority.contains("Hemat", ignoreCase = true) -> binding.chipTerhemat.isChecked = true
                priority.contains("Jalan", ignoreCase = true) -> binding.chipMinimJalanKaki.isChecked = true
                priority.contains("Transit", ignoreCase = true) -> binding.chipMinimTransit.isChecked = true
                else -> binding.chipTercepat.isChecked = true
            }
        }"""
content = content.replace(old_priority, new_priority)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

# RiwayatFragment
riwayat_path = "app/src/main/java/com/example/naikapa/presentation/history/RiwayatFragment.kt"
with open(riwayat_path, "r", encoding="utf-8") as f:
    r_content = f.read()

r_content = r_content.replace(
    '        binding.cardHeader.apply {\n            com.example.naikapa.common.applyStatusBarTopPadding(this, 16)\n        }',
    '        binding.cardHeader.applyStatusBarTopPadding(16)'
)
if 'import com.example.naikapa.common.applyStatusBarTopPadding' not in r_content:
    r_content = r_content.replace('import com.example.naikapa.R', 'import com.example.naikapa.R\nimport com.example.naikapa.common.applyStatusBarTopPadding')

with open(riwayat_path, "w", encoding="utf-8") as f:
    f.write(r_content)
    
print("Done")
