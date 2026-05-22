import re

file_path = "app/src/main/res/layout/fragment_home.xml"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# 1. Change root layout
content = content.replace(
    '<androidx.constraintlayout.widget.ConstraintLayout xmlns:android="http://schemas.android.com/apk/res/android"',
    '<androidx.coordinatorlayout.widget.CoordinatorLayout xmlns:android="http://schemas.android.com/apk/res/android"\n    xmlns:app="http://schemas.android.com/apk/res-auto"\n    xmlns:tools="http://schemas.android.com/tools"\n    android:layout_width="match_parent"\n    android:layout_height="match_parent"\n    android:background="@color/colorBackground"\n    tools:context=".presentation.home.HomeFragment">\n\n<androidx.constraintlayout.widget.ConstraintLayout'
)
# We need to remove the xmlns and tools from the original ConstraintLayout since it's now inside CoordinatorLayout
content = re.sub(
    r'<androidx\.constraintlayout\.widget\.ConstraintLayout\s+xmlns:app="http://schemas\.android\.com/apk/res-auto"\s+xmlns:tools="http://schemas\.android\.com/tools"\s+android:layout_width="match_parent"\s+android:layout_height="match_parent"\s+android:background="@color/colorBackground"\s+tools:context="\.presentation\.home\.HomeFragment">',
    '<androidx.constraintlayout.widget.ConstraintLayout\n    android:layout_width="match_parent"\n    android:layout_height="match_parent">',
    content
)

# 2. Fix closing tags
content = content.replace('</androidx.constraintlayout.widget.ConstraintLayout>', '</androidx.constraintlayout.widget.ConstraintLayout>\n</androidx.coordinatorlayout.widget.CoordinatorLayout>')
# We will pull cardRecommendation outside ConstraintLayout later.

# 3. cardHeader marginTop
content = re.sub(r'android:layout_marginTop="48dp"', 'android:layout_marginTop="0dp"', content, count=1)

# 4. Drawables
content = content.replace('android:background="@drawable/ic_warning"\n                    android:backgroundTint="@color/colorAccentOrange"', 'android:background="@drawable/bg_notification_dot"')
content = content.replace('android:background="@drawable/ic_warning"\n                    android:backgroundTint="@color/colorPrimary"', 'android:background="@drawable/bg_dot_origin"')
content = content.replace('android:background="@drawable/ic_warning"\n                    android:backgroundTint="@color/colorAccentOrange"', 'android:background="@drawable/bg_dot_destination"')
content = content.replace('android:backgroundTint="@color/colorAccentOrangeLight"', 'android:backgroundTint="@color/colorAmberLight"')
content = content.replace('android:textColor="@color/colorAccentOrange"', 'android:textColor="@color/colorAmber"')

# 5. Transit Modes
modes_old = """            <HorizontalScrollView
                android:id="@+id/scrollTransitModes"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:scrollbars="none">

                <LinearLayout
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:paddingBottom="4dp">"""
                    
modes_new = """            <HorizontalScrollView
                android:id="@+id/scrollTransitModes"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:scrollbars="none">

                <com.google.android.material.chip.ChipGroup
                    android:id="@+id/chipGroupModes"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:paddingBottom="4dp"
                    app:singleSelection="true"
                    app:singleLine="true">
                    
                    <com.google.android.material.chip.Chip
                        android:id="@+id/modeCampur"
                        style="@style/Widget.Material3.Chip.Filter"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Campur"
                        app:chipIcon="@drawable/ic_directions"
                        android:checked="true" />

                    <com.google.android.material.chip.Chip
                        android:id="@+id/modeTJ"
                        style="@style/Widget.Material3.Chip.Filter"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="TransJakarta"
                        app:chipIcon="@drawable/ic_bus" />

                    <com.google.android.material.chip.Chip
                        android:id="@+id/modeKRL"
                        style="@style/Widget.Material3.Chip.Filter"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="KRL"
                        app:chipIcon="@drawable/ic_train" />

                    <com.google.android.material.chip.Chip
                        android:id="@+id/modeMRT"
                        style="@style/Widget.Material3.Chip.Filter"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="MRT"
                        app:chipIcon="@drawable/ic_train" />

                    <com.google.android.material.chip.Chip
                        android:id="@+id/modeLRT"
                        style="@style/Widget.Material3.Chip.Filter"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="LRT"
                        app:chipIcon="@drawable/ic_train" />

                    <com.google.android.material.chip.Chip
                        android:id="@+id/modeMotor"
                        style="@style/Widget.Material3.Chip.Filter"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Motor"
                        app:chipIcon="@drawable/ic_motorcycle" />

                    <com.google.android.material.chip.Chip
                        android:id="@+id/modeMobil"
                        style="@style/Widget.Material3.Chip.Filter"
                        android:layout_width="wrap_content"
                        android:layout_height="wrap_content"
                        android:text="Mobil"
                        app:chipIcon="@drawable/ic_car" />

                </com.google.android.material.chip.ChipGroup>
"""

# Extract the old modes content and replace
start_idx = content.find(modes_old)
if start_idx != -1:
    end_idx = content.find('</HorizontalScrollView>', start_idx) + len('</HorizontalScrollView>')
    content = content[:start_idx] + modes_new + content[end_idx:]

# 6. Priority Chips
priority_old = """            <HorizontalScrollView
                android:id="@+id/scrollRouteCriteria"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="8dp"
                android:layout_marginBottom="12dp"
                android:scrollbars="none">

                <LinearLayout
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:orientation="horizontal"
                    android:paddingBottom="4dp">"""
                    
priority_new = """            <HorizontalScrollView
                android:id="@+id/scrollRouteCriteria"
                android:layout_width="match_parent"
                android:layout_height="wrap_content"
                android:layout_marginTop="8dp"
                android:layout_marginBottom="12dp"
                android:scrollbars="none">

                <com.google.android.material.chip.ChipGroup
                    android:id="@+id/chipGroupCriteria"
                    android:layout_width="wrap_content"
                    android:layout_height="wrap_content"
                    android:paddingBottom="4dp"
                    app:singleSelection="true"
                    app:singleLine="true">"""
                    
content = content.replace(priority_old, priority_new)
content = content.replace('                </LinearLayout>\n\n            </HorizontalScrollView>', '                </com.google.android.material.chip.ChipGroup>\n\n            </HorizontalScrollView>')

# Update cardRecommendation to be outside ConstraintLayout and have BottomSheetBehavior
recommendation_start = content.find('    <!-- 5. Bottom Recommendation Panel -->')
if recommendation_start != -1:
    recommendation_block = content[recommendation_start:]
    # remove the block from the constraint layout
    content = content[:recommendation_start]
    
    # fix the block to act as a bottom sheet
    recommendation_block = recommendation_block.replace('</androidx.constraintlayout.widget.ConstraintLayout>', '')
    recommendation_block = recommendation_block.replace('</androidx.coordinatorlayout.widget.CoordinatorLayout>', '')
    
    # We remove the constraints
    recommendation_block = re.sub(r'app:layout_constraintBottom_toBottomOf="parent"\s*', '', recommendation_block)
    
    # add behavior and margins
    recommendation_block = recommendation_block.replace('android:layout_marginBottom="16dp"', 'app:layout_behavior="com.google.android.material.bottomsheet.BottomSheetBehavior"\n        app:behavior_peekHeight="120dp"\n        app:behavior_hideable="false"\n        app:shapeAppearanceOverlay="@style/ShapeAppearance.Material3.Corner.Top"')
    
    # insert drag handle inside the layout
    padding_start = recommendation_block.find('android:paddingBottom="16dp">') + len('android:paddingBottom="16dp">')
    drag_handle = """
            <!-- Drag Handle -->
            <View
                android:layout_width="32dp"
                android:layout_height="4dp"
                android:layout_gravity="center_horizontal"
                android:layout_marginBottom="16dp"
                android:background="@drawable/bg_bottom_sheet_handle" />
"""
    recommendation_block = recommendation_block[:padding_start] + drag_handle + recommendation_block[padding_start:]
    
    # Assemble final
    content = content + '</androidx.constraintlayout.widget.ConstraintLayout>\n\n' + recommendation_block + '\n</androidx.coordinatorlayout.widget.CoordinatorLayout>\n'

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)
print("Done")
