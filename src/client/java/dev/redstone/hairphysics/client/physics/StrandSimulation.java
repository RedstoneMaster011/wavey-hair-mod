package dev.redstone.hairphysics.client.physics;

import dev.redstone.hairphysics.client.data.HairStrand;
import dev.redstone.hairphysics.client.data.SkinUvMapper;
import dev.redstone.hairphysics.client.data.StrandPhysicsConfig;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerSkinType;








public class StrandSimulation {

    private static final int CONSTRAINT_ITERATIONS = 6;
    private static final float MODEL_PIXEL = 1.0f / 16.0f;

    public final HairStrand definition;
    private final StrandPhysicsConfig cfg;
    public final PhysicsParticle[] particles;

    
    public double entityX, entityY, entityZ;

    private double lastRootX, lastRootY, lastRootZ;

    public StrandSimulation(HairStrand definition, double rootX, double rootY, double rootZ) {
        this.definition = definition;
        this.cfg = definition.physics;

        int count = cfg.segments + 1;
        particles = new PhysicsParticle[count];

        for (int i = 0; i < count; i++) {
            double py = rootY - i * getRestLength();
            particles[i] = new PhysicsParticle(rootX, py, rootZ, i == 0);
        }

        lastRootX = rootX;
        lastRootY = rootY;
        lastRootZ = rootZ;
        entityX = rootX;
        entityY = rootY;
        entityZ = rootZ;
    }

    









    public void tick(double rootX, double rootY, double rootZ,
                     double entX, double entY, double entZ,
                     AbstractClientPlayerEntity player) {
        entityX = entX;
        entityY = entY;
        entityZ = entZ;

        double moveDx = rootX - lastRootX;
        double moveDy = rootY - lastRootY;
        double moveDz = rootZ - lastRootZ;

        float windX = (float)(moveDx * cfg.moveResponse);
        float windY = (float)(moveDy * cfg.moveResponse);
        float windZ = (float)(moveDz * cfg.moveResponse);
        if (cfg.windResponse > 0.0001f && player != null) {
            float phase = (player.age + Math.floorMod(definition.id.hashCode(), 97)) * 0.12f;
            float gust = (0.65f + 0.35f * (float)Math.sin(phase)) * cfg.windResponse;
            float localWindX = cfg.windDirection * gust * 0.010f;
            float localWindZ = (float)Math.cos(phase * 0.7f) * cfg.windResponse * 0.0025f;
            float yaw = (float)Math.toRadians(anchorYaw(player));
            float cosYaw = (float)Math.cos(yaw);
            float sinYaw = (float)Math.sin(yaw);
            windX += localWindX * cosYaw - localWindZ * sinYaw;
            windZ += localWindX * sinYaw + localWindZ * cosYaw;
        }

        lastRootX = rootX;
        lastRootY = rootY;
        lastRootZ = rootZ;

        particles[0].setPosition(rootX, rootY, rootZ);

        for (int i = 1; i < particles.length; i++) {
            particles[i].integrate(cfg.gravity, cfg.damping, windX, windY, windZ);
        }

        for (int iter = 0; iter < CONSTRAINT_ITERATIONS; iter++) {
            solveConstraints();
            solveSelfCollision();
            solveBodyCollision(player);
        }
    }

    private void solveConstraints() {
        float restLength = getRestLength();
        float stiffness  = cfg.stiffness;

        for (int i = 0; i < particles.length - 1; i++) {
            PhysicsParticle a = particles[i];
            PhysicsParticle b = particles[i + 1];

            double dx = b.x - a.x, dy = b.y - a.y, dz = b.z - a.z;
            double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
            if (dist < 1e-6) continue;

            double diff = (dist - restLength) / dist * stiffness;
            double cx = dx * diff * 0.5, cy = dy * diff * 0.5, cz = dz * diff * 0.5;

            if (!a.pinned) { a.x += cx; a.y += cy; a.z += cz; }
            if (!b.pinned) { b.x -= cx; b.y -= cy; b.z -= cz; }
        }
    }

    private void solveSelfCollision() {
        double minDist = Math.max(0.035, definition.render.thickness);
        double minDistSq = minDist * minDist;

        for (int i = 1; i < particles.length; i++) {
            PhysicsParticle a = particles[i];
            for (int j = i + 2; j < particles.length; j++) {
                PhysicsParticle b = particles[j];
                double dx = b.x - a.x, dy = b.y - a.y, dz = b.z - a.z;
                double distSq = dx * dx + dy * dy + dz * dz;
                if (distSq >= minDistSq || distSq < 1e-8) continue;

                double dist = Math.sqrt(distSq);
                double push = (minDist - dist) * 0.5 / dist;
                moveParticle(a, -dx * push, -dy * push, -dz * push);
                moveParticle(b,  dx * push,  dy * push,  dz * push);
            }
        }
    }

    private void solveBodyCollision(AbstractClientPlayerEntity player) {
        if (player == null) return;

        float yaw = (float) Math.toRadians(player.getBodyYaw());
        double cosYaw = Math.cos(yaw);
        double sinYaw = Math.sin(yaw);
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();

        double padding = Math.max(0.04, definition.render.thickness);
        double sheetHalfWidth = definition.render.sheetWidthForPixels(definition.skinRegion.width) * 0.5;
        double headCenterY = player.getEyeHeight(player.getPose()) - 0.1;
        int preferredZ = preferredZPush();
        boolean slim = false;
        try {
            slim = player.getSkin().model() == PlayerSkinType.SLIM;
        } catch (Exception ignored) {}

        double armHalfWidth = slim ? 0.09375 : 0.125;
        double armCenterX = 0.25 + armHalfWidth;

        double[][] boxes = {
            {-0.25, headCenterY - 0.25, -0.25,  0.25, headCenterY + 0.25,  0.25}, 
            {-0.25, 0.70,              -0.125, 0.25, 1.45,               0.125}, 
            {-armCenterX - armHalfWidth, 0.70, -0.125, -armCenterX + armHalfWidth, 1.45, 0.125}, 
            { armCenterX - armHalfWidth, 0.70, -0.125,  armCenterX + armHalfWidth, 1.45, 0.125}, 
            {-0.25, 0.00,              -0.125, 0.00, 0.75,               0.125}, 
            { 0.00, 0.00,              -0.125, 0.25, 0.75,               0.125}, 
        };

        for (int i = 1; i < particles.length; i++) {
            PhysicsParticle p = particles[i];
            for (int boxIndex = 0; boxIndex < boxes.length; boxIndex++) {
                if (pushHairSheetOutOfModelBox(p, px, py, pz, cosYaw, sinYaw, boxes[boxIndex],
                    padding, sheetHalfWidth, preferredZ)) {
                    break;
                }
            }
        }
    }

    private int preferredZPush() {
        if (Math.abs(definition.origin.offsetZ) < 0.08f) {
            return 0;
        }
        return definition.origin.offsetZ <= 0.0f ? -1 : 1;
    }

    private float anchorYaw(AbstractClientPlayerEntity player) {
        String bone = definition.origin.bone == null ? "" : definition.origin.bone.trim();
        return bone.equalsIgnoreCase("head") || bone.isBlank() ? player.getHeadYaw() : player.getBodyYaw();
    }

    private static boolean pushHairSheetOutOfModelBox(PhysicsParticle p,
                                                      double playerX, double playerY, double playerZ,
                                                      double cosYaw, double sinYaw,
                                                      double[] box,
                                                      double padding,
                                                      double sheetHalfWidth,
                                                      int preferredZ) {
        double dx = p.x - playerX;
        double dz = p.z - playerZ;

        double lx = dx * cosYaw + dz * sinYaw;
        double ly = p.y - playerY;
        double lz = -dx * sinYaw + dz * cosYaw;

        double minX = box[0] - padding, minY = box[1] - padding, minZ = box[2] - padding;
        double maxX = box[3] + padding, maxY = box[4] + padding, maxZ = box[5] + padding;

        double[] sampleOffsets = {0.0, -sheetHalfWidth, sheetHalfWidth};
        for (double sampleOffset : sampleOffsets) {
            double sampleX = lx + sampleOffset;

            if (sampleX < minX || sampleX > maxX || ly < minY || ly > maxY || lz < minZ || lz > maxZ) {
                continue;
            }

            double pushX = nearestFacePush(sampleX, minX, maxX);
            double pushY = nearestFacePush(ly, minY, maxY);
            double pushZ = nearestFacePush(lz, minZ, maxZ);

            double ax = Math.abs(pushX);
            double ay = Math.abs(pushY);
            double az = Math.abs(pushZ);

            double localDx = 0.0;
            double localDy = 0.0;
            double localDz = 0.0;

            if (preferredZ < 0) {
                localDz = minZ - lz;
            } else if (preferredZ > 0) {
                localDz = maxZ - lz;
            } else if (ax <= ay && ax <= az) {
                localDx = pushX;
            } else if (ay <= az) {
                localDy = pushY;
            } else {
                localDz = pushZ;
            }

            double worldDx = localDx * cosYaw - localDz * sinYaw;
            double worldDz = localDx * sinYaw + localDz * cosYaw;
            moveParticle(p, worldDx, localDy, worldDz);
            return true;
        }

        return false;
    }

    private static double nearestFacePush(double value, double min, double max) {
        double toMin = min - value;
        double toMax = max - value;
        return Math.abs(toMin) < Math.abs(toMax) ? toMin : toMax;
    }

    private static void moveParticle(PhysicsParticle p, double dx, double dy, double dz) {
        if (p.pinned) return;
        p.x += dx; p.y += dy; p.z += dz;
        p.prevX += dx; p.prevY += dy; p.prevZ += dz;
    }

    public float totalRestLength() {
        float startDown = attachedStartDown();
        float pixelLength = Math.max(1, definition.skinRegion.height) * MODEL_PIXEL;
        return Math.max(0.01f, startDown + pixelLength * cfg.lengthScale);
    }

    private float attachedStartDown() {
        SkinUvMapper.SurfacePoint point = SkinUvMapper.pointFor(definition.skinRegion.u, definition.skinRegion.v);
        if (!point.bone().equalsIgnoreCase(definition.origin.bone)) {
            return 0.0f;
        }
        return Math.max(0.0f, definition.origin.offsetY - point.offsetY());
    }

    private float getRestLength() {
        int segmentCount = Math.max(1, particles.length - 1);
        return totalRestLength() / segmentCount;
    }

    public void reset(double rootX, double rootY, double rootZ) {
        for (int i = 0; i < particles.length; i++) {
            double py = rootY - i * getRestLength();
            particles[i].x = rootX; particles[i].prevX = rootX;
            particles[i].y = py;    particles[i].prevY = py;
            particles[i].z = rootZ; particles[i].prevZ = rootZ;
        }
        lastRootX = rootX; lastRootY = rootY; lastRootZ = rootZ;
        entityX = rootX;   entityY = rootY;   entityZ = rootZ;
    }

    public int particleCount() { return particles.length; }
}
