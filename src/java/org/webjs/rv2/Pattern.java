package org.webjs.rv2;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Шаблоны позиций
 *
 * @author rmr
 */
public class Pattern {

    // Тип фигуры: 
    //      0 - пятерка, 
    //      1 - открытая четверка, 
    //      2 - четверка,
    //      3 - открытая тройка, 
    //      4 - тройка, 
    //      5 - открыая двойка
    //      6 - открыая единица
    public static final byte FIVE = 0;
    public static final byte OPEN_FOUR = 3;
    public static final byte FOUR = 4;
    public static final byte OPEN_THREE = 7;
    public static final byte THREE = 10;
    public static final byte OPEN_TWO = 11;
    public static final byte TWO = 14;
    public static final byte ONE = 15;

    // Последовательность сканирования шаблонов
    // Шаблон содержит следующие символы:
    //      x заполненное поле
    //      + поле возможной атака и закрывающее атаку оппонента
    //      - поле возможной атаки и не закрыващее атаку оппонента
    //      ! поле безусловно закрывающее атаку оппонента
    //      . поле отступа, влияет на развитие атаки
    public static Pattern[][] SOLVER_PATTERNS = new Pattern[][]{{
        // Пятерка: XXXXX*
        new Pattern(FIVE, "xxxxx", 0)
    }, {
        // Открытая четверка: *XXXX*
        new Pattern(OPEN_FOUR, "+xxxx+", 0),
        // Четверки: XXXX*, XXX*X, XX*XX
        new Pattern(FOUR, "xxxx+", 5),
        new Pattern(FOUR, "+xxxx", 5),
        new Pattern(FOUR, "xxx+x", 5),
        new Pattern(FOUR, "x+xxx", 5),
        new Pattern(FOUR, "xx+xx", 5)
    }, {
        // Открытые тройки: **XXX**, *XXX**, *X*XX*
        new Pattern(OPEN_THREE, ".+xxx+.", 4), // 4 points
        new Pattern(OPEN_THREE, "!xxx+!", 3), // 3 points 
        new Pattern(OPEN_THREE, "!+xxx!", 3), // 3 points 
        new Pattern(OPEN_THREE, "!xx+x!", 2), // 2 points
        new Pattern(OPEN_THREE, "!x+xx!", 2), // 2 points
        // Тройки: XXX**, X*XX*, XX*X*, XX**X, X**XX
        new Pattern(THREE, "xxx++", 1),
        new Pattern(THREE, "++xxx", 1),
        new Pattern(THREE, "+xxx+", 1),
        new Pattern(THREE, "x+xx+", 1),
        new Pattern(THREE, "+xx+x", 1),
        new Pattern(THREE, "xx+x+", 1),
        new Pattern(THREE, "+x+xx", 1),
        new Pattern(THREE, "xx++x", 1),
        new Pattern(THREE, "x++xx", 1),
        new Pattern(THREE, "x+x+x", 1)
    }, {
        // Открытые двойки: ***XX***, **XX***, *XX***, **XX**, *X*X**, *X**X*
        new Pattern(OPEN_TWO, ".-+xx+-.", 2), // 2 points   .-x--.. .x-... 
        new Pattern(OPEN_TWO, ".+xx+-.", 2), // 2 points
        new Pattern(OPEN_TWO, ".-+xx+.", 2), // 2 points
        new Pattern(OPEN_TWO, ".+xx+.", 2), // 2 points
        new Pattern(OPEN_TWO, "!xx+-.", 2), // 2 points
        new Pattern(OPEN_TWO, ".-+xx!", 2), // 2 points
        new Pattern(OPEN_TWO, ".+x+x+.", 1), // 1 points
        new Pattern(OPEN_TWO, "!x+x+.", 1),
        new Pattern(OPEN_TWO, ".+x+x!", 1),
        new Pattern(OPEN_TWO, "!x++x!", 1),
        // Двойки: XX***, *XX**, X*X**, *X*X*, X**X*, X***X
        new Pattern(TWO, "xx+--", 0),
        new Pattern(TWO, "--+xx", 0),
        new Pattern(TWO, "+xx+-", 0),
        new Pattern(TWO, "-+xx+", 0),
        new Pattern(TWO, "x+x+-", 0),
        new Pattern(TWO, "-+x+x", 0),
        new Pattern(TWO, "+x+x+", 0),
        new Pattern(TWO, "x++x+", 0),
        new Pattern(TWO, "+x++x", 0),
        new Pattern(TWO, "x+++x", 0)
    }, {
        // Перспективные единицы: ****X****, ***X****, ***X***, **X****, **X***, *X****
        new Pattern(ONE, "..--x--..", 0),
        new Pattern(ONE, "..-x--..", 0),
        new Pattern(ONE, "..--x-..", 0),
        new Pattern(ONE, "..-x-..", 0),
        new Pattern(ONE, ".-x--..", 0),
        new Pattern(ONE, ".--x-..", 0),
        new Pattern(ONE, ".-x-..", 0),
        new Pattern(ONE, "..-x-.", 0),
        new Pattern(ONE, ".x-...", 0),
        new Pattern(ONE, "...-x.", 0)
    }};

    // Тип фигуры: 
    //      0 - пятерка, 
    //      1 - открытая четверка, 
    //      2 - четверка,
    //      3 - открытая тройка, 
    //      4 - тройка, 
    //      5 - открыая двойка
    public final int type;

    // Количество очков фигуры
    public final int rating;

    // Ширина шаблона
    public final int length;

    // Маска по длине фигуры
    public final int mask;

    // Сравнение для белых камней
    public final int white;

    // Сравнение для черных камней
    public final int black;

    // Расстояние от левого края до левого камня
    public final int move;

    // Растояние от левого края до камней 
    public final int[] moves;

    // Растояние от левого края усиливающих позицию ходов 
    public final int[] gains;

    // Растояние от левой края закрывающих позицию ходов
    public final int[] downs;

    // Растояние от левой края ходов влюящих на позицию
    public final int[] rifts;

    // Конструктор
    public Pattern(int type, String template, int rating) {
        this.type = type;
        this.rating = rating;
        // Битовая строка для сравнения
        int s = 0;
        length = template.length();
        mask = (1 << (length << 1)) - 1;

        for (int i = length - 1; i >= 0; --i) {
            if (template.charAt(i) == 'x') {
                s = s | 1;
            }
            s = s << 2;
        }
        s = s >> 2;
        white = s;
        black = s << 1;

        // Смещение полей на линии
        List<Integer> moveList = new ArrayList<>();
        List<Integer> gainList = new ArrayList<>();
        List<Integer> downList = new ArrayList<>();
        List<Integer> riftList = new ArrayList<>();
        for (int i = 0; i < length; i++) {
            int ch = template.charAt(i);
            switch (ch) {
                case 'x':
                    moveList.add(i);
                    break;
                case '-':
                    gainList.add(i);
                    riftList.add(i);
                    break;
                case '+':
                    gainList.add(i);
                    downList.add(i);
                    riftList.add(i);
                    break;
                case '!':
                    downList.add(i);
                    riftList.add(i);
                    break;
                default:
                    riftList.add(i);
                    break;
            }
        }
        moves = moveList.stream().mapToInt(i -> i).toArray();
        move = moves[0];
        gains = gainList.stream().mapToInt(i -> i).toArray();
        downs = downList.stream().mapToInt(i -> i).toArray();
        rifts = riftList.stream().mapToInt(i -> i).toArray();
    }

    // Поиск выбранных фигур в строке
    public static short[] precalc(int stroke, int len, int type) {
        short[] figures = null;
        int size = 0;
        // Маска строки
        int probe = stroke;
        // Текущий номер камня
        int move = 0;
        while (probe > 0) {

            // По черным камням
            if ((probe & 2) > 0) {
                // По все шаблонам
                int i = 0;
                for (Pattern[] patterns : Pattern.SOLVER_PATTERNS) {
                    int j = 0;
                    for (Pattern pattern : patterns) {
                        // Левый край шаблона
                        int offset = move - pattern.move;
                        // Если шаблон соответствует запросу
                        if (pattern.type <= type
                                // Не выходит за границу доски
                                && offset >= 0 && offset + pattern.length <= len
                                // Нет черного каменя слева
                                && (offset > 0 ? (stroke >> ((offset - 1) << 1)) & 2 : 0) == 0
                                // Нет черного камня справа
                                && ((stroke >> ((offset + pattern.length) << 1)) & 2) == 0
                                // Соответствует проверочное значение
                                && ((stroke >> (offset << 1)) & pattern.mask) == pattern.black) {
                            // Добавить найденную фигуры и перейти к поиску от следующего камня
                            if (figures == null) {
                                figures = new short[15];
                            }
                            figures[size] = (short) ((i << 10) | (j << 5) | (offset << 1) | 1);
                            size++;
                            break;
                        }
                        j++;
                    }
                    i++;
                }
            }

            // По белым камням
            if ((probe & 1) > 0) {
                // По все шаблонам
                int i = 0;
                for (Pattern[] patterns : Pattern.SOLVER_PATTERNS) {
                    int j = 0;
                    for (Pattern pattern : patterns) {
                        // Левый край шаблона
                        int offset = move - pattern.move;
                        // Если шаблон соответствует запросу
                        if (pattern.type <= type
                                // Не выходит за границу доски
                                && offset >= 0 && offset + pattern.length <= len
                                // Нет белого каменя слева
                                && (offset > 0 ? (stroke >> ((offset - 1) << 1)) & 1 : 0) == 0
                                // Нет белого камня справа
                                && ((stroke >> ((offset + pattern.length) << 1)) & 1) == 0
                                // Соответствует проверочное значение
                                && ((stroke >> (offset << 1)) & pattern.mask) == pattern.white) {
                            // Добавить найденную фигуры и перейти к поиску от следующего камня
                            if (figures == null) {
                                figures = new short[15];
                            }
                            figures[size] = (short) ((i << 10) | (j << 5) | (offset << 1));
                            size++;
                            break;
                        }
                        j++;
                    }
                    i++;
                }
            }

            probe >>= 2;
            move++;
        }
        if (size > 0) {
            return Arrays.copyOf(figures, size);
        }
        return null;
    }

    public static short[][][] precalc() {

        // По всем длинам строк
        short[][][] result = new short[15][][];
        for (int len = 1; len <= 15; len++) {
            int size = 0;
            int max = 1;
            for (int i = 0; i < len; i++) {
                max = max * 3;
            }
            result[len - 1] = new short[max][];
            for (int i = 0; i < max; i++) {
                int k = i;
                int stroke = 0;
                int n = 0;
                while (k > 0) {
                    int m = k % 3;
                    k = k / 3;
                    stroke = stroke | ((m == 1 ? 1 : m == 2 ? 2 : 0) << (n << 1));
                    n++;
                }
                short[] figures = precalc(stroke, len, ONE);
                if (figures != null) {
                    size++;
                }
                result[len - 1][i] = figures;
            }
            System.out.println("Len=[" + len + "] Size=[" + size + "] Max=[" + max + "]");
        }
        return result;
    }

    public static short[][][] readPrecalc(InputStream in) throws IOException {
        short[][][] array;
        DataInputStream data = new DataInputStream(new BufferedInputStream(in));
        if (data.read() != 'P' || data.read() != 'D') {
            throw new IOException("Invalid format");
        }
        int n0 = 15;
        int n1 = 1;
        array = new short[n0][][];
        for (int i0 = 0; i0 < n0; i0++) {
            n1 = n1 * 3;
            short[][] array1 = new short[n1][];
            array[i0] = array1;
            int size = 0;
            for (int i1 = 0; i1 < n1; i1++) {
                int n2 = data.read();
                if (n2 > 0) {
                    short[] array2 = new short[n2];
                    array1[i1] = array2;
                    for (int i2 = 0; i2 < n2; i2++) {
                        array2[i2] = data.readShort();
                    }
                    size = size + n2;
                } else {
                    array1[i1] = null;
                }
            }
        }
        return array;
    }

    public static void writePrecalc(OutputStream out, short[][][] array) throws IOException {
        DataOutputStream data = new DataOutputStream(new BufferedOutputStream(out));
        data.write('P');
        data.write('D');
        int n0 = array.length;
        for (int i0 = 0; i0 < n0; i0++) {
            short[][] array1 = array[i0];
            int n1 = array1.length;
            int size = 0;
            for (int i1 = 0; i1 < n1; i1++) {
                short[] array2 = array1[i1];
                if (array2 != null) {
                    int n2 = array2.length;
                    size = size + n2;
                    data.write(n2);
                    for (int i2 = 0; i2 < n2; i2++) {
                        data.writeShort(array2[i2]);
                    }
                } else {
                    data.write(0);
                }
            }
        }
        data.flush();
    }

    public static int findFigures(Figure[] figures, int size, Line line, int type) {
        // Длина линии
        int len = Line.lineLength(line.number);
        // Строка
        int stroke = line.stroke;
        // Перевод в индекс
        int k = 0;
        int n = 1;
        while (stroke > 0) {
            k = k + (stroke & 3) * n;
            stroke = stroke >> 2;
            n = n * 3;
        }
        // Закодированные фигуры
        short[] array = PRECALCED_PATTERNS[len - 1][k];
        if (array != null) {
            for (short m : array) {
                figures[size] = new Figure(line.number, (m >> 1) & 0xf, m & 1,
                        SOLVER_PATTERNS[m >> 10][(m >> 5) & 0x1f]);
                size++;
            }
        }
        return size;
    }

    public static short[][][] PRECALCED_PATTERNS;

}
