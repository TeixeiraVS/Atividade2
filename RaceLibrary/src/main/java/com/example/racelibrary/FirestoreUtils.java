package com.example.racelibrary;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FirestoreUtils {

    private static final String TAG = "FirestoreUtils";

    /**
     * Interface para callbacks ao carregar estados dos carros.
     */
    public interface FirestoreLoadCallback {
        void onSuccess(Map<String, Object> carStates);

        void onSuccess(List<CarData> carDataList);
        void onFailure(Exception e);
    }

    /**
     * Salva o estado dos carros no Firestore.
     *
     * @param db           Instância do FirebaseFirestore.
     * @param carDataList  Lista de CarData representando o estado dos carros.
     */
    public static void saveCarStates(FirebaseFirestore db, List<CarData> carDataList) {
        if (carDataList == null || carDataList.isEmpty()) {
            Log.w(TAG, "Nenhum carro para salvar.");
            return;
        }

        // Converte a lista de carros para um mapa de estados
        Map<String, Map<String, Object>> carStates = new java.util.HashMap<>();
        for (CarData carData : carDataList) {
            Map<String, Object> carState = new java.util.HashMap<>();
            carState.put("x", carData.getX());
            carState.put("y", carData.getY());
            carState.put("size", carData.getSize());
            carState.put("speed", carData.getSpeed());
            carState.put("directionAngle", carData.getDirectionAngle());
            carState.put("lapCounter", carData.getLapCounter());
            carState.put("color", carData.getColor());
            carStates.put(carData.getName(), carState);
        }

        // Salva no Firestore
        db.collection("raceStates").document("currentRace")
                .set(carStates)
                .addOnSuccessListener(unused -> Log.d(TAG, "Estados dos carros salvos com sucesso!"))
                .addOnFailureListener(e -> Log.e(TAG, "Erro ao salvar estados dos carros!", e));
    }

    /**
     * Carrega os estados dos carros do Firestore.
     *
     * @param db       Instância do FirebaseFirestore.
     * @param callback Callback para processar os dados carregados.
     */
    public static void loadCarStates(FirebaseFirestore db, FirestoreLoadCallback callback) {
        db.collection("raceStates").document("currentRace")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Map<String, Object> carStates = documentSnapshot.getData();
                        if (carStates != null) {
                            List<CarData> carDataList = recreateCarsFromStates(carStates);
                            callback.onSuccess(carDataList);
                        } else {
                            callback.onFailure(new Exception("Nenhum dado encontrado."));
                        }
                    } else {
                        callback.onFailure(new Exception("Nenhum estado salvo encontrado."));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Erro ao carregar estados dos carros!", e);
                    callback.onFailure(e);
                });
    }

    /**
     * Recria uma lista de CarData a partir dos estados carregados do Firestore.
     *
     * @param carStates Mapa de estados dos carros.
     * @return Lista de CarData recriada.
     */
    private static List<CarData> recreateCarsFromStates(Map<String, Object> carStates) {
        List<CarData> carDataList = new ArrayList<>();
        for (Map.Entry<String, Object> entry : carStates.entrySet()) {
            String carName = entry.getKey();
            Map<String, Object> carState = (Map<String, Object>) entry.getValue();

            CarData carData = new CarData(
                    carName,
                    ((Double) carState.get("x")).floatValue(),
                    ((Double) carState.get("y")).floatValue(),
                    ((Double) carState.get("size")).floatValue(),
                    ((Long) carState.get("speed")).intValue(),
                    ((Double) carState.get("directionAngle")).floatValue(),
                    ((Long) carState.get("lapCounter")).intValue(),
                    ((Long) carState.get("color")).intValue()
            );
            carDataList.add(carData);
        }
        return carDataList;
    }
}
