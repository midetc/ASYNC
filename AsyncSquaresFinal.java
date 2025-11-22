import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;

// Клас задачі: приймає список чисел, повертає масив їх квадратів
class SquareTask implements Callable<Double[]> {
    private final List<Double> numbers;
    private final int partId;

    public SquareTask(List<Double> numbers, int partId) {
        this.numbers = numbers;
        this.partId = partId;
    }

    @Override
    public Double[] call() throws Exception {
        System.out.println("Потік " + Thread.currentThread().getName() + " обробляє частину " + partId);
        Thread.sleep(200); 
        
        Double[] squares = new Double[numbers.size()];
        for (int i = 0; i < numbers.size(); i++) {
            squares[i] = numbers.get(i) * numbers.get(i);
        }
        return squares;
    }
}

public class AsyncSquaresFinal {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Введіть діапазон (Enter для стандартного [0.5; 99.5]):");
        double min = 0.5;
        System.out.print("Min: ");
        String minStr = scanner.nextLine();
        if (!minStr.isEmpty()) min = Double.parseDouble(minStr);
        double max = 99.5;
        System.out.print("Max: ");
        String maxStr = scanner.nextLine();
        if (!maxStr.isEmpty()) max = Double.parseDouble(maxStr);

        long startTime = System.currentTimeMillis();
        int size = 40 + (int)(Math.random() * 21);
        List<Double> inputList = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            inputList.add(min + (max - min) * Math.random());
        }
        System.out.println("\nЗгенеровано елементів: " + size);

        CopyOnWriteArraySet<Double> resultSet = new CopyOnWriteArraySet<>();
        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<Future<Double[]>> futures = new ArrayList<>();
        int threadsCount = 4;
        int chunkSize = (int) Math.ceil((double) size / threadsCount);

        for (int i = 0; i < threadsCount; i++) {
            int start = i * chunkSize;
            int end = Math.min(start + chunkSize, size);
            if (start < end) {
                futures.add(executor.submit(new SquareTask(inputList.subList(start, end), i + 1)));
            }
        }

        System.out.println("Очікування завершення потоків...");
        
        for (Future<Double[]> future : futures) {
            try {
                while (!future.isDone()) {
                    Thread.sleep(50); /
                }

                if (!future.isCancelled()) {
                    Double[] result = future.get();
                    resultSet.addAll(Arrays.asList(result));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        executor.shutdown();
        long endTime = System.currentTimeMillis();

        System.out.println("\n--- Результати (Квадрати) ---");
        System.out.println(resultSet);
        System.out.println("Всього значень: " + resultSet.size());
        System.out.println("\nЧас обчислень: " + (endTime - startTime) + " мс");
    }
}
