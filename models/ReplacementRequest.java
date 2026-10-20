package models;

public class ReplacementRequest extends RepairRequest {
    private final int testMinutes;

    public ReplacementRequest(int id, int customerId, int units, int priority, int testMinutes) {
        super(id, customerId, units, priority);
        if (testMinutes < 0 || testMinutes > 15) {
            throw new IllegalArgumentException("testMinutes должен быть от 0 до 15");
        }
        this.testMinutes = testMinutes;
    }

    public int getTestMinutes() {
        return testMinutes;
    }

    @Override
    public int getRequiredMinutes() {
        return getUnits() * 10 + testMinutes;
    }

    @Override
    public int getRequiredParts() {
        return getUnits();
    }

    @Override
    public String getDetails() {
        return "ReplacementRequest{id=" + getId() + ", customerId=" + getCustomerId()
                + ", units=" + getUnits() + ", priority=" + getPriority()
                + ", testMinutes=" + testMinutes + ", minutes=" + getRequiredMinutes()
                + ", parts=" + getRequiredParts()
                + ", status=" + (isCompleted() ? "выполнена" : "ожидает") + "}";
    }
}
