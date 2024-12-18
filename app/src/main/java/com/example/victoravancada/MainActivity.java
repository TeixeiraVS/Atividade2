package com.example.victoravancada;

import static com.example.victoravancada.Car.GRID_END_X;
import static com.example.victoravancada.Car.GRID_END_Y;
import static com.example.victoravancada.Car.GRID_SIZE;
import static com.example.victoravancada.Car.GRID_START_X;
import static com.example.victoravancada.Car.GRID_START_Y;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.racelibrary.CarData;
import com.example.racelibrary.FirestoreUtils;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Semaphore;

public class MainActivity extends AppCompatActivity {

    private RacingTrackView racingTrackView;
    private Bitmap trackBitmap;
    private Button startButton, endButton, pauseButton, safetyCarButton;
    private EditText numberOfCarsInput;
    private LinearLayout lapCounterContainer;
    private boolean isMoving = false;
    private boolean isPaused = false;
    private int numberOfCars = 3;
    private Map<String, TextView> carLapTextViews = new HashMap<>();
    private Handler handler = new Handler();
    private List<Car> carsList;
    private SafetyCar safetyCar;
    private boolean isSafetyCarActive = false;
    private Semaphore[][] trackGrid;
    private long raceStartTime; // Hora de início da corrida
    private double trackTotalDistance = 7600; // Distância total da pista (ajustar conforme necessário)
    private long deadline = 20000; // Tempo limite da corrida em milissegundos


    private FirebaseFirestore firestore; // Firestore instance

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firestore = FirebaseFirestore.getInstance();

        racingTrackView = findViewById(R.id.RacingTrackView);
        trackBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.pista);
        racingTrackView.setTrackBitmap(trackBitmap);
        racingTrackView.invalidate();

        lapCounterContainer = findViewById(R.id.lapCounterContainer);
        numberOfCarsInput = findViewById(R.id.numberOfCarsInput);

        startButton = findViewById(R.id.startButton);
        pauseButton = findViewById(R.id.pauseButton);
        endButton = findViewById(R.id.endButton);
        safetyCarButton = findViewById(R.id.safetyCarButton);

        startButton.setOnClickListener(v -> {
            if (!isMoving) {
                loadCarStatesAndStart();
            }
        });

        pauseButton.setOnClickListener(v -> {
            togglePauseResume();
            if (isPaused) {
                saveCarStates(); // Salva o estado dos carros ao pausar
                Toast.makeText(MainActivity.this, "Corrida pausada! Estado salvo no Firebase.", Toast.LENGTH_SHORT).show();
            }
        });

        endButton.setOnClickListener(v -> {
            stopCarMovement();
            saveCarStates(); // Salva o estado dos carros ao finalizar
            Toast.makeText(MainActivity.this, "Corrida finalizada!", Toast.LENGTH_LONG).show();
        });

        safetyCarButton.setOnClickListener(v -> {
            if (isSafetyCarActive) {
                deactivateSafetyCar();
            } else {
                activateSafetyCar();
            }
            isSafetyCarActive = !isSafetyCarActive;
            safetyCarButton.setText(isSafetyCarActive ? "Desativar Safety Car" : "Ativar Safety Car");
        });
    }

    private List<CarData> convertToCarData(List<Car> carsList) {
        List<CarData> carDataList = new ArrayList<>();
        for (Car car : carsList) {
            CarData carData = new CarData(
                    car.getName(),
                    car.getX(),
                    car.getY(),
                    car.size,
                    car.getSpeed(),
                    car.getDirectionAngle(),
                    car.getLapCounter(),
                    car.getColor()
            );
            carData.setMaxSpeed(car.getMaxSpeed());
            carData.setPriority(car.getPriority());
            carData.addDistanceTraveled(car.getDistanceTraveled());
            carDataList.add(carData);
        }
        return carDataList;
    }

    private List<Car> convertToCar(List<CarData> carDataList) {
        List<Car> cars = new ArrayList<>();
        for (CarData carData : carDataList) {
            Car car = new Car(
                    carData.getName(),
                    (float) carData.getX(),
                    (float) carData.getY(),
                    (float) carData.getSize(),
                    trackBitmap,
                    BitmapFactory.decodeResource(getResources(), R.drawable.car_icon),
                    carData.getColor(),
                    cars, // Use a nova lista 'cars' em vez de 'carsList'
                    trackGrid
            );
            car.setSpeed(carData.getSpeed(), false); // Respeita o limite de velocidade máxima
            car.directionAngle = (float) carData.getDirectionAngle();
            car.setLapCounter(carData.getLapCounter());
            cars.add(car);
        }
        return cars;
    }



    private void saveCarStates() {
        FirestoreUtils.saveCarStates(firestore, convertToCarData(carsList));
    }


    private void loadCarStatesAndStart() {
        FirestoreUtils.loadCarStates(firestore, new FirestoreUtils.FirestoreLoadCallback() {
            @Override
            public void onSuccess(List<CarData> carDataList) {
                carsList = convertToCar(carDataList);
                racingTrackView.setCars(carsList);
                setupLapCounters(carsList);
                startCarMovement();
                isMoving = true;
                Toast.makeText(MainActivity.this, "Corrida retomada!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onSuccess(Map<String, Object> carStates) {
                // Opcional: se necessário, use este método ou delegue ao outro
                List<CarData> carDataList = new ArrayList<>();
                for (Map.Entry<String, Object> entry : carStates.entrySet()) {
                    Map<String, Object> carState = (Map<String, Object>) entry.getValue();
                    carDataList.add(new CarData(
                            entry.getKey(),
                            ((Double) carState.get("x")).floatValue(),
                            ((Double) carState.get("y")).floatValue(),
                            ((Double) carState.get("size")).floatValue(),
                            ((Long) carState.get("speed")).intValue(),
                            ((Double) carState.get("directionAngle")).floatValue(),
                            ((Long) carState.get("lapCounter")).intValue(),
                            ((Long) carState.get("color")).intValue()
                    ));
                }
                onSuccess(carDataList); // Chama o outro método para evitar duplicação
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("MainActivity", "Erro ao carregar estados dos carros!", e);
                Toast.makeText(MainActivity.this, "Erro ao carregar estado. Iniciando nova corrida.", Toast.LENGTH_SHORT).show();
                startNewRace();
            }
        });
    }

    private void startNewRace() {
        String inputText = numberOfCarsInput.getText().toString();
        if (!inputText.isEmpty()) {
            numberOfCars = Integer.parseInt(inputText);
        }

        trackGrid = createTrackGrid();
        carsList = createCars(numberOfCars);
        racingTrackView.setCars(carsList);
        setupLapCounters(carsList);

        raceStartTime = System.currentTimeMillis(); // Registra o início da corrida
        startCarMovement();
        isMoving = true;
        Toast.makeText(this, "Corrida iniciada!", Toast.LENGTH_SHORT).show();

        // Monitorar progresso da corrida
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isMoving) {
                    handler.postDelayed(this, 100); // Atualiza a cada 100ms
                }
            }
        }, 100);
    }
    private Semaphore[][] createTrackGrid() {
        if (trackGrid != null) {
            return trackGrid; // Retorna o grid existente
        }

        int rows = (int) Math.ceil((double) (GRID_END_Y - GRID_START_Y) / GRID_SIZE);
        int cols = (int) Math.ceil((double) (GRID_END_X - GRID_START_X) / GRID_SIZE);
        Semaphore[][] grid = new Semaphore[cols][rows];

        for (int i = 0; i < cols; i++) {
            for (int j = 0; j < rows; j++) {
                grid[i][j] = new Semaphore(1); // Apenas um carro por célula
            }
        }
        return grid;
    }

    /**
     * Cria os carros da corrida.
     */
    private List<Car> createCars(int numberOfCars) {
        int carSize = 35;
        int initialX = 600;
        int initialY = 100;
        int xOffset = 55; // Distância horizontal entre os carros
        int yOffset = 40;  // Pequena diferença vertical para criar o efeito de fila

        List<Car> carsList = new ArrayList<>();
        for (int i = 0; i < numberOfCars; i++) {
            int color = generateRandomColor();
            Bitmap carImage = BitmapFactory.decodeResource(getResources(), R.drawable.car_icon);

            // Calcula posições dinâmicas para o efeito de fila
            int posX = initialX + i * xOffset; // Cada carro fica mais à frente horizontalmente
            int posY = initialY + (i % 2 == 0 ? 0 : yOffset); // Alterna ligeiramente o eixo vertical para criar o efeito

            Car car = new Car("Carro" + (i + 1), posX, posY, carSize, trackBitmap, carImage, color, carsList, trackGrid);
            carsList.add(car);
        }
        return carsList;
    }
    /**
     * Configura os contadores de voltas para exibir na interface.
     */
    private void setupLapCounters(List<Car> carsList) {
        lapCounterContainer.removeAllViews();
        carLapTextViews.clear();

        for (Car car : carsList) {
            TextView lapTextView = new TextView(this);
            lapTextView.setText(car.getName() + " - Voltas: 0");
            lapCounterContainer.addView(lapTextView);
            carLapTextViews.put(car.getName(), lapTextView);
        }
    }

    /**
     * Inicia o movimento dos carros.
     */
    private void startCarMovement() {
        if (carsList == null || carsList.isEmpty()) {
            Log.e("MainActivity", "Nenhum carro disponível para iniciar o movimento.");
            return;
        }

        for (Car car : carsList) {
            car.resumeRunning(); // Garante que o carro esteja pronto para mover
            Thread carThread = new Thread(car);
            carThread.start();
        }

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateLapCounters();
                racingTrackView.invalidate();

                if (isMoving) {
                    handler.postDelayed(this, 50);
                }
            }
        }, 50);
    }

    /**
     * Atualiza os contadores de voltas dos carros na interface.
     */
    private void updateLapCounters() {
        for (Car car : carsList) {
            TextView lapTextView = carLapTextViews.get(car.getName());
            if (lapTextView != null) {
                lapTextView.setText(car.getName() + " - Voltas: " + car.getLapCounter());
            }
        }
    }

    /**
     * Para o movimento dos carros.
     */
    private void stopCarMovement() {
        isMoving = false;

        // Para o movimento dos carros
        if (carsList != null) {
            for (Car car : carsList) {
                car.stopRunning();
            }
        }

        // Remove os carros da tela
        racingTrackView.setCars(null);
        racingTrackView.invalidate();

        // Remove os contadores de voltas
        lapCounterContainer.removeAllViews();
        carLapTextViews.clear(); // Limpa o mapa de contadores
    }

    /**
     * Alterna entre pausar e retomar a corrida.
     */
    private void togglePauseResume() {
        if (isPaused) {
            for (Car car : carsList) {
                car.resumeRunning();
            }
            pauseButton.setText("Pausar");
            isPaused = false;
        } else {
            for (Car car : carsList) {
                car.pauseRunning();
            }
            pauseButton.setText("Retomar");
            isPaused = true;
        }
    }

    /**
     * Gera uma cor aleatória para os carros.
     */
    private int generateRandomColor() {
        int[] colors = {0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFF00, 0xFF00FFFF};
        return colors[new Random().nextInt(colors.length)];
    }

    /**
     * Ativa o Safety Car na pista.
     */
    private void activateSafetyCar() {
        Bitmap carImage = BitmapFactory.decodeResource(getResources(), R.drawable.car_icon);
        safetyCar = new SafetyCar("Safety Car", 700, 100, 30, trackBitmap, carImage, Color.YELLOW, carsList, trackGrid);
        carsList.add(safetyCar);
        Thread safetyCarThread = new Thread(safetyCar);
        safetyCarThread.start();
    }

    /**
     * Desativa o Safety Car.
     */
    private void deactivateSafetyCar() {
        if (safetyCar != null) {
            safetyCar.stopRunning();
            carsList.remove(safetyCar);
            safetyCar = null;
        }
    }
}
