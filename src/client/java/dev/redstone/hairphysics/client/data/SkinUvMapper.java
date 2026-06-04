package dev.redstone.hairphysics.client.data;




public final class SkinUvMapper {
    private static final int X = 0;
    private static final int Y = 1;
    private static final int Z = 2;
    private static final float SURFACE_LIFT = 0.5f / 16.0f;
    private static final float NEAR_SURFACE_TOLERANCE = 0.04f;

    private static final Face[] FACES = {
        
        face("head", 8, 0, 8, 8, 32, 0, X, -0.25f, 0.25f, Z, 0.25f, -0.25f, Y, 0.25f, 1.0f),
        face("head", 16, 0, 8, 8, 32, 0, X, -0.25f, 0.25f, Z, -0.25f, 0.25f, Y, -0.25f, -1.0f),
        face("head", 0, 8, 8, 8, 32, 0, Z, 0.25f, -0.25f, Y, 0.25f, -0.25f, X, -0.25f, -1.0f),
        face("head", 8, 8, 8, 8, 32, 0, X, -0.25f, 0.25f, Y, 0.25f, -0.25f, Z, -0.25f, -1.0f),
        face("head", 16, 8, 8, 8, 32, 0, Z, -0.25f, 0.25f, Y, 0.25f, -0.25f, X, 0.25f, 1.0f),
        face("head", 24, 8, 8, 8, 32, 0, X, 0.25f, -0.25f, Y, 0.25f, -0.25f, Z, 0.25f, 1.0f),

        
        face("body", 20, 16, 8, 4, 0, 16, X, -0.25f, 0.25f, Z, 0.125f, -0.125f, Y, 0.375f, 1.0f),
        face("body", 28, 16, 8, 4, 0, 16, X, -0.25f, 0.25f, Z, -0.125f, 0.125f, Y, -0.375f, -1.0f),
        face("body", 16, 20, 4, 12, 0, 16, Z, 0.125f, -0.125f, Y, 0.375f, -0.375f, X, -0.25f, -1.0f),
        face("body", 20, 20, 8, 12, 0, 16, X, -0.25f, 0.25f, Y, 0.375f, -0.375f, Z, -0.125f, -1.0f),
        face("body", 28, 20, 4, 12, 0, 16, Z, -0.125f, 0.125f, Y, 0.375f, -0.375f, X, 0.25f, 1.0f),
        face("body", 32, 20, 8, 12, 0, 16, X, 0.25f, -0.25f, Y, 0.375f, -0.375f, Z, 0.125f, 1.0f),

        
        face("right_arm", 44, 16, 4, 4, 0, 16, X, -0.125f, 0.125f, Z, 0.125f, -0.125f, Y, 0.375f, 1.0f),
        face("right_arm", 48, 16, 4, 4, 0, 16, X, -0.125f, 0.125f, Z, -0.125f, 0.125f, Y, -0.375f, -1.0f),
        face("right_arm", 40, 20, 4, 12, 0, 16, Z, 0.125f, -0.125f, Y, 0.375f, -0.375f, X, -0.125f, -1.0f),
        face("right_arm", 44, 20, 4, 12, 0, 16, X, -0.125f, 0.125f, Y, 0.375f, -0.375f, Z, -0.125f, -1.0f),
        face("right_arm", 48, 20, 4, 12, 0, 16, Z, -0.125f, 0.125f, Y, 0.375f, -0.375f, X, 0.125f, 1.0f),
        face("right_arm", 52, 20, 4, 12, 0, 16, X, 0.125f, -0.125f, Y, 0.375f, -0.375f, Z, 0.125f, 1.0f),

        
        face("right_leg", 4, 16, 4, 4, 0, 16, X, -0.125f, 0.125f, Z, 0.125f, -0.125f, Y, 0.375f, 1.0f),
        face("right_leg", 8, 16, 4, 4, 0, 16, X, -0.125f, 0.125f, Z, -0.125f, 0.125f, Y, -0.375f, -1.0f),
        face("right_leg", 0, 20, 4, 12, 0, 16, Z, 0.125f, -0.125f, Y, 0.375f, -0.375f, X, -0.125f, -1.0f),
        face("right_leg", 4, 20, 4, 12, 0, 16, X, -0.125f, 0.125f, Y, 0.375f, -0.375f, Z, -0.125f, -1.0f),
        face("right_leg", 8, 20, 4, 12, 0, 16, Z, -0.125f, 0.125f, Y, 0.375f, -0.375f, X, 0.125f, 1.0f),
        face("right_leg", 12, 20, 4, 12, 0, 16, X, 0.125f, -0.125f, Y, 0.375f, -0.375f, Z, 0.125f, 1.0f),

        
        face("left_arm", 36, 48, 4, 4, 16, 0, X, -0.125f, 0.125f, Z, 0.125f, -0.125f, Y, 0.375f, 1.0f),
        face("left_arm", 40, 48, 4, 4, 16, 0, X, -0.125f, 0.125f, Z, -0.125f, 0.125f, Y, -0.375f, -1.0f),
        face("left_arm", 32, 52, 4, 12, 16, 0, Z, 0.125f, -0.125f, Y, 0.375f, -0.375f, X, -0.125f, -1.0f),
        face("left_arm", 36, 52, 4, 12, 16, 0, X, -0.125f, 0.125f, Y, 0.375f, -0.375f, Z, -0.125f, -1.0f),
        face("left_arm", 40, 52, 4, 12, 16, 0, Z, -0.125f, 0.125f, Y, 0.375f, -0.375f, X, 0.125f, 1.0f),
        face("left_arm", 44, 52, 4, 12, 16, 0, X, 0.125f, -0.125f, Y, 0.375f, -0.375f, Z, 0.125f, 1.0f),

        
        face("left_leg", 20, 48, 4, 4, -16, 0, X, -0.125f, 0.125f, Z, 0.125f, -0.125f, Y, 0.375f, 1.0f),
        face("left_leg", 24, 48, 4, 4, -16, 0, X, -0.125f, 0.125f, Z, -0.125f, 0.125f, Y, -0.375f, -1.0f),
        face("left_leg", 16, 52, 4, 12, -16, 0, Z, 0.125f, -0.125f, Y, 0.375f, -0.375f, X, -0.125f, -1.0f),
        face("left_leg", 20, 52, 4, 12, -16, 0, X, -0.125f, 0.125f, Y, 0.375f, -0.375f, Z, -0.125f, -1.0f),
        face("left_leg", 24, 52, 4, 12, -16, 0, Z, -0.125f, 0.125f, Y, 0.375f, -0.375f, X, 0.125f, 1.0f),
        face("left_leg", 28, 52, 4, 12, -16, 0, X, 0.125f, -0.125f, Y, 0.375f, -0.375f, Z, 0.125f, 1.0f),
    };

    private SkinUvMapper() {}

    public record SurfacePoint(String bone, String layer, float offsetX, float offsetY, float offsetZ) {}
    public record SurfaceNormal(float x, float y, float z) {}
    public record SurfaceFrame(SurfacePoint point, SurfaceNormal normal, SurfaceNormal uTangent) {}

    public static SurfacePoint pointForRegion(SkinRegion region) {
        int u = region.u + Math.max(0, region.width - 1) / 2;
        int v = region.v + Math.max(0, region.height - 1) / 2;
        return pointFor(u, v);
    }

    public static SurfacePoint pointFor(int u, int v) {
        for (Face face : FACES) {
            SurfacePoint outer = face.pointAt(u, v, true);
            if (outer != null) return outer;
            SurfacePoint inner = face.pointAt(u, v, false);
            if (inner != null) return inner;
        }
        return new SurfacePoint("head", "outer", 0.0f, 0.25f, -0.25f);
    }

    public static SurfaceNormal normalForRegion(SkinRegion region) {
        int u = region.u + Math.max(0, region.width - 1) / 2;
        int v = region.v + Math.max(0, region.height - 1) / 2;
        return normalFor(u, v);
    }

    public static SurfaceNormal normalFor(int u, int v) {
        for (Face face : FACES) {
            if (face.contains(u, v, true) || face.contains(u, v, false)) {
                return face.normal();
            }
        }
        return new SurfaceNormal(0.0f, 0.0f, -1.0f);
    }

    public static SurfaceFrame frameFor(int u, int v) {
        for (Face face : FACES) {
            SurfacePoint outer = face.pointAt(u, v, true);
            if (outer != null) return new SurfaceFrame(outer, face.normal(), face.uTangent());
            SurfacePoint inner = face.pointAt(u, v, false);
            if (inner != null) return new SurfaceFrame(inner, face.normal(), face.uTangent());
        }
        SurfacePoint fallback = new SurfacePoint("head", "outer", 0.0f, 0.25f, -0.25f);
        return new SurfaceFrame(fallback, new SurfaceNormal(0.0f, 0.0f, -1.0f), new SurfaceNormal(1.0f, 0.0f, 0.0f));
    }

    public static boolean isDefaultOffset(StrandOrigin origin) {
        return Math.abs(origin.offsetX) < 0.0001f
            && Math.abs(origin.offsetY - 0.25f) < 0.0001f
            && Math.abs(origin.offsetZ + 0.25f) < 0.0001f;
    }

    public static boolean isNearSurfacePoint(StrandOrigin origin, SurfacePoint point) {
        return point.bone().equalsIgnoreCase(origin.bone)
            && Math.abs(origin.offsetX - point.offsetX()) <= NEAR_SURFACE_TOLERANCE
            && Math.abs(origin.offsetY - point.offsetY()) <= NEAR_SURFACE_TOLERANCE
            && Math.abs(origin.offsetZ - point.offsetZ()) <= NEAR_SURFACE_TOLERANCE;
    }

    private static Face face(String bone, int x, int y, int w, int h, int outerDx, int outerDy,
                             int uAxis, float uMin, float uMax,
                             int vAxis, float vMin, float vMax,
                             int constAxis, float constValue, float normalSign) {
        return new Face(bone, x, y, w, h, outerDx, outerDy,
            uAxis, uMin, uMax, vAxis, vMin, vMax, constAxis, constValue, normalSign);
    }

    private record Face(
            String bone, int x, int y, int w, int h, int outerDx, int outerDy,
            int uAxis, float uMin, float uMax,
            int vAxis, float vMin, float vMax,
            int constAxis, float constValue, float normalSign
    ) {
        SurfacePoint pointAt(int px, int py, boolean outer) {
            if (!contains(px, py, outer)) {
                return null;
            }

            int fx = x + (outer ? outerDx : 0);
            int fy = y + (outer ? outerDy : 0);
            float[] coords = new float[3];
            coords[uAxis] = lerp(uMin, uMax, (px - fx + 0.5f) / w);
            coords[vAxis] = lerp(vMin, vMax, (py - fy + 0.5f) / h);
            coords[constAxis] = constValue + normalSign * SURFACE_LIFT;
            return new SurfacePoint(bone, outer ? "outer" : "inner", coords[X], coords[Y], coords[Z]);
        }

        boolean contains(int px, int py, boolean outer) {
            int fx = x + (outer ? outerDx : 0);
            int fy = y + (outer ? outerDy : 0);
            return px >= fx && py >= fy && px < fx + w && py < fy + h;
        }

        SurfaceNormal normal() {
            return switch (constAxis) {
                case X -> new SurfaceNormal(normalSign, 0.0f, 0.0f);
                case Y -> new SurfaceNormal(0.0f, normalSign, 0.0f);
                default -> new SurfaceNormal(0.0f, 0.0f, normalSign);
            };
        }

        SurfaceNormal uTangent() {
            float sign = Math.signum(uMax - uMin);
            if (Math.abs(sign) < 0.0001f) {
                sign = 1.0f;
            }
            return switch (uAxis) {
                case X -> new SurfaceNormal(sign, 0.0f, 0.0f);
                case Y -> new SurfaceNormal(0.0f, sign, 0.0f);
                default -> new SurfaceNormal(0.0f, 0.0f, sign);
            };
        }
    }

    private static float lerp(float min, float max, float t) {
        return min + (max - min) * t;
    }
}
