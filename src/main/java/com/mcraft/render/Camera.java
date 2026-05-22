package com.mcraft.render;

public class Camera {

    private float x, y, z;
    private float yaw   = 0f;   
    private float pitch = 0f;   

    private float[] front = {0, 0, -1};
    private float[] right = {1, 0,  0};
    private float[] up    = {0, 1,  0};

    private static final float MAX_PITCH = (float) Math.toRadians(89.0);

    public Camera(float x, float y, float z) {
        this.x = x; this.y = y; this.z = z;
        updateVectors();
    }

    public void rotate(float deltaYaw, float deltaPitch) {
        yaw   += deltaYaw;
        pitch  = Math.max(-MAX_PITCH, Math.min(MAX_PITCH, pitch + deltaPitch));
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
        float rx = right[0], ry = right[1], rz = right[2];
        float ux = up[0],    uy = up[1],    uz = up[2];
        float fx = front[0], fy = front[1], fz = front[2];

        float tx = -(rx*x + ry*y + rz*z);
        float ty = -(ux*x + uy*y + uz*z);
        float tz =  (fx*x + fy*y + fz*z); 

        return new float[] {
             rx,  ux, -fx, 0,   
             ry,  uy, -fy, 0,  
             rz,  uz, -fz, 0,   
             tx,  ty,  tz, 1  
        };
    }

    /**
     * @param fovDeg  
     * @param aspect 
     * @param near    
     * @param far   
     */
    public static float[] perspective(float fovDeg, float aspect, float near, float far) {
        float f  = (float)(1.0 / Math.tan(Math.toRadians(fovDeg) / 2.0));
        float nf = 1.0f / (near - far);

        float[] m = new float[16];
        m[0]  = f / aspect;
        m[5]  = f;
        m[10] = (far + near) * nf;
        m[11] = -1f;
        m[14] = 2f * far * near * nf;
        return m;
    }

    public static float[] ortho(float w, float h) {
        float[] m = new float[16];
        m[0]  =  2f / w;
        m[5]  = -2f / h;  
        m[10] = -1f;
        m[12] = -1f;      
        m[13] =  1f;       
        m[15] =  1f;
        return m;
    }

    public float   getX()     { return x; }
    public float   getY()     { return y; }
    public float   getZ()     { return z; }
    public float   getYaw()   { return yaw; }     
    public float   getPitch() { return pitch; }
    public float[] getFront() { return front; }
    public float[] getRight() { return right; }
    public float[] getUp()    { return up; }

    public void setPosition(float x, float y, float z) {
        this.x = x; this.y = y; this.z = z;
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
        if (len < 1e-6f) return new float[]{0, 1, 0};
        return new float[]{ v[0]/len, v[1]/len, v[2]/len };
    }
}