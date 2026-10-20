package models;

import interfaces.RepairDemand;

public abstract class RepairRequest implements RepairDemand {
    private final int id;
    private final int customerId;
    private final int units;
    private final int priority;
    private boolean completed;

    protected RepairRequest(int id, int customerId, int units, int priority) {
        if (id <= 0 || customerId <= 0) {
            throw new IllegalArgumentException("ID должны быть положительными");
        }
        if (units < 1 || units > 20) {
            throw new IllegalArgumentException("units должен быть от 1 до 20");
        }
        if (priority < 1 || priority > 5) {
            throw new IllegalArgumentException("priority должен быть от 1 до 5");
        }
        this.id = id;
        this.customerId = customerId;
        this.units = units;
        this.priority = priority;
        this.completed = false;
    }

    public int getId() {
        return id;
    }

    public int getCustomerId() {
        return customerId;
    }

    public int getUnits() {
        return units;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void markCompleted() {
        completed = true;
    }

    public abstract String getDetails();
}
