package sorting;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

class InnerException extends RuntimeException {
    public InnerException(String message) {
        super(message);
    }
}

public class Main {
    public static void main(String[] args) {
        try {
            String inputFile = null;
            String outputFile = null;
            DataType dataType = DataType.LONG; // Default to LONG if no data type is provided
            SortingType sortingType = SortingType.NATURAL; // Default to NATURAL if no sorting type is provided

            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    String token = args[i];
                    try {
                        if (token.startsWith("-")) {
                            switch (token) {
                                case "-dataType":
                                    if (i + 1 < args.length) {
                                        dataType = DataType.valueOf(args[++i].toUpperCase());
                                    } else {
                                        throw new RuntimeException("No data type defined!");
                                    }
                                    break;
                                case "-sortingType":
                                    if (i + 1 < args.length) {
                                        sortingType = SortingType.fromString(args[++i]);
                                        if (sortingType == null) {
                                            throw new RuntimeException("Invalid sorting type provided: " + args[i]);
                                        }
                                    } else {
                                        throw new RuntimeException("No sorting type defined!");
                                    }
                                    break;
                                case "-inputFile":
                                    if (i + 1 < args.length) {
                                        inputFile = args[++i];
                                    } else {
                                        throw new RuntimeException("No input file defined!");
                                    }
                                    break;
                                case "-outputFile":
                                    if (i + 1 < args.length) {
                                        outputFile = args[++i];
                                    } else {
                                        throw new RuntimeException("No output file defined!");
                                    }
                                    break;
                                default:
                                    throw new InnerException(String.format("\"%s\" isn't a valid parameter. It's skipped.", token));
                            }
                        } else {
                            throw new RuntimeException("Unexpected token as parameter: " + token);
                        }
                    } catch (InnerException e) {
                        System.out.println(e.getMessage());
                    }
                }
            }

            Context context = new Context(dataType, sortingType, inputFile, outputFile);
            context.execute();
        } catch (RuntimeException e) {
            System.out.println(e.getMessage());
        }
    }
}

enum DataType {
    LINE,
    WORD,
    LONG
}

enum SortingType {
    NATURAL, BY_COUNT;  // Using an underscore to match "BY_COUNT"

    // Static method to get enum from string
    public static SortingType fromString(String str) {
        String normalized = str.replace("byCount", "BY_COUNT").toUpperCase();
        try {
            return SortingType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            return null;  // Return null or throw a custom exception if the value is not found
        }
    }
}

class Context {
    private DataStatMethod method;
    private String inputFile;
    private String outputFile;

    public Context(DataType dataType, SortingType sortingType, String inputFile, String outputFile) {
        this.inputFile = inputFile;
        this.outputFile = outputFile;
        switch (dataType) {
            case LONG:
                this.method = new LongStat(sortingType, inputFile, outputFile);
                break;
            case WORD:
                this.method = new WordStat(sortingType, inputFile, outputFile);
                break;
            case LINE:
                this.method = new LineStat(sortingType, inputFile, outputFile);
                break;
            default:
                throw new IllegalArgumentException("Unsupported data type: " + dataType);
        }
    }

    public void execute() {
        method.invokeMethod();
    }
}

interface DataStatMethod {
    void invokeMethod();
}

abstract class DataStat implements DataStatMethod {
    protected SortingType sortingType;

    public DataStat() {
        this.sortingType = SortingType.NATURAL;  // Default sorting type
    }

    public DataStat(SortingType sortingType) {
        this.sortingType = sortingType;
    }
}

class LongStat extends DataStat {
    private String inputFile;
    private String outputFile;

    public LongStat(SortingType sortingType, String inputFile, String outputFile) {
        super(sortingType);
        this.inputFile = inputFile;
        this.outputFile = outputFile;
    }

    @Override
    public void invokeMethod() {
        List<Long> longs = new ArrayList<>();
        Scanner scanner = null;

        try {
            scanner = (inputFile != null) ? new Scanner(new FileInputStream(inputFile)) : new Scanner(System.in);
        } catch (FileNotFoundException e) {
            System.out.println("Input file not found: " + inputFile);
            return; // Exit if the input file cannot be found
        }

        // Read all numbers from input, including those separated by whitespace
        while (scanner.hasNext()) {
            if (scanner.hasNextLong()) {
                longs.add(scanner.nextLong());
            } else {
                scanner.next(); // Skip non-long tokens
            }
        }
        scanner.close();

        PrintStream output;
        try {
            output = (outputFile != null) ? new PrintStream(new FileOutputStream(outputFile)) : System.out;
        } catch (FileNotFoundException e) {
            System.out.println("Output file not found: " + outputFile);
            return; // Exit if the output file cannot be opened
        }

        // Execute sorting based on the specified type
        if (sortingType == SortingType.BY_COUNT) {
            // Group numbers by frequency
            Map<Long, Long> frequencyMap = longs.stream()
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

            List<Map.Entry<Long, Long>> entries = new ArrayList<>(frequencyMap.entrySet());
            entries.sort(Map.Entry.<Long, Long>comparingByValue()
                    .thenComparing(Map.Entry.comparingByKey()));

            output.printf("Total numbers: %d.\n", longs.size());
//            output.printf("Total numbers: %d.\n", frequencyMap.size());
            for (Map.Entry<Long, Long> entry : entries) {
                double percentage = (entry.getValue() * 100.0) / longs.size();
                output.printf("%d: %d time(s), %.0f%%\n", entry.getKey(), entry.getValue(), percentage);
            }

            // Print sorted data based on unique values
//            output.print("Sorted data: ");
            List<Long> sortedLongs = new ArrayList<>();
            entries.forEach(entry -> {
                for (long i = 0; i < entry.getValue(); i++) {
                    sortedLongs.add(entry.getKey());
                }
            });
//            sortedLongs.forEach(n -> output.print(n + " "));
            output.println();

        } else {
            // Natural sorting including duplicates
            longs.sort(Comparator.naturalOrder());
            output.printf("Total numbers: %d.\n", longs.size());
            output.print("Sorted data: ");
            longs.forEach(n -> output.print(n + " "));
            output.println();
        }

        if (outputFile != null) {
            output.close();
        }
    }
}


class LineStat extends DataStat {
    private String inputFile;
    private String outputFile;

    public LineStat(SortingType sortingType, String inputFile, String outputFile) {
        super(sortingType);
        this.inputFile = inputFile;
        this.outputFile = outputFile;
    }

    @Override
    public void invokeMethod() {
        List<String> lines = new ArrayList<>();
        Scanner scanner = null;

        try {
            scanner = (inputFile != null) ? new Scanner(new FileInputStream(inputFile)) : new Scanner(System.in);
        } catch (FileNotFoundException e) {
            System.out.println("Input file not found: " + inputFile);
            return; // Exit if the input file cannot be found
        }

        // Read all lines from input
        while (scanner.hasNextLine()) {
            lines.add(scanner.nextLine());
        }
        scanner.close();

        PrintStream output;
        try {
            output = (outputFile != null) ? new PrintStream(new FileOutputStream(outputFile)) : System.out;
        } catch (FileNotFoundException e) {
            System.out.println("Output file not found: " + outputFile);
            return; // Exit if the output file cannot be opened
        }

        // Execute sorting based on the specified type
        if (sortingType == SortingType.BY_COUNT) {
            // Group lines by frequency
            Map<String, Long> frequencyMap = lines.stream()
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

            List<Map.Entry<String, Long>> entries = new ArrayList<>(frequencyMap.entrySet());
            entries.sort(Map.Entry.<String, Long>comparingByValue()
                    .thenComparing(Map.Entry.comparingByKey()));

            output.printf("Total lines: %d.\n", lines.size());
            for (Map.Entry<String, Long> entry : entries) {
                double percentage = (entry.getValue() * 100.0) / lines.size();
                output.printf("%s: %d time(s), %.0f%%\n", entry.getKey(), entry.getValue(), percentage);
            }

            // Print sorted data based on unique values
//            output.print("Sorted data: ");
//            entries.forEach(entry -> output.print(entry.getKey() + " "));
            output.println();

        } else {
            // Natural sorting including duplicates
            lines.sort(Comparator.naturalOrder());
            output.printf("Total lines: %d.\n", lines.size());
            output.print("Sorted data: ");
            lines.forEach(line -> output.print(line + " "));
            output.println();
        }

        if (outputFile != null) {
            output.close();
        }
    }
}

class WordStat extends DataStat {
    private String inputFile;
    private String outputFile;

    public WordStat(SortingType sortingType, String inputFile, String outputFile) {
        super(sortingType);
        this.inputFile = inputFile;
        this.outputFile = outputFile;
    }

    @Override
    public void invokeMethod() {
        List<String> words = new ArrayList<>();
        Scanner scanner = null;

        try {
            scanner = (inputFile != null) ? new Scanner(new FileInputStream(inputFile)) : new Scanner(System.in);
        } catch (FileNotFoundException e) {
            System.out.println("Input file not found: " + inputFile);
            return; // Exit if the input file cannot be found
        }

        // Read all words from input, including those separated by whitespace
        while (scanner.hasNext()) {
            words.add(scanner.next());
        }
        scanner.close();

        PrintStream output;
        try {
            output = (outputFile != null) ? new PrintStream(new FileOutputStream(outputFile)) : System.out;
        } catch (FileNotFoundException e) {
            System.out.println("Output file not found: " + outputFile);
            return; // Exit if the output file cannot be opened
        }

        // Execute sorting based on the specified type
        if (sortingType == SortingType.BY_COUNT) {
            // Group words by frequency
            Map<String, Long> frequencyMap = words.stream()
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

            List<Map.Entry<String, Long>> entries = new ArrayList<>(frequencyMap.entrySet());
            entries.sort(Map.Entry.<String, Long>comparingByValue()
                    .thenComparing(Map.Entry.comparingByKey()));

            output.printf("Total words: %d.\n", words.size());
//            output.printf("Total unique words: %d.\n", frequencyMap.size());
            for (Map.Entry<String, Long> entry : entries) {
                double percentage = (entry.getValue() * 100.0) / words.size();
                output.printf("%s: %d time(s), %.0f%%\n", entry.getKey(), entry.getValue(), percentage);
            }

            // Print sorted data based on unique values
//            output.print("Sorted data: ");
            List<String> sortedWords = new ArrayList<>();
            entries.forEach(entry -> {
                for (long i = 0; i < entry.getValue(); i++) {
                    sortedWords.add(entry.getKey());
                }
            });
//            sortedWords.forEach(word -> output.print(word + " "));
            output.println();

        } else {
            // Natural sorting including duplicates
            words.sort(Comparator.naturalOrder());
            output.printf("Total words: %d.\n", words.size());
            output.print("Sorted data: ");
            words.forEach(word -> output.print(word + " "));
            output.println();
        }

        if (outputFile != null) {
            output.close();
        }
    }
}


