package dev.redstone.hairphysics.client.render;

import dev.redstone.hairphysics.client.HairphysicsClient;
import dev.redstone.hairphysics.client.data.SkinRegion;
import dev.redstone.hairphysics.client.data.SkinUvMapper;
import dev.redstone.hairphysics.client.data.StrandRenderConfig;
import dev.redstone.hairphysics.client.data.StrandOrigin;
import dev.redstone.hairphysics.client.physics.PhysicsParticle;
import dev.redstone.hairphysics.client.physics.PhysicsTickHandler;
import dev.redstone.hairphysics.client.physics.StrandSimulation;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerSkinType;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.UUID;











public class HairFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {

    private static final float HEAD_CENTER_Y = -0.25f;
    private static final float BODY_CENTER_Y = 0.375f;
    private static final float ARM_CENTER_Y = 0.25f;
    private static final float LEG_CENTER_Y = 0.375f;
    private static final float MODEL_PIXEL = 1.0f / 16.0f;
    private static final float HEAD_ROOT_VISUAL_LIFT = MODEL_PIXEL * 0.5f;
    private static final float BODY_ROOT_VISUAL_LIFT = MODEL_PIXEL;
    private static final float BODY_ROOT_VISUAL_DROP = MODEL_PIXEL;
    private static final float BODY_SURFACE_TUCK = MODEL_PIXEL * 0.25f;
    private static final float BODY_Z_FIGHT_NUDGE = MODEL_PIXEL * 0.06f;
    private static final float BODY_ANCHOR_LOCK_LENGTH = MODEL_PIXEL * 2.0f;
    private static final float BODY_ANCHOR_SMOOTH_LENGTH = MODEL_PIXEL * 2.0f;

    
    private final PlayerEntityRenderer<?> renderer;

    public HairFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> ctx) {
        super(ctx);
        
        this.renderer = (PlayerEntityRenderer<?>) ctx;
    }

    @Override
    public void render(
            MatrixStack matrices,
            OrderedRenderCommandQueue queue,
            int light,
            PlayerEntityRenderState state,
            float limbAngle,
            float limbDistance
    ) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        
        Entity entity = mc.world.getEntityById(state.id);
        if (!(entity instanceof AbstractClientPlayerEntity player)) return;
        if (!HairphysicsClient.shouldDisplayHairFor(player)) return;

        
        Identifier skinId = state.skinTextures.body().texturePath();
        if (skinId == null) return;

        
        boolean slim = false;
        try {
            PlayerSkinType skinType = state.skinTextures.model();
            slim = skinType != null && skinType.name().equalsIgnoreCase("slim");
        } catch (Exception ignored) {}
        UUID uuid = player.getUuid();
        SkinTextureCache.store(uuid, skinId, slim);

        List<StrandSimulation> sims = PhysicsTickHandler.MANAGER.get(uuid);
        if (sims.isEmpty()) return;

        RenderLayer renderLayer = RenderLayers.entityTranslucent(skinId, false);

        
        VertexConsumerProvider.Immediate consumers = mc.getBufferBuilders().getEntityVertexConsumers();
        VertexConsumer vc = consumers.getBuffer(renderLayer);

        float tickDelta = mc.getRenderTickCounter().getTickProgress(true);

        PlayerEntityModel model = getContextModel();

        for (StrandSimulation sim : sims) {
            matrices.push();
            model.getRootPart().applyTransform(matrices);
            BoneFrame bone = applyBoneTransform(model, sim.definition.origin, matrices);
            renderStrand(sim, player, matrices, vc, light, tickDelta, bone, skinId);
            matrices.pop();
        }
    }

    private void renderStrand(
            StrandSimulation sim,
            AbstractClientPlayerEntity player,
            MatrixStack matrices,
            VertexConsumer vc,
            int light,
            float tickDelta,
            BoneFrame bone,
            Identifier skinId
    ) {
        PhysicsParticle[] particles = sim.particles;
        if (particles.length < 2) return;

        SkinRegion region = sim.definition.skinRegion;
        StrandRenderConfig renderCfg = sim.definition.render;
        int segCount = particles.length - 1;
        float columnWidth = renderCfg.sheetWidthForPixels(1);
        float totalRestLength = sim.totalRestLength();
        StrandOrigin origin = sim.definition.origin;
        SkinUvMapper.SurfaceNormal pivotNormal = normalForOrigin(origin);
        float pivotLocalX = surfaceTuckedOffsetX(bone, origin.offsetX, pivotNormal);
        float pivotLocalY = bone.centerLocalY() - origin.offsetY - bone.rootVisualLift() + bone.rootVisualDrop();
        float pivotLocalZ = surfaceTuckedOffsetZ(bone, origin.offsetZ, pivotNormal);

        PhysicsParticle root = particles[0];
        double rootWorldX = root.lerpX(tickDelta);
        double rootWorldY = root.lerpY(tickDelta);
        double rootWorldZ = root.lerpZ(tickDelta);

        float yaw = (float) Math.toRadians(renderYaw(player, bone));
        float pitch = bone.usesHeadPitch() ? (float) Math.toRadians(player.getPitch()) : 0.0f;
        float cosYaw = (float) Math.cos(yaw);
        float sinYaw = (float) Math.sin(yaw);
        float cosPitch = (float) Math.cos(pitch);
        float sinPitch = (float) Math.sin(pitch);

        int columns = Math.max(1, region.width);
        for (int column = 0; column < columns; column++) {
            int skinU = region.u + column;
            float u0 = skinU / 64.0f;
            float u1 = (skinU + 1) / 64.0f;
            int row = 0;

            while (row < region.height) {
                while (row < region.height && !SkinPixelCache.isVisible(skinId, skinU, region.v + row)) {
                    row++;
                }
                if (row >= region.height) {
                    break;
                }

                int runStart = row;
                SkinUvMapper.SurfaceFrame frame = SkinUvMapper.frameFor(skinU, region.v + runStart);
                row++;
                while (row < region.height
                    && SkinPixelCache.isVisible(skinId, skinU, region.v + row)
                    && sameSurfaceFrame(frame, SkinUvMapper.frameFor(skinU, region.v + row))) {
                    row++;
                }

                int runHeight = row - runStart;
                renderColumnRun(sim, matrices, vc, light, tickDelta, bone, particles,
                    rootWorldX, rootWorldY, rootWorldZ, totalRestLength,
                    pivotLocalX, pivotLocalY, pivotLocalZ,
                    cosYaw, sinYaw, cosPitch, sinPitch,
                    renderCfg, columnWidth, segCount, frame, skinU, region.v + runStart,
                    runHeight, u0, u1);
            }
        }
    }

    private static void renderColumnRun(
            StrandSimulation sim,
            MatrixStack matrices,
            VertexConsumer vc,
            int light,
            float tickDelta,
            BoneFrame bone,
            PhysicsParticle[] particles,
            double rootWorldX,
            double rootWorldY,
            double rootWorldZ,
            float totalRestLength,
            float pivotLocalX,
            float pivotLocalY,
            float pivotLocalZ,
            float cosYaw,
            float sinYaw,
            float cosPitch,
            float sinPitch,
            StrandRenderConfig renderCfg,
            float columnWidth,
            int segCount,
            SkinUvMapper.SurfaceFrame frame,
            int skinU,
            int skinV,
            int runHeight,
            float u0,
            float u1
    ) {
        RenderSurfaceFrame renderFrame = renderSurfaceFrame(bone, sim.definition.origin, frame);
        SkinUvMapper.SurfaceNormal normal = renderFrame.normal();
        SkinUvMapper.SurfaceNormal tangent = renderFrame.tangent();
        float columnLocalX = surfaceTuckedOffsetX(bone, renderFrame.offsetX(), normal);
        float columnLocalY = bone.centerLocalY() - renderFrame.offsetY() - bone.rootVisualLift() + bone.rootVisualDrop();
        float columnLocalZ = surfaceTuckedOffsetZ(bone, renderFrame.offsetZ(), normal);
        float columnStartDown = Math.max(0.0f, columnLocalY - pivotLocalY);
        float clusterLength = Math.max(1, runHeight) * MODEL_PIXEL * sim.definition.physics.lengthScale;
        int renderSegments = Math.max(1, Math.max(runHeight, Math.min(segCount, runHeight * 2)));
        float v0 = skinV / 64.0f;
        float v1 = (skinV + runHeight) / 64.0f;
        boolean capEnds = "head".equals(bone.name());

        for (int i = 0; i < renderSegments; i++) {
            float aPart = (float) i / renderSegments;
            float bPart = (float)(i + 1) / renderSegments;
            float aBaseY = columnLocalY + clusterLength * aPart;
            float bBaseY = columnLocalY + clusterLength * bPart;
            float aRestDown = columnStartDown + clusterLength * aPart;
            float bRestDown = columnStartDown + clusterLength * bPart;
            float aMotion = anchorSmoothedMotion(bone, renderCfg.motionIntensity, aRestDown);
            float bMotion = anchorSmoothedMotion(bone, renderCfg.motionIntensity, bRestDown);

            float[] aLocal = toHeadLocal(
                particles, tickDelta, rootWorldX, rootWorldY, rootWorldZ,
                totalRestLength, aRestDown,
                pivotLocalX, pivotLocalY, pivotLocalZ,
                columnLocalX, aBaseY, columnLocalZ,
                cosYaw, sinYaw, cosPitch, sinPitch, aMotion
            );
            float[] bLocal = toHeadLocal(
                particles, tickDelta, rootWorldX, rootWorldY, rootWorldZ,
                totalRestLength, bRestDown,
                pivotLocalX, pivotLocalY, pivotLocalZ,
                columnLocalX, bBaseY, columnLocalZ,
                cosYaw, sinYaw, cosPitch, sinPitch, bMotion
            );

            float segV0 = v0 + (v1 - v0) * aPart;
            float segV1 = v0 + (v1 - v0) * bPart;

            float segmentDepth = anchorSmoothedDepth(bone, renderCfg.sheetDepth(), (aRestDown + bRestDown) * 0.5f);

            StrandMeshBuilder.emitOrientedSheetSegment(vc, matrices,
                aLocal[0], aLocal[1], aLocal[2],
                bLocal[0], bLocal[1], bLocal[2],
                columnWidth, segmentDepth,
                tangent.x(), tangent.y(), tangent.z(),
                normal.x(), normal.y(), normal.z(),
                capEnds && i == 0, capEnds && i == renderSegments - 1,
                u0, segV0, u1, segV1, 255, light);
        }
    }

    private static BoneFrame applyBoneTransform(PlayerEntityModel model, StrandOrigin origin, MatrixStack matrices) {
        BoneFrame bone = boneFrame(origin.bone);
        ModelPart part = switch (bone.name()) {
            case "body" -> model.body;
            case "left_arm" -> model.leftArm;
            case "right_arm" -> model.rightArm;
            case "left_leg" -> model.leftLeg;
            case "right_leg" -> model.rightLeg;
            default -> model.head;
        };
        part.applyTransform(matrices);
        return bone;
    }

    private static BoneFrame boneFrame(String rawBone) {
        String bone = normalizeBone(rawBone);
        return switch (bone) {
            case "body" -> new BoneFrame(bone, BODY_CENTER_Y, false, BODY_ROOT_VISUAL_LIFT, BODY_ROOT_VISUAL_DROP);
            case "left_arm", "right_arm" -> new BoneFrame(bone, ARM_CENTER_Y, false, BODY_ROOT_VISUAL_LIFT, 0.0f);
            case "left_leg", "right_leg" -> new BoneFrame(bone, LEG_CENTER_Y, false, BODY_ROOT_VISUAL_LIFT, 0.0f);
            default -> new BoneFrame("head", HEAD_CENTER_Y, true, HEAD_ROOT_VISUAL_LIFT, 0.0f);
        };
    }

    private static String normalizeBone(String rawBone) {
        if (rawBone == null) return "head";
        return switch (rawBone.trim().toLowerCase()) {
            case "body", "torso", "chest", "jacket" -> "body";
            case "left_arm", "leftarm", "l_arm", "left sleeve", "left_sleeve" -> "left_arm";
            case "right_arm", "rightarm", "r_arm", "right sleeve", "right_sleeve" -> "right_arm";
            case "left_leg", "leftleg", "l_leg", "left pants", "left_pants" -> "left_leg";
            case "right_leg", "rightleg", "r_leg", "right pants", "right_pants" -> "right_leg";
            default -> "head";
        };
    }

    private record BoneFrame(String name, float centerLocalY, boolean usesHeadPitch,
                             float rootVisualLift, float rootVisualDrop) {}

    private static float renderYaw(AbstractClientPlayerEntity player, BoneFrame bone) {
        return bone.usesHeadPitch() ? player.getHeadYaw() : player.getBodyYaw();
    }

    private record RenderSurfaceFrame(float offsetX, float offsetY, float offsetZ,
                                      SkinUvMapper.SurfaceNormal normal,
                                      SkinUvMapper.SurfaceNormal tangent) {}

    private static float[] toHeadLocal(
            PhysicsParticle[] particles,
            float tickDelta,
            double rootWorldX,
            double rootWorldY,
            double rootWorldZ,
            float totalRestLength,
            float restDown,
            float rootLocalX,
            float rootLocalY,
            float rootLocalZ,
            float baseLocalX,
            float baseLocalY,
            float baseLocalZ,
            float cosYaw,
            float sinYaw,
            float cosPitch,
            float sinPitch,
            float motionIntensity
    ) {
        double[] sample = sampleParticle(particles, tickDelta, totalRestLength, restDown);
        double dx = sample[0] - rootWorldX;
        double dy = sample[1] - rootWorldY;
        double dz = sample[2] - rootWorldZ;

        double playerX = dx * cosYaw + dz * sinYaw;
        double playerY = dy;
        double playerZ = -dx * sinYaw + dz * cosYaw;

        double expectedY = -restDown * cosPitch;
        double expectedZ = restDown * sinPitch;

        double devX = playerX;
        double devY = playerY - expectedY;
        double devZ = playerZ - expectedZ;

        double headX = devX;
        double headY = devY * cosPitch - devZ * sinPitch;
        double headZ = devY * sinPitch + devZ * cosPitch;

        float maxBend = Math.max(0.08f, restDown * 0.75f + 0.04f);
        float maxVerticalBend = Math.max(0.025f, restDown * 0.22f);
        float localDx = clamp((float) -headX * motionIntensity, -maxBend, maxBend);
        float localDy = clamp((float) -headY * motionIntensity * 0.28f, -maxVerticalBend, maxVerticalBend);
        float localDz = clamp((float) headZ * motionIntensity, -maxBend, maxBend);

        return new float[]{
            baseLocalX + localDx,
            baseLocalY + localDy,
            baseLocalZ + localDz
        };
    }

    private static double[] sampleParticle(PhysicsParticle[] particles, float tickDelta, float totalRestLength, float restDown) {
        if (particles.length == 0) {
            return new double[]{0.0, 0.0, 0.0};
        }
        if (particles.length == 1 || totalRestLength <= 1e-5f) {
            PhysicsParticle p = particles[0];
            return new double[]{p.lerpX(tickDelta), p.lerpY(tickDelta), p.lerpZ(tickDelta)};
        }

        float normalized = clamp(restDown / totalRestLength, 0.0f, 1.0f) * (particles.length - 1);
        int index = Math.min(particles.length - 2, Math.max(0, (int)Math.floor(normalized)));
        float t = normalized - index;
        PhysicsParticle a = particles[index];
        PhysicsParticle b = particles[index + 1];
        return new double[]{
            lerp(a.lerpX(tickDelta), b.lerpX(tickDelta), t),
            lerp(a.lerpY(tickDelta), b.lerpY(tickDelta), t),
            lerp(a.lerpZ(tickDelta), b.lerpZ(tickDelta), t)
        };
    }

    private static double lerp(double a, double b, float t) {
        return a + (b - a) * t;
    }

    private static boolean sameSurfaceFrame(SkinUvMapper.SurfaceFrame a, SkinUvMapper.SurfaceFrame b) {
        return a.point().bone().equals(b.point().bone())
            && a.point().layer().equals(b.point().layer())
            && close(a.normal().x(), b.normal().x())
            && close(a.normal().y(), b.normal().y())
            && close(a.normal().z(), b.normal().z())
            && close(a.uTangent().x(), b.uTangent().x())
            && close(a.uTangent().y(), b.uTangent().y())
            && close(a.uTangent().z(), b.uTangent().z());
    }

    private static boolean close(float a, float b) {
        return Math.abs(a - b) < 0.0001f;
    }

    private static RenderSurfaceFrame renderSurfaceFrame(
            BoneFrame bone,
            StrandOrigin origin,
            SkinUvMapper.SurfaceFrame skinFrame
    ) {
        SkinUvMapper.SurfacePoint point = skinFrame.point();
        if (!"body".equals(bone.name()) || origin == null) {
            return new RenderSurfaceFrame(point.offsetX(), point.offsetY(), point.offsetZ(),
                skinFrame.normal(), skinFrame.uTangent());
        }

        SkinUvMapper.SurfaceNormal normal = normalForOrigin(origin);
        SkinUvMapper.SurfaceNormal tangent = tangentForNormal(normal, skinFrame.uTangent());
        float offsetX = point.offsetX();
        float offsetZ = point.offsetZ();

        if (Math.abs(normal.z()) >= Math.abs(normal.x()) && Math.abs(normal.z()) > 0.0001f) {
            offsetZ = origin.offsetZ;
        } else if (Math.abs(normal.x()) > 0.0001f) {
            offsetX = origin.offsetX;
        }

        return new RenderSurfaceFrame(offsetX, point.offsetY(), offsetZ, normal, tangent);
    }

    private static SkinUvMapper.SurfaceNormal tangentForNormal(
            SkinUvMapper.SurfaceNormal normal,
            SkinUvMapper.SurfaceNormal fallback
    ) {
        if (Math.abs(normal.z()) >= Math.abs(normal.x()) && Math.abs(normal.z()) > 0.0001f) {
            return new SkinUvMapper.SurfaceNormal(1.0f, 0.0f, 0.0f);
        }
        if (Math.abs(normal.x()) > 0.0001f) {
            return new SkinUvMapper.SurfaceNormal(0.0f, 0.0f, normal.x() > 0.0f ? -1.0f : 1.0f);
        }
        return fallback;
    }

    private static float anchorSmoothedMotion(BoneFrame bone, float motion, float restDown) {
        if (!"body".equals(bone.name())) {
            return motion;
        }
        return motion * anchorSmoothFactor(restDown);
    }

    private static float anchorSmoothedDepth(BoneFrame bone, float depth, float restDown) {
        return depth;
    }

    private static float anchorSmoothFactor(float restDown) {
        float t = clamp((restDown - BODY_ANCHOR_LOCK_LENGTH) / BODY_ANCHOR_SMOOTH_LENGTH, 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    private static SkinUvMapper.SurfaceNormal normalForOrigin(StrandOrigin origin) {
        if (origin == null) {
            return new SkinUvMapper.SurfaceNormal(0.0f, 0.0f, -1.0f);
        }
        if (Math.abs(origin.offsetZ) >= 0.12f) {
            return new SkinUvMapper.SurfaceNormal(0.0f, 0.0f, Math.signum(origin.offsetZ));
        }
        if (Math.abs(origin.offsetX) >= 0.12f) {
            return new SkinUvMapper.SurfaceNormal(Math.signum(origin.offsetX), 0.0f, 0.0f);
        }
        return new SkinUvMapper.SurfaceNormal(0.0f, 0.0f, -1.0f);
    }

    private static float surfaceTuckedOffsetX(BoneFrame bone, float offsetX, SkinUvMapper.SurfaceNormal normal) {
        if ("head".equals(bone.name()) || Math.abs(normal.x()) < 0.0001f) {
            return offsetX;
        }
        return tuckTowardBody(offsetX, normal.x());
    }

    private static float surfaceTuckedOffsetZ(BoneFrame bone, float offsetZ, SkinUvMapper.SurfaceNormal normal) {
        if ("head".equals(bone.name()) || Math.abs(normal.z()) < 0.0001f) {
            return offsetZ;
        }
        return tuckTowardBody(offsetZ, normal.z());
    }

    private static float tuckTowardBody(float offset, float normal) {
        if (Math.abs(offset) < 0.0001f || Math.abs(normal) < 0.0001f) {
            return offset;
        }
        return offset - normal * BODY_SURFACE_TUCK + normal * BODY_Z_FIGHT_NUDGE;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
