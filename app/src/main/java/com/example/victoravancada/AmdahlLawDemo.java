package com.example.victoravancada;

import java.util.concurrent.*;

public class AmdahlLawDemo {
    public static void main(String[] args) throws InterruptedException {
        int numProcessors = Runtime.getRuntime().availableProcessors();
        System.out.println("Número de Processadores: " + numProcessors);

        // Fração paralelizável e sequencial
        double S = 0.3; // Parte sequencial do programa (30%)
        double P = 1 - S; // Parte paralelizável (70%)

        int workload = 1_000_000; // Tamanho do problema

        for (int threads = 1; threads <= numProcessors; threads++) {
            long startTime = System.nanoTime();

            // Parte sequencial
            performSequentialWorkload(S * workload);

            // Parte paralelizável
            performParallelWorkload(P * workload, threads);

            long endTime = System.nanoTime();
            double totalTime = (endTime - startTime) / 1e6; // Tempo em ms

            // Calcula o speedup teórico usando a Lei de Amdahl
            double theoreticalSpeedup = 1 / (S + (P / threads));

            System.out.printf("Threads: %d | Tempo: %.2f ms | Speedup Teórico: %.2f\n",
                    threads, totalTime, theoreticalSpeedup);
        }
    }

    private static void performSequentialWorkload(double workUnits) {
        for (int i = 0; i < (int) workUnits; i++) {
            Math.sqrt(i); // Simula trabalho sequencial
        }
    }

    private static void performParallelWorkload(double workUnits, int threads) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        int chunkSize = (int) (workUnits / threads);

        for (int t = 0; t < threads; t++) {
            executor.submit(() -> {
                for (int i = 0; i < chunkSize; i++) {
                    Math.sqrt(i); // Simula trabalho paralelizável
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
    }
}
