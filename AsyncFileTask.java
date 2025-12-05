import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class AsyncFileTask {

    public static void main(String[] args) {
        
        CompletableFuture.runAsync(() -> {
            try {
                Files.write(Paths.get("text1.txt"), Arrays.asList("Hello World!", "Java Future API 2025"));
                Files.write(Paths.get("text2.txt"), Arrays.asList("Async Programming", "KPI Students Best"));
                System.out.println("Файли успішно створено.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).join(); 

        System.out.println("--- Початок асинхронної обробки ---");

       
        CompletableFuture<List<String>> textProcessingChain = CompletableFuture.supplyAsync(() -> {
            long start = System.nanoTime();
            List<String> combinedList = new ArrayList<>();
            try {
               
                combinedList.addAll(Files.readAllLines(Paths.get("text1.txt")));
                combinedList.addAll(Files.readAllLines(Paths.get("text2.txt")));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            
            long end = System.nanoTime();
            System.out.println("1. Читання файлів зайняло: " + (end - start) + " нс");
            System.out.println("   Прочитано: " + combinedList);
            return combinedList; 
        });

       
        CompletableFuture<List<String>> filteredTask = textProcessingChain.thenApplyAsync(sentences -> {
            long start = System.nanoTime();
            
           
            List<String> filtered = sentences.stream()
                    .map(s -> s.replaceAll("[a-zA-Zа-яА-ЯіІїЇєЄ]", "")) 
                    .collect(Collectors.toList());

            long end = System.nanoTime();
            System.out.println("2. Видалення літер зайняло: " + (end - start) + " нс");
            return filtered; 
        });

       
        CompletableFuture<Void> printTask = filteredTask.thenAcceptAsync(result -> {
            long start = System.nanoTime();
            System.out.println("   Результат (залишок): " + result);
            long end = System.nanoTime();
            System.out.println("3. Виведення на екран зайняло: " + (end - start) + " нс");
        });

       
        printTask.thenRunAsync(() -> {
            System.out.println("4. Всі задачі виконано успішно! Час зафіксовано.");
        }).join(); 
    }
}
