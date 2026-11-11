public class NotificationService {
    private final Object lock = new Object();

    public void send(CustomerOrder order, String message) {
        synchronized (lock) {
            System.out.println("[Customer " + order.getCustomerId() + "] Order " + order.getOrderId() +
                    " -> " + message);
        }
    }
}
