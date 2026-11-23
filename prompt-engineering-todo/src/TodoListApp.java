import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TodoListApp {

    private static class Task {
        final String text;
        boolean done;

        Task(String text) {
            this.text = text;
        }
    }

    public static void main(String[] args) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
        List<Task> tasks = new ArrayList<>();
        String line;

        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty()) {
                continue;
            }

            if (line.equals("exit")) {
                System.out.println("BYE");
                break;
            }

            if (line.equals("list")) {
                printTasks(tasks);
                continue;
            }

            if (line.equals("add") || line.startsWith("add ")) {
                handleAdd(line, tasks);
                continue;
            }

            if (line.equals("done") || line.startsWith("done ")) {
                handleDone(line, tasks);
                continue;
            }

            if (line.equals("remove") || line.startsWith("remove ")) {
                handleRemove(line, tasks);
                continue;
            }

            System.out.println("ERROR: UNKNOWN_COMMAND");
        }
    }

    private static void printTasks(List<Task> tasks) {
        if (tasks.isEmpty()) {
            System.out.println("EMPTY");
            return;
        }
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            String mark = task.done ? "[X]" : "[ ]";
            System.out.println((i + 1) + ". " + mark + " " + task.text);
        }
    }

    private static void handleAdd(String line, List<Task> tasks) {
        String text = line.length() > 4 ? line.substring(4).trim() : "";
        if (text.isEmpty()) {
            System.out.println("ERROR: EMPTY_TASK");
            return;
        }
        tasks.add(new Task(text));
        System.out.println("ADDED " + tasks.size());
    }

    private static void handleDone(String line, List<Task> tasks) {
        Integer index = parseIndex(line, 4, tasks.size());
        if (index == null) {
            return;
        }
        Task task = tasks.get(index);
        if (task.done) {
            System.out.println("ALREADY_DONE " + (index + 1));
        } else {
            task.done = true;
            System.out.println("DONE " + (index + 1));
        }
    }

    private static void handleRemove(String line, List<Task> tasks) {
        Integer index = parseIndex(line, 6, tasks.size());
        if (index == null) {
            return;
        }
        tasks.remove(index.intValue());
        System.out.println("REMOVED " + (index + 1));
    }

    private static Integer parseIndex(String line, int cmdLength, int size) {
        String arg = line.length() > cmdLength ? line.substring(cmdLength).trim() : "";
        if (arg.isEmpty()) {
            System.out.println("ERROR: INVALID_ID");
            return null;
        }
        int id;
        try {
            id = Integer.parseInt(arg);
        } catch (NumberFormatException e) {
            System.out.println("ERROR: INVALID_ID");
            return null;
        }
        if (id < 1 || id > size) {
            System.out.println("ERROR: NOT_FOUND");
            return null;
        }
        return id - 1;
    }
}
