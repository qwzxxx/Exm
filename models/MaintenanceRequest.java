package models;

public class MaintenanceRequest extends RepairRequest {
    public MaintenanceRequest(int id, int customerId, int units, int priority) {
        super(id, customerId, units, priority);
    }

    @Override
    public int getRequiredMinutes() {
        return getUnits() * 5;
    }

    @Override
    public int getRequiredParts() {
        return 0;
    }

    @Override
    public String getDetails() {
        return "MaintenanceRequest{id=" + getId() + ", customerId=" + getCustomerId()
                + ", units=" + getUnits() + ", priority=" + getPriority()
                + ", minutes=" + getRequiredMinutes() + ", parts=" + getRequiredParts()
                + ", status=" + (isCompleted() ? "выполнена" : "ожидает") + "}";
    }
}
