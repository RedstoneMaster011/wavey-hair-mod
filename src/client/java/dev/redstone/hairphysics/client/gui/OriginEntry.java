package dev.redstone.hairphysics.client.gui;





public class OriginEntry {

    
    private static final int[] PALETTE = {
        0x80FF4444, 
        0x8044AAFF, 
        0x8044FF88, 
        0x80FFAA00, 
        0x80CC44FF, 
        0x80FF44CC, 
        0x8000DDDD, 
        0x80FFFF44, 
    };

    private static int nextColorIdx = 0;

    public static final int DEFAULT_SEGMENTS = 6;
    public static final float DEFAULT_LENGTH_SCALE = 1.0f;
    public static final float DEFAULT_STIFFNESS = 0.7f;
    public static final float DEFAULT_GRAVITY = 0.035f;
    public static final float DEFAULT_DAMPING = 0.85f;
    public static final float DEFAULT_MOVE_RESPONSE = 0.4f;
    public static final float DEFAULT_WIND_RESPONSE = 0.6211f;
    public static final float DEFAULT_WIND_DIRECTION = 1.0f;
    public static final float DEFAULT_THICKNESS = 0.05f;
    public static final float DEFAULT_MOTION_INTENSITY = 0.65f;

    public String id;
    public final boolean anchorOnly;

    
    public String bone = "head";
    public float offsetX = 0.0f;
    public float offsetY = 0.25f;
    public float offsetZ = -0.25f;

    
    public int regionU = 32;
    public int regionV = 0;
    public int regionW = 8;
    public int regionH = 8;
    public String layer = "outer";

    
    public int segments = DEFAULT_SEGMENTS;
    public float segmentLength = DEFAULT_LENGTH_SCALE;
    public float stiffness = DEFAULT_STIFFNESS;
    public float gravity = DEFAULT_GRAVITY;
    public float damping = DEFAULT_DAMPING;
    public float moveResponse = DEFAULT_MOVE_RESPONSE;
    public float windResponse = DEFAULT_WIND_RESPONSE;
    public float windDirection = DEFAULT_WIND_DIRECTION;

    
    public float thickness = DEFAULT_THICKNESS;
    public float motionIntensity = DEFAULT_MOTION_INTENSITY;
    public String style = "ribbon";

    
    public final int color;

    public OriginEntry(String id) {
        this(id, false);
    }

    public OriginEntry(String id, boolean anchorOnly) {
        this.id = id;
        this.anchorOnly = anchorOnly;
        if (anchorOnly) {
            this.color = 0;
        } else {
            this.color = PALETTE[nextColorIdx % PALETTE.length];
            nextColorIdx++;
        }
    }

    public static void resetColorCycle() {
        nextColorIdx = 0;
    }

    
    public int solidColor() {
        return anchorOnly ? 0xFFB0B0B0 : color | 0xFF000000;
    }

    @Override
    public String toString() {
        return id;
    }
}
