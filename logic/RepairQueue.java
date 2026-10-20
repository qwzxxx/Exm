package logic;

import models.RepairRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RepairQueue<T extends RepairRequest> {
    private final List<T> requests = new ArrayList<>();
    private int minutes;
    private int parts;

    public RepairQueue(int minutes, int parts) {
        if (minutes < 0 || minutes > 1440) {
            throw new IllegalArgumentException("minutes должен быть от 0 до 1440");
        }
        if (parts < 0 || parts > 1000) {
            throw new IllegalArgumentException("parts должен быть от 0 до 1000");
        }
        this.minutes = minutes;
        this.parts = parts;
    }

    public boolean add(T request) {
        if (request == null || request.isCompleted()) {
            return false;
        }
        boolean duplicate = requests.stream().anyMatch(existing -> existing.getId() == request.getId());
        if (duplicate) {
            return false;
        }
        requests.add(request);
        return true;
    }

    public int getMinutes() {
        return minutes;
    }

    public int getParts() {
        return parts;
    }

    public boolean replenish(int addMinutes, int addParts) {
        if (addMinutes < 0 || addParts < 0 || (addMinutes == 0 && addParts == 0)) {
            return false;
        }
        long newMinutes = (long) minutes + addMinutes;
        long newParts = (long) parts + addParts;
        if (newMinutes > 1440 || newParts > 1000) {
            return false;
        }
        minutes = (int) newMinutes;
        parts = (int) newParts;
        return true;
    }

    public Stream<T> getAnalyticsStream() {
        return requests.stream();
    }

    public List<T> process(Predicate<T> condition, Comparator<T> order, Consumer<T> action) {
        if (condition == null || order == null || action == null) {
            throw new IllegalArgumentException("Параметры process не должны быть null");
        }
        List<T> candidates = requests.stream()
                .filter(request -> !request.isCompleted())
                .sorted(order)
                .collect(Collectors.toList());
        List<T> processed = new ArrayList<>();
        for (T request : candidates) {
            int requiredMinutes = request.getRequiredMinutes();
            int requiredParts = request.getRequiredParts();
            if (requiredMinutes > minutes || requiredParts > parts) {
                continue;
            }
            if (!condition.test(request)) {
                continue;
            }
            int previousMinutes = minutes;
            int previousParts = parts;
            minutes -= requiredMinutes;
            parts -= requiredParts;
            try {
                action.accept(request);
            } catch (RuntimeException exception) {
                minutes = previousMinutes;
                parts = previousParts;
                throw exception;
            }
            if (request.isCompleted()) {
                processed.add(request);
            } else {
                minutes = previousMinutes;
                parts = previousParts;
            }
        }
        return processed;
    }
}
