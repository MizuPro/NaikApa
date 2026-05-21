import re

file_path = "app/src/main/java/com/example/naikapa/presentation/history/RiwayatFragment.kt"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# Delete updateTabStyles
update_pattern = r'    private fun updateTabStyles\(\) \{.*?        \}\n    \}\n'
content = re.sub(update_pattern, "", content, flags=re.DOTALL)

# Replace setupTabs
setup_pattern = r'    private fun setupTabs\(\) \{.*?    \}\n'
new_setup = """    private fun setupTabs() {
        binding.tabLayoutHistory.addOnTabSelectedListener(object : com.google.android.material.tabs.TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: com.google.android.material.tabs.TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> selectTab(ActiveTab.FAVORIT)
                    1 -> selectTab(ActiveTab.PENCARIAN)
                    2 -> selectTab(ActiveTab.PERJALANAN)
                }
            }
            override fun onTabUnselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
            override fun onTabReselected(tab: com.google.android.material.tabs.TabLayout.Tab?) {}
        })
        binding.btnClearAll.setOnClickListener { confirmClearAll() }
    }
"""
content = re.sub(setup_pattern, new_setup, content, flags=re.DOTALL)

# Replace selectTab
select_pattern = r'    private fun selectTab\(tab: ActiveTab\) \{.*?    \}\n'
new_select = """    private fun selectTab(tab: ActiveTab) {
        activeTab = tab
        loadCurrentTab()
    }
"""
content = re.sub(select_pattern, new_select, content, flags=re.DOTALL)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)
print("Done")
