package org.webjs.rv2;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Фигура на доске
 *
 * @author rmr
 */
public class Figure {

    // Цвет камней фигуры
    public final int hand;

    // Номер строки
    public final int number;

    // Смещение начала фигуры
    public final int offset;

    // Шаблон фигуры
    public final Pattern pattern;

    public static class Comparator implements java.util.Comparator<Figure> {

        private final int hand;

        public Comparator(int hand) {
            this.hand = hand;
        }

        @Override
        public int compare(Figure f1, Figure f2) {
            return (f1.hand == hand ? f1.pattern.type : (f1.pattern.type + 2))
                    - (f2.hand == hand ? f2.pattern.type : (f2.pattern.type + 2));
        }
    }

    // Конструктор
    public Figure(int number, int offset, int hand, Pattern pattern) {
        this.number = number;
        this.offset = offset;
        this.hand = hand;
        this.pattern = pattern;
    }

    // Построить массив полей по относительным координатам
    public byte[] moves_(int[] cols) {
        byte[] r = new byte[cols.length];
        for (int i = 0; i < cols.length; i++) {
            r[i] = (byte) Line.posSquare(number, offset + cols[i]);
        }
        return r;
    }

    // Поместить в коллекцию
    public void addMoves(Set<Integer> r, int[] cols) {
        for (int i = 0; i < cols.length; i++) {
            r.add(Line.posSquare(number, offset + cols[i]));
        }
    }

    // Построить массив полей по относительным координатам
    public Set<Integer> moves(int[] cols) {
        Set<Integer> r = new HashSet<>();
        addMoves(r, cols);
        return r;
    }

    // Построить поток полей по относительным координатам
    public Stream<Integer> moves__(int[] cols) {
        return IntStream.range(0, cols.length)
                .mapToObj(i -> Line.posSquare(number, offset + cols[i]));
    }

    // Занято ли поле фигурой
    public boolean contains(int square, int[] cols) {
        for (int i = 0; i < cols.length; i++) {
            if (square == Line.posSquare(number, offset + cols[i])) {
                return true;
            }
        }
        return false;
    }

    // Установленные камни
    public Set<Integer> moves() {
        return moves(pattern.moves);
    }

    // Установленные камни
    public void addMoves(Set<Integer> r) {
        addMoves(r, pattern.moves);
    }

    // Занято ли поле фигурой
    public boolean contains(int square) {
        return contains(square, pattern.moves);
    }

    // Ходы усиления
    public Set<Integer> gains() {
        return moves(pattern.gains);
    }

    // Ходы усиления
    public void addGains(Set<Integer> r) {
        addMoves(r, pattern.gains);
    }

    // Ходы закрытия
    public Set<Integer> downs() {
        return moves(pattern.downs);
    }

    // Ходы закрытия
    public void addDowns(Set<Integer> r) {
        addMoves(r, pattern.downs);
    }

    // Ходы, ломающие фигуру
    public Set<Integer> rifts() {
        return moves(pattern.rifts);
    }

    // Ходы, ломающие фигуру
    public void addRifts(Set<Integer> r) {
        addMoves(r, pattern.rifts);
    }

    // Проверить находится ли ход на линии
    public boolean onSameLine(int square) {
        return Line.onSameLine(number, square);
    }

}
