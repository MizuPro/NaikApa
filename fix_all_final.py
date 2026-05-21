import re

file_path = "app/src/main/res/values/styles.xml"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# Replace NaikApaBottomSheet and remove ShapeAppearance.Material3.Corner.Top
old_style = """    <style name="NaikApaBottomSheet" parent="Widget.Material3.BottomSheet">
        <item name="backgroundTint">@color/colorSurface</item>
        <item name="shapeAppearance">@style/ShapeAppearance.Material3.Corner.Top</item>
    </style>

    <style name="ShapeAppearance.Material3.Corner.Top" parent="ShapeAppearance.Material3">
        <item name="cornerFamily">rounded</item>
        <item name="cornerSizeTopLeft">16dp</item>
        <item name="cornerSizeTopRight">16dp</item>
        <item name="cornerSizeBottomLeft">0dp</item>
        <item name="cornerSizeBottomRight">0dp</item>
    </style>"""

new_style = """    <style name="NaikApaBottomSheet" parent="Widget.Material3.BottomSheet.Modal">
        <item name="backgroundTint">@color/colorSurface</item>
    </style>"""

content = content.replace(old_style, new_style)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

# Fix applyStatusBarTopPadding in HomeFragment.kt
home_path = "app/src/main/java/com/example/naikapa/presentation/home/HomeFragment.kt"
with open(home_path, "r", encoding="utf-8") as f:
    home_content = f.read()

home_content = home_content.replace('applyStatusBarTopPadding(16)', 'applyStatusBarTopPadding()')

with open(home_path, "w", encoding="utf-8") as f:
    f.write(home_content)

print("Done")
