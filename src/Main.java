import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        final int totalOrders = 1000;

        InventoryManager inventoryManager = new InventoryManager();
        NotificationService notificationService = new NotificationService();

        BlockingQueue<CustomerOrder> submittedOrders = new LinkedBlockingQueue<>();
        BlockingQueue<CustomerOrder> paymentQueue = new LinkedBlockingQueue<>();
        BlockingQueue<CustomerOrder> warehouseQueue = new LinkedBlockingQueue<>();
        BlockingQueue<CustomerOrder> deliveryQueue = new LinkedBlockingQueue<>();

        AtomicInteger totalReceived = new AtomicInteger(0);
        AtomicInteger successfulPayments = new AtomicInteger(0);
        AtomicInteger failedPayments = new AtomicInteger(0);
        AtomicInteger cancelledOrders = new AtomicInteger(0);
        AtomicInteger fulfilledOrders = new AtomicInteger(0);
        AtomicInteger deliveredOrders = new AtomicInteger(0);

        AtomicBoolean submissionFinished = new AtomicBoolean(false);
        AtomicBoolean paymentStageFinished = new AtomicBoolean(false);
        AtomicBoolean warehouseStageFinished = new AtomicBoolean(false);
        AtomicBoolean deliveryStageFinished = new AtomicBoolean(false);

        Set<Integer> cancelledOrderIds = new HashSet<>();
        Random random = new Random();

        for (int i = 0; i < 80; i++) {
            cancelledOrderIds.add(random.nextInt(totalOrders) + 1);
        }

        ExecutorService submissionPool = Executors.newFixedThreadPool(20);
        for (int orderId = 1; orderId <= totalOrders; orderId++) {
            final int finalOrderId = orderId;
            submissionPool.submit(() -> {
                String[] products = {"Product A", "Product B"};
                String product = products[random.nextInt(products.length)];
                int quantity = 1 + random.nextInt(3);
                int customerId = 1000 + random.nextInt(2000);
                CustomerOrder order = new CustomerOrder(finalOrderId, customerId, product, quantity);
                totalReceived.incrementAndGet();
                submittedOrders.offer(order);
            });
        }

        ExecutorService paymentPool = Executors.newFixedThreadPool(3);
        for (int i = 0; i < 3; i++) {
            paymentPool.submit(new PaymentProcessor(
                    submittedOrders,
                    paymentQueue,
                    inventoryManager,
                    notificationService,
                    successfulPayments,
                    failedPayments,
                    cancelledOrders,
                    paymentStageFinished,
                    cancelledOrderIds));
        }

        ExecutorService warehousePool = Executors.newFixedThreadPool(5);
        for (int i = 0; i < 5; i++) {
            warehousePool.submit(new WarehouseWorker(
                    paymentQueue,
                    deliveryQueue,
                    inventoryManager,
                    notificationService,
                    fulfilledOrders,
                    warehouseStageFinished));
        }

        Semaphore vehicleSemaphore = new Semaphore(4);
        List<DeliveryVehicle> vehicles = new ArrayList<>();
        for (int i = 1; i <= 4; i++) {
            vehicles.add(new DeliveryVehicle(i));
        }

        ExecutorService deliveryPool = Executors.newFixedThreadPool(4);
        for (int i = 0; i < 4; i++) {
            deliveryPool.submit(new DeliveryManager(
                    deliveryQueue,
                    vehicles,
                    vehicleSemaphore,
                    notificationService,
                    deliveredOrders,
                    deliveryStageFinished));
        }

        submissionPool.shutdown();
        submissionPool.awaitTermination(10, TimeUnit.SECONDS);
        submissionFinished.set(true);

        paymentPool.shutdown();
        paymentPool.awaitTermination(20, TimeUnit.SECONDS);
        paymentStageFinished.set(true);

        warehousePool.shutdown();
        warehousePool.awaitTermination(25, TimeUnit.SECONDS);
        warehouseStageFinished.set(true);

        deliveryPool.shutdown();
        deliveryPool.awaitTermination(25, TimeUnit.SECONDS);
        deliveryStageFinished.set(true);

        System.out.println("\n=== Final Report ===");
        System.out.println("Total orders received: " + totalReceived.get());
        System.out.println("Successful payments: " + successfulPayments.get());
        System.out.println("Failed payments: " + failedPayments.get());
        System.out.println("Orders fulfilled: " + fulfilledOrders.get());
        System.out.println("Orders delivered: " + deliveredOrders.get());
        System.out.println("Orders cancelled: " + cancelledOrders.get());
        System.out.println("Remaining inventory: " + inventoryManager.getAvailableInventory());

        int totalVehicleTrips = 0;
        for (DeliveryVehicle vehicle : vehicles) {
            totalVehicleTrips += vehicle.getTotalTrips();
        }
        System.out.println("Vehicle utilization: total trips=" + totalVehicleTrips + ", vehicle details=" + vehicles);
        System.out.println("\nSimulation complete.");
    }
}
