package com.mcraft;

import com.mcraft.render.Camera;

public class Main {

    public static void main(String[] args) {

        Camera camera = new Camera(0, 80, 0);

        camera.rotate(
                (float)Math.toRadians(45),
                (float)Math.toRadians(-20)
        );

        float[] view = camera.getViewMatrix();

        System.out.println("View Matrix:");

        for (int i = 0; i < 16; i++) {

            System.out.printf("%8.3f ", view[i]);

            if ((i + 1) % 4 == 0) {
                System.out.println();
            }
        }
    }
}