import java.util.concurrent.atomic.AtomicInteger;

public class DeliveryVehicle {
    private final int vehicleId;
    private final AtomicInteger totalTrips = new AtomicInteger(0);
    private final AtomicInteger totalItemsDelivered = new AtomicInteger(0);
    private volatile boolean inUse = false;

    public DeliveryVehicle(int vehicleId) {
        this.vehicleId = vehicleId;
    }

    public int getVehicleId() {
        return vehicleId;
    }

    public boolean isInUse() {
        return inUse;
    }

    public void setInUse(boolean inUse) {
        this.inUse = inUse;
    }

    public void recordDelivery(int quantity) {
        totalTrips.incrementAndGet();
        totalItemsDelivered.addAndGet(quantity);
    }

    public int getTotalTrips() {
        return totalTrips.get();
    }

    public int getTotalItemsDelivered() {
        return totalItemsDelivered.get();
    }

    @Override
    public String toString() {
        return "Vehicle " + vehicleId + "{trips=" + totalTrips.get() +
                ", itemsDelivered=" + totalItemsDelivered.get() + '}';
    }
}
