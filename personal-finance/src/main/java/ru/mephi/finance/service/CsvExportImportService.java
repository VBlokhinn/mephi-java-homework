package ru.mephi.finance.service;

import ru.mephi.finance.model.Operation;
import ru.mephi.finance.model.OperationType;
import ru.mephi.finance.model.Wallet;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CsvExportImportService {
    private static final String HEADER = "id,type,amount,category,date,description";

    public void exportOperations(Wallet wallet, Path destinationPath) throws IOException {
        if (destinationPath.getParent() != null && !Files.exists(destinationPath.getParent())) {
            Files.createDirectories(destinationPath.getParent());
        }

        try (BufferedWriter writer = Files.newBufferedWriter(destinationPath, StandardCharsets.UTF_8)) {
            writer.write(HEADER);
            writer.newLine();

            for (Operation op : wallet.getOperations()) {
                String desc = op.getDescription().replace("\"", "\"\"");
                String line = String.format(Locale.US, "%s,%s,%.2f,\"%s\",%s,\"%s\"",
                        op.getId(),
                        op.getType().name(),
                        op.getAmount(),
                        op.getCategory().replace("\"", "\"\""),
                        op.getDate(),
                        desc);
                writer.write(line);
                writer.newLine();
            }
        }
    }

    public List<Operation> importOperations(Path sourcePath) throws IOException {
        if (!Files.exists(sourcePath)) {
            throw new IllegalArgumentException("Файл для импорта не найден: " + sourcePath);
        }

        List<Operation> operations = new ArrayList<>();
        try (BufferedReader reader = Files.newBufferedReader(sourcePath, StandardCharsets.UTF_8)) {
            String line = reader.readLine();
            if (line == null) {
                return operations;
            }

            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                Operation op = parseCsvLine(line, lineNum);
                if (op != null) {
                    operations.add(op);
                }
            }
        }
        return operations;
    }

    private Operation parseCsvLine(String line, int lineNum) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim());

        if (tokens.size() < 5) {
            System.err.printf("Пропущена некорректная строка %d: %s%n", lineNum, line);
            return null;
        }

        try {
            String id = unquote(tokens.get(0));
            OperationType type = OperationType.valueOf(unquote(tokens.get(1)).toUpperCase());
            double amount = Double.parseDouble(unquote(tokens.get(2)).replace(',', '.'));
            String category = unquote(tokens.get(3));
            LocalDate date = LocalDate.parse(unquote(tokens.get(4)));
            String description = tokens.size() > 5 ? unquote(tokens.get(5)) : "";

            return new Operation(id, type, amount, category, date, description);
        } catch (Exception e) {
            System.err.printf("Ошибка разбора строки %d (%s): %s%n", lineNum, line, e.getMessage());
            return null;
        }
    }

    private String unquote(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            s = s.substring(1, s.length() - 1);
        }
        return s.replace("\"\"", "\"");
    }
}
