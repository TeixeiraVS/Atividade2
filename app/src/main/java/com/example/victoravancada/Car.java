package com.example.victoravancada;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.util.Log;
import java.util.concurrent.ConcurrentHashMap;
import com.example.racelibrary.CarCalculations;
import com.example.racelibrary.SensorUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

public class Car implements Runnable {
    private String name;
    protected float x, y;
    protected float size;
    protected Bitmap trackBitmap;
    private Bitmap carImage;
    private int carColor;
    protected Map<String, Integer> trackSensors;
    protected Map<String, Integer> carProximitySensors;
    protected float directionAngle;
    private boolean lapCounted = false;
    private int lapCounter = 0;
    protected int speed;
    protected int maxSpeed;
    protected boolean isRunning = true;
    protected boolean isPaused = false;
    private Random random;
    private List<Car> allCars;
    private long slowDownEndTime = 0;
    private int priority;
    private double distanceTraveled = 0;
    private LapCompletionListener lapCompletionListener;
    private long elapsedTime = 0; // Tempo decorrido para o carro
    private double expectedProgress = 0.0; // Progresso esperado para o carro
    private double trackTotalDistance = 7600;
    // Variável para controlar o cooldown
    private long lastSpeedAdjustmentTime = 0; // Timestamp do último ajuste
    private static final long SPEED_ADJUSTMENT_COOLDOWN = 500; // Cooldown de 2000ms
    private long lastLogTime = 0; // Armazena o timestamp do último log
    private Thread sensorThread;
    private Thread movementThread;
    private Thread adjustmentThread;
    private boolean isRunningThreads = true;
    private final Map<String, Long> totalTaskTimes = new ConcurrentHashMap<>();
    private final Map<String, Integer> taskCounts = new ConcurrentHashMap<>();
    private ExecutorService executorService;
    private static final Map<String, Long> globalTotalTaskTimes = new HashMap<>();
    private static final Map<String, Integer> globalTaskCounts = new HashMap<>();
    private final Map<String, Long> maxTaskTimes = new HashMap<>(); // Armazena o tempo máximo
    protected static Semaphore[][] trackGrid;
    public static final int GRID_SIZE = 20;
    public static final int GRID_START_X = 650; // Início do grid na pista
    public static final int GRID_END_X = 750;  // Fim do grid na pista
    public static final int GRID_START_Y = 50; // Início do grid na pista
    public static final int GRID_END_Y = 150; // Fim do grid na pista
    public int currentGridX = -1;
    public int currentGridY = -1;

    public Car(String name, float x, float y, float size, Bitmap trackBitmap, Bitmap carImage, int color, List<Car> allCars, Semaphore[][] grid) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.size = size;
        this.trackBitmap = trackBitmap;
        this.carImage = carImage;
        this.carColor = color;
        this.directionAngle = 180; // Movimento inicial para a esquerda
        this.trackSensors = new HashMap<>();
        this.carProximitySensors = new HashMap<>();
        this.random = new Random();
        this.maxSpeed = random.nextInt(51);
        this.speed = this.maxSpeed;
        this.allCars = allCars;
        trackGrid = grid;
        initSensors();
        int numCores = Runtime.getRuntime().availableProcessors();
        this.executorService = Executors.newFixedThreadPool(numCores);
        Log.d("ExecutorService", "Executor configurado com " + (numCores) + " threads.");

    }

    public String getName() {
        return name;
    }

    public int getLapCounter() {
        return lapCounter;
    }

    public void incrementLapCounter() {
        lapCounter++;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public int getSpeed() {
        return speed;
    }

    public double getDistanceTraveled() {
        return distanceTraveled;
    }

    public void setLapCounter(int lapCounter) {
        this.lapCounter = lapCounter;
    }

    public Bitmap getTintedCarImage() {
        return tintBitmap(carImage, carColor);
    }

    public float getDirectionAngle() {
        return directionAngle;
    }

    public int getColor() {
        return carColor;
    }

    public void stopRunning() {
        isRunning = false;
    }

    public void pauseRunning() {
        isPaused = true;
    }

    public void resumeRunning() {
        isPaused = false;
    }

    public void setSpeed(int speed, boolean overrideMaxSpeed) {
        if (overrideMaxSpeed) {
            this.speed = speed; // Ignora maxSpeed temporariamente
            //Log.d("SpeedAdjustment", name + " - Velocidade ajustada acima do máximo: " + this.speed);
        } else {
            if (this.speed == speed) {
                //Log.d("SpeedAdjustment", name + " - Velocidade já configurada: " + speed);
                return;
            }
            this.speed = Math.min(speed, maxSpeed);
            //Log.d("SpeedAdjustment", name + " - Velocidade configurada: " + this.speed);
        }
    }

    public int getMaxSpeed() {
        return maxSpeed;
    }

    public interface LapCompletionListener {
        void onLapCompleted(Car car);
    }

    public int getFormattedProgress() {
        return (int) ((distanceTraveled / trackTotalDistance) * 100);
    }

    public void setLapCompletionListener(LapCompletionListener listener) {
        this.lapCompletionListener = listener;
    }

    public void updateElapsedTime(long deltaTime) {
        elapsedTime += deltaTime; // Incrementa o tempo decorrido
        //Log.d("ElapsedTime", name + " - Tempo decorrido: " + elapsedTime);
    }

    public void calculateExpectedProgress(long raceDeadline) {
        expectedProgress = (double) elapsedTime / raceDeadline;
    }

    public double getExpectedProgress() {
        return expectedProgress;
    }

    public void resetProgressMetrics() {
        elapsedTime = 0;
        expectedProgress = 0.0;
    }

    @Override
    public void run() {
        long lastUpdateTime = System.currentTimeMillis(); // Tempo inicial
        while (isRunning) {
            if (isPaused) {
                try {
                    Thread.sleep(5);
                    continue;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            long currentTime = System.currentTimeMillis();
            long deltaTime = currentTime - lastUpdateTime;
            lastUpdateTime = currentTime;

            updateElapsedTime(deltaTime);
            calculateExpectedProgress(30000);
            move();

            try {
                // Controle do tempo de atualização do loop (por exemplo, 5ms de pausa)
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }


    public void move() {
        // Tarefa 1
        executorService.submit(() -> new T1(this).start());

        // Tarefa 2
        executorService.submit(() -> new T2(this).start());

        // Tarefa 3
        executorService.submit(() -> new T3(this).start());

        // Tarefa 4
        executorService.submit(() -> new T4(this).start());

        // Tarefa 5
        executorService.submit(() -> new T5(this).start());

        logAverageTaskTimes();

        logGlobalAverageTaskTimes();
    }

    public class T1 extends Thread {
        private Car carro;

        public T1(Car carro) {
            this.carro = carro;
        }

        @Override
        public void run() {
            carro.Tarefa1();
        }
    }

    public void Tarefa1() {
        long inicioTarefa1 = System.nanoTime();
        updateSensors();
        detectObstacleAndAdjustSpeed();
        long tempoTarefa1 = System.nanoTime() - inicioTarefa1;
        recordTaskTime("Tarefa1", tempoTarefa1);
    }

    public class T2 extends Thread {
        private Car carro;

        public T2(Car carro) {
            this.carro = carro;
        }

        @Override
        public void run() {
            carro.Tarefa2();
        }
    }

    public void Tarefa2() {
        long inicioTarefa2 = System.nanoTime();
        Car safetyCar = findSafetyCar();
        Car carInFront = findCarInFront();

        if (safetyCar != null) {
            adjustSpeedWithSafetyCar(safetyCar);
        }
        if (carInFront != null) {
            adjustSpeedWithCarInFront(carInFront);
        }
        long tempoTarefa2 = System.nanoTime() - inicioTarefa2;
        recordTaskTime("Tarefa2", tempoTarefa2);
    }

    public class T3 extends Thread {
        private Car carro;

        public T3(Car carro) {
            this.carro = carro;
        }

        @Override
        public void run() {
            carro.Tarefa3();
        }
    }

    public void Tarefa3() {
        long inicioTarefa3 = System.nanoTime();
        updateElapsedTime(5);
        calculateExpectedProgress(30000);
        if (isBehindSchedule(30000)) {
            adjustSpeedIfBehindSchedule();
        }
        long tempoTarefa3 = System.nanoTime() - inicioTarefa3;
        recordTaskTime("Tarefa3", tempoTarefa3);
    }

    public class T4 extends Thread {
        private Car carro;

        public T4(Car carro) {
            this.carro = carro;
        }

        @Override
        public void run() {
            carro.Tarefa4();
        }
    }

    public void Tarefa4() {
        long inicioTarefa4 = System.nanoTime();
        float previousX = x;
        float previousY = y;

        float radianAngle = (float) Math.toRadians(directionAngle);
        x += (float) Math.cos(radianAngle) * (speed / 30.0f);
        y += (float) Math.sin(radianAngle) * (speed / 30.0f);

        distanceTraveled += CarCalculations.calculateDistance(previousX, previousY, x, y);
        manageGridSemaphore();
        centralizeOnTrack();
        long tempoTarefa4 = System.nanoTime() - inicioTarefa4;
        recordTaskTime("Tarefa4", tempoTarefa4);
    }

    public class T5 extends Thread {
        private Car carro;

        public T5(Car carro) {
            this.carro = carro;
        }

        @Override
        public void run() {
            carro.Tarefa5();
        }
    }

    public void Tarefa5() {
        long inicioTarefa5 = System.nanoTime();
        if (isNearStartingPoint()) {
            if (!lapCounted) {
                incrementLapCounter();
                lapCounted = true;

                distanceTraveled = 0; // Reset do progresso atual
                resetProgressMetrics(); // Reinicia tempo e progresso esperado

                // Notifique a MainActivity
                if (lapCompletionListener != null) {
                    lapCompletionListener.onLapCompleted(this);
                }
            }
        } else {
            lapCounted = false;
        }
        long tempoTarefa5 = System.nanoTime() - inicioTarefa5;
        recordTaskTime("Tarefa5", tempoTarefa5);
    }

    private void manageGridSemaphore() {
        boolean inGridArea = x >= GRID_START_X && x <= GRID_END_X && y >= GRID_START_Y && y <= GRID_END_Y;
        if (inGridArea) {
            int gridX = (int) ((x - GRID_START_X) / GRID_SIZE);
            int gridY = (int) ((y - GRID_START_Y) / GRID_SIZE);
            if (gridX >= 0 && gridY >= 0 && gridX < trackGrid.length && gridY < trackGrid[0].length) {
                try {
                    if (currentGridX != -1 && currentGridY != -1) {
                        trackGrid[currentGridX][currentGridY].release();
                    }
                    trackGrid[gridX][gridY].acquire();
                    currentGridX = gridX;
                    currentGridY = gridY;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } else {
            if (currentGridX != -1 && currentGridY != -1) {
                try {
                    trackGrid[currentGridX][currentGridY].release();
                } catch (Exception ignored) {
                }
                currentGridX = -1;
                currentGridY = -1;
            }
        }
    }

    private synchronized void recordTaskTime(String taskName, long taskTime) {
        // Atualiza os tempos para o carro específico
        totalTaskTimes.put(taskName, totalTaskTimes.getOrDefault(taskName, 0L) + taskTime);
        taskCounts.put(taskName, taskCounts.getOrDefault(taskName, 0) + 1);

        // Atualiza os tempos no mapa global
        globalTotalTaskTimes.put(taskName, globalTotalTaskTimes.getOrDefault(taskName, 0L) + taskTime);
        globalTaskCounts.put(taskName, globalTaskCounts.getOrDefault(taskName, 0) + 1);

        // Atualiza o valor máximo
        if (!maxTaskTimes.containsKey(taskName) || taskTime > maxTaskTimes.get(taskName)) {
            maxTaskTimes.put(taskName, taskTime);
        }
    }

    private void logGlobalAverageTaskTimes() {
        // Copia temporária dos mapas para evitar concorrência
        Map<String, Long> snapshotTotalTaskTimes = new HashMap<>(totalTaskTimes);
        Map<String, Integer> snapshotTaskCounts = new HashMap<>(taskCounts);
        Map<String, Long> snapshotMaxTaskTimes = new HashMap<>(maxTaskTimes);

        for (String taskName : snapshotTotalTaskTimes.keySet()) {
            long totalTaskTime = snapshotTotalTaskTimes.get(taskName);
            int count = snapshotTaskCounts.getOrDefault(taskName, 1); // Evita divisão por zero
            long averageTime = totalTaskTime / count; // Média de tempo em nanossegundos
            long maxTime = snapshotMaxTaskTimes.getOrDefault(taskName, 0L); // Tempo máximo

            //Log.d("GlobalAverageTaskTime", taskName +
            //        ": Media Global = " + averageTime + " ns em " + count + " execucoes" +
             //       ", Maximo = " + maxTime + " ns");
        }
    }

    private void logAverageTaskTimes() {
        for (String taskName : totalTaskTimes.keySet()) {
            long totalTaskTime = totalTaskTimes.get(taskName);
            int count = taskCounts.get(taskName);
            long averageTime = totalTaskTime / count; // Média de tempo em nanossegundos
            //Log.d("AverageTaskTime", name + " - " + taskName + ": Media = " + averageTime + " ns em " + count + " execucoes");
        }
    }
    // Métodos auxiliares para ajustar velocidade e movimento
    private void adjustSpeedWithSafetyCar(Car safetyCar) {
        double distanceToSafetyCar = calculateDistanceTo(safetyCar);
        if (distanceToSafetyCar < 50) {
            speed = Math.min(safetyCar.getSpeed() - 10, maxSpeed / 3);
        } else if (distanceToSafetyCar < 100) {
            speed = Math.min(safetyCar.getSpeed() - 5, maxSpeed / 2);
        }
    }

    private void adjustSpeedWithCarInFront(Car carInFront) {
        double distanceToCarInFront = calculateDistanceTo(carInFront);
        if (distanceToCarInFront < 50) {
            speed = Math.max(carInFront.getSpeed() - 10, maxSpeed / 3);
        } else if (distanceToCarInFront < 100) {
            speed = Math.max(carInFront.getSpeed() - 5, maxSpeed / 2);
        }
    }

    private void adjustSpeedIfBehindSchedule() {
        long currentTime = System.currentTimeMillis();

        // Verifica se o cooldown já passou
        if (currentTime - lastSpeedAdjustmentTime > SPEED_ADJUSTMENT_COOLDOWN) {
            double delayFactor = (getExpectedProgress() - (distanceTraveled / trackTotalDistance)) * 100;
            int adjustmentRate = (int) Math.max(5, Math.min(delayFactor, 20)); // Incremento dinâmico entre 5 e 20
            setSpeed(Math.min(speed + adjustmentRate, maxSpeed + 100), true); // Permite exceder maxSpeed temporariamente

            lastSpeedAdjustmentTime = currentTime; // Atualiza o timestamp do último ajuste
            //Log.d("DeadlineAdjustment", name + " - Velocidade ajustada por atraso: " + speed);
        } else {
            //Log.d("DeadlineAdjustment", name + " - Cooldown ativo, sem ajuste de velocidade.");
        }
    }

    public boolean isBehindSchedule(long raceDeadline) {
        if (raceDeadline <= 0) {
            //Log.d("DeadlineAdjustment", name + " - Deadline inválido: " + raceDeadline);
            return false;
        }

        // Calcula a proporção do tempo decorrido em relação ao prazo da corrida
        double elapsedRatio = (double) elapsedTime / raceDeadline;
        // Calcula a proporção da distância percorrida em relação à distância total da pista
        double progressRatio = distanceTraveled / trackTotalDistance;

        // Obtém o timestamp atual
        long currentTime = System.currentTimeMillis();

        // Exibe o log apenas uma vez a cada 1 segundo
        if (currentTime - lastLogTime >= 1000) {
            // Atualiza o timestamp do último log
            lastLogTime = currentTime;

            // Calcula os progressos em porcentagem (inteiros)
            int actualProgressPercentage = (int) (progressRatio * 100); // Progresso atual como porcentagem
            int expectedProgressPercentage = (int) (elapsedRatio * 100); // Progresso esperado como porcentagem

            // Loga os valores
            Log.d("DeadlineAjuste", name + " - Progresso atual: " + actualProgressPercentage + "%, Progresso esperado: " + expectedProgressPercentage + "%");
        }

        // Retorna se o progresso está atrasado em relação ao tempo decorrido, com uma margem de 5%
        return progressRatio < elapsedRatio - 0.05; // Margem de atraso de 5%
    }

    public void setPriority(int priority) {
        if (priority < 1 || priority > 10) {
            throw new IllegalArgumentException("A prioridade deve estar entre 1 e 10.");
        }
        this.priority = priority;
    }

    // Método para obter a prioridade
    public int getPriority() {
        return this.priority;
    }

    public double calculateDistanceTo(Car otherCar) {
        return CarCalculations.calculateDistance(this.x, this.y, otherCar.getX(), otherCar.getY());
    }


    private float calculateAngleTo(Car otherCar) {
        return CarCalculations.calculateAngle(this.x, this.y, otherCar.getX(), otherCar.getY());
    }


    private Car findCarInFront() {
        if (allCars.size() <= 1) {
            return null; // Sem outros carros na pista
        }
        Car closestCar = null;
        double minDistance = Double.MAX_VALUE;

        for (Car car : allCars) {
            if (car != this) {
                double distance = calculateDistanceTo(car);
                float angleToCar = calculateAngleTo(car);

                if (distance < minDistance && Math.abs(angleToCar - directionAngle) < 30) {
                    closestCar = car;
                    minDistance = distance;
                }
            }
        }
        return closestCar;
    }

    public Car findSafetyCar() {
        for (Car car : allCars) {
            if (car instanceof SafetyCar) {
                return car;
            }
        }
        return null;
    }

    public boolean isNearOtherCar(Car otherCar) {
        double distance = calculateDistanceTo(otherCar);
        return distance < 100;
    }

    public boolean isNearSafetyCar(Car safetyCar) {
        double distance = Math.sqrt(Math.pow(safetyCar.getX() - this.x, 2) + Math.pow(safetyCar.getY() - this.y, 2));
        return distance < 100;
    }

    private void detectObstacleAndAdjustSpeed() {
        boolean shouldSlowDown = false;

        for (int sensorValue : trackSensors.values()) {
            if (sensorValue != Color.WHITE && sensorValue != Color.BLACK) {
                shouldSlowDown = true;
                break;
            }
        }

        for (int sensorValue : carProximitySensors.values()) {
            if (sensorValue != Color.WHITE && sensorValue != Color.BLACK) {
                shouldSlowDown = true;
                break;
            }
        }

        if (shouldSlowDown) {
            slowDownEndTime = System.currentTimeMillis() + 1000;
        }
    }

    public void updateSensors() {
        int sensorDistance = (int) (size + 60);

        // Chama a biblioteca para calcular as posições dos sensores
        Map<String, int[]> sensorPositions = SensorUtils.calculateSensorPositions(x, y, directionAngle, sensorDistance);

        // Atualiza os valores dos sensores da pista
        for (Map.Entry<String, int[]> entry : sensorPositions.entrySet()) {
            int[] position = entry.getValue();
            trackSensors.put(entry.getKey(), readSensor(position[0], position[1]));
            carProximitySensors.put(entry.getKey(), detectCar(position[0], position[1]));
        }
    }

    private int readSensor(int x, int y) {
        if (x >= 0 && x < trackBitmap.getWidth() && y >= 0 && y < trackBitmap.getHeight()) {
            int pixelColor = trackBitmap.getPixel(x, y);
            return pixelColor;
        }
        return Color.BLACK;
    }

    private int detectCar(int x, int y) {
        if (x >= 0 && x < trackBitmap.getWidth() && y >= 0 && y < trackBitmap.getHeight()) {
            int pixelColor = trackBitmap.getPixel(x, y);
            return (pixelColor != Color.WHITE && pixelColor != Color.BLACK) ? pixelColor : Color.WHITE;
        }
        return Color.WHITE;
    }

    private void initSensors() {
        trackSensors.put("front", Color.WHITE);
        trackSensors.put("left", Color.WHITE);
        trackSensors.put("right", Color.WHITE);
        trackSensors.put("frontLeft", Color.WHITE);
        trackSensors.put("frontRight", Color.WHITE);

        carProximitySensors.put("front", Color.WHITE);
        carProximitySensors.put("left", Color.WHITE);
        carProximitySensors.put("right", Color.WHITE);
        carProximitySensors.put("frontLeft", Color.WHITE);
        carProximitySensors.put("frontRight", Color.WHITE);
    }

    private void centralizeOnTrack() {
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
    }

    protected void normalizeDirection() {
        directionAngle = CarCalculations.normalizeAngle(directionAngle);
    }

    private boolean isNearStartingPoint() {
        return CarCalculations.isNearPoint(x, y, 600, 100, 50);
    }

    private Bitmap tintBitmap(Bitmap bitmap, int color) {
        Bitmap tintedBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());
        Canvas canvas = new Canvas(tintedBitmap);
        Paint paint = new Paint();
        paint.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, 0, 0, paint);
        return tintedBitmap;
    }
}
