import bpy
import sys
import os

bpy.ops.wm.read_factory_settings(use_empty=True)

input_obj = r"c:\Users\matsu\template-mod-template-1.20.1\models\Untitled.obj"
output_dir = r"c:\Users\matsu\template-mod-template-1.20.1\models"

# Import OBJ
try:
    bpy.ops.wm.obj_import(filepath=input_obj)
except AttributeError:
    bpy.ops.import_scene.obj(filepath=input_obj)

# Get the main object
obj = None
for o in bpy.context.scene.objects:
    if o.type == 'MESH':
        obj = o
        break

if obj:
    bpy.context.view_layer.objects.active = obj
    obj.select_set(True)
    
    ratios = {
        "02": 0.02, # 2% (slightly better than 1%)
        "05": 0.05, # 5% (good balance)
        "10": 0.10  # 10% (very high quality, ~80k polys)
    }
    
    for suffix, ratio in ratios.items():
        # Add modifier
        mod = obj.modifiers.new(name="Decimate", type='DECIMATE')
        mod.ratio = ratio
        
        # Apply modifier
        bpy.ops.object.modifier_apply(modifier=mod.name)
        
        # Export GLB and OBJ
        glb_path = os.path.join(output_dir, f"Untitled_decimated_{suffix}.glb")
        obj_path = os.path.join(output_dir, f"Untitled_decimated_{suffix}.obj")
        
        bpy.ops.export_scene.gltf(filepath=glb_path, use_selection=True, export_format='GLB')
        
        try:
            bpy.ops.wm.obj_export(filepath=obj_path, export_selected_objects=True)
        except AttributeError:
            bpy.ops.export_scene.obj(filepath=obj_path, use_selection=True)
        
        # Re-import original to undo decimation for the next iteration
        bpy.ops.object.delete()
        try:
            bpy.ops.wm.obj_import(filepath=input_obj)
        except AttributeError:
            bpy.ops.import_scene.obj(filepath=input_obj)
        
        for o in bpy.context.scene.objects:
            if o.type == 'MESH':
                obj = o
                bpy.context.view_layer.objects.active = obj
                obj.select_set(True)
                break

print("All resolutions exported.")
