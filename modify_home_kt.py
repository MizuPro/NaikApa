import re

file_path = "app/src/main/java/com/example/naikapa/presentation/home/HomeFragment.kt"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# Replace setupTransitModes
setup_modes_pattern = r'    private fun setupTransitModes\(\) \{.*?(?=    private fun selectTransitMode)'
new_setup_modes = """    private fun setupTransitModes() {
        binding.chipGroupModes.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                selectedModeCardId = checkedIds.first()
            }
        }
        selectedModeCardId = R.id.modeCampur
    }

"""
content = re.sub(setup_modes_pattern, new_setup_modes, content, flags=re.DOTALL)

# Delete selectTransitMode
select_mode_pattern = r'    private fun selectTransitMode\(selectedCard: MaterialCardView\) \{.*?(?=    private fun setupDestinationSearch\(\) \{)'
content = re.sub(select_mode_pattern, "", content, flags=re.DOTALL)

# Replace setupSortChips
setup_sort_pattern = r'    private fun setupSortChips\(\) \{.*?(?=    private fun updateChipStyles\(\) \{)'
new_setup_sort = """    private fun setupSortChips() {
        binding.chipGroupCriteria.setOnCheckedStateChangeListener { group, checkedIds ->
            if (selectedOrigin != null && selectedDestination != null && binding.scrollRecommendations.visibility == View.VISIBLE) {
                handleFindRouteClick()
            }
        }
    }

"""
content = re.sub(setup_sort_pattern, new_setup_sort, content, flags=re.DOTALL)

# Delete updateChipStyles
update_styles_pattern = r'    private fun updateChipStyles\(\) \{.*?(?=    private fun scheduleDestinationSearch)'
content = re.sub(update_styles_pattern, "", content, flags=re.DOTALL)

# Also fix the top padding for cardHeader
on_view_created_pattern = r'        setupPanelToggle\(\)\n    \}'
new_on_view_created = """        setupPanelToggle()
        
        binding.cardHeader.apply {
            com.example.naikapa.common.applyStatusBarTopPadding(this, 16)
        }
    }"""
content = re.sub(on_view_created_pattern, new_on_view_created, content)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)
print("Done")
