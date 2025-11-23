import java.io.IOException;
import java.nio.file.*;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class FileSearchApp {

    static class FileStealTask extends RecursiveTask<Integer> {
        private final Path dir;
        private final long minSize;

        FileStealTask(Path dir, long minSize) {
            this.dir = dir;
            this.minSize = minSize;
        }

        @Override
        protected Integer compute() {
            int c = 0;
            var subs = new java.util.ArrayList<FileStealTask>();
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
                for (Path p : ds) {
                    try {
                        if (Files.isDirectory(p)) {
                            FileStealTask t = new FileStealTask(p, minSize);
                            t.fork();
                            subs.add(t);
                        } else if (Files.size(p) > minSize) c++;
                    } catch (IOException ignored) {}
                }
            } catch (IOException ignored) {}
            for (var t : subs) c += t.join();
            return c;
        }
    }

    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);

        System.out.print("Шлях до директорії: ");
        String d = sc.nextLine().trim();
        System.out.print("Мін розмір (байт): ");
        long minSize = sc.nextLong();

        Path root = Paths.get(d);
        if (!Files.isDirectory(root)) {
            System.out.println("Це не директорія.");
            return;
        }

        ForkJoinPool fj = ForkJoinPool.commonPool();
        long t1 = System.nanoTime();
        int stealCount = fj.invoke(new FileStealTask(root, minSize));
        long t2 = System.nanoTime();
        System.out.println("\nForkJoin (work stealing):");
        System.out.println("Файлів > " + minSize + ": " + stealCount);
        System.out.printf("Час: %.3f ms%n", (t2 - t1) / 1_000_000.0);

        int threads = Runtime.getRuntime().availableProcessors();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        ConcurrentLinkedQueue<Path> q = new ConcurrentLinkedQueue<>();
        q.add(root);
        AtomicInteger c = new AtomicInteger();

        Runnable w = () -> {
            while (true) {
                Path dir = q.poll();
                if (dir == null) break;
                try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
                    for (Path p : ds) {
                        try {
                            if (Files.isDirectory(p)) q.add(p);
                            else if (Files.size(p) > minSize) c.incrementAndGet();
                        } catch (IOException ignored) {}
                    }
                } catch (IOException ignored) {}
            }
        };

        t1 = System.nanoTime();
        for (int i = 0; i < threads; i++) pool.submit(w);
        pool.shutdown();
        pool.awaitTermination(10, TimeUnit.MINUTES);
        t2 = System.nanoTime();

        System.out.println("\nThreadPool (work dealing):");
        System.out.println("Файлів > " + minSize + ": " + c.get());
        System.out.printf("Час: %.3f ms%n", (t2 - t1) / 1_000_000.0);
    }
}
