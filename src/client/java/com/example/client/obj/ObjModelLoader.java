package com.example.client.obj;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ObjModelLoader {

    public static class VertexData {
        public float x, y, z;
        public float u, v;
        public float nx, ny, nz;
    }

    public static class Face {
        public VertexData[] vertices;
        public Face(int size) {
            vertices = new VertexData[size];
            for (int i = 0; i < size; i++) vertices[i] = new VertexData();
        }
    }

    public static class MeshPart {
        public List<Face> faces = new ArrayList<>();
    }

    public static class ObjModel {
        public Map<String, MeshPart> parts = new HashMap<>();
        
        public MeshPart getPart(String name) {
            return parts.getOrDefault(name, new MeshPart());
        }
    }

    public static ObjModel load(ResourceLocation location) {
        ObjModel model = new ObjModel();
        ResourceManager manager = Minecraft.getInstance().getResourceManager();

        try {
            Optional<Resource> resourceOpt = manager.getResource(location);
            if (resourceOpt.isEmpty()) {
                System.err.println("Could not find OBJ file: " + location);
                return model;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resourceOpt.get().open()))) {
                List<float[]> v = new ArrayList<>();
                List<float[]> vt = new ArrayList<>();
                List<float[]> vn = new ArrayList<>();
                
                // Add dummy 0-index entries because OBJ is 1-indexed
                v.add(new float[]{0,0,0});
                vt.add(new float[]{0,0});
                vn.add(new float[]{0,1,0});

                String currentPartName = "default";
                MeshPart currentPart = new MeshPart();
                model.parts.put(currentPartName, currentPart);

                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) continue;

                    String[] tokens = line.split("\\s+");
                    if (tokens.length == 0) continue;

                    String type = tokens[0];

                    if (type.equals("o") || type.equals("g")) {
                        if (tokens.length > 1) {
                            currentPartName = tokens[1];
                            currentPart = new MeshPart();
                            model.parts.put(currentPartName, currentPart);
                        }
                    } else if (type.equals("v")) {
                        v.add(new float[]{Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]), Float.parseFloat(tokens[3])});
                    } else if (type.equals("vt")) {
                        // OpenGL/Minecraft V axis might be inverted depending on the exporter, typically v = 1.0 - vt
                        vt.add(new float[]{Float.parseFloat(tokens[1]), 1.0f - Float.parseFloat(tokens[2])});
                    } else if (type.equals("vn")) {
                        vn.add(new float[]{Float.parseFloat(tokens[1]), Float.parseFloat(tokens[2]), Float.parseFloat(tokens[3])});
                    } else if (type.equals("f")) {
                        int numVertices = tokens.length - 1;
                        Face face = new Face(numVertices);
                        
                        for (int i = 0; i < numVertices; i++) {
                            String[] indices = tokens[i + 1].split("/");
                            int vIdx = Integer.parseInt(indices[0]);
                            float[] pos = v.get(vIdx < 0 ? v.size() + vIdx : vIdx);
                            face.vertices[i].x = pos[0];
                            face.vertices[i].y = pos[1];
                            face.vertices[i].z = pos[2];

                            if (indices.length > 1 && !indices[1].isEmpty()) {
                                int vtIdx = Integer.parseInt(indices[1]);
                                float[] tex = vt.get(vtIdx < 0 ? vt.size() + vtIdx : vtIdx);
                                face.vertices[i].u = tex[0];
                                face.vertices[i].v = tex[1];
                            }

                            if (indices.length > 2 && !indices[2].isEmpty()) {
                                int vnIdx = Integer.parseInt(indices[2]);
                                float[] norm = vn.get(vnIdx < 0 ? vn.size() + vnIdx : vnIdx);
                                face.vertices[i].nx = norm[0];
                                face.vertices[i].ny = norm[1];
                                face.vertices[i].nz = norm[2];
                            }
                        }
                        
                        currentPart.faces.add(face);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return model;
    }
}
