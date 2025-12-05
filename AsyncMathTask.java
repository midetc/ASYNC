import java.util.*;
import java.util.concurrent.*;

public class AsyncMathTask {

    public static void main(String[] args) {
        
        System.out.println("--- Початок Завдання 2: Математика ---");
        
        long globalStart = System.nanoTime();

        CompletableFuture<List<Double>> sequenceTask = CompletableFuture.supplyAsync(() -> {
            Random rand = new Random();
            List<Double> numbers = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                numbers.add(rand.nextDouble() * 100); /
            }
            
            System.out.println("1. Згенерована послідовність: " + numbers);
            return numbers; 
        });

      
        CompletableFuture<Double> calculationTask = sequenceTask.thenApplyAsync(numbers -> {
            double sum = 0;
      
            for (int i = 0; i < numbers.size() - 1; i++) {
                sum += numbers.get(i) * numbers.get(i + 1);
            }
            
            System.out.println("2. Обчислення за формулою виконано.");
            return sum; 
        });

       
        CompletableFuture<Void> printTask = calculationTask.thenAcceptAsync(result -> {
            System.out.printf("3. Фінальний результат суми добутків: %.4f%n", result);
        });

      
        printTask.thenRunAsync(() -> {
            long globalEnd = System.nanoTime();
            System.out.println("4. Загальний час роботи всіх асинхронних операцій: " + (globalEnd - globalStart) + " нс");
        }).join(); 
    }
}
