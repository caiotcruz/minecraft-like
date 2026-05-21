package com.mcraft.render;

public class Camera {

    private float x, y, z;
    private float yaw   = 0f;   // radianos
    private float pitch = 0f;   // radianos

    private float[] front = {0, 0, -1};
    private float[] right = {1, 0,  0};
    private float[] up    = {0, 1,  0};

    private static final float MAX_PITCH = (float) Math.toRadians(89);

    public Camera(float x, float y, float z) {
        this.x = x; this.y = y; this.z = z;
        updateVectors();
    }

    public void rotate(float deltaYaw, float deltaPitch) {
        yaw   += deltaYaw;
        pitch += deltaPitch;
        pitch  = Math.max(-MAX_PITCH, Math.min(MAX_PITCH, pitch));
        updateVectors();
    }

    private void updateVectors() {
        float cy = (float) Math.cos(yaw);
        float sy = (float) Math.sin(yaw);
        float cp = (float) Math.cos(pitch);
        float sp = (float) Math.sin(pitch);

        front[0] = cy * cp;
        front[1] = sp;
        front[2] = sy * cp;

        float[] worldUp = {0, 1, 0};
        right = normalize(cross(front, worldUp));

        up = cross(right, front);
    }

    public float[] getViewMatrix() {
        float[] f = normalize(front);
        float[] r = normalize(right);
        float[] u = up;

        return new float[] {
             r[0],  u[0], -f[0], 0,
             r[1],  u[1], -f[1], 0,
             r[2],  u[2], -f[2], 0,
            -dot(r, new float[]{x,y,z}),
            -dot(u, new float[]{x,y,z}),
             dot(f, new float[]{x,y,z}),
            1
        };
    }

    public static float[] perspectiveMatrix(float fovDeg, float aspect,
                                             float near, float far) {
        float f = (float)(1.0 / Math.tan(Math.toRadians(fovDeg) / 2.0));
        float nf = 1.0f / (near - far);

        float[] m = new float[16]; 

        m[0]  = f / aspect;
        m[5]  = f;
        m[10] = (far + near) * nf;
        m[11] = -1f;
        m[14] = 2 * far * near * nf;

        return m;
    }


    private static float[] cross(float[] a, float[] b) {
        return new float[]{
            a[1]*b[2] - a[2]*b[1],
            a[2]*b[0] - a[0]*b[2],
            a[0]*b[1] - a[1]*b[0]
        };
    }

    private static float dot(float[] a, float[] b) {
        return a[0]*b[0] + a[1]*b[1] + a[2]*b[2];
    }

    private static float[] normalize(float[] v) {
        float len = (float) Math.sqrt(dot(v, v));
        if (len < 0.00001f) return v;
        return new float[]{ v[0]/len, v[1]/len, v[2]/len };
    }

    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
    public void setPos(float x, float y, float z) { this.x = x; this.y = y; this.z = z; }
    public float[] getFront() { return front; }
    public float[] getRight() { return right; }
}