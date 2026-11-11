public class CustomerOrder {
    public enum OrderStatus {
        SUBMITTED,
        PAYMENT_PROCESSING,
        PAYMENT_FAILED,
        PAYMENT_SUCCEEDED,
        INVENTORY_RESERVED,
        PACKAGING,
        READY_FOR_DELIVERY,
        OUT_FOR_DELIVERY,
        DELIVERED,
        CANCELLED
    }

    private final int orderId;
    private final int customerId;
    private final String productName;
    private final int quantity;
    private volatile OrderStatus status;
    private volatile String lastMessage;

    public CustomerOrder(int orderId, int customerId, String productName, int quantity) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.productName = productName;
        this.quantity = quantity;
        this.status = OrderStatus.SUBMITTED;
        this.lastMessage = "Order submitted";
    }

    public int getOrderId() {
        return orderId;
    }

    public int getCustomerId() {
        return customerId;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    @Override
    public String toString() {
        return "CustomerOrder{" +
                "orderId=" + orderId +
                ", customerId=" + customerId +
                ", productName='" + productName + '\'' +
                ", quantity=" + quantity +
                ", status=" + status +
                '}';
    }
}
