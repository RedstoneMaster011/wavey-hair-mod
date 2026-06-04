package dev.redstone.hairphysics.client.physics;











public class PhysicsParticle {

    
    public double x, y, z;

    
    public double prevX, prevY, prevZ;

    
    public boolean pinned;

    public PhysicsParticle(double x, double y, double z, boolean pinned) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.prevX = x;
        this.prevY = y;
        this.prevZ = z;
        this.pinned = pinned;
    }

    








    public void integrate(float gravity, float damping, float windX, float windY, float windZ) {
        if (pinned) return;

        double vx = (x - prevX) * damping + windX;
        double vy = (y - prevY) * damping - gravity + windY;
        double vz = (z - prevZ) * damping + windZ;

        prevX = x;
        prevY = y;
        prevZ = z;

        x += vx;
        y += vy;
        z += vz;
    }

    



    public void setPosition(double nx, double ny, double nz) {
        double dx = nx - x;
        double dy = ny - y;
        double dz = nz - z;
        x = nx;
        y = ny;
        z = nz;
        prevX += dx;
        prevY += dy;
        prevZ += dz;
    }

    



    public void resetVelocity() {
        prevX = x;
        prevY = y;
        prevZ = z;
    }

    
    public double lerpX(float t) { return prevX + (x - prevX) * t; }
    public double lerpY(float t) { return prevY + (y - prevY) * t; }
    public double lerpZ(float t) { return prevZ + (z - prevZ) * t; }
}
