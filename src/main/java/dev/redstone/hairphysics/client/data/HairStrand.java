package dev.redstone.hairphysics.client.data;





public class HairStrand {

    public final String id;
    public final StrandOrigin origin;
    public final SkinRegion skinRegion;
    public final StrandPhysicsConfig physics;
    public final StrandRenderConfig render;
    public final boolean anchorOnly;

    public HairStrand(String id, StrandOrigin origin, SkinRegion skinRegion,
                      StrandPhysicsConfig physics, StrandRenderConfig render) {
        this(id, origin, skinRegion, physics, render, false);
    }

    public HairStrand(String id, StrandOrigin origin, SkinRegion skinRegion,
                      StrandPhysicsConfig physics, StrandRenderConfig render,
                      boolean anchorOnly) {
        this.id = id != null ? id : "strand";
        this.origin = origin != null ? origin : StrandOrigin.defaultHead();
        this.skinRegion = skinRegion != null ? skinRegion : SkinRegion.defaultHead();
        this.physics = physics != null ? physics : StrandPhysicsConfig.defaults();
        this.render = render != null ? render : StrandRenderConfig.defaults();
        this.anchorOnly = anchorOnly;
    }

    @Override
    public String toString() {
        return "HairStrand{id='" + id + "', anchorOnly=" + anchorOnly +
               ", origin=" + origin + ", region=" + skinRegion + "}";
    }
}
