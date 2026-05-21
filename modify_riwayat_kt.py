import re

file_path = "app/src/main/java/com/example/naikapa/presentation/history/RiwayatFragment.kt"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# Replace setupTabs
setup_tabs_pattern = r'    private fun setupTabs\(\) \{.*?(?=    private fun selectTab)'
new_setup_tabs = """    private fun setupTabs() {
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
content = re.sub(setup_tabs_pattern, new_setup_tabs, content, flags=re.DOTALL)

# Replace selectTab and updateTabStyles
select_tab_pattern = r'    private fun selectTab\(tab: ActiveTab\) \{.*?    private fun loadCurrentTab\(\) \{'
new_select_tab = """    private fun selectTab(tab: ActiveTab) {
        activeTab = tab
        loadCurrentTab()
    }

    private fun loadCurrentTab() {"""
content = re.sub(select_tab_pattern, new_select_tab, content, flags=re.DOTALL)

# Add applyStatusBarTopPadding on onViewCreated for edge-to-edge
on_view_created_pattern = r'        setupTabs\(\)\n        selectTab\(ActiveTab\.FAVORIT\)\n    \}'
new_on_view_created = """        setupTabs()
        selectTab(ActiveTab.FAVORIT)
        
        binding.cardHeader.apply {
            com.example.naikapa.common.applyStatusBarTopPadding(this, 16)
        }
    }"""
content = re.sub(on_view_created_pattern, new_on_view_created, content)


with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)
print("Done")
