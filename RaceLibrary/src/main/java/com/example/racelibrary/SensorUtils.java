package com.example.racelibrary;

import java.util.HashMap;
import java.util.Map;

public class SensorUtils {

    /**
     * Atualiza os sensores com base na posição, direção e distância do sensor.
     *
     * @param x             Posição X do carro.
     * @param y             Posição Y do carro.
     * @param directionAngle Ângulo de direção do carro.
     * @param sensorDistance Distância dos sensores.
     * @return Um mapa contendo os pontos calculados para cada sensor.
     */
    public static Map<String, int[]> calculateSensorPositions(float x, float y, float directionAngle, int sensorDistance) {
        Map<String, int[]> sensorPositions = new HashMap<>();

        sensorPositions.put("front", new int[]{
                (int) (x + Math.cos(Math.toRadians(directionAngle)) * sensorDistance),
                (int) (y + Math.sin(Math.toRadians(directionAngle)) * sensorDistance)
        });

        sensorPositions.put("left", new int[]{
                (int) (x + Math.cos(Math.toRadians(directionAngle - 60)) * sensorDistance),
                (int) (y + Math.sin(Math.toRadians(directionAngle - 60)) * sensorDistance)
        });

        sensorPositions.put("right", new int[]{
                (int) (x + Math.cos(Math.toRadians(directionAngle + 60)) * sensorDistance),
                (int) (y + Math.sin(Math.toRadians(directionAngle + 60)) * sensorDistance)
        });

        sensorPositions.put("frontLeft", new int[]{
                (int) (x + Math.cos(Math.toRadians(directionAngle - 45)) * sensorDistance),
                (int) (y + Math.sin(Math.toRadians(directionAngle - 45)) * sensorDistance)
        });

        sensorPositions.put("frontRight", new int[]{
                (int) (x + Math.cos(Math.toRadians(directionAngle + 45)) * sensorDistance),
                (int) (y + Math.sin(Math.toRadians(directionAngle + 45)) * sensorDistance)
        });

        return sensorPositions;
    }
}
