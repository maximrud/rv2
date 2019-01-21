package org.webjs.rv2;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Расположение камней по линиям
 *
 * @author rmr
 */
public class Layout {

    // Цвет фигур    
    public static final int BLACK = 1;
    public static final int WHITE = 0;

    // Линии
    public Line[] lines;

    // Найденные фигуры
    public Figure[] figures;

    // Текущий ход
    public final int hand;

    // Максимальный шаблон
    public final int type;

    // Количество камней
    public final int count;

    // Конструктор пустой позиции
    public Layout(int type) {
        this.type = type;
        count = 0;
        lines = new Line[0];
        figures = new Figure[0];
        hand = BLACK;
    }

    // Конструктор пустой позиции
    public Layout() {
        this(Pattern.ONE);
    }

    // Конструктор позиции с записи партии
    public Layout(byte[] moves, int type) {
        this.type = type;
        if (moves != null && moves.length > 0) {
            count = moves.length;
            int h = BLACK;
            int maxline = count * 4;
            Line[] ls = new Line[maxline];
            int size = 0;
            for (byte move : moves) {
                size = putStone(ls, size, move & 0xff, h);
                h = h == BLACK ? WHITE : BLACK;
            }
            lines = new Line[size];
            System.arraycopy(ls, 0, lines, 0, size);
            Arrays.sort(lines);
            hand = h;
            Figure[] fs = new Figure[4 * maxline];
            size = 0;
            for (Line line : lines) {
                size = line.findFigures(fs, size, type);
            }
            figures = new Figure[size];
            System.arraycopy(fs, 0, figures, 0, size);
            Arrays.sort(figures, new Figure.Comparator(hand));
        } else {
            count = 0;
            lines = new Line[0];
            figures = new Figure[0];
            hand = BLACK;
        }
    }

    // Конструктор позиции с записи партии
    public Layout(byte[] moves) {
        this(moves, Pattern.ONE);
    }

    // Конструктор позиции с записи партии
    public Layout(Position position, int type) {
        this.type = type;
        count = position.blacks.length + position.whites.length;
        int maxline = count * 4;
        if (maxline > 0) {
            Line[] ls = new Line[maxline];
            int size = 0;
            for (byte move : position.blacks) {
                size = putStone(ls, size, move & 0xff, BLACK);
            }
            for (byte move : position.whites) {
                size = putStone(ls, size, move & 0xff, WHITE);
            }
            lines = new Line[size];
            System.arraycopy(ls, 0, lines, 0, size);
            Arrays.sort(lines);
            hand = position.blacks.length > position.whites.length ? WHITE : BLACK;
            Figure[] fs = new Figure[4 * maxline];
            size = 0;
            for (Line line : lines) {
                size = line.findFigures(fs, size, type);
            }
            figures = new Figure[size];
            System.arraycopy(fs, 0, figures, 0, size);
            Arrays.sort(figures, new Figure.Comparator(hand));
        } else {
            lines = new Line[0];
            figures = new Figure[0];
            hand = BLACK;
        }
    }

    public Layout(Position position) {
        this(position, Pattern.ONE);
    }

    // Конструктор позиции с готовых объектов
    public Layout(Line[] lines, int hand, Figure[] figures, int type, int count) {
        this.lines = lines;
        this.hand = hand;
        this.figures = figures;
        this.type = type;
        this.count = count;
    }

    // Сделать ход 
    public Layout makeMove(int square) {
        // Один ход может образовать по две фигуры на каждой строке
        Figure[] fs = new Figure[figures.length + 8];
        int size = 0;
        // Скопировать фигуры, незадетые ходом
        for (Figure figure : figures) {
            if (!figure.onSameLine(square)) {
                fs[size] = figure;
                size++;
            }
        }
        Line[] ls = addStone(square);
        int h = hand == BLACK ? WHITE : BLACK;
        for (Line line : ls) {
            if (Line.onSameLine(line.number, square)) {
                size = line.findFigures(fs, size, type);
            }
        }
        Figure[] rs = new Figure[size];
        System.arraycopy(fs, 0, rs, 0, size);
        Arrays.sort(rs, new Figure.Comparator(h));
        return new Layout(ls, h, rs, type, count + 1);
    }

    // Отменить ход 
    public Layout backMove(int square) {
        // Один ход может образовать по две фигуры на каждой строке
        Figure[] fs = new Figure[figures.length + 8];
        int size = 0;
        // Скопировать фигуры, незадетые ходом
        for (Figure figure : figures) {
            if (!figure.onSameLine(square)) {
                fs[size] = figure;
                size++;
            }
        }
        Line[] ls = removeStone(square);
        int h = hand == BLACK ? WHITE : BLACK;
        for (Line line : ls) {
            if (Line.onSameLine(line.number, square)) {
                size = line.findFigures(fs, size, type);
            }
        }
        Figure[] rs = new Figure[size];
        System.arraycopy(fs, 0, rs, 0, size);
        Arrays.sort(rs, new Figure.Comparator(h));
        return new Layout(ls, h, rs, type, count - 1);
    }

    // Получить цвет камня на поле. -1 если камня нет
    public int getStone(int square) {
        int r = -1;
        if (lines.length > 0) {
            int number = square >> 4;
            // Поиск по существующим линиям
            int i = 0;
            Line line = lines[0];
            while (line.number < 0x20) {
                if (line.number == number) {
                    int offset = square & 0xf;
                    return line.getStone(offset);
                }
                i++;
                line = lines[i];
            }
        }
        return r;
    }

    // Поменять цвет следующего хода
    public Layout swapHand(int type) {
        int h = hand == BLACK ? WHITE : BLACK;
        Figure[] fs = new Figure[figures.length];
        int size = 0;
        for (Figure f : figures) {
            if (f.pattern.type <= type) {
                fs[size] = f;
                size++;
            }
        }
        fs = Arrays.copyOf(fs, size);
        Arrays.sort(fs, new Figure.Comparator(h));
        return new Layout(lines, h, fs, type, count);
    }

    // Получить выборку с меньшем типом
    public Layout alignType(int type) {
        if (this.type == type) {
            return this;
        } else if (this.type > type) {
            Figure[] fs = new Figure[figures.length];
            int size = 0;
            for (Figure f : figures) {
                if (f.pattern.type <= type) {
                    fs[size] = f;
                    size++;
                }
            }
            fs = Arrays.copyOf(fs, size);
            return new Layout(lines, hand, fs, type, count);
        } else {
            // Максимум по три фигуры на каждой строке
            Figure[] fs = new Figure[lines.length * 3];
            int size = 0;
            for (Line line : lines) {
                size = line.findFigures(fs, size, type);
            }
            fs = Arrays.copyOf(fs, size);
            Arrays.sort(fs, new Figure.Comparator(hand));
            return new Layout(lines, hand, fs, type, count);
        }
    }

    // Получить главную фигуру
    public Figure top() {
        if (figures.length > 0) {
            return figures[0];
        } else {
            return null;
        }
    }

    // Подсчет баллов позиции
    public int rate() {
        // Подсчитать полученное число очков по решению
        int rating = 0;
        for (Figure figure : figures) {
            if (figure.hand == hand) {
                rating = rating + figure.pattern.rating;
            } else {
                rating = rating - figure.pattern.rating;
            }
        }
        return rating;
    }

    // Ходы атаки до определенного уровня
    public Set<Integer> gains(int type) {
        Set<Integer> r = new HashSet<>();
        for (Figure figure : figures) {
            if (figure.hand == hand && figure.pattern.type <= type) {
                r.addAll(figure.gains());
            }
        }
        return r;
    }

    // Ходы обороны до определенного уровня
    public Set<Integer> downs(int type) {
        Set<Integer> r = new HashSet<>();
        for (Figure figure : figures) {
            if (figure.hand != hand && figure.pattern.type <= type) {
                r.addAll(figure.downs());
            }
        }
        return r;
    }

    // Пустые поля
    public Set<Integer> availables() {
        Set<Integer> r = new HashSet<>();
        if (count < 1) {
            // Только центральный камень
            r.add((7 << 4) | 7);
        } else if (count < 2) {
            // Только 1/8 с учетом симметрий без центрального камня
            int right = 15;
            for (int number = 0; number < 7; number++) {
                for (int offset = 7; offset < right; offset++) {
                    r.add((number << 4) | offset);
                }
                right--;
            }
        } else {
            Line e = lines[0];
            int i = 0;
            int number = 0;
            while (e.number < 15) {
                // Добавить все строки до текущей
                while (number < e.number) {
                    for (int offset = 0; offset < 15; offset++) {
                        r.add((number << 4) | offset);
                    }
                    number++;
                }
                // Маска строки
                int probe = e.stroke;
                // Текущий номер камня
                int offset = 0;
                while (offset < 15) {
                    // Камень отсутствует - добавить в список
                    if ((probe & 3) == 0) {
                        r.add((number << 4) | offset);
                    }
                    probe >>= 2;
                    offset++;
                }
                number++;
                i++;
                e = lines[i];
            }
            // Добавить все пустые строки до максимальной
            while (number < 15) {
                for (int offset = 0; offset < 15; offset++) {
                    r.add((number << 4) | offset);
                }
                number++;
            }
        }
        return r;
    }

    // Добавление значения с сортировкой
    public static Line[] addLine_(Line[] lines, Line line) {
        int len = lines.length;
        Line[] r = Arrays.copyOf(lines, len + 1);
        r[len] = line;
        Arrays.sort(r);
        return r;
    }

    // Добавление значения с сортировкой
    public static Line[] addLine(Line[] lines, Line line) {
        int len = lines.length;
        Line n = line;
        Line[] r = new Line[len + 1];
        int j = 0;
        for (int i = 0; i < len; i++) {
            Line m = lines[i];
            if (j == 0 && m.number > n.number) {
                r[i] = n;
                j++;
            }
            r[i + j] = m;
        }
        if (j == 0) {
            r[len] = n;
        }
        return r;
    }

    // Добавить один камень в текущую позицию
    public Line[] addStone(int square) {
        int size = lines.length;
        Line[] ls = new Line[size + 4];
        System.arraycopy(lines, 0, ls, 0, size);
        size = putStone(ls, size, square, hand);
        Line[] rs = new Line[size];
        System.arraycopy(ls, 0, rs, 0, size);
        Arrays.sort(rs);
        return rs;
    }

    // Убрать камень из позиции
    public Line[] removeStone(int square) {
        Line[] ls = new Line[lines.length];
        System.arraycopy(lines, 0, ls, 0, lines.length);
        for (int direction = 0; direction < 4; direction++) {
            int number = Line.lineNumber(direction, square);
            int offset = Line.lineOffset(direction, square);
            // Поиск по существующим линиям
            int i = 0;
            while (i < ls.length) {
                Line line = ls[i];
                if (line.number == number) {
                    // Удалить камень с линии
                    ls[i] = line.removeStone(offset, hand == BLACK ? WHITE : BLACK);
                    break;
                }
                i++;
            }
        }
        // Убрать лишние линии
        int size = 0;
        Line[] rs = new Line[ls.length];
        for (Line line : ls) {
            if (line.stroke != 0) {
                rs[size] = line;
                size++;
            }
        }
        return Arrays.copyOf(rs, size);
    }

    // Установить камен в требуемую позицию на всех линиях
    public static int putStone(Line[] lines, int size, int square, int hand) {
        // Добавим линии по направлениям
        for (int direction = 0; direction < 4; direction++) {
            int number = Line.lineNumber(direction, square);
            int offset = Line.lineOffset(direction, square);
            // Поиск по существующим линиям
            int i = 0;
            while (i < size) {
                Line line = lines[i];
                if (line.number == number) {
                    lines[i] = line.putStone(offset, hand);
                    break;
                }
                i++;
            }
            // Линия не найдена - добавить новую
            if (i == size) {
                lines[size] = new Line(number, offset, hand);
                size++;
            }
        }
        return size;
    }

    public Position toPosition() {
        if (lines.length == 0) {
            return new Position();
        }
        // Подсчет количества
        int blen = 0;
        int wlen = 0;
        Line e = lines[0];
        int i = 0;
        while (e.number < 0x20) {
            wlen = wlen + e.wlen;
            blen = blen + e.blen;
            i++;
            e = lines[i];
        }
        // Заполнение структур
        byte[] blacks = new byte[blen];
        byte[] whites = new byte[wlen];
        e = lines[0];
        i = 0;
        while (e.number < 0x20) {
            // Маска строки
            int probe = e.stroke;
            // Текущий номер камня
            int move = 0;
            while (probe > 0) {
                // По черным камням
                if ((probe & 2) > 0) {
                    blen--;
                    blacks[blen] = (byte) ((e.number << 4) | move);
                }
                // По белым камням
                if ((probe & 1) > 0) {
                    wlen--;
                    whites[wlen] = (byte) ((e.number << 4) | move);
                }
                probe >>= 2;
                move++;
            }
            i++;
            e = lines[i];
        }
        return new Position(blacks, whites);
    }

    // Хэш одинаков для 8 траспозиций
    @Override
    public int hashCode() {
        if (lines.length == 0) {
            return 3 * 89 * 89;
        }
        int h0 = 5;
        int h1 = 5;
        int h2 = 5;
        int h3 = 5;
        int i = 0;
        Line e = lines[0];
        while (e.number < 0x20) {
            h0 = 37 * h0 + e.number;
            h0 = 37 * h0 + (e.stroke ^ e.inverse);
            i++;
            e = lines[i];
        }
        while (e.number < 0x40) {
            h2 = 37 * h2 + (e.number & 0x1f);
            h2 = 37 * h2 + (e.stroke ^ e.inverse);
            i++;
            e = lines[i];
        }
        i--;
        e = lines[i];
        while (e.number >= 0x20) {
            h3 = 37 * h3 + (14 - (e.number & 0x1f));
            h3 = 37 * h3 + (e.stroke ^ e.inverse);
            i--;
            e = lines[i];
        }
        while (true) {
            h1 = 37 * h1 + (14 - e.number);
            h1 = 37 * h1 + (e.stroke ^ e.inverse);
            i--;
            if (i < 0) {
                break;
            }
            e = lines[i];
        }
        int hash = 3;
        hash = hash * 89 + lines.length;
        hash = hash * 89 + (h0 ^ h1 ^ h2 ^ h3);
        return hash;
    }

    // Сравнить строки в 4х направлениях
    public static int checkLines(Line[] ls0, int ofs0, Line[] ls1, int ofs1, int count) {
        int r = 0;
        Line e = ls0[ofs0];
        Line e1 = ls1[ofs1];
        if (e.number == (e1.number & 0x1f)) {
            if (e.stroke == e1.stroke) {
                r = r | 1;
            }
            if (e.stroke == e1.inverse) {
                r = r | 2;
            }
        }
        e1 = ls1[ofs1 + count - 1];
        if (e.number == 14 - (e1.number & 0x1f)) {
            if (e.stroke == e1.stroke) {
                r = r | 4;
            }
            if (e.stroke == e1.inverse) {
                r = r | 8;
            }
        }
        if (r == 0) {
            return -1;
        }
        for (int i = 1; i < count; i++) {
            e = ls0[ofs0 + i];
            e1 = ls1[ofs1 + i];
            if (e.number == (e1.number & 0x1f)) {
                if (e.stroke != e1.stroke) {
                    r = r & (~1);
                }
                if (e.stroke != e1.inverse) {
                    r = r & (~2);
                }
            } else {
                r = r & (~3);
            }
            e1 = ls1[ofs1 + count - 1 - i];
            if (e.number == 14 - (e1.number & 0x1f)) {
                if (e.stroke != e1.stroke) {
                    r = r & (~4);
                }
                if (e.stroke != e1.inverse) {
                    r = r & (~8);
                }
            } else {
                r = r & (~12);
            }
            if (r == 0) {
                return -1;
            }
        }
        return (r & 1) > 0 ? 0 : (r & 2) > 0 ? 6 : (r & 4) > 0 ? 2 : 4;
    }

    // Трансформация клетки
    public static int transSquare(int square, int transCode) {
        switch (transCode) {
            case 1:
                return ((square & 0xf) << 4) | (square >> 4);
            case 2:
                return ((14 - (square >> 4)) << 4) | (square & 0xf);
            case 3:
                return ((square & 0xf) << 4) | (14 - (square >> 4));
            case 4:
                return ((14 - (square >> 4)) << 4) | (14 - (square & 0xf));
            case 5:
                return ((14 - (square & 0xf)) << 4) | (14 - (square >> 4));
            case 6:
                return (square & 0xf0) | (14 - (square & 0x0f));
            case 7:
                return ((14 - (square & 0xf)) << 4) | (square >> 4);
            default:
                return square;
        }
    }

    // Получить номер транспозиции
    public int transCode(Layout layout) {
        if (layout == null) {
            return -1;
        }
        Line[] ls = layout.lines;
        if (ls.length != this.lines.length) {
            return -1;
        }
        // Пустые позиции одинаковы
        if (this.lines.length == 0) {
            return 0;
        }
        // Без замены направлений
        int n = 0;
        Line e = this.lines[0];
        while (e.number < 0x20) {
            n++;
            e = this.lines[n];
        }
        int n1 = 0;
        Line e1 = ls[0];
        while (e1.number < 0x20) {
            n1++;
            e1 = ls[n1];
        }
        if (n == n1) {
            int r = checkLines(this.lines, 0, ls, 0, n);
            if (r >= 0) {
                return r;
            }
        }
        // С заменой направлений
        int m1 = 0;
        while (e1.number < 0x40) {
            m1++;
            e1 = ls[n1 + m1];
        }
        if (n == m1) {
            int r = checkLines(this.lines, 0, ls, n1, n);
            if (r >= 0) {
                return r + 1;
            }
        }
        return -1;
    }

    public static int transMultiple(int tranCode1, int tranCode2) {
        final int[] mirror = new int[]{6, 3, 4, 1, 2, 7, 0, 5};
        final int[] turn = new int[]{0, 3, 2, 1, 2, 1, 0, 3};
        final int[] rotate0 = new int[]{0, 3, 4, 7};
        final int[] rotate1 = new int[]{6, 5, 2, 1};

        // Зеркальное отборажение
        if (tranCode2 == 6 || tranCode2 == 5 || tranCode2 == 2 || tranCode2 == 1) {
            tranCode1 = mirror[tranCode1];
        }
        // Число повортов
        int rotor = (turn[tranCode1] + turn[tranCode2]) % 4;
        if (tranCode1 == 6 || tranCode1 == 5 || tranCode1 == 2 || tranCode1 == 1) {
            return rotate1[rotor];
        } else {
            return rotate0[rotor];
        }
    }

    // Равенство с учетом транспозиций
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Layout other = (Layout) obj;
        if (this.hand != other.hand) {
            return false;
        }
        return transCode(other) >= 0;
    }

}
