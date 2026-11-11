import java.util.HashMap;
import java.util.Map;

public class InventoryManager {
    private final Map<String, Integer> availableInventory = new HashMap<>();
    private final Map<String, Integer> reservedInventory = new HashMap<>();
    private final Object lock = new Object();

    public InventoryManager() {
        availableInventory.put("Product A", 100);
        availableInventory.put("Product B", 50);
        reservedInventory.put("Product A", 0);
        reservedInventory.put("Product B", 0);
    }

    public boolean reserveStock(String productName, int quantity) {
        synchronized (lock) {
            int available = availableInventory.getOrDefault(productName, 0);
            if (available < quantity) {
                return false;
            }

            availableInventory.put(productName, available - quantity);
            int reserved = reservedInventory.getOrDefault(productName, 0);
            reservedInventory.put(productName, reserved + quantity);
            return true;
        }
    }

    public void releaseReservedStock(String productName, int quantity) {
        synchronized (lock) {
            int reserved = reservedInventory.getOrDefault(productName, 0);
            int released = Math.min(reserved, quantity);
            reservedInventory.put(productName, reserved - released);
            availableInventory.put(productName, availableInventory.getOrDefault(productName, 0) + released);
        }
    }

    public void markOrderFulfilled(String productName, int quantity) {
        synchronized (lock) {
            int reserved = reservedInventory.getOrDefault(productName, 0);
            if (reserved >= quantity) {
                reservedInventory.put(productName, reserved - quantity);
            }
        }
    }

    public Map<String, Integer> getAvailableInventory() {
        synchronized (lock) {
            return new HashMap<>(availableInventory);
        }
    }

    public Map<String, Integer> getReservedInventory() {
        synchronized (lock) {
            return new HashMap<>(reservedInventory);
        }
    }
}
