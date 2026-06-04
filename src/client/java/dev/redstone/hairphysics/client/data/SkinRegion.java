package dev.redstone.hairphysics.client.data;





public class SkinRegion {

    public final int u;         
    public final int v;         
    public final int width;     
    public final int height;    
    public final String layer;  

    public SkinRegion(int u, int v, int width, int height, String layer) {
        this.u = u;
        this.v = v;
        this.width = width;
        this.height = height;
        this.layer = layer != null ? layer : "outer";
    }

    
    public boolean isOuterLayer() {
        return "outer".equalsIgnoreCase(layer);
    }

    



    public float getLayerOffset() {
        return isOuterLayer() ? 0.5f / 64.0f : 0.0f;
    }

    
    public static SkinRegion defaultHead() {
        return new SkinRegion(32, 0, 8, 8, "outer");
    }

    @Override
    public String toString() {
        return "SkinRegion{u=" + u + ", v=" + v + ", w=" + width + ", h=" + height + ", layer='" + layer + "'}";
    }
}
