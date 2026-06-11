package dev.redstone.hairphysics.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.redstone.hairphysics.client.data.StrandRenderConfig;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;
import org.joml.Vector3f;










public class StrandMeshBuilder {

    
    private static final int NO_OVERLAY = OverlayTexture.NO_OVERLAY;

    public static void emitSegment(
            VertexConsumer vc,
            PoseStack matrices,
            float ax, float ay, float az,
            float bx, float by, float bz,
            StrandRenderConfig cfg,
            float u0, float v0, float u1, float v1,
            int alpha, int light
    ) {
        if (cfg.style == StrandRenderConfig.Style.BOX) {
            emitBox(vc, matrices, ax, ay, az, bx, by, bz,
                    cfg.thickness, u0, v0, u1, v1, alpha, light);
        } else {
            emitRibbon(vc, matrices, ax, ay, az, bx, by, bz,
                       cfg.thickness, u0, v0, u1, v1, alpha, light);
        }
    }

    public static void emitSheetSegment(
            VertexConsumer vc,
            PoseStack matrices,
            float ax, float ay, float az,
            float bx, float by, float bz,
            float width,
            float depth,
            boolean capStart,
            boolean capEnd,
            float u0, float v0, float u1, float v1,
            int alpha, int light
    ) {
        emitOrientedSheetSegment(vc, matrices,
            ax, ay, az, bx, by, bz,
            width, depth, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f,
            capStart, capEnd, u0, v0, u1, v1, alpha, light);
    }

    public static void emitOrientedSheetSegment(
            VertexConsumer vc,
            PoseStack matrices,
            float ax, float ay, float az,
            float bx, float by, float bz,
            float width,
            float depth,
            float widthX, float widthY, float widthZ,
            float depthX, float depthY, float depthZ,
            boolean capStart,
            boolean capEnd,
            float u0, float v0, float u1, float v1,
            int alpha, int light
    ) {
        if (width <= 0.0f) return;

        float half = width * 0.5f;
        float halfDepth = Math.max(0.0f, depth) * 0.5f;
        Matrix4f pose = matrices.last().pose();
        Vector3f widthAxis = normalizeOr(widthX, widthY, widthZ, 1.0f, 0.0f, 0.0f);
        Vector3f depthAxis = normalizeOr(depthX, depthY, depthZ, 0.0f, 0.0f, 1.0f);
        Vector3f segmentAxis = normalizeOr(bx - ax, by - ay, bz - az, 0.0f, 1.0f, 0.0f);

        if (halfDepth < 1e-5f) {
            emitVertex(vc, pose,
                ax - widthAxis.x * half, ay - widthAxis.y * half, az - widthAxis.z * half,
                u0, v0, alpha, light, depthAxis.x, depthAxis.y, depthAxis.z);
            emitVertex(vc, pose,
                ax + widthAxis.x * half, ay + widthAxis.y * half, az + widthAxis.z * half,
                u1, v0, alpha, light, depthAxis.x, depthAxis.y, depthAxis.z);
            emitVertex(vc, pose,
                bx + widthAxis.x * half, by + widthAxis.y * half, bz + widthAxis.z * half,
                u1, v1, alpha, light, depthAxis.x, depthAxis.y, depthAxis.z);
            emitVertex(vc, pose,
                bx - widthAxis.x * half, by - widthAxis.y * half, bz - widthAxis.z * half,
                u0, v1, alpha, light, depthAxis.x, depthAxis.y, depthAxis.z);
            return;
        }

        float[] cx = {
            ax - widthAxis.x * half - depthAxis.x * halfDepth,
            ax + widthAxis.x * half - depthAxis.x * halfDepth,
            ax + widthAxis.x * half + depthAxis.x * halfDepth,
            ax - widthAxis.x * half + depthAxis.x * halfDepth,
            bx - widthAxis.x * half - depthAxis.x * halfDepth,
            bx + widthAxis.x * half - depthAxis.x * halfDepth,
            bx + widthAxis.x * half + depthAxis.x * halfDepth,
            bx - widthAxis.x * half + depthAxis.x * halfDepth
        };
        float[] cy = {
            ay - widthAxis.y * half - depthAxis.y * halfDepth,
            ay + widthAxis.y * half - depthAxis.y * halfDepth,
            ay + widthAxis.y * half + depthAxis.y * halfDepth,
            ay - widthAxis.y * half + depthAxis.y * halfDepth,
            by - widthAxis.y * half - depthAxis.y * halfDepth,
            by + widthAxis.y * half - depthAxis.y * halfDepth,
            by + widthAxis.y * half + depthAxis.y * halfDepth,
            by - widthAxis.y * half + depthAxis.y * halfDepth
        };
        float[] cz = {
            az - widthAxis.z * half - depthAxis.z * halfDepth,
            az + widthAxis.z * half - depthAxis.z * halfDepth,
            az + widthAxis.z * half + depthAxis.z * halfDepth,
            az - widthAxis.z * half + depthAxis.z * halfDepth,
            bz - widthAxis.z * half - depthAxis.z * halfDepth,
            bz + widthAxis.z * half - depthAxis.z * halfDepth,
            bz + widthAxis.z * half + depthAxis.z * halfDepth,
            bz - widthAxis.z * half + depthAxis.z * halfDepth
        };

        float halfTexel = 0.5f / 64.0f;
        float leftU = Math.min(u1, u0 + halfTexel);
        float rightU = Math.max(u0, u1 - halfTexel);
        float topV = Math.min(v1, v0 + halfTexel);
        float bottomV = Math.max(v0, v1 - halfTexel);

        emitFace(vc, pose, cx, cy, cz, 0,1,5,4, u0,v0,u1,v1, alpha, light,
            -depthAxis.x, -depthAxis.y, -depthAxis.z);
        emitFace(vc, pose, cx, cy, cz, 2,3,7,6, u1,v0,u0,v1, alpha, light,
            depthAxis.x, depthAxis.y, depthAxis.z);
        emitFace(vc, pose, cx, cy, cz, 1,2,6,5, rightU,v0,rightU,v1, alpha, light,
            widthAxis.x, widthAxis.y, widthAxis.z);
        emitFace(vc, pose, cx, cy, cz, 3,0,4,7, leftU,v0,leftU,v1, alpha, light,
            -widthAxis.x, -widthAxis.y, -widthAxis.z);
        if (capStart) {
            emitFace(vc, pose, cx, cy, cz, 0,3,2,1, u0,topV,u1,topV, alpha, light,
                -segmentAxis.x, -segmentAxis.y, -segmentAxis.z);
        }
        if (capEnd) {
            emitFace(vc, pose, cx, cy, cz, 4,5,6,7, u0,bottomV,u1,bottomV, alpha, light,
                segmentAxis.x, segmentAxis.y, segmentAxis.z);
        }
    }

    
    
    

    private static void emitRibbon(
            VertexConsumer vc, PoseStack matrices,
            float ax, float ay, float az,
            float bx, float by, float bz,
            float thickness,
            float u0, float v0, float u1, float v1,
            int alpha, int light
    ) {
        Vector3f segDir = new Vector3f(bx - ax, by - ay, bz - az);
        if (segDir.lengthSquared() < 1e-8f) return;
        segDir.normalize();

        Vector3f ref = Math.abs(segDir.y) < 0.9f ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0);
        Vector3f perp = segDir.cross(ref, new Vector3f()).normalize().mul(thickness * 0.5f);

        Matrix4f pose = matrices.last().pose();

        emitVertex(vc, pose, ax - perp.x, ay - perp.y, az - perp.z, u0, v0, alpha, light);
        emitVertex(vc, pose, ax + perp.x, ay + perp.y, az + perp.z, u1, v0, alpha, light);
        emitVertex(vc, pose, bx + perp.x, by + perp.y, bz + perp.z, u1, v1, alpha, light);
        emitVertex(vc, pose, bx - perp.x, by - perp.y, bz - perp.z, u0, v1, alpha, light);
    }

    
    
    

    private static void emitBox(
            VertexConsumer vc, PoseStack matrices,
            float ax, float ay, float az,
            float bx, float by, float bz,
            float thickness,
            float u0, float v0, float u1, float v1,
            int alpha, int light
    ) {
        float half = thickness * 0.5f;
        Vector3f fwd = new Vector3f(bx - ax, by - ay, bz - az);
        if (fwd.length() < 1e-6f) return;
        fwd.normalize();

        Vector3f up     = Math.abs(fwd.y) < 0.9f ? new Vector3f(0, 1, 0) : new Vector3f(1, 0, 0);
        Vector3f right  = fwd.cross(up, new Vector3f()).normalize().mul(half);
        Vector3f realUp = right.cross(fwd, new Vector3f()).normalize().mul(half);

        Matrix4f pose = matrices.last().pose();

        float[] cx = new float[8], cy = new float[8], cz = new float[8];
        cx[0]=ax-right.x-realUp.x; cy[0]=ay-right.y-realUp.y; cz[0]=az-right.z-realUp.z;
        cx[1]=ax+right.x-realUp.x; cy[1]=ay+right.y-realUp.y; cz[1]=az+right.z-realUp.z;
        cx[2]=ax+right.x+realUp.x; cy[2]=ay+right.y+realUp.y; cz[2]=az+right.z+realUp.z;
        cx[3]=ax-right.x+realUp.x; cy[3]=ay-right.y+realUp.y; cz[3]=az-right.z+realUp.z;
        cx[4]=bx-right.x-realUp.x; cy[4]=by-right.y-realUp.y; cz[4]=bz-right.z-realUp.z;
        cx[5]=bx+right.x-realUp.x; cy[5]=by+right.y-realUp.y; cz[5]=bz+right.z-realUp.z;
        cx[6]=bx+right.x+realUp.x; cy[6]=by+right.y+realUp.y; cz[6]=bz+right.z+realUp.z;
        cx[7]=bx-right.x+realUp.x; cy[7]=by-right.y+realUp.y; cz[7]=bz-right.z+realUp.z;

        emitFace(vc, pose, cx, cy, cz, 0,1,2,3, u0,v0,u1,v1, alpha, light);
        emitFace(vc, pose, cx, cy, cz, 5,4,7,6, u0,v0,u1,v1, alpha, light);
        emitFace(vc, pose, cx, cy, cz, 4,0,3,7, u0,v0,u1,v1, alpha, light);
        emitFace(vc, pose, cx, cy, cz, 1,5,6,2, u0,v0,u1,v1, alpha, light);
        emitFace(vc, pose, cx, cy, cz, 3,2,6,7, u0,v0,u1,v1, alpha, light);
        emitFace(vc, pose, cx, cy, cz, 4,5,1,0, u0,v0,u1,v1, alpha, light);
    }

    private static void emitFace(
            VertexConsumer vc, Matrix4f pose,
            float[] cx, float[] cy, float[] cz,
            int i0, int i1, int i2, int i3,
            float u0, float v0, float u1, float v1,
            int alpha, int light
    ) {
        Vector3f normal = faceNormal(cx, cy, cz, i0, i1, i2);
        emitFace(vc, pose, cx, cy, cz, i0, i1, i2, i3,
            u0, v0, u1, v1, alpha, light, normal.x, normal.y, normal.z);
    }

    private static void emitFace(
            VertexConsumer vc, Matrix4f pose,
            float[] cx, float[] cy, float[] cz,
            int i0, int i1, int i2, int i3,
            float u0, float v0, float u1, float v1,
            int alpha, int light,
            float nx, float ny, float nz
    ) {
        emitVertex(vc, pose, cx[i0], cy[i0], cz[i0], u0, v0, alpha, light, nx, ny, nz);
        emitVertex(vc, pose, cx[i1], cy[i1], cz[i1], u1, v0, alpha, light, nx, ny, nz);
        emitVertex(vc, pose, cx[i2], cy[i2], cz[i2], u1, v1, alpha, light, nx, ny, nz);
        emitVertex(vc, pose, cx[i3], cy[i3], cz[i3], u0, v1, alpha, light, nx, ny, nz);
    }

    private static Vector3f faceNormal(float[] cx, float[] cy, float[] cz, int i0, int i1, int i2) {
        Vector3f a = new Vector3f(cx[i1] - cx[i0], cy[i1] - cy[i0], cz[i1] - cz[i0]);
        Vector3f b = new Vector3f(cx[i2] - cx[i0], cy[i2] - cy[i0], cz[i2] - cz[i0]);
        Vector3f normal = a.cross(b, new Vector3f());
        if (normal.lengthSquared() < 1e-8f) {
            return new Vector3f(0.0f, 1.0f, 0.0f);
        }
        return normal.normalize();
    }

    private static Vector3f normalizeOr(float x, float y, float z, float fallbackX, float fallbackY, float fallbackZ) {
        Vector3f vector = new Vector3f(x, y, z);
        if (vector.lengthSquared() < 1e-8f) {
            vector.set(fallbackX, fallbackY, fallbackZ);
        }
        return vector.normalize();
    }

    private static void emitVertex(
            VertexConsumer vc, Matrix4f pose,
            float x, float y, float z,
            float u, float v,
            int alpha, int light
    ) {
        emitVertex(vc, pose, x, y, z, u, v, alpha, light, 0.0f, 1.0f, 0.0f);
    }

    private static void emitVertex(
            VertexConsumer vc, Matrix4f pose,
            float x, float y, float z,
            float u, float v,
            int alpha, int light,
            float nx, float ny, float nz
    ) {
//? if >=1.21 {
        vc.addVertex(pose, x, y, z)
          .setColor(255, 255, 255, alpha)
          .setUv(u, v)
          .setOverlay(NO_OVERLAY)
          .setLight(light)
          .setNormal(nx, ny, nz);
//?}
//? if <1.21 {
        vc.vertex(pose, x, y, z)
          .color(255, 255, 255, alpha)
          .uv(u, v)
          .overlayCoords(NO_OVERLAY)
          .uv2(light)
          .normal(nx, ny, nz)
          .endVertex();
//?}
    }
}
