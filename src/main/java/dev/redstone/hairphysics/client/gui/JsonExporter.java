package dev.redstone.hairphysics.client.gui;

import dev.redstone.hairphysics.client.HairphysicsClient;
import dev.redstone.hairphysics.client.data.SkinRegion;
import dev.redstone.hairphysics.client.data.SkinUvMapper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;






public class JsonExporter {
    private static final String PRESET_SUFFIX = "-hairconfig.json";
    private static final Pattern ATTACHED_HAIR_ID = Pattern.compile("^(.+)_hair_\\d+$");

    private static Path getBaseDir() {
        return Path.of("config").resolve("hairphysics");
    }

    public static File getActiveFile() {
        return getBaseDir().resolve("active" + PRESET_SUFFIX).toFile();
    }

    public static Path getPresetDir() {
        return getBaseDir().resolve("presets");
    }

    public static File getPresetFile(String presetName) {
        return getPresetDir().resolve(sanitizePresetName(presetName) + PRESET_SUFFIX).toFile();
    }

    
    public static File getOutputFile(UUID uuid) {
        Path config = getBaseDir().resolve("skins");
        return config.resolve(uuid + ".json").toFile();
    }

    public static List<File> listPresetFiles() throws IOException {
        Path dir = getPresetDir();
        if (!Files.isDirectory(dir)) {
            return List.of();
        }

        try (Stream<Path> files = Files.list(dir)) {
            return files
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith(PRESET_SUFFIX))
                .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT)))
                .map(Path::toFile)
                .toList();
        }
    }

    public static String presetNameFromFile(File file) {
        String name = file.getName();
        if (name.endsWith(PRESET_SUFFIX)) {
            return name.substring(0, name.length() - PRESET_SUFFIX.length());
        }
        if (name.endsWith(".json")) {
            return name.substring(0, name.length() - ".json".length());
        }
        return name;
    }

    


    public static File exportActive(List<OriginEntry> origins, boolean slim) throws IOException {
        File out = getActiveFile();
        writeConfig(out, origins, slim);
        HairphysicsClient.LOGGER.info("[HairPhysics] Exported active JSON to {}", out.getAbsolutePath());
        return out;
    }

    


    public static File exportPreset(List<OriginEntry> origins, String presetName, boolean slim)
            throws IOException {
        File out = getPresetFile(presetName);
        writeConfig(out, origins, slim);
        exportActive(origins, slim);
        HairphysicsClient.LOGGER.info("[HairPhysics] Exported preset JSON to {}", out.getAbsolutePath());
        return out;
    }

    private static void writeConfig(File out, List<OriginEntry> origins, boolean slim) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"version\": 1,\n");
        sb.append("  \"model\": \"").append(slim ? "slim" : "classic").append("\",\n");
        sb.append("  \"strands\": [\n");

        Map<String, OriginEntry> anchors = anchorMap(origins);
        int exported = 0;
        for (OriginEntry o : origins) {
            OriginEntry root = rootForExport(o, anchors);
            SkinUvMapper.SurfacePoint point = surfacePoint(root);
            String bone = boneForExport(root, point);
            float[] offset = offsetForExport(root, point);

            if (exported > 0) sb.append(",\n");
            sb.append("    {\n");
            sb.append("      \"id\": \"").append(escapeJson(o.id)).append("\",\n");
            if (o.anchorOnly) {
                sb.append("      \"anchor_only\": true,\n");
            }
            sb.append("      \"origin\": {\n");
            sb.append("        \"bone\": \"").append(escapeJson(bone)).append("\",\n");
            sb.append("        \"offset\": { ");
            sb.append("\"x\": ").append(fmt(offset[0])).append(", ");
            sb.append("\"y\": ").append(fmt(offset[1])).append(", ");
            sb.append("\"z\": ").append(fmt(offset[2]));
            sb.append(" }\n");
            sb.append("      },\n");
            sb.append("      \"skin_region\": {\n");
            sb.append("        \"u\": ").append(o.regionU).append(",\n");
            sb.append("        \"v\": ").append(o.regionV).append(",\n");
            sb.append("        \"width\": ").append(o.regionW).append(",\n");
            sb.append("        \"height\": ").append(o.regionH).append(",\n");
            sb.append("        \"layer\": \"").append(escapeJson(o.layer)).append("\"\n");
            sb.append("      },\n");
            sb.append("      \"physics\": {\n");
            sb.append("        \"segments\": ").append(o.segments).append(",\n");
            sb.append("        \"segment_length\": ").append(fmt(pixelSegmentLength(o))).append(",\n");
            sb.append("        \"length_scale\": ").append(fmt(o.segmentLength)).append(",\n");
            sb.append("        \"stiffness\": ").append(fmt(o.stiffness)).append(",\n");
            sb.append("        \"gravity\": ").append(fmt(o.gravity)).append(",\n");
            sb.append("        \"damping\": ").append(fmt(o.damping)).append(",\n");
            sb.append("        \"move_response\": ").append(fmt(o.moveResponse)).append(",\n");
            sb.append("        \"wind_response\": ").append(fmt(o.windResponse)).append(",\n");
            sb.append("        \"wind_direction\": ").append(fmt(o.windDirection)).append("\n");
            sb.append("      },\n");
            sb.append("      \"render\": {\n");
            sb.append("        \"thickness\": ").append(fmt(o.thickness)).append(",\n");
            sb.append("        \"style\": \"").append(escapeJson(o.style)).append("\",\n");
            sb.append("        \"motion_intensity\": ").append(fmt(o.motionIntensity)).append("\n");
            sb.append("      }\n");
            sb.append("    }");
            exported++;
        }

        if (exported > 0) sb.append("\n");
        sb.append("  ]\n");
        sb.append("}\n");

        Files.createDirectories(out.toPath().getParent());
        Files.writeString(out.toPath(), sb.toString(), StandardCharsets.UTF_8);
    }

    private static String fmt(float v) {
        
        String s = String.format(Locale.ROOT, "%.4f", v);
        s = s.replaceAll("0+$", "").replaceAll("\\.$", ".0");
        return s;
    }

    private static float pixelSegmentLength(OriginEntry o) {
        float totalLength = Math.max(1, o.regionH) / 16.0f * Math.max(0.05f, o.segmentLength);
        return totalLength / Math.max(1, o.segments);
    }

    private static Map<String, OriginEntry> anchorMap(List<OriginEntry> origins) {
        Map<String, OriginEntry> anchors = new HashMap<>();
        for (OriginEntry origin : origins) {
            if (origin.anchorOnly) {
                anchors.put(origin.id, origin);
            }
        }
        return anchors;
    }

    private static OriginEntry rootForExport(OriginEntry origin, Map<String, OriginEntry> anchors) {
        if (origin.anchorOnly) return origin;

        OriginEntry anchor = anchors.get(parentAnchorId(origin.id));
        return anchor == null ? origin : anchor;
    }

    private static String parentAnchorId(String id) {
        if (id == null) return null;
        Matcher matcher = ATTACHED_HAIR_ID.matcher(id);
        return matcher.matches() ? matcher.group(1) : null;
    }

    private static SkinUvMapper.SurfacePoint surfacePoint(OriginEntry origin) {
        return SkinUvMapper.pointForRegion(new SkinRegion(
            origin.regionU, origin.regionV, origin.regionW, origin.regionH, origin.layer));
    }

    private static String boneForExport(OriginEntry origin, SkinUvMapper.SurfacePoint point) {
        String bone = origin.bone == null ? "" : origin.bone.trim();
        if (!bone.isBlank() && !"head".equalsIgnoreCase(bone)) {
            return bone;
        }

        String inferred = point.bone();
        return inferred == null || inferred.isBlank() ? "head" : inferred;
    }

    private static float[] offsetForExport(OriginEntry origin, SkinUvMapper.SurfacePoint point) {
        if (isDefaultOffset(origin)
                || ("head".equalsIgnoreCase(origin.bone) && !"head".equals(point.bone()))) {
            return new float[]{point.offsetX(), point.offsetY(), point.offsetZ()};
        }
        return new float[]{origin.offsetX, origin.offsetY, origin.offsetZ};
    }

    private static boolean isDefaultOffset(OriginEntry origin) {
        return Math.abs(origin.offsetX) < 0.0001f
            && Math.abs(origin.offsetY - 0.25f) < 0.0001f
            && Math.abs(origin.offsetZ + 0.25f) < 0.0001f;
    }

    private static boolean isNearSurfacePoint(OriginEntry origin, SkinUvMapper.SurfacePoint point) {
        return point.bone().equalsIgnoreCase(origin.bone)
            && Math.abs(origin.offsetX - point.offsetX()) <= 0.04f
            && Math.abs(origin.offsetY - point.offsetY()) <= 0.04f
            && Math.abs(origin.offsetZ - point.offsetZ()) <= 0.04f;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String sanitizePresetName(String name) {
        String cleaned = name == null ? "" : name.trim();
        if (cleaned.endsWith(PRESET_SUFFIX)) {
            cleaned = cleaned.substring(0, cleaned.length() - PRESET_SUFFIX.length());
        } else if (cleaned.endsWith(".json")) {
            cleaned = cleaned.substring(0, cleaned.length() - ".json".length());
        }
        cleaned = cleaned.replaceAll("[^A-Za-z0-9._-]+", "_");
        cleaned = cleaned.replaceAll("^_+|_+$", "");
        return cleaned.isBlank() ? "hair" : cleaned;
    }
}
