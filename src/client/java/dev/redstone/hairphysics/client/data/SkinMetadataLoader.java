package dev.redstone.hairphysics.client.data;

import com.google.gson.*;
import dev.redstone.hairphysics.client.HairphysicsClient;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;












public class SkinMetadataLoader {

    private static final Map<UUID, Optional<HairDefinition>> CACHE = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Pattern ATTACHED_HAIR_ID = Pattern.compile("^(.+)_hair_\\d+$");

    private static Path getBaseDir() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("hairphysics");
    }

    private static Path getLegacySkinDir() {
        return getBaseDir().resolve("skins");
    }

    private static Path getActiveFile() {
        return getBaseDir().resolve("active-hairconfig.json");
    }

    



    public static Optional<HairDefinition> loadForPlayer(UUID uuid) {
        return CACHE.computeIfAbsent(uuid, SkinMetadataLoader::parseFromDisk);
    }

    


    public static Optional<HairDefinition> loadForEditor(UUID uuid) {
        return parseFromDisk(uuid, true);
    }

    private static Optional<HairDefinition> parseFromDisk(UUID uuid) {
        return parseFromDisk(uuid, false);
    }

    private static Optional<HairDefinition> parseFromDisk(UUID uuid, boolean includeAnchors) {
        Path activeFile = getActiveFile();
        if (Files.exists(activeFile)) {
            return parseFile(activeFile, includeAnchors, "active preset");
        }

        Path legacyFile = getLegacySkinDir().resolve(uuid.toString() + ".json");
        if (Files.exists(legacyFile)) {
            return parseFile(legacyFile, includeAnchors, uuid.toString());
        }

        return Optional.empty();
    }

    public static Optional<HairDefinition> loadFromFile(Path file, boolean includeAnchors) {
        return parseFile(file, includeAnchors, file.toString());
    }

    private static Optional<HairDefinition> parseFile(Path file, boolean includeAnchors, String label) {
        try (Reader reader = Files.newBufferedReader(file)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            HairDefinition def = parseDefinition(root, includeAnchors);
            HairphysicsClient.LOGGER.info("[HairPhysics] Loaded hair definition from {}: {}", label, def);
            return Optional.of(def);
        } catch (Exception e) {
            HairphysicsClient.LOGGER.warn("[HairPhysics] Failed to parse hair JSON from {}: {}", label, e.getMessage());
            return Optional.empty();
        }
    }

    private static HairDefinition parseDefinition(JsonObject root, boolean includeAnchors) {
        int version = getInt(root, "version", 1);
        String model = getString(root, "model", "classic");

        List<HairStrand> parsed = new ArrayList<>();
        if (root.has("strands") && root.get("strands").isJsonArray()) {
            JsonArray strandArray = root.getAsJsonArray("strands");
            Set<String> anchorIds = findAnchorIds(strandArray);
            for (JsonElement el : strandArray) {
                if (el.isJsonObject()) {
                    JsonObject strandObj = el.getAsJsonObject();
                    String id = getString(strandObj, "id", "strand");
                    boolean anchorOnly = getBoolean(strandObj, "anchor_only", false) || anchorIds.contains(id);
                    parsed.add(parseStrand(strandObj, anchorOnly));
                }
            }
        }

        Map<String, HairStrand> anchors = new HashMap<>();
        for (HairStrand strand : parsed) {
            if (strand.anchorOnly) {
                anchors.put(strand.id, repairSurfaceOrigin(strand));
            }
        }

        List<HairStrand> strands = new ArrayList<>();
        for (HairStrand strand : parsed) {
            HairStrand repaired = repairLinkedOrigin(strand, anchors);
            if (includeAnchors || !repaired.anchorOnly) {
                strands.add(repaired);
            }
        }

        return new HairDefinition(version, model, strands);
    }

    private static HairStrand parseStrand(JsonObject obj, boolean anchorOnly) {
        String id = getString(obj, "id", "strand");

        
        StrandOrigin origin = StrandOrigin.defaultHead();
        if (obj.has("origin") && obj.get("origin").isJsonObject()) {
            JsonObject o = obj.getAsJsonObject("origin");
            String bone = getString(o, "bone", "head");
            float ox = 0, oy = 0.25f, oz = -0.25f;
            if (o.has("offset") && o.get("offset").isJsonObject()) {
                JsonObject off = o.getAsJsonObject("offset");
                ox = getFloat(off, "x", 0.0f);
                oy = getFloat(off, "y", 0.25f);
                oz = getFloat(off, "z", -0.25f);
            }
            origin = new StrandOrigin(bone, ox, oy, oz);
        }

        
        SkinRegion region = SkinRegion.defaultHead();
        if (obj.has("skin_region") && obj.get("skin_region").isJsonObject()) {
            JsonObject r = obj.getAsJsonObject("skin_region");
            int u = getInt(r, "u", 32);
            int v = getInt(r, "v", 0);
            int w = getInt(r, "width", 8);
            int h = getInt(r, "height", 8);
            String layer = getString(r, "layer", "outer");
            region = new SkinRegion(u, v, w, h, layer);
        }

        
        StrandPhysicsConfig physics = StrandPhysicsConfig.defaults();
        if (obj.has("physics") && obj.get("physics").isJsonObject()) {
            JsonObject p = obj.getAsJsonObject("physics");
            float legacySegmentLength = getFloat(p, "segment_length", 0.15f);
            float lengthScale = getFloat(p, "length_scale",
                legacySegmentLength <= 0.5f ? 1.0f : legacySegmentLength);
            float legacyWindResponse = getFloat(p, "wind_response", 0.6211f);
            physics = new StrandPhysicsConfig(
                getInt(p, "segments", 6),
                legacySegmentLength,
                getFloat(p, "stiffness", 0.7f),
                getFloat(p, "gravity", 0.035f),
                getFloat(p, "damping", 0.85f),
                getFloat(p, "move_response", legacyWindResponse),
                legacyWindResponse,
                getFloat(p, "wind_direction", 1.0f),
                lengthScale
            );
        }

        
        StrandRenderConfig render = StrandRenderConfig.defaults();
        if (obj.has("render") && obj.get("render").isJsonObject()) {
            JsonObject r = obj.getAsJsonObject("render");
            render = new StrandRenderConfig(
                getFloat(r, "thickness", 0.05f),
                getString(r, "style", "ribbon"),
                getBoolean(r, "billboard", true),
                getFloat(r, "motion_intensity", 0.65f)
            );
        }

        return new HairStrand(id, origin, region, physics, render, anchorOnly);
    }

    private static HairStrand repairLinkedOrigin(HairStrand strand, Map<String, HairStrand> anchors) {
        if (strand.anchorOnly) {
            return anchors.getOrDefault(strand.id, repairSurfaceOrigin(strand));
        }

        HairStrand anchor = anchors.get(parentAnchorId(strand.id));
        if (anchor != null) {
            return new HairStrand(strand.id, anchor.origin, strand.skinRegion,
                strand.physics, strand.render, false);
        }

        return repairSurfaceOrigin(strand);
    }

    private static HairStrand repairSurfaceOrigin(HairStrand strand) {
        SkinUvMapper.SurfacePoint point = SkinUvMapper.pointForRegion(strand.skinRegion);
        if (!shouldUseSurfacePoint(strand.origin, point)) {
            return strand;
        }

        StrandOrigin origin = new StrandOrigin(point.bone(), point.offsetX(), point.offsetY(), point.offsetZ());
        return new HairStrand(strand.id, origin, strand.skinRegion,
            strand.physics, strand.render, strand.anchorOnly);
    }

    private static boolean shouldUseSurfacePoint(StrandOrigin origin, SkinUvMapper.SurfacePoint point) {
        return SkinUvMapper.isDefaultOffset(origin)
            || (isHeadBone(origin.bone) && !"head".equals(point.bone()));
    }

    private static Set<String> findAnchorIds(JsonArray strands) {
        Set<String> ids = new HashSet<>();
        for (JsonElement el : strands) {
            if (!el.isJsonObject()) continue;
            String id = getString(el.getAsJsonObject(), "id", "");
            Matcher matcher = ATTACHED_HAIR_ID.matcher(id);
            if (matcher.matches()) {
                ids.add(matcher.group(1));
            }
        }
        return ids;
    }

    private static String parentAnchorId(String id) {
        if (id == null) return null;
        Matcher matcher = ATTACHED_HAIR_ID.matcher(id);
        return matcher.matches() ? matcher.group(1) : null;
    }

    

    private static int getInt(JsonObject obj, String key, int def) {
        return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsInt() : def;
    }

    private static float getFloat(JsonObject obj, String key, float def) {
        return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsFloat() : def;
    }

    private static String getString(JsonObject obj, String key, String def) {
        return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsString() : def;
    }

    private static boolean getBoolean(JsonObject obj, String key, boolean def) {
        return obj.has(key) && obj.get(key).isJsonPrimitive() ? obj.get(key).getAsBoolean() : def;
    }

    private static boolean isHeadBone(String bone) {
        return bone == null || bone.isBlank() || "head".equalsIgnoreCase(bone.trim());
    }

    
    public static void clearCache() {
        CACHE.clear();
    }

    
    public static void clearCache(UUID uuid) {
        CACHE.remove(uuid);
    }

    



    public static void writeExampleIfAbsent(UUID uuid) {
        Path file = getActiveFile();
        if (Files.exists(file)) return;
        try {
            Files.createDirectories(file.getParent());
            String example = """
                    {
                      "version": 1,
                      "model": "classic",
                      "strands": [
                        {
                          "id": "main_hair",
                          "origin": {
                            "bone": "head",
                            "offset": { "x": 0.0, "y": 0.25, "z": -0.25 }
                          },
                          "skin_region": {
                            "u": 32, "v": 0,
                            "width": 8, "height": 8,
                            "layer": "outer"
                          },
                          "physics": {
                            "segments": 6,
                            "segment_length": 0.15,
                            "length_scale": 1.0,
                            "stiffness": 0.7,
                            "gravity": 0.035,
                            "damping": 0.85,
                            "wind_response": 0.4
                          },
                          "render": {
                            "thickness": 0.05,
                            "style": "ribbon",
                            "billboard": true,
                            "motion_intensity": 0.65
                          }
                        }
                      ]
                    }
                    """;
            Files.writeString(file, example);
            HairphysicsClient.LOGGER.info("[HairPhysics] Wrote example hair JSON to {}", file);
        } catch (IOException e) {
            HairphysicsClient.LOGGER.warn("[HairPhysics] Could not write example JSON: {}", e.getMessage());
        }
    }
}
