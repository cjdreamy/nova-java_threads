import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class PaymentProcessor implements Runnable {
    private final BlockingQueue<CustomerOrder> inputQueue;
    private final BlockingQueue<CustomerOrder> nextQueue;
    private final InventoryManager inventoryManager;
    private final NotificationService notificationService;
    private final AtomicInteger successfulPayments;
    private final AtomicInteger failedPayments;
    private final AtomicInteger cancelledOrders;
    private final AtomicBoolean paymentStageFinished;
    private final Set<Integer> cancelledOrderIds;

    public PaymentProcessor(
            BlockingQueue<CustomerOrder> inputQueue,
            BlockingQueue<CustomerOrder> nextQueue,
            InventoryManager inventoryManager,
            NotificationService notificationService,
            AtomicInteger successfulPayments,
            AtomicInteger failedPayments,
            AtomicInteger cancelledOrders,
            AtomicBoolean paymentStageFinished,
            Set<Integer> cancelledOrderIds) {
        this.inputQueue = inputQueue;
        this.nextQueue = nextQueue;
        this.inventoryManager = inventoryManager;
        this.notificationService = notificationService;
        this.successfulPayments = successfulPayments;
        this.failedPayments = failedPayments;
        this.cancelledOrders = cancelledOrders;
        this.paymentStageFinished = paymentStageFinished;
        this.cancelledOrderIds = cancelledOrderIds;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                CustomerOrder order = inputQueue.poll(200, TimeUnit.MILLISECONDS);
                if (order == null) {
                    if (paymentStageFinished.get() && inputQueue.isEmpty()) {
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
        order.setStatus(CustomerOrder.OrderStatus.PAYMENT_PROCESSING);
        order.setLastMessage("Processing payment");

        try {
            Thread.sleep(60 + (int) (Math.random() * 150));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        if (cancelledOrderIds.contains(order.getOrderId())) {
            order.setStatus(CustomerOrder.OrderStatus.CANCELLED);
            order.setLastMessage("Order cancelled before dispatch");
            cancelledOrders.incrementAndGet();
            notificationService.send(order, "Order cancelled before payment was processed.");
            return;
        }

        boolean paymentSucceeded = (order.getOrderId() % 17 != 0);

        if (!paymentSucceeded) {
            order.setStatus(CustomerOrder.OrderStatus.PAYMENT_FAILED);
            order.setLastMessage("Payment failed");
            failedPayments.incrementAndGet();
            notificationService.send(order, "Payment failed. Please retry.");
            return;
        }

        order.setStatus(CustomerOrder.OrderStatus.PAYMENT_SUCCEEDED);
        order.setLastMessage("Payment succeeded");
        successfulPayments.incrementAndGet();

        boolean reserved = inventoryManager.reserveStock(order.getProductName(), order.getQuantity());
        if (!reserved) {
            order.setStatus(CustomerOrder.OrderStatus.CANCELLED);
            order.setLastMessage("Stock unavailable; order cancelled");
            cancelledOrders.incrementAndGet();
            notificationService.send(order, "Inventory was insufficient, so the order was cancelled and stock was released.");
            return;
        }

        order.setStatus(CustomerOrder.OrderStatus.INVENTORY_RESERVED);
        order.setLastMessage("Stock reserved");
        nextQueue.offer(order);
    }
}
