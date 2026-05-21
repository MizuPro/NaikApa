import re

file_path = "app/src/main/res/values/styles.xml"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

new_shape = """    <style name="NaikApaBottomSheet" parent="Widget.Material3.BottomSheet">
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

content = content.replace(
    '    <style name="NaikApaBottomSheet" parent="Widget.Material3.BottomSheet">\n        <item name="backgroundTint">@color/colorSurface</item>\n        <item name="shapeAppearance">@style/ShapeAppearance.Material3.Corner.Top</item>\n    </style>',
    new_shape
)

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)
print("Done")
