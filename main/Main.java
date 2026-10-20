package main;

import logic.RepairQueue;
import models.MaintenanceRequest;
import models.RepairRequest;
import models.ReplacementRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    private static int n;
    private static RepairQueue<RepairRequest> queue;
    private static int variantA;
    private static int variantB;
    private static int variantV;
    private static boolean stage1Done;
    private static boolean stage2Done;
    private static boolean stage3Done;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Integer enteredN = readInt(scanner, "Введите N (1..36): ", 1, 36);
        if (enteredN == null) {
            System.out.println("Ввод завершён. Программа закрыта.");
            return;
        }
        n = enteredN;
        while (true) {
            System.out.println();
            System.out.println("1. Выполнить задание 1 и назначить вариант задания 2");
            System.out.println("2. Выполнить задание 2 и назначить вариант задания 3");
            System.out.println("3. Выполнить задание 3 и вывести отчёт");
            System.out.println("4. Показать очередь и остатки ресурсов");
            System.out.println("5. Выход");
            Integer choice = readInt(scanner, "Выберите пункт меню (1..5): ", 1, 5);
            if (choice == null) {
                System.out.println("Ввод завершён. Программа закрыта.");
                return;
            }
            switch (choice) {
                case 1:
                    runStage1();
                    break;
                case 2:
                    runStage2();
                    break;
                case 3:
                    runStage3();
                    break;
                case 4:
                    showQueue();
                    break;
                case 5:
                    System.out.println("Программа завершена.");
                    return;
                default:
                    System.out.println("Нет такого пункта меню.");
            }
        }
    }

    private static Integer readInt(Scanner scanner, String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            if (!scanner.hasNextLine()) {
                return null;
            }
            String value = scanner.nextLine().trim();
            try {
                int number = Integer.parseInt(value);
                if (number < min || number > max) {
                    System.out.println("Введите целое число от " + min + " до " + max + ".");
                    continue;
                }
                return number;
            } catch (NumberFormatException exception) {
                System.out.println("Ошибка ввода. Требуется целое число от " + min + " до " + max + ".");
            }
        }
    }

    private static void runStage1() {
        if (stage1Done) {
            System.out.println("Этап 1 уже завершён. Данные не изменены.");
            return;
        }
        try {
            int selectedVariant = StageAccess.initial(n);
            RepairQueue<RepairRequest> candidate = new RepairQueue<>(60, 2);
            List<RepairRequest> selected = allRequests().stream()
                    .filter(selectionFor(selectedVariant))
                    .sorted(Comparator.comparingInt(RepairRequest::getId))
                    .collect(Collectors.toList());
            for (RepairRequest request : selected) {
                if (!candidate.add(request)) {
                    throw new IllegalStateException("Не удалось добавить заявку " + request.getId());
                }
            }
            List<Integer> ids = candidate.getAnalyticsStream()
                    .map(RepairRequest::getId)
                    .sorted()
                    .collect(Collectors.toList());
            int totalMinutes = candidate.getAnalyticsStream()
                    .mapToInt(RepairRequest::getRequiredMinutes)
                    .sum();
            int totalParts = candidate.getAnalyticsStream()
                    .mapToInt(RepairRequest::getRequiredParts)
                    .sum();
            int nextVariant = StageAccess.second(n, ids, totalMinutes, totalParts);
            queue = candidate;
            variantA = selectedVariant;
            variantB = nextVariant;
            stage1Done = true;
            System.out.println("Этап 1 завершён. Вариант А" + variantA);
            System.out.println("ID добавленных заявок: " + ids);
            System.out.println("Суммарная потребность: " + totalMinutes + " мин., " + totalParts + " дет.");
            System.out.println("Назначен вариант Б" + variantB + ".");
            System.out.println("Начальные ресурсы: 60 мин., 2 дет.; на этапе 1 они не списываются.");
        } catch (RuntimeException exception) {
            System.out.println("Этап 1 не пройден: " + safeMessage(exception));
        }
    }

    private static void runStage2() {
        if (stage2Done) {
            System.out.println("Этап 2 уже завершён. Данные не изменены.");
            return;
        }
        if (!stage1Done) {
            System.out.println("Сначала успешно выполните этап 1.");
            return;
        }
        try {
            RepairQueue<RepairRequest> candidate = copyQueue(queue);
            List<RepairRequest> processed = processForVariant(variantB, candidate);
            List<Integer> ids = processed.stream()
                    .map(RepairRequest::getId)
                    .collect(Collectors.toList());
            int nextVariant = StageAccess.third(n, ids, candidate.getMinutes(), candidate.getParts());
            queue = candidate;
            variantV = nextVariant;
            stage2Done = true;
            System.out.println("Этап 2 завершён. Выполнен вариант Б" + variantB);
            System.out.println("ID выполненных заявок в порядке выполнения: " + ids);
            System.out.println("Остаток: " + queue.getMinutes() + " мин., " + queue.getParts() + " дет.");
            System.out.println("Назначен вариант В" + variantV + ".");
        } catch (RuntimeException exception) {
            System.out.println("Этап 2 не пройден: " + safeMessage(exception));
        }
    }

    private static void runStage3() {
        if (stage3Done) {
            System.out.println("Этап 3 уже завершён. Данные не изменены.");
            return;
        }
        if (!stage2Done) {
            System.out.println("Сначала успешно выполните этапы 1 и 2.");
            return;
        }
        try {
            RepairQueue<RepairRequest> candidate = copyQueue(queue);
            int addMinutes;
            int addParts;
            switch (variantV) {
                // В1
                case 1:
                    addMinutes = 15;
                    addParts = 1;
                    break;
                // В2
                case 2:
                    addMinutes = 20;
                    addParts = 0;
                    break;
                // В3
                case 3:
                    addMinutes = 0;
                    addParts = 2;
                    break;
                // В4
                case 4:
                    addMinutes = 10;
                    addParts = 1;
                    break;
                // В5
                case 5:
                    addMinutes = 25;
                    addParts = 1;
                    break;
                // В6
                case 6:
                    addMinutes = 15;
                    addParts = 2;
                    break;
                default:
                    throw new IllegalStateException("Неизвестный вариант этапа 3");
            }
            if (!candidate.replenish(addMinutes, addParts)) {
                System.out.println("Пополнение отклонено. Данные не изменены.");
                return;
            }
            List<RepairRequest> processed = processForVariant(variantB, candidate);
            Object report = buildReport(variantV, candidate);
            queue = candidate;
            stage3Done = true;
            System.out.println("Этап 3 завершён. Выполнен вариант В" + variantV);
            System.out.println("Добавлено ресурсов: " + addMinutes + " мин., " + addParts + " дет.");
            System.out.println("ID новых выполненных заявок в порядке выполнения: "
                    + processed.stream().map(RepairRequest::getId).collect(Collectors.toList()));
            System.out.println("Остаток: " + queue.getMinutes() + " мин., " + queue.getParts() + " дет.");
            if (report instanceof Optional<?>) {
                Optional<?> optional = (Optional<?>) report;
                if (optional.isPresent() && optional.get() instanceof RepairRequest) {
                    System.out.println("Отчёт В" + variantV + ": "
                            + ((RepairRequest) optional.get()).getDetails());
                } else {
                    System.out.println("Отчёт В" + variantV + ": " + optional.map(Object::toString).orElse("Нет подходящих данных"));
                }
            } else {
                System.out.println("Отчёт В" + variantV + ": " + report);
            }
        } catch (RuntimeException exception) {
            System.out.println("Этап 3 не выполнен: " + safeMessage(exception));
        }
    }

    private static Predicate<RepairRequest> selectionFor(int variant) {
        switch (variant) {
            // А1
            case 1:
                return request -> request.getPriority() >= 3 && request.getRequiredMinutes() <= 30;
            // А2
            case 2:
                return request -> request.getUnits() >= 2 && request.getRequiredMinutes() <= 30;
            // А3
            case 3:
                return request -> request.getRequiredParts() <= 1 && request.getPriority() >= 2;
            // А4
            case 4:
                return request -> (request instanceof ReplacementRequest
                        && ((ReplacementRequest) request).getTestMinutes() <= 5)
                        || (request instanceof MaintenanceRequest && request.getPriority() >= 4);
            // А5
            case 5:
                return request -> request.getRequiredMinutes() >= 15 && request.getPriority() >= 3;
            // А6
            case 6:
                return request -> (request instanceof MaintenanceRequest
                        && request.getUnits() <= 3 && request.getPriority() >= 2)
                        || (request instanceof ReplacementRequest
                        && request.getUnits() <= 2 && request.getPriority() >= 3);
            default:
                return request -> false;
        }
    }

    private static List<RepairRequest> processForVariant(int variant, RepairQueue<RepairRequest> target) {
        Map<Integer, Integer> customerCounts = new HashMap<>();
        Map<Integer, Long> previousDurations = target.getAnalyticsStream()
                .filter(RepairRequest::isCompleted)
                .collect(Collectors.groupingBy(RepairRequest::getCustomerId,
                        Collectors.summingLong(RepairRequest::getRequiredMinutes)));
        int partsAtStart = target.getParts();
        int reservedParts = (partsAtStart + 2) / 3;
        Comparator<RepairRequest> order;
        Predicate<RepairRequest> condition = request -> true;
        switch (variant) {
            // Б1
            case 1:
                order = Comparator.comparingInt((RepairRequest request) -> request.getPriority()).reversed()
                        .thenComparingInt(RepairRequest::getRequiredMinutes)
                        .thenComparingInt(RepairRequest::getId);
                break;
            // Б2
            case 2:
                order = Comparator.comparingInt((RepairRequest request) -> request.getPriority()).reversed()
                        .thenComparingInt(RepairRequest::getRequiredParts)
                        .thenComparingInt(RepairRequest::getRequiredMinutes)
                        .thenComparingInt(RepairRequest::getId);
                break;
            // Б3
            case 3:
                order = Comparator.comparingInt((RepairRequest request) -> request.getPriority()).reversed()
                        .thenComparingInt(RepairRequest::getId);
                condition = request -> customerCounts.getOrDefault(request.getCustomerId(), 0) < 1;
                break;
            // Б4
            case 4:
                order = Comparator.comparingInt((RepairRequest request) -> request.getPriority()).reversed()
                        .thenComparingInt(RepairRequest::getRequiredMinutes)
                        .thenComparingInt(RepairRequest::getId);
                condition = request -> target.getParts() - request.getRequiredParts() >= reservedParts;
                break;
            // Б5
            case 5:
                order = Comparator.comparingInt((RepairRequest request) -> request.getRequiredMinutes())
                        .thenComparingInt(RepairRequest::getRequiredParts)
                        .thenComparing(Comparator.comparingInt((RepairRequest request) -> request.getPriority()).reversed())
                        .thenComparingInt(RepairRequest::getId);
                condition = request -> customerCounts.getOrDefault(request.getCustomerId(), 0) < 2;
                break;
            // Б6
            case 6:
                order = Comparator.comparingLong((RepairRequest request) ->
                                previousDurations.getOrDefault(request.getCustomerId(), 0L))
                        .thenComparing(Comparator.comparingInt((RepairRequest request) -> request.getPriority()).reversed())
                        .thenComparingInt(RepairRequest::getRequiredMinutes)
                        .thenComparingInt(RepairRequest::getId);
                break;
            default:
                throw new IllegalArgumentException("Номер варианта Б должен быть от 1 до 6");
        }
        final int selectedVariant = variant;
        Predicate<RepairRequest> selectedCondition = condition;
        Consumer<RepairRequest> action = request -> {
            request.markCompleted();
            if (selectedVariant == 3 || selectedVariant == 5) {
                customerCounts.merge(request.getCustomerId(), 1, Integer::sum);
            }
        };
        return target.process(selectedCondition, order, action);
    }

    private static Object buildReport(int variant, RepairQueue<RepairRequest> target) {
        switch (variant) {
            // В1
            case 1:
                return target.getAnalyticsStream()
                        .filter(RepairRequest::isCompleted)
                        .collect(Collectors.groupingBy(RepairRequest::getCustomerId,
                                Collectors.summingLong(RepairRequest::getRequiredMinutes)));
            // В2
            case 2:
                Map<Integer, List<RepairRequest>> byCustomer = target.getAnalyticsStream()
                        .collect(Collectors.groupingBy(RepairRequest::getCustomerId));
                return byCustomer.entrySet().stream()
                        .filter(entry -> entry.getValue().stream().anyMatch(RepairRequest::isCompleted)
                                && entry.getValue().stream().anyMatch(request -> !request.isCompleted()))
                        .map(Map.Entry::getKey)
                        .sorted()
                        .collect(Collectors.toList());
            // В3
            case 3:
                return target.getAnalyticsStream()
                        .filter(request -> !request.isCompleted())
                        .filter(request -> request.getRequiredParts() <= target.getParts())
                        .min(Comparator.comparingInt((RepairRequest request) ->
                                        Math.max(0, request.getRequiredMinutes() - target.getMinutes()))
                                .thenComparing(Comparator.comparingInt((RepairRequest request) -> request.getPriority()).reversed())
                                .thenComparingInt(RepairRequest::getId));
            // В4
            case 4:
                Map<String, Long> completedCustomers = target.getAnalyticsStream()
                        .filter(RepairRequest::isCompleted)
                        .collect(Collectors.groupingBy(request -> request.getClass().getSimpleName(),
                                Collectors.mapping(RepairRequest::getCustomerId,
                                        Collectors.collectingAndThen(Collectors.toSet(), set -> (long) set.size()))));
                return Stream.of(MaintenanceRequest.class.getSimpleName(), ReplacementRequest.class.getSimpleName())
                        .collect(Collectors.toMap(name -> name, name -> completedCustomers.getOrDefault(name, 0L)));
            // В5
            case 5:
                return target.getAnalyticsStream()
                        .filter(request -> !request.isCompleted())
                        .collect(Collectors.groupingBy(RepairRequest::getCustomerId,
                                Collectors.summingLong(RepairRequest::getRequiredParts)))
                        .entrySet().stream()
                        .filter(entry -> entry.getValue() > 0)
                        .sorted(Comparator.<Map.Entry<Integer, Long>>comparingLong(Map.Entry::getValue).reversed()
                                .thenComparingInt(Map.Entry::getKey))
                        .map(Map.Entry::getKey)
                        .findFirst();
            // В6
            case 6:
                return target.getAnalyticsStream()
                        .collect(Collectors.groupingBy(RepairRequest::getCustomerId,
                                Collectors.collectingAndThen(Collectors.toList(), requests ->
                                        requests.stream().filter(RepairRequest::isCompleted).count()
                                                / (double) requests.size())));
            default:
                throw new IllegalArgumentException("Номер варианта В должен быть от 1 до 6");
        }
    }

    private static RepairQueue<RepairRequest> copyQueue(RepairQueue<RepairRequest> source) {
        RepairQueue<RepairRequest> copy = new RepairQueue<>(source.getMinutes(), source.getParts());
        source.getAnalyticsStream().forEach(request -> {
            RepairRequest cloned;
            if (request instanceof MaintenanceRequest) {
                cloned = new MaintenanceRequest(request.getId(), request.getCustomerId(),
                        request.getUnits(), request.getPriority());
            } else {
                ReplacementRequest replacement = (ReplacementRequest) request;
                cloned = new ReplacementRequest(request.getId(), request.getCustomerId(),
                        request.getUnits(), request.getPriority(), replacement.getTestMinutes());
            }
            if (copy.add(cloned) && request.isCompleted()) {
                cloned.markCompleted();
            }
        });
        return copy;
    }

    private static List<RepairRequest> allRequests() {
        return new ArrayList<>(Arrays.asList(
                new MaintenanceRequest(201, 21, 3, 5),
                new MaintenanceRequest(203, 22, 4, 5),
                new MaintenanceRequest(205, 23, 2, 2),
                new MaintenanceRequest(208, 24, 5, 4),
                new MaintenanceRequest(210, 25, 4, 2),
                new ReplacementRequest(202, 21, 2, 4, 5),
                new ReplacementRequest(204, 22, 1, 3, 5),
                new ReplacementRequest(206, 23, 3, 4, 0),
                new ReplacementRequest(207, 24, 1, 1, 0),
                new ReplacementRequest(209, 22, 1, 4, 0)
        ));
    }

    private static void showQueue() {
        if (queue == null) {
            System.out.println("Очередь ещё не сформирована.");
            return;
        }
        queue.getAnalyticsStream()
                .sorted(Comparator.comparingInt(RepairRequest::getId))
                .map(RepairRequest::getDetails)
                .forEach(System.out::println);
        System.out.println("Остаток ресурсов: " + queue.getMinutes() + " мин., " + queue.getParts() + " дет.");
        System.out.println("Завершённые этапы: " + (stage1Done ? "1 " : "")
                + (stage2Done ? "2 " : "") + (stage3Done ? "3" : ""));
    }

    private static String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.trim().isEmpty() ? "операция отклонена" : message;
    }
}


final class StageAccess {
    private static final int[] S = {0, 3, 3, 2, 0, 1};
    private static final int[] A = {
        0x2e02a10d, 0x658e283b, 0x4bdc593a, 0xc22e0b9e, 0xbf0fca1e,
        0xa0951f33
    };
    private static final int[] B = {
        0x29657c36, 0x96d25ad4, 0xa1c42961, 0xa1c42961, 0x49f39b4a,
        0x49f39b4a, 0x49f39b4a, 0x1f4445f7, 0x3fb6878e, 0x3fb6878e,
        0x49f39b4a, 0x49f39b4a, 0x574556c0, 0xa1c42961, 0xa1c42961,
        0x49f39b4a, 0x49f39b4a, 0xa37914c6, 0xa1c42961, 0x49f39b4a,
        0x49f39b4a, 0x29657c36, 0xe3f8f103, 0xa1c42961, 0x49f39b4a,
        0x49f39b4a, 0xf3b1ab4a, 0x3fb6878e, 0x3fb6878e, 0x49f39b4a,
        0x83f6e3cb, 0x574556c0, 0x892997a2, 0x892997a2, 0x892997a2,
        0x83f6e3cb
    };

    static int initial(int n) {
        if (n < 1 || n > 36) {
            throw new IllegalArgumentException("N: 1..36");
        }
        return (n - 1) / 6 + 1;
    }

    static int second(int n, List<Integer> ids, int minutes, int parts) {
        return check(n, ids, minutes, parts, A[initial(n) - 1], 0);
    }

    static int third(int n, List<Integer> ids, int minutes, int parts) {
        int a = initial(n);
        int b = 1 + Math.floorMod((long) A[a - 1] + (n - 1) % 6, 6);
        return check(n, ids, minutes, parts, B[n - 1], b);
    }

    private static int check(int n, List<Integer> ids, int minutes, int parts, int expected, int previous) {
        if (ids == null) {
            throw new IllegalArgumentException("ids: null");
        }
        int actual = (ids + "|" + minutes + "|" + parts).hashCode();
        if (actual != expected) {
            throw new IllegalStateException("Check stage result");
        }
        int p = (n - 1) % 6;
        long key = previous == 0 ? (long) actual + p
                : (actual >>> 5) + 1L * p + 0L * previous + S[initial(n) - 1];
        return 1 + Math.floorMod(key, 6);
    }
}
