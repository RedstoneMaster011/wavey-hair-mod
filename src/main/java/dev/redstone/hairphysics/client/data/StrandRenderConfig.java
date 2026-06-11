package dev.redstone.hairphysics.client.data;




public class StrandRenderConfig {

    public enum Style {
        RIBBON,  
        BOX      
    }

    public final float thickness;   
    public final Style style;       
    public final boolean billboard; 
    public final float motionIntensity; 

    public StrandRenderConfig(float thickness, String style, boolean billboard) {
        this(thickness, style, billboard, 0.65f);
    }

    public StrandRenderConfig(float thickness, String style, boolean billboard, float motionIntensity) {
        this.thickness = Math.max(0.01f, thickness);
        this.style = parseStyle(style);
        this.billboard = billboard;
        this.motionIntensity = Math.min(2.0f, Math.max(0.0f, motionIntensity));
    }

    private static Style parseStyle(String s) {
        if (s == null) return Style.RIBBON;
        return switch (s.toLowerCase()) {
            case "box" -> Style.BOX;
            default -> Style.RIBBON;
        };
    }

    
    public static StrandRenderConfig defaults() {
        return new StrandRenderConfig(0.05f, "ribbon", true, 0.65f);
    }

    public float sheetWidthForPixels(int pixelWidth) {
        float baseWidth = Math.max(1, pixelWidth) / 16.0f;
        float scale = thickness / 0.05f;
        return Math.max(1.0f / 32.0f, baseWidth * scale);
    }

    public float sheetDepth() {
        return Math.max(1.0f / 32.0f, thickness);
    }

    @Override
    public String toString() {
        return "RenderConfig{thickness=" + thickness + ", style=" + style +
               ", billboard=" + billboard + ", motion=" + motionIntensity + "}";
    }
}
