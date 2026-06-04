package dev.redstone.hairphysics.client.data;




public class StrandPhysicsConfig {

    public final int segments;          
    public final float segmentLength;   
    public final float lengthScale;     
    public final float stiffness;       
    public final float gravity;         
    public final float damping;         
    public final float moveResponse;    
    public final float windResponse;    
    public final float windDirection;   

    public StrandPhysicsConfig(int segments, float segmentLength, float stiffness,
                               float gravity, float damping, float windResponse) {
        this(segments, segmentLength, stiffness, gravity, damping, windResponse,
            segmentLength <= 0.5f ? 1.0f : segmentLength);
    }

    public StrandPhysicsConfig(int segments, float segmentLength, float stiffness,
                               float gravity, float damping, float windResponse,
                               float lengthScale) {
        this(segments, segmentLength, stiffness, gravity, damping, windResponse, windResponse,
            1.0f, lengthScale);
    }

    public StrandPhysicsConfig(int segments, float segmentLength, float stiffness,
                               float gravity, float damping, float moveResponse,
                               float windResponse, float windDirection, float lengthScale) {
        this.segments = Math.max(2, segments);
        this.segmentLength = Math.max(0.01f, segmentLength);
        this.lengthScale = Math.min(4.0f, Math.max(0.05f, lengthScale));
        this.stiffness = Math.min(1.0f, Math.max(0.0f, stiffness));
        this.gravity = gravity;
        this.damping = Math.min(1.0f, Math.max(0.0f, damping));
        this.moveResponse = Math.min(2.0f, Math.max(0.0f, moveResponse));
        this.windResponse = Math.min(2.0f, Math.max(0.0f, windResponse));
        this.windDirection = Math.min(1.0f, Math.max(-1.0f, windDirection));
    }

    
    public static StrandPhysicsConfig defaults() {
        return new StrandPhysicsConfig(6, 0.15f, 0.7f, 0.035f, 0.85f, 0.4f, 0.6211f, 1.0f, 1.0f);
    }

    @Override
    public String toString() {
        return "PhysicsConfig{segments=" + segments + ", scale=" + lengthScale +
               ", stiffness=" + stiffness + ", gravity=" + gravity +
               ", damping=" + damping + ", move=" + moveResponse +
               ", wind=" + windResponse + ", windDir=" + windDirection + "}";
    }
}
