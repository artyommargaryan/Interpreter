import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.Scanner;
import java.io.File;
import java.io.FileNotFoundException;
import java.lang.NumberFormatException;

public class Interp3 {
    Map<String, Integer> myVars = new Hashtable<>();
    static final String PRINT = "print";
    static final Set<String> compOp = setOf("<", "==", ">");
    boolean ifStarted = false;
    boolean ifCond;
    boolean ifWhere;

    List<String> loop = new ArrayList<>();
    boolean loopStarted = false;
    boolean loopCollected = false;
    String[] strLoopCond;

    public static void main(String[] args) throws FileNotFoundException {
        Scanner scanner = buildScanner(args);
        new Interp3().run(scanner);
    }

    private static Scanner buildScanner(String[] args) throws FileNotFoundException {
        if (args.length > 0) {
            File file = new File(args[0]);
            System.out.println(file);
            return new Scanner(file);
        } else {
            System.out.println("System.in");
            return new Scanner(System.in);
        }
    }

    private static Set<String> setOf(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    private void run(Scanner scanner) {
        while (true) {
            var line = scanner.nextLine();
            if (!line.isEmpty()) {
                if ("end".equals(line)) {
                    break;
                }
                process(line);
            }
        }
    }

    private boolean execCurrentLine() {
        if (!ifStarted) {
            return true;
        }
        return ifCond == ifWhere;
    }

    private void process(String line) {
        var lineElem = line.split(" ");
        if (isIf(lineElem[0])) {
            ifStarted = true;
            ifWhere = true;
            String[] newLineElem = Arrays.copyOfRange(lineElem, 1, lineElem.length);
            ifCond = condition(newLineElem);
            return;
        }
        if (isElse(lineElem[0])) {
            ifWhere = false;
            return;
        }
        if (isEndIf(lineElem[0])) {
            ifStarted = false;
            return;
        }

        if (!execCurrentLine()) {
            return;
        }

        if (isLoop(lineElem[0])) {
            strLoopCond = Arrays.copyOfRange(lineElem, 1, lineElem.length);
            loopStarted = true;
            return;
        }

        if (isLoopEnd(lineElem[0])) {
            loopCollected = true;
        }

        if (loopStarted && !loopCollected) {
            loop.add(line);
            return;
        }

        if (loopCollected) {
            loopCollected = false;
            loopStarted = false;
            boolean loopCond = condition(strLoopCond);
            while (loopCond) {
                for (int i = 0; i < loop.size(); ++i) {
                    process(loop.get(i));
                }
                loopCond = condition(strLoopCond);
            }
            loop.clear();
            return;
        }

        if (isPrint(lineElem[0])) {
            if (lineElem.length > 3) {
                String[] newLineElem = Arrays.copyOfRange(lineElem, 1, lineElem.length);
                print(calculate(newLineElem));
            } else {
                if (isInteger(lineElem[1])) {
                    print(Integer.parseInt(lineElem[1]));
                } else if (varExist(lineElem[1])) {
                    print(myVars.get(lineElem[1]));
                } else {
                    throw new RuntimeException("Syntax Error!!!\nUnknown Variable: " + lineElem[1]);
                }
            }
        } else if (isAssignment(lineElem[1])) {
            int result;
            if (lineElem.length > 3) {
                String[] newLineElem = Arrays.copyOfRange(lineElem, 2, lineElem.length);
                result = calculate(newLineElem);
            } else {
                if (isInteger(lineElem[2])) {
                    result = Integer.parseInt(lineElem[2]);
                } else {
                    if (varExist(lineElem[2])) {
                        result = myVars.get(lineElem[2]);
                    } else {
                        throw new RuntimeException("Syntax Error!!!\nUnknown Variable: " + lineElem[2]);
                    }
                }
            }
            myVars.put(lineElem[0], result);
        }
    }

    private boolean condition(String[] lineElem) {
        int index = 0;
        for (int i = 0; i < lineElem.length; ++i) {
            if (compOp.contains(lineElem[i])) {
                index = i;
                break;
            }
        }

        String[] part1 = Arrays.copyOfRange(lineElem, 0, index);
        String[] part2 = Arrays.copyOfRange(lineElem, index + 1, lineElem.length);

        int exp1;
        int exp2;
        if (part1.length >= 3) {
            exp1 = calculate(part1);
        } else if (part1.length == 1) {
            if (isInteger(part1[0])) {
                exp1 = Integer.parseInt(part1[0]);
            } else if (varExist(part1[0])) {
                exp1 = myVars.get(part1[0]);
            } else {
                throw new RuntimeException("Syntax Error!!!\nUnknown Expression: " + Arrays.toString(part1));
            }

        } else {
            throw new RuntimeException("Syntax Error!!!\nUnknown Expression: " + Arrays.toString(part1));

        }

        if (part2.length >= 3) {
            exp2 = calculate(part2);
        } else if (part2.length == 1) {
            if (isInteger(part2[0])) {
                exp2 = Integer.parseInt(part2[0]);
            } else if (varExist(part2[0])) {
                exp2 = myVars.get(part2[0]);
            } else {
                throw new RuntimeException("Syntax Error!!!\nUnknown Expression: " + Arrays.toString(part2));
            }
        } else {
            throw new RuntimeException("Syntax Error!!!\nUnknown Expression: " + Arrays.toString(part2));
        }

        switch (lineElem[index]) {
            case "<":
                return exp1 < exp2;
            case "==":
                return exp1 == exp2;
            case ">":
                return exp1 > exp2;
            default:
                throw new RuntimeException("Syntax Error!!!\nUnknown Operator: " + lineElem[index]);
        }

    }

    private boolean isLoop(String str) {
        return "loop".equals(str);
    }

    private boolean isLoopEnd(String str) {
        return "endLoop".equals(str);
    }

    private boolean isIf(String str) {
        return "if".equals(str);
    }

    private boolean isElse(String str) {
        return "else".equals(str);
    }

    private boolean isEndIf(String str) {
        return "endIf".equals(str);
    }

    private boolean isAssignment(String str) {
        return "=".equals(str);
    }

    private boolean isPrint(String str) {
        return PRINT.equals(str);
    }

    private boolean isInteger(String num) {
        try {
            Integer.parseInt(num);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private boolean varExist(String str) {
        return myVars.containsKey(str);
    }

    private boolean hasHighPriority(String operation) {
        return "*".equals(operation) || "/".equals(operation);
    }

    private int calculate(String[] lineElem) {
        int num1;
        int num2;

        if (lineElem.length > 3) {
            if (hasHighPriority(lineElem[1])) {
                String[] newLineElem = Arrays.copyOfRange(lineElem, 0, 3);
                int num3 = calculate(newLineElem);
                newLineElem = Arrays.copyOfRange(lineElem, 2, lineElem.length);
                newLineElem[0] = Integer.toString(num3);
                num2 = calculate(newLineElem);
                return num2;
            }
            String[] newLineElem = Arrays.copyOfRange(lineElem, 2, lineElem.length);
            num2 = calculate(newLineElem);
        } else {
            if (isInteger(lineElem[2])) {
                num2 = Integer.parseInt(lineElem[2]);
            } else if (varExist(lineElem[2])) {
                num2 = myVars.get(lineElem[2]);
            } else {
                throw new RuntimeException("Syntax Error!!!\nUnknown Variable: " + lineElem[2]);
            }
        }

        if (isInteger(lineElem[0])) {
            num1 = Integer.parseInt(lineElem[0]);
        } else if (varExist(lineElem[0])) {
            num1 = myVars.get(lineElem[0]);
        } else {
            throw new RuntimeException("Syntax Error!!!\nUnknown Variable: " + lineElem[0]);
        }

        switch (lineElem[1]) {
            case "+":
                return num1 + num2;
            case "-":
                return num1 - num2;
            case "*":
                return num1 * num2;
            case "/":
                return num1 / num2;
            default:
                throw new RuntimeException("Syntax Error!!!\nUnknown Operator: " + lineElem[1]);
        }
    }

    private void print(int result) {
        System.out.println(result);
    }

    private void ln(String str) {
        System.out.println(str);
    }

}
