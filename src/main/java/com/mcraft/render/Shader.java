package com.mcraft.render;

import java.io.IOException;
import java.io.InputStream;

import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetProgrami;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glUniform3f;

public class Shader {

    private final int programId;

    public Shader(String vertResource, String fragResource) {
        int vert = compile(GL_VERTEX_SHADER,   loadText("/shaders/" + vertResource));
        int frag = compile(GL_FRAGMENT_SHADER, loadText("/shaders/" + fragResource));

        programId = glCreateProgram();
        glAttachShader(programId, vert);
        glAttachShader(programId, frag);
        glLinkProgram(programId);

        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(programId);
            glDeleteProgram(programId);
            throw new RuntimeException("Erro ao linkar shader:\n" + log);
        }

        glDeleteShader(vert);
        glDeleteShader(frag);
    }


    private int compile(int type, String src) {
        int id = glCreateShader(type);
        glShaderSource(id, src);
        glCompileShader(id);

        if (glGetShaderi(id, GL_COMPILE_STATUS) == GL_FALSE) {
            String log  = glGetShaderInfoLog(id);
            String kind = (type == GL_VERTEX_SHADER) ? "vertex" : "fragment";
            glDeleteShader(id);
            throw new RuntimeException("Erro ao compilar shader " + kind + ":\n" + log);
        }
        return id;
    }

    private String loadText(String classPath) {
        try (InputStream is = getClass().getResourceAsStream(classPath)) {
            if (is == null) {
                throw new RuntimeException("Shader não encontrado: " + classPath);
            }
            return new String(is.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException("Falha ao ler shader: " + classPath, e);
        }
    }


    public void use() {
        glUseProgram(programId);
    }

    public void setMatrix4(String name, float[] mat) {
        int loc = glGetUniformLocation(programId, name);
        glUniformMatrix4fv(loc, false, mat);
    }

    public void setInt(String name, int value) {
        glUniform1i(glGetUniformLocation(programId, name), value);
    }

    public void setFloat(String name, float value) {
        glUniform1f(glGetUniformLocation(programId, name), value);
    }

    public void delete() {
        glDeleteProgram(programId);
    }

    public void setVec3(String name, float x, float y, float z) {
        glUniform3f(glGetUniformLocation(programId, name), x, y, z);
    }
}