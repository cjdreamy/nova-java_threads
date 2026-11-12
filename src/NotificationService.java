
public class NotificationService {
    private final Object lock = new Object();

    public void send(CustomerOrder order, String message) {
        synchronized (lock) {
            System.out.println("[Customer " + order.getCustomerId() + "] Order " + order.getOrderId() +
                    " -> " + message);
        }
    }

    // Additional notification methods
    public void notifyPaymentSuccessful(CustomerOrder order) {
        send(order, "Payment successful");
    }

    public void notifyOrderDispatched(CustomerOrder order) {
        send(order, "Your order has been dispatched");
    }

    public void notifyOrderDelivered(CustomerOrder order) {
        send(order, "Your order has been delivered");
    }
}

