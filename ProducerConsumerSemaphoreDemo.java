import java.time.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class ProducerConsumerSemaphoreDemo {

    static class WorkHours {
        static final int OPEN = 9;
        static final int CLOSE = 18;

        static boolean isOpen(LocalDateTime now) {
            DayOfWeek day = now.getDayOfWeek();
            boolean isWeekday = day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
            int hour = now.getHour();
            return isWeekday && hour >= OPEN && hour < CLOSE;
        }

        static long millisToNextOpen(LocalDateTime now) {
            LocalDateTime nextOpen = now.withHour(OPEN).withMinute(0).withSecond(0).withNano(0);
            
            if (!now.toLocalTime().isBefore(LocalTime.of(OPEN, 0))) {
                nextOpen = nextOpen.plusDays(1);
            }
            
            while (nextOpen.getDayOfWeek() == DayOfWeek.SATURDAY || 
                   nextOpen.getDayOfWeek() == DayOfWeek.SUNDAY) {
                nextOpen = nextOpen.plusDays(1);
            }
            
            return Duration.between(now, nextOpen).toMillis();
        }
    }

    static class Warehouse {
        private final Semaphore items = new Semaphore(0);
        private int stock = 0;

        public synchronized int getStock() {
            return stock;
        }

        public synchronized void supply(int quantity) {
            if (quantity <= 0) {
                throw new IllegalArgumentException("Кількість має бути більше 0");
            }
            stock += quantity;
            items.release(quantity);
            log("Постачальник привіз " + quantity + " од. На складі: " + stock);
        }

        public void takeOne() throws InterruptedException {
            items.acquire();
            synchronized (this) {
                stock -= 1;
                log("Покупець купив 1 од. Залишилось: " + stock);
            }
        }
    }

    static class Supplier implements Runnable {
        private final Warehouse warehouse;

        public Supplier(Warehouse warehouse) {
            this.warehouse = warehouse;
        }

        @Override
        public void run() {
            Thread.currentThread().setName("Supplier");
            log("Постачальник почав роботу");
            
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    warehouse.supply(1);
                    TimeUnit.MILLISECONDS.sleep(800);
                }
            } catch (InterruptedException e) {
                log("Постачальник завершує роботу");
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log("Помилка постачальника: " + e.getMessage());
            }
        }
    }

    static class Customer implements Runnable {
        private final Warehouse warehouse;

        public Customer(Warehouse warehouse) {
            this.warehouse = warehouse;
        }

        @Override
        public void run() {
            Thread.currentThread().setName("Customer");
            log("Покупець почав роботу");
            
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    LocalDateTime now = LocalDateTime.now();
                    
                    if (!WorkHours.isOpen(now)) {
                        log("Склад зачинений. Покупець чекає відкриття");
                        long waitTime = WorkHours.millisToNextOpen(now);
                        TimeUnit.MILLISECONDS.sleep(Math.min(waitTime, 30000));
                        continue;
                    }
                    
                    log("Склад відкритий. Покупець намагається купити товар");
                    warehouse.takeOne();
                    TimeUnit.MILLISECONDS.sleep(1000);
                }
            } catch (InterruptedException e) {
                log("Покупець завершує роботу");
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log("Помилка покупця: " + e.getMessage());
            }
        }
    }

    static class StateMonitor implements Runnable {
        private final Thread[] threads;

        public StateMonitor(Thread... threads) {
            this.threads = threads;
        }

        @Override
        public void run() {
            Thread.currentThread().setName("Monitor");
            
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    StringBuilder info = new StringBuilder("\n--- Стани потоків ---\n");
                    for (Thread t : threads) {
                        info.append(t.getName()).append(": ").append(t.getState()).append("\n");
                    }
                    info.append("---------------------");
                    System.out.println(info);
                    
                    TimeUnit.SECONDS.sleep(3);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("\n========================================");
        System.out.println("Лабораторна робота: Багатопоточність");
        System.out.println("Тема: Producer-Consumer з Semaphore");
        System.out.println("========================================\n");
        
        log("Початок демонстрації");
        log("Постачальник працює цілодобово");
        log("Покупець працює Пн-Пт 9:00-18:00");
        log("");
        
        try {
            Warehouse warehouse = new Warehouse();
            
            Thread supplier = new Thread(new Supplier(warehouse));
            Thread customer = new Thread(new Customer(warehouse));
            Thread monitor = new Thread(new StateMonitor(supplier, customer));
            
            log("Стан потоків перед запуском:");
            log("  Supplier: " + supplier.getState());
            log("  Customer: " + customer.getState());
            log("");
            
            supplier.start();
            customer.start();
            monitor.setDaemon(true);
            monitor.start();
            
            log("Стан потоків після запуску:");
            log("  Supplier: " + supplier.getState());
            log("  Customer: " + customer.getState());
            log("");
            log("Система працює 20 секунд...");
            log("");
            
            TimeUnit.SECONDS.sleep(20);
            
            log("");
            log("Завершення роботи...");
            supplier.interrupt();
            customer.interrupt();
            
            supplier.join();
            customer.join();
            
            System.out.println("\n========================================");
            log("Всі потоки завершені");
            log("Залишок товару на складі: " + warehouse.getStock());
            log("Демонстрація завершена");
            System.out.println("========================================\n");
            
        } catch (InterruptedException e) {
            log("Помилка: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log("Критична помилка: " + e.getMessage());
            e.printStackTrace();
        }
    }

    static void log(String message) {
        String time = LocalTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString();
        System.out.println("[" + time + "] " + message);
    }
}
