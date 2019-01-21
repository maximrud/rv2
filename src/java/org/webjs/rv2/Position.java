package org.webjs.rv2;

import java.util.Arrays;

/**
 * Позиция
 *
 * @author rmr
 */
public class Position {

    // Поля, занятые черными камнями
    public final byte[] blacks;

    // Поле, занятые белыми камнями 
    public final byte[] whites;

    // Конструктор
    public Position() {
        this.blacks = new byte[0];
        this.whites = new byte[0];
    }

    // Конструктор
    public Position(byte[] blacks, byte[] whites) {
        this.blacks = blacks;
        this.whites = whites;
    }

    // Конструктор
    public Position(byte[] moves) {
        if (moves != null && moves.length > 0) {
            boolean odd = true;
            blacks = new byte[(moves.length + 1) >> 1];
            whites = new byte[moves.length >> 1];
            for (int i = 0; i < moves.length; i++) {
                if (odd) {
                    blacks[i >> 1] = moves[i];
                } else {
                    whites[(i - 1) >> 1] = moves[i];
                }
                odd = !odd;
            }
            Arrays.sort(blacks);
            Arrays.sort(whites);
        } else {
            blacks = new byte[0];
            whites = new byte[0];
        }
    }

    // Добавление значения с сортировкой
    public static byte[] addMove_(byte[] moves, int move) {
        int len = moves.length;
        byte[] r = Arrays.copyOf(moves, len + 1);
        r[len] = (byte) move;
        Arrays.sort(r);
        return r;
    }

    // Добавление значения с сортировкой
    public static byte[] addMove(byte[] moves, int square) {
        int len = moves.length;
        byte n = (byte) square;
        byte[] r = new byte[len + 1];
        int j = 0;
        for (int i = 0; i < len; i++) {
            byte m = moves[i];
            if (j == 0 && m > n) {
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

    // Сделать ход указанным цветом
    public Position makeMove(int hand, int square) {
        return hand > 0
                ? new Position(addMove(blacks, square), whites)
                : new Position(blacks, addMove(whites, square));
    }

    // Повернуть позицию
    public void rotate() {
        for (int i = 0; i < blacks.length; i++) {
            blacks[i] = (byte) (((blacks[i] & 0xf) << 4) | (14 - ((blacks[i] & 0xf0) >> 4)));
        }
        Arrays.sort(blacks);
        for (int i = 0; i < whites.length; i++) {
            whites[i] = (byte) (((whites[i] & 0xf) << 4) | (14 - ((whites[i] & 0xf0) >> 4)));
        }
        Arrays.sort(whites);
    }

    // Отобразить позицию
    public void mirror() {
        for (int i = 0; i < blacks.length; i++) {
            blacks[i] = (byte) ((blacks[i] & 0xf0) | (14 - (blacks[i] & 0x0f)));
        }
        Arrays.sort(blacks);
        for (int i = 0; i < whites.length; i++) {
            whites[i] = (byte) ((whites[i] & 0xf0) | (14 - (whites[i] & 0x0f)));
        }
        Arrays.sort(whites);
    }

    // Хэш код для коллекций
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Arrays.hashCode(this.blacks);
        hash = 67 * hash + Arrays.hashCode(this.whites);
        return hash;
    }

    // Хэш код для коллекций
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
        final Position other = (Position) obj;
        return Arrays.equals(this.blacks, other.blacks)
                && Arrays.equals(this.whites, other.whites);
    }

}
