package com.mcraft.audio;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.*;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.system.MemoryStack;

import com.mcraft.world.Block;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.*;

import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.system.MemoryStack.stackPush;

public class SoundManager {

    private long device;
    private long context;

    private final Map<SoundEvent, Integer> buffers = new EnumMap<>(SoundEvent.class);

    private static final int SOURCE_POOL = 16;
    private final int[] sources = new int[SOURCE_POOL];
    private int sourceIdx = 0;


    public void init() {
        device = alcOpenDevice((ByteBuffer) null);
        if (device == 0) {
            System.err.println("[Audio] Dispositivo OpenAL não encontrado — sons desativados");
            return;
        }

        context = alcCreateContext(device, (IntBuffer) null);
        alcMakeContextCurrent(context);
        AL.createCapabilities(ALC.createCapabilities(device));

        alListener3f(AL_POSITION,    0, 0, 0);
        alListener3f(AL_VELOCITY,    0, 0, 0);
        alListenerfv(AL_ORIENTATION, new float[]{ 0, 0, -1,   0, 1, 0 });

        alGenSources(sources);
        for (int src : sources) {
            alSourcef(src, AL_ROLLOFF_FACTOR, 1.0f);
            alSourcef(src, AL_REFERENCE_DISTANCE, 4.0f);
            alSourcef(src, AL_MAX_DISTANCE, 32.0f);
        }

        for (SoundEvent e : SoundEvent.values()) {
            int buf = loadOgg("/" + e.path);
            if (buf != -1) buffers.put(e, buf);
        }

        System.out.println("[Audio] OpenAL inicializado: " + buffers.size() + " sons carregados");
    }


    public void play(SoundEvent event, float x, float y, float z,
                     float volume, float pitch) {
        Integer buf = buffers.get(event);
        if (buf == null) return;

        int src = sources[sourceIdx % SOURCE_POOL];
        sourceIdx++;

        alSourceStop(src);
        alSourcei(src, AL_BUFFER, buf);
        alSource3f(src, AL_POSITION, x, y, z);
        alSourcef(src, AL_GAIN, volume);
        alSourcef(src, AL_PITCH, pitch);
        alSourcei(src, AL_LOOPING, AL_FALSE);
        alSourcePlay(src);
    }

    public void playRandom(SoundEvent event, float x, float y, float z, float volume) {
        float pitchVar = 0.9f + (float)Math.random() * 0.2f;
        play(event, x, y, z, volume, pitchVar);
    }

    public void updateListener(float px, float py, float pz,
                                float fx, float fy, float fz,
                                float ux, float uy, float uz) {
        alListener3f(AL_POSITION, px, py, pz);
        alListenerfv(AL_ORIENTATION, new float[]{ fx, fy, fz, ux, uy, uz });
    }


    public SoundEvent breakSound(com.mcraft.world.Block block) {
        return switch (block) {
            case GRASS -> SoundEvent.BLOCK_BREAK_GRASS;
            case SAND ->  SoundEvent.BLOCK_BREAK_GRASS;
            case DIRT -> SoundEvent.BLOCK_BREAK_GRASS;
            case LEAVES ->  SoundEvent.BLOCK_BREAK_GRASS;
            case STONE, BEDROCK            -> SoundEvent.BLOCK_BREAK_STONE;
            case WOOD_LOG, PLANKS,
                 CRAFTING_TABLE            -> SoundEvent.BLOCK_BREAK_WOOD;
            default                         -> SoundEvent.BLOCK_BREAK_STONE;
        };
    }

    public SoundEvent placeSound(Block block) {
        return switch (block) {

            case GRASS, DIRT, SAND, LEAVES ->
                SoundEvent.BLOCK_PLACE_GRASS;

            case STONE, BEDROCK ->
                SoundEvent.BLOCK_PLACE_STONE;

            case WOOD_LOG, PLANKS, CRAFTING_TABLE ->
                SoundEvent.BLOCK_PLACE_WOOD;

            default ->
                SoundEvent.BLOCK_PLACE_STONE;
        };
    }

    public SoundEvent stepSound(Block block) {
        return switch (block) {

            case STONE, BEDROCK ->
                SoundEvent.STEP_STONE;

            case WOOD_LOG, PLANKS, CRAFTING_TABLE ->
                SoundEvent.STEP_WOOD;

            case SAND ->
                SoundEvent.STEP_SAND;

            case WATER ->
                SoundEvent.STEP_WATER;
                
            default ->
                SoundEvent.STEP_GRASS;
        };
    }


    public SoundEvent hitSound(Block block) {
        return switch (block) {
            case GRASS, DIRT, SAND, LEAVES -> SoundEvent.BLOCK_HIT_GRASS;
            case STONE, BEDROCK             -> SoundEvent.BLOCK_HIT_STONE;
            case WOOD_LOG, PLANKS,
                CRAFTING_TABLE             -> SoundEvent.BLOCK_HIT_WOOD;
            default                          -> SoundEvent.BLOCK_HIT_STONE;
        };
    }


    private int loadOgg(String classPath) {
        try (InputStream is = getClass().getResourceAsStream(classPath)) {
            if (is == null) return -1;
            byte[] raw = is.readAllBytes();
            ByteBuffer fileData = BufferUtils.createByteBuffer(raw.length);
            fileData.put(raw).flip();

            try (MemoryStack stack = stackPush()) {
                IntBuffer channels   = stack.mallocInt(1);
                IntBuffer sampleRate = stack.mallocInt(1);
                ShortBuffer samples  = STBVorbis.stb_vorbis_decode_memory(
                        fileData, channels, sampleRate);
                if (samples == null) return -1;

                int format = (channels.get(0) == 1) ? AL_FORMAT_MONO16 : AL_FORMAT_STEREO16;
                int buf = alGenBuffers();
                alBufferData(buf, format, samples, sampleRate.get(0));
                return buf;
            }
        } catch (IOException e) {
            System.err.println("[Audio] Não foi possível carregar: " + classPath);
            return -1;
        }
    }

    public void cleanup() {
        alDeleteSources(sources);
        for (int buf : buffers.values()) alDeleteBuffers(buf);
        alcDestroyContext(context);
        alcCloseDevice(device);
    }


    public int playLoop(SoundEvent event, float volume) {
        Integer buf = buffers.get(event);
        if (buf == null) return -1;

        int src = alGenSources();

        alSourcei(src, AL_BUFFER, buf);
        alSourcef(src, AL_GAIN, volume);
        alSourcei(src, AL_LOOPING, AL_TRUE);

        alSourcei(src, AL_SOURCE_RELATIVE, AL_TRUE);
        alSource3f(src, AL_POSITION, 0, 0, 0);

        alSourcePlay(src);

        return src;
    }

    public void stop(int source) {
        alSourceStop(source);
        alDeleteSources(source);
    }
}