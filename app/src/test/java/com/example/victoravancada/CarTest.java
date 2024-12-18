package com.example.victoravancada;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class CarTest {

    private List<Car> cars;
    private Car testCar;

    @Before
    public void setUp() {
        // Inicializa a lista de carros
        cars = new ArrayList<>();

        // Cria um carro de teste
        testCar = new Car("TestCar", 100, 100, 25, null, null, 0xFF0000, cars, null);

        // Adiciona o carro de teste à lista
        cars.add(testCar);
    }

    @Test
    public void testCalculateDistanceTo() {
        // Setup: Criar outro carro para calcular a distância
        Car anotherCar = new Car("AnotherCar", 200, 200, 25, null, null, 0x00FF00, cars, null);

        // Calcula a distância
        double distance = testCar.calculateDistanceTo(anotherCar);

        // Distância esperada
        double expectedDistance = Math.sqrt(100 * 100 + 100 * 100);

        // Logar resultados
        System.out.println("Distância calculada: " + distance);
        System.out.println("Distância esperada: " + expectedDistance);

        // Verifica se a distância calculada está correta
        assertEquals("A distância calculada deve ser igual à esperada", expectedDistance, distance, 0.01);
    }

    @Test
    public void testFindSafetyCar() {
        // Setup: Adicionar um Safety Car à lista
        SafetyCar safetyCar = new SafetyCar("SafetyCar", 300, 300, 30, null, null, 0xFFFF00, cars, null);
        cars.add(safetyCar);

        // Verifica se o método encontra o Safety Car
        Car foundSafetyCar = testCar.findSafetyCar();

        // Logar resultados
        System.out.println("SafetyCar encontrado: " + (foundSafetyCar != null));
        if (foundSafetyCar != null) {
            System.out.println("Nome do SafetyCar encontrado: " + foundSafetyCar.getName());
        }

        // Assert: O Safety Car deve ser encontrado e ser o mesmo adicionado
        assertNotNull("O método findSafetyCar deve retornar um SafetyCar", foundSafetyCar);
        assertTrue("O SafetyCar encontrado deve ser uma instância de SafetyCar", foundSafetyCar instanceof SafetyCar);
        assertEquals("O SafetyCar encontrado deve ser o mesmo que foi adicionado", safetyCar, foundSafetyCar);
    }

    @Test
    public void testIsNearSafetyCar() {
        // Setup: Criar um Safety Car próximo ao carro de teste
        SafetyCar safetyCar = new SafetyCar("SafetyCar", 120, 120, 30, null, null, 0xFFFF00, cars, null);

        // Adiciona o Safety Car à lista
        cars.add(safetyCar);

        // Verifica se o carro está próximo ao Safety Car
        boolean isNear = testCar.isNearSafetyCar(safetyCar);

        // Logar resultados
        System.out.println("Proximidade detectada: " + isNear);

        // Assert: O método deve retornar true se o carro estiver próximo do Safety Car
        assertTrue("O método isNearSafetyCar deve retornar true para carros próximos", isNear);
    }
}
