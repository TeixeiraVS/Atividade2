package com.example.racelibrary;

public class CarData {
    private String name;
    private double x;
    private double y;
    private double size;
    private int speed;
    private int maxSpeed; // Adicionado
    private double directionAngle;
    private int lapCounter;
    private int color;
    private int priority; // Adicionado
    private double distanceTraveled; // Adicionado

    public CarData() { }

    public CarData(String name, double x, double y, double size, int speed, double directionAngle, int lapCounter, int color) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.size = size;
        this.speed = speed;
        this.directionAngle = directionAngle;
        this.lapCounter = lapCounter;
        this.color = color;
        this.maxSpeed = 200; // Velocidade máxima padrão
        this.priority = 5; // Prioridade padrão
        this.distanceTraveled = 0; // Inicialmente zero
    }

    public String getName() {
        return name;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getSize() {
        return size;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = Math.min(speed, maxSpeed);
    }

    public int getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(int maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public double getDirectionAngle() {
        return directionAngle;
    }

    public int getLapCounter() {
        return lapCounter;
    }

    public void incrementLapCounter() {
        lapCounter++;
    }

    public int getColor() {
        return color;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public double getDistanceTraveled() {
        return distanceTraveled;
    }

    public void addDistanceTraveled(double distance) {
        this.distanceTraveled += distance;
    }
}
