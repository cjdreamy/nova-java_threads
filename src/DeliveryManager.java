import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class DeliveryManager implements Runnable {
    private final BlockingQueue<CustomerOrder> inputQueue;
    private final List<DeliveryVehicle> vehicles;
    private final Semaphore vehicleSemaphore;
    private final NotificationService notificationService;
    private final AtomicInteger deliveredOrders;
    private final AtomicBoolean deliveryStageFinished;

    public DeliveryManager(
            BlockingQueue<CustomerOrder> inputQueue,
            List<DeliveryVehicle> vehicles,
            Semaphore vehicleSemaphore,
            NotificationService notificationService,
            AtomicInteger deliveredOrders,
            AtomicBoolean deliveryStageFinished) {
        this.inputQueue = inputQueue;
        this.vehicles = vehicles;
        this.vehicleSemaphore = vehicleSemaphore;
        this.notificationService = notificationService;
        this.deliveredOrders = deliveredOrders;
        this.deliveryStageFinished = deliveryStageFinished;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                CustomerOrder order = inputQueue.poll(200, TimeUnit.MILLISECONDS);
                if (order == null) {
                    if (deliveryStageFinished.get() && inputQueue.isEmpty()) {
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
        boolean permitHeld = false;
        DeliveryVehicle vehicle = null;
        try {
            vehicleSemaphore.acquire();
            permitHeld = true;

            vehicle = getAvailableVehicle();
            if (vehicle == null) {
                inputQueue.offer(order);
                return;
            }

            vehicle.setInUse(true);
            order.setStatus(CustomerOrder.OrderStatus.OUT_FOR_DELIVERY);
            order.setLastMessage("Shipment dispatched");
            notificationService.send(order, "Order dispatched by vehicle " + vehicle.getVehicleId() + ".");

            Thread.sleep(140 + (int) (Math.random() * 180));

            vehicle.recordDelivery(order.getQuantity());
            order.setStatus(CustomerOrder.OrderStatus.DELIVERED);
            order.setLastMessage("Order delivered successfully");
            deliveredOrders.incrementAndGet();
            notificationService.send(order, "Order delivered successfully to customer " + order.getCustomerId() + ".");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (vehicle != null) {
                vehicle.setInUse(false);
            }
            if (permitHeld) {
                vehicleSemaphore.release();
            }
        }
    }

    private DeliveryVehicle getAvailableVehicle() {
        for (DeliveryVehicle vehicle : vehicles) {
            if (!vehicle.isInUse()) {
                return vehicle;
            }
        }
        return null;
    }
}
