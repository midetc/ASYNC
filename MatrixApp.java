import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.*;

public class MatrixApp {

    static class StealTask extends RecursiveTask<long[]> {
        private final int[][] a;
        private final int fromCol, toCol;
        private static final int THRESHOLD = 10;

        StealTask(int[][] a, int fromCol, int toCol) {
            this.a = a;
            this.fromCol = fromCol;
            this.toCol = toCol;
        }

        @Override
        protected long[] compute() {
            int len = toCol - fromCol;
            if (len <= THRESHOLD) {
                long[] r = new long[len];
                for (int j = fromCol; j < toCol; j++) {
                    long s = 0;
                    for (int i = 0; i < a.length; i++) s += a[i][j];
                    r[j - fromCol] = s;
                }
                return r;
            }
            int mid = fromCol + len / 2;
            StealTask l = new StealTask(a, fromCol, mid);
            StealTask r = new StealTask(a, mid, toCol);
            l.fork();
            long[] rr = r.compute();
            long[] ll = l.join();
            long[] res = new long[len];
            System.arraycopy(ll, 0, res, 0, ll.length);
            System.arraycopy(rr, 0, res, ll.length, rr.length);
            return res;
        }
    }

    static void printMatrix(int[][] a) {
        for (int[] row : a) {
            for (int x : row) System.out.printf("%5d", x);
            System.out.println();
        }
    }

    static void printSums(String t, long[] s, double ms) {
        System.out.println("\n" + t);
        for (int j = 0; j < s.length; j++)
            System.out.printf("Стовпець %d = %d%n", j, s[j]);
        System.out.printf("Час: %.3f ms%n", ms);
    }

    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);

        System.out.print("Рядків: ");
        int rows = sc.nextInt();
        System.out.print("Стовпців: ");
        int cols = sc.nextInt();
        System.out.print("Мін значення: ");
        int min = sc.nextInt();
        System.out.print("Макс значення: ");
        int max = sc.nextInt();

        int[][] a = new int[rows][cols];
        Random r = new Random();
        for (int i = 0; i < rows; i++)
            for (int j = 0; j < cols; j++)
                a[i][j] = r.nextInt(max - min + 1) + min;

        System.out.println("\nМатриця:");
        printMatrix(a);

        ForkJoinPool fj = ForkJoinPool.commonPool();
        long t1 = System.nanoTime();
        long[] steal = fj.invoke(new StealTask(a, 0, cols));
        long t2 = System.nanoTime();
        printSums("ForkJoin (work stealing)", steal, (t2 - t1) / 1_000_000.0);

        int threads = Runtime.getRuntime().availableProcessors();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        long[] dealing = new long[cols];
        int part = (cols + threads - 1) / threads;

        t1 = System.nanoTime();
        for (int start = 0; start < cols; start += part) {
            int from = start;
            int to = Math.min(cols, start + part);
            pool.submit(() -> {
                for (int j = from; j < to; j++) {
                    long s = 0;
                    for (int i = 0; i < a.length; i++) s += a[i][j];
                    dealing[j] = s;
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.MINUTES);
        t2 = System.nanoTime();

        printSums("ThreadPool (work dealing)", dealing, (t2 - t1) / 1_000_000.0);
    }
}
