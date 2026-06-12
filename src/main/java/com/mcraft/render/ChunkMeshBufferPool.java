package com.mcraft.render;

import org.lwjgl.BufferUtils;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class ChunkMeshBufferPool {
    
    private static final int VERT_FLOATS = 8; 
    
    private static final int MAX_FLOATS = 49152 * 4 * VERT_FLOATS; 
    private static final int MAX_INTS   = 49152 * 6; 

    public static final ThreadLocal<FloatBuffer> OPAQUE_V_BUF = ThreadLocal.withInitial(
        () -> BufferUtils.createFloatBuffer(MAX_FLOATS)
    );

    public static final ThreadLocal<IntBuffer> OPAQUE_I_BUF = ThreadLocal.withInitial(
        () -> BufferUtils.createIntBuffer(MAX_INTS)
    );

    public static final ThreadLocal<FloatBuffer> WATER_V_BUF = ThreadLocal.withInitial(
        () -> BufferUtils.createFloatBuffer(MAX_FLOATS / 2)
    );
    
    public static final ThreadLocal<IntBuffer> WATER_I_BUF = ThreadLocal.withInitial(
        () -> BufferUtils.createIntBuffer(MAX_INTS / 2)
    );
}