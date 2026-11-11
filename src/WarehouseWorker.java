import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class WarehouseWorker implements Runnable {
    private final BlockingQueue<CustomerOrder> inputQueue;
    private final BlockingQueue<CustomerOrder> nextQueue;
    private final InventoryManager inventoryManager;
    private final NotificationService notificationService;
    private final AtomicInteger fulfilledOrders;
    private final AtomicBoolean warehouseStageFinished;

    public WarehouseWorker(
            BlockingQueue<CustomerOrder> inputQueue,
            BlockingQueue<CustomerOrder> nextQueue,
            InventoryManager inventoryManager,
            NotificationService notificationService,
            AtomicInteger fulfilledOrders,
            AtomicBoolean warehouseStageFinished) {
        this.inputQueue = inputQueue;
        this.nextQueue = nextQueue;
        this.inventoryManager = inventoryManager;
        this.notificationService = notificationService;
        this.fulfilledOrders = fulfilledOrders;
        this.warehouseStageFinished = warehouseStageFinished;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                CustomerOrder order = inputQueue.poll(200, TimeUnit.MILLISECONDS);
                if (order == null) {
                    if (warehouseStageFinished.get() && inputQueue.isEmpty()) {
                        break;
                    }
                    continue;
                }

                processOrder(order);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void processOrder(CustomerOrder order) {
        if (order.getStatus() == CustomerOrder.OrderStatus.CANCELLED ||
                order.getStatus() == CustomerOrder.OrderStatus.PAYMENT_FAILED) {
            return;
        }

        order.setStatus(CustomerOrder.OrderStatus.PACKAGING);
        order.setLastMessage("Picking and packing order");

        try {
            Thread.sleep(120 + (int) (Math.random() * 180));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        inventoryManager.markOrderFulfilled(order.getProductName(), order.getQuantity());
        order.setStatus(CustomerOrder.OrderStatus.READY_FOR_DELIVERY);
        order.setLastMessage("Order packed and ready for dispatch");
        fulfilledOrders.incrementAndGet();
        notificationService.send(order, "Order packed and ready for delivery.");
        nextQueue.offer(order);
    }
}
