package dev.redstone.hairphysics.client.data;








public class StrandOrigin {

    public final String bone;   
    public final float offsetX;
    public final float offsetY;
    public final float offsetZ;

    public StrandOrigin(String bone, float offsetX, float offsetY, float offsetZ) {
        this.bone = bone;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
    }

    
    public static StrandOrigin defaultHead() {
        return new StrandOrigin("head", 0.0f, 0.25f, -0.25f);
    }

    @Override
    public String toString() {
        return "StrandOrigin{bone='" + bone + "', offset=(" + offsetX + "," + offsetY + "," + offsetZ + ")}";
    }
}
