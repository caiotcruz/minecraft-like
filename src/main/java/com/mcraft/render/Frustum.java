package com.mcraft.render;

public class Frustum {
    private final float[][] planes = new float[6][4];

    public void update(float[] m) {
        planes[0] = plane(m[3]+m[0],  m[7]+m[4],  m[11]+m[8],  m[15]+m[12]);
        planes[1] = plane(m[3]-m[0],  m[7]-m[4],  m[11]-m[8],  m[15]-m[12]);
        planes[2] = plane(m[3]+m[1],  m[7]+m[5],  m[11]+m[9],  m[15]+m[13]);
        planes[3] = plane(m[3]-m[1],  m[7]-m[5],  m[11]-m[9],  m[15]-m[13]);
        planes[4] = plane(m[3]+m[2],  m[7]+m[6],  m[11]+m[10], m[15]+m[14]);
        planes[5] = plane(m[3]-m[2],  m[7]-m[6],  m[11]-m[10], m[15]-m[14]);
    }

    public boolean isVisible(float x0, float y0, float z0,
                              float x1, float y1, float z1) {
        for (float[] p : planes) {
            float px = (p[0] > 0) ? x1 : x0;
            float py = (p[1] > 0) ? y1 : y0;
            float pz = (p[2] > 0) ? z1 : z0;
            if (p[0]*px + p[1]*py + p[2]*pz + p[3] < 0) return false;
        }
        return true;
    }

    private float[] plane(float a, float b, float c, float d) {
        float len = (float) Math.sqrt(a*a + b*b + c*c);
        return new float[]{ a/len, b/len, c/len, d/len };
    }
}