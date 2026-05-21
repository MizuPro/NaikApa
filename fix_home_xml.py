import re

file_path = "app/src/main/res/layout/fragment_home.xml"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

content = content.replace('app:shapeAppearanceOverlay="@style/ShapeAppearance.Material3.Corner.Top"', '')

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

print("Done")
