package com.example.racelibrary;

public class CarCalculations {

        public static double calculateDistance(float x1, float y1, float x2, float y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    public static float calculateAngle(float x1, float y1, float x2, float y2) {
        float deltaX = x2 - x1;
        float deltaY = y2 - y1;
        float angle = (float) Math.toDegrees(Math.atan2(deltaY, deltaX));
        return (angle < 0) ? angle + 360 : angle;
    }


    public static float normalizeAngle(float angle) {
        if (angle >= 360) {
            return angle - 360;
        } else if (angle < 0) {
            return angle + 360;
        }
        return angle;
    }

    public static boolean isNearPoint(float x, float y, float pointX, float pointY, float margin) {
        return Math.abs(x - pointX) < margin && Math.abs(y - pointY) < margin;
    }
}
