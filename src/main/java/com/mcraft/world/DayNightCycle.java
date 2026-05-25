package com.mcraft.world;

public class DayNightCycle {

    public static final float DAY_DURATION = 600f; 
    
    private float timeScale = 1.0f;

    private float t = 0.33f;

    public void update(float dt) {
        t = (t + (dt * timeScale) / DAY_DURATION) % 1.0f;
    }

    public float getAmbientLight() {
        float raw = (float) Math.cos(2 * Math.PI * (t - 0.5));
        return 0.05f + (raw + 1.0f) * 0.5f * 0.95f;
    }

    public float[] getSkyColor() {
        float[][] keys = {
            {0.01f, 0.01f, 0.05f}, 
            {0.01f, 0.01f, 0.05f},   
            {0.80f, 0.45f, 0.20f},  
            {0.50f, 0.70f, 1.00f},   
            {0.40f, 0.65f, 1.00f},   
            {0.80f, 0.45f, 0.20f},   
            {0.01f, 0.01f, 0.05f},   
            {0.01f, 0.01f, 0.05f},  
        };
        float[] times  = {0f, 0.20f, 0.25f, 0.35f, 0.65f, 0.75f, 0.85f, 1.00f};

        for (int i = 0; i < times.length - 1; i++) {
            if (t >= times[i] && t <= times[i + 1]) {
                float alpha = (t - times[i]) / (times[i + 1] - times[i]);
                return lerpColor(keys[i], keys[i + 1], smoothstep(alpha));
            }
        }
        return keys[0];
    }

    public float[] getFogColor() {
        float[] sky = getSkyColor();
        return new float[]{
            sky[0] * 0.85f + 0.15f,
            sky[1] * 0.85f + 0.15f,
            sky[2] * 0.85f + 0.15f
        };
    }

    public float getTime() { return t; }

    public String getPhase() {
        if (t < 0.2f || t > 0.85f) return "Noite";
        if (t < 0.3f) return "Amanhecer";
        if (t < 0.7f) return "Dia";
        return "Entardecer";
    }

    private float smoothstep(float x) { return x * x * (3 - 2 * x); }

    private float[] lerpColor(float[] a, float[] b, float t) {
        return new float[]{
            a[0] + (b[0] - a[0]) * t,
            a[1] + (b[1] - a[1]) * t,
            a[2] + (b[2] - a[2]) * t
        };
    }

    //Métodos para Debug
    public void setTimeScale(float scale) {
        this.timeScale = scale;
    }

    public void setTime(float t) {
        this.t = t % 1.0f;
    }

    public boolean isNight() {
        return t < 0.20f || t > 0.80f;
    }
}