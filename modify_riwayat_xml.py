import re

file_path = "app/src/main/res/layout/fragment_riwayat.xml"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Update marginTop of cardHeader to be 0dp because we will handle padding top programmatically
content = re.sub(r'android:paddingTop="48dp"', 'android:paddingTop="16dp"', content)

# 2. Segmented Tab Replace
tab_pattern = r'            <!-- Segmented Tab Control -->.*?            </com\.google\.android\.material\.card\.MaterialCardView>'
new_tabs = """            <!-- Segmented Tab Control -->
            <com.google.android.material.tabs.TabLayout
                android:id="@+id/tabLayoutHistory"
                android:layout_width="match_parent"
                android:layout_height="36dp"
                android:layout_marginTop="16dp"
                style="@style/NaikApaSegmentedTab"
                app:tabBackground="@drawable/bg_segment_unselected">

                <com.google.android.material.tabs.TabItem
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/history_tab_favorit" />

                <com.google.android.material.tabs.TabItem
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/history_tab_pencarian" />

                <com.google.android.material.tabs.TabItem
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:text="@string/history_tab_perjalanan" />

            </com.google.android.material.tabs.TabLayout>"""

content = re.sub(tab_pattern, new_tabs, content, flags=re.DOTALL)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)
print("Done")
