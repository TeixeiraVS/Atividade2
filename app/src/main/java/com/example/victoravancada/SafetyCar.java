package com.example.victoravancada;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

import java.util.List;
import java.util.concurrent.Semaphore;

public class SafetyCar extends Car {

    public SafetyCar(String name, float x, float y, float size, Bitmap trackBitmap, Bitmap carImage, int color, List<Car> allCars, Semaphore[][] grid) {
        super(name, x, y, size, trackBitmap, carImage, color, allCars, grid);
        this.maxSpeed = 150; // Velocidade máxima específica para o Safety Car
        this.speed = maxSpeed;
        this.directionAngle = 180;
        this.setPriority(10); // Safety Car sempre tem prioridade máxima
    }

    @Override
    public void run() {
        while (isRunning) {
            if (isPaused) {
                try {
                    Thread.sleep(25);
                    Log.d("SafetyCar", "SafetyCar esta pausado");
                    continue;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    isRunning = false;
                }
            }
            moveAndStayOnTrack();
            updateSensors();

            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                isRunning = false;
            }
        }
    }

    private void moveAndStayOnTrack() {
        float radianAngle = (float) Math.toRadians(directionAngle);
        float nextX = x + (float) Math.cos(radianAngle) * (speed / 30.0f);
        float nextY = y + (float) Math.sin(radianAngle) * (speed / 30.0f);

        boolean inGridArea = nextX >= GRID_START_X && nextX <= GRID_END_X &&
                nextY >= GRID_START_Y && nextY <= GRID_END_Y;

        Log.d("SafetyCar", "Movendo para próxima posição: (" + nextX + ", " + nextY + ")");
        Log.d("SafetyCar", "SafetyCar está na área do grid: " + inGridArea);

        if (inGridArea) {
            int newGridX = (int) ((nextX - GRID_START_X) / GRID_SIZE);
            int newGridY = (int) ((nextY - GRID_START_Y) / GRID_SIZE);

            if (newGridX >= 0 && newGridY >= 0 && newGridX < trackGrid.length && newGridY < trackGrid[0].length) {
                if (newGridX != currentGridX || newGridY != currentGridY) {
                    // Mudou de célula
                    try {
                        Log.d("SafetyCar", "Tentando adquirir o grid (" + newGridX + ", " + newGridY + ")");
                        Log.d("SafetyCar", "Semáforo disponível: " + trackGrid[newGridX][newGridY].availablePermits());

                        trackGrid[newGridX][newGridY].acquire();
                        Log.d("SafetyCar", "Adquiriu o grid (" + newGridX + ", " + newGridY + ")");

                        if (currentGridX != -1 && currentGridY != -1) {
                            trackGrid[currentGridX][currentGridY].release();
                            Log.d("SafetyCar", "Liberou o grid (" + currentGridX + ", " + currentGridY + ")");
                        }

                        currentGridX = newGridX;
                        currentGridY = newGridY;
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        Log.e("SafetyCar", "Interrompido ao tentar adquirir o grid (" + newGridX + ", " + newGridY + ")");
                    }
                }
            } else {
                Log.e("SafetyCar", "Índices do grid fora dos limites: (" + newGridX + ", " + newGridY + ")");
            }
        } else {
            x = nextX;
            y = nextY;

            if (currentGridX != -1 && currentGridY != -1) {
                try {
                    trackGrid[currentGridX][currentGridY].release();
                    Log.d("SafetyCar", "Liberou o grid (" + currentGridX + ", " + currentGridY + ")");
                } catch (Exception e) {
                    Log.e("SafetyCar", "Falhou ao liberar o grid (" + currentGridX + ", " + currentGridY + ")", e);
                }
                currentGridX = -1;
                currentGridY = -1;
            }
        }

        // Atualiza posição final
        x = nextX;
        y = nextY;

        // Ajusta direção para manter o SafetyCar na pista
        if (trackSensors.get("left") != null && trackSensors.get("left") != Color.WHITE) {
            directionAngle += 3.0f;
        } else if (trackSensors.get("right") != null && trackSensors.get("right") != Color.WHITE) {
            directionAngle -= 3.0f;
        }

        if (trackSensors.get("frontLeft") != null && trackSensors.get("frontLeft") != Color.WHITE) {
            directionAngle += 1.5f;
        } else if (trackSensors.get("frontRight") != null && trackSensors.get("frontRight") != Color.WHITE) {
            directionAngle -= 1.5f;
        }

        normalizeDirection();

        // Garante que o SafetyCar permaneça dentro dos limites da pista
        if (x < 0) x = 0;
        if (x > trackBitmap.getWidth() - size) x = trackBitmap.getWidth() - size;
        if (y < 0) y = 0;
        if (y > trackBitmap.getHeight() - size) y = trackBitmap.getHeight() - size;

        Log.d("SafetyCar", "Nova posição: (" + x + ", " + y + "), direção: " + directionAngle);
    }
}
