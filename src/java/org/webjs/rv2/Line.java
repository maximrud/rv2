package org.webjs.rv2;

/**
 * Линия
 *
 * @author rmr
 */
public class Line implements Comparable {

    // Направление линии
    public static final int LEFT_RIGTH = 0;
    public static final int TOP_DOWN = 1;
    public static final int LTOP_RDOWN = 2;
    public static final int RTOP_LDOWN = 3;

    // Строка байт 01 - белые, 10 - черные
    public final int stroke;

    // Номер линии
    public final int number;

    // Количество черных камней
    public final int blen;

    // Количество белых камней
    public final int wlen;

    // Инверсная строка байт 
    public final int inverse;

    // Создать пустую линию
    public Line(int number) {
        this.number = number;
        this.blen = 0;
        this.wlen = 0;
        this.stroke = 0;
        this.inverse = 0;
    }

    // Создать линию с одним камнем
    public Line(int number, int offset, int hand) {
        this.number = number;
        if (hand > 0) {
            this.blen = 1;
            this.wlen = 0;
        } else {
            this.blen = 0;
            this.wlen = 1;
        }
        int shift = offset << 1;
        this.stroke = 1 << (shift + hand);
        int right = (lineLength(number) - 1) << 1;
        this.inverse = 1 << (right - shift + hand);
    }

    public Line(int number, int blen, int wlen, int stroke, int inverse) {
        this.number = number;
        this.blen = blen;
        this.wlen = wlen;
        this.stroke = stroke;
        this.inverse = inverse;
    }

    // Установить камен в требуемую позицию на линии
    public Line putStone(int offset, int hand) {
        int shift = offset << 1;
        if ((stroke & (3 << shift)) > 0) { // Ячейка уже занята
            throw new java.lang.IndexOutOfBoundsException();
        }
        int str = (stroke & (~(3 << shift))) | (1 << (shift + hand));
        int right = (lineLength(number) - 1) << 1;
        int inv = (inverse & (~(3 << (right - shift)))) | (1 << (right - shift + hand));
        return hand > 0
                ? new Line(number, blen + 1, wlen, str, inv)
                : new Line(number, blen, wlen + 1, str, inv);
    }

    // Получить цвет камня на поле. -1 если камня нет
    public int getStone(int offset) {
        int shift = offset << 1;
        if ((stroke & (1 << (shift + Layout.BLACK))) > 0) {
           return Layout.BLACK; 
        }
        if ((stroke & (1 << (shift + Layout.WHITE))) > 0) {
           return Layout.WHITE; 
        }
        return -1;
    }
    
    // Убрать камень из позиции
    public Line removeStone(int offset, int hand) {
        int shift = offset << 1;
        if ((stroke & (3 << shift)) != (1 << (shift + hand))) { // В ячейке нет нужного камня
            throw new java.lang.IndexOutOfBoundsException();
        }
        int str = (stroke & (~(3 << shift)));
        int right = (lineLength(number) - 1) << 1;
        int inv = (inverse & (~(3 << (right - shift))));
        return hand > 0
                ? new Line(number, blen - 1, wlen, str, inv)
                : new Line(number, blen, wlen - 1, str, inv);
    }


    // Идентификатор линии с учетом направления
    public static int lineNumber(int direction, int square) {
        switch (direction) {
            case LEFT_RIGTH:
                // Номер строки row
                return (direction << 5) | (square >> 4);
            case TOP_DOWN:
                // Номер колонки col 
                return (direction << 5) | (square & 0xf);
            case LTOP_RDOWN:
                // Номер диагонали из левого верхнего угла col - row + 14
                return (direction << 5) | ((square & 0xf) - (square >> 4) + 14);
            case RTOP_LDOWN:
                // Номер диагонали из правого верхнего угла col + row
                return (direction << 5) | ((square & 0xf) + (square >> 4));
            default:
                return 0;
        }
    }

    // Проверка находится ли поле на линии по номеру
    public static boolean onSameLine(int number, int square) {
        return number == lineNumber(number >> 5, square);
    }

    // Длина строки
    public static int lineLength(int number) {
        int direction = number >> 5;
        switch (direction) {
            case LTOP_RDOWN:
            case RTOP_LDOWN:
                // 15 середина от нее влево вправо на один меньше
                int i = number & 0x1f;
                return i > 14 ? 29 - i : i + 1;
            default:
                return 15;
        }
    }

    // Индекс позиции на линии
    public static int lineOffset(int direction, int square) {
        int col = square & 0xf;
        int row = square >> 4;
        switch (direction) {
            case LEFT_RIGTH:
                // Номер колонки col
                return col;
            case TOP_DOWN:
                // Номер строки row
                return row;
            case LTOP_RDOWN:
                // До середины номер колонки col, после номер строки row
                return col > row ? row : col;
            case RTOP_LDOWN:
                // До середины номер строки row, затем 14 - номер колонки col
                return col + row > 14 ? 14 - col : row;
            default:
                return 0;
        }
    }

    // Идентификатор поля на доске по номеру линии и расстоянию
    public static int posSquare(int number, int offset) {
        int direction = number >> 5;
        int n = number & 0x1f;
        switch (direction) {
            case LEFT_RIGTH:
                // По строкам
                return (n << 4) | offset;
            case TOP_DOWN:
                // По колонкам
                return (offset << 4) | n;
            case LTOP_RDOWN:
                // Диагональ из левого верхнего угла 
                return n > 14 ? (offset << 4) | (n + offset - 14)
                        : ((offset - n + 14) << 4) | offset;
            case RTOP_LDOWN:
                // Диагональ из правого верхнего угла
                return n > 14 ? ((n + offset - 14) << 4) | (14 - offset)
                        : (offset << 4) | (n - offset);
            default:
                return 0;
        }
    }

    // Поиск выбранных фигур в строке
    public int findFigures(Figure[] figures, int size, int type) {
        // Длина линии
        int len = Line.lineLength(number);
        // Число черных камней
        int bl = this.blen;
        // Число белых камней
        int wl = this.wlen;
        // Маска строки
        int probe = stroke;
        // Текущий номер камня
        int move = 0;
        while (probe > 0) {

            // По черным камням
            if ((probe & 2) > 0) {
                // По все шаблонам
                for (Pattern[] patterns : Pattern.SOLVER_PATTERNS) {
                    for (Pattern pattern : patterns) {
                        // Левый край шаблона
                        int offset = move - pattern.move;
                        // Если шаблон соответствует запросу
                        if (pattern.type <= type
                                // Число камней в шаблоне не больше оставшихся
                                && bl >= pattern.moves.length
                                // Не выходит за границу доски
                                && offset >= 0 && offset + pattern.length <= len
                                // Нет черного каменя слева
                                && (offset > 0 ? (stroke >> ((offset - 1) << 1)) & 2 : 0) == 0
                                // Нет черного камня справа
                                && ((stroke >> ((offset + pattern.length) << 1)) & 2) == 0
                                // Соответствует проверочное значение
                                && ((stroke >> (offset << 1)) & pattern.mask) == pattern.black) {
                            // Добавить найденную фигуры и перейти к поиску от следующего камня
                            figures[size] = new Figure(this.number, offset, 1, pattern);
                            size++;
                            break;
                        }
                    }
                }
                bl--;
            }

            // По белым камням
            if ((probe & 1) > 0) {
                // По все шаблонам
                for (Pattern[] patterns : Pattern.SOLVER_PATTERNS) {
                    for (Pattern pattern : patterns) {
                        // Левый край шаблона
                        int offset = move - pattern.move;
                        // Если шаблон соответствует запросу
                        if (pattern.type <= type
                                // Число камней в шаблоне не больше оставшихся
                                && wl >= pattern.moves.length
                                // Не выходит за границу доски
                                && offset >= 0 && offset + pattern.length <= len
                                // Нет белого каменя слева
                                && (offset > 0 ? (stroke >> ((offset - 1) << 1)) & 1 : 0) == 0
                                // Нет белого камня справа
                                && ((stroke >> ((offset + pattern.length) << 1)) & 1) == 0
                                // Соответствует проверочное значение
                                && ((stroke >> (offset << 1)) & pattern.mask) == pattern.white) {
                            // Добавить найденную фигуры и перейти к поиску от следующего камня
                            figures[size] = new Figure(this.number, offset, 0, pattern);
                            size++;
                            break;
                        }
                    }
                }
                wl--;
            }

            probe >>= 2;
            move++;
        }
        return size;
    }

    // Поиск всех фигур в строке
    public int findFigures(Figure[] figures, int size) {
        return findFigures(figures, size, Pattern.ONE);
    }

    
    @Override
    public int compareTo(Object o) {
        Line e = ((Line) o);
        return number < e.number ? -1 : number > e.number ? 1
                : stroke < e.stroke ? - 1 : stroke > e.stroke ? 1 : 0;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 89 * hash + this.stroke;
        hash = 89 * hash + this.number;
        return hash;
    }

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
        final Line other = (Line) obj;
        return this.stroke == other.stroke
                && this.number == other.number;
    }

}
