package org.webjs.rv2;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.json.JSONObject;

/**
 * Хранение позиций
 *
 * @author rmr
 */
public final class Store extends AbstractSet<Vertex> implements Set<Vertex> {

    // Корневая позиция
    protected Vertex root;

    // Хранилище позиций
    public Map<Vertex, Vertex> vertexes;

    public Store() {
        vertexes = new HashMap<>();
        root = new Vertex(new byte[0], Layout.BLACK);
        root.state = -128;
        vertexes.put(root, root);
    }

    public Store(JSONObject object) {
        vertexes = new HashMap<>();
        fromJSON(object);
    }

    // Перестроение коллекций от корневых вершин
    public synchronized void normalize() {
        // Пересчет графа от корневой вершины
        check(root);
        // Перестроение индекса
        clear();
        refresh(root);
    }

    // Добавление перебора полей
    public synchronized void merge(byte[] moves) {
        if (moves != null && moves.length > 0) {
            Vertex r = root;
            for (int i = 0; i < moves.length; i++) {
                int square = moves[i] & 0xff;
                Vertex p = r.makeMove(square);
                Vertex v = get(p);
                if (v == null) {
                    p.state = -128;
                    put(p);
                    Edge e = new Edge(square, p);
                    if (r.edges == null) {
                        r.edges = new Edge[]{e};
                    } else {
                        int size = r.edges.length;
                        r.edges = Arrays.copyOf(r.edges, size + 1);
                        r.edges[size] = e;
                    }
                    r = p;
                } else {
                    r = v;
                }
            }
        }
    }

    // Добавление графа
    public synchronized void merge(byte[] moves, Vertex vertex) {
        merge(moves);
        merge(vertex);
        normalize();
    }

    // Записать граф в файл
    public synchronized void writex(OutputStream out) throws IOException {
        Set<Vertex> check = new HashSet<>();
        out.write('V');
        out.write('S');
        write(out, root, 0, check);
        check.clear();
    }

    // Прочитать граф из файла
    public synchronized void readx(InputStream in) throws IOException {
        if (in.read() != 'V' || in.read() != 'S') {
            throw new IOException("Invalid format");
        }
        clear();
        read(in, root, 0);
    }

    // Записать граф в файл
    public synchronized void write(OutputStream out) throws IOException {
        out.write('V');
        out.write('Y');
        write2s(out, root, 0);
    }

    // Прочитать граф из файла
    public synchronized void read(InputStream in) throws IOException {
        if (in.read() != 'V' || in.read() != 'Y') {
            throw new IOException("Invalid format");
        }
        clear();
        read2s(in, root);
    }

    // Преобразовать в JSON
    public synchronized JSONObject toJSON() {
        JSONObject object = toJSON(root, 0);
        return object;
    }

    public synchronized void fromJSON(JSONObject object) {
        fromJSON(object, root);
    }

    // Добавление графа
    private Vertex merge(Vertex vertex) {
        Vertex v = get(vertex);
        if (v != null) {
            // Найдена вершина с более коротким путем
            if (v.state == -128 || (vertex.state != 0 && (v.state == 0
                    || Math.abs(vertex.state) < Math.abs(v.state)))) {
                // Заменить параметры
                v.state = vertex.state;
                v.rating = vertex.rating;
                v.edges = vertex.edges;
            } else {
                // Замена вершины в графе
                return v;
            }
        } else {
            add(vertex);
        }
        if (vertex.edges != null) {
            for (Edge e : vertex.edges) {
                e.vertex = merge(e.vertex);
            }
        }
        return vertex;
    }

    // Пересчет состояний узлов
    private void check(Vertex vertex) {
        if (vertex.edges != null) {
            for (Edge e : vertex.edges) {
                check(e.vertex);
            }
        }
        vertex.check();
    }

    // Построение коллекции объектов
    private void refresh(Vertex vertex) {
        if (!contains(vertex)) {
            put(vertex);
            if (vertex.edges != null) {
                for (Edge e : vertex.edges) {
                    refresh(e.vertex);
                }
            }
        }
    }

    // Записать граф в файл
    private void write(OutputStream out, Vertex vertex, int tranCode,
            Set<Vertex> check) throws IOException {
        // Записать состояние
        out.write(vertex.state);
        if (vertex.edges == null || check.contains(vertex) || tranCode != 0) {
            // Нет дочерних узлов или узел уже записан раньше или будет записан позже
            out.write(0);
            return;
        }
        // Сохранить информацию о записи узла
        check.add(vertex);
        // Записать количество ходов
        out.write(vertex.edges.length);
        for (Edge e : vertex.edges) {
            // Записать ход
            out.write(e.square);
            Vertex p = vertex.makeMove(e.square);
            // Записать следующий узел
            write(out, e.vertex, p.transCode(e.vertex), check);
        }
    }

    // Прочитать граф из файла
    private void read(InputStream in, Vertex vertex, int level) throws IOException {
        // Прочитать состояние
        vertex.state = (byte) in.read();
        // Прочитать количество ходов
        int size = in.read();
        if (size <= 0) {
            // Не требуется продолжать чтение
            return;
        }
        vertex.edges = new Edge[size];
        for (int i = 0; i < size; i++) {
            // Прочитать ход
            int square = in.read();
            Vertex p = vertex.makeMove(square);
            Vertex v = get(p);
            if (v == null) {
                p.prepare(true);
                put(p);
                v = p;
            } else if (v.state == -128) {
                // Неподготовленный узел
                v.prepare(true);
                v.edges = null;
            }
            Edge e = new Edge(square, v);
            read(in, p, level + 1);
            if (v.edges == null && p.edges != null) {
                // Замена вершины
                v.lines = p.lines;
                v.figures = p.figures;
                v.edges = p.edges;
                v.prepare(true);
            }
            vertex.edges[i] = e;
        }
    }

    // Записать граф в файл
    private void write2s(OutputStream out, Vertex vertex, int transCode) throws IOException {

        // Записать состояние
        out.write(vertex.state);

        // Если нет дочерних ходов
        if (vertex.edges == null) {
            out.write(0);
            return;
        }

        // Записать дочерние ходы
        if (vertex.hand == Layout.BLACK && vertex.state > 0) {
            out.write(1);
        } else {
            out.write(vertex.edges.length);
        }
        for (Edge e : vertex.edges) {

            // Записать ход
            int square = Layout.transSquare(e.square, transCode);
            // System.out.println("H                                               ".substring(0, vertex.count)
            //         + "[" + (square >> 4) + ":" + (square & 0xf) + "]");
            out.write(square);

            // Код трансформации
            int nextTransCode = Layout.transMultiple(e.vertex.transCode(vertex.makeMove(e.square)),
                    transCode);

            // Записать состояние дочерних ходов
            write2s(out, e.vertex, nextTransCode);

            if (vertex.hand == Layout.BLACK && vertex.state > 0) {
                break;
            }
        }

    }

    // Прочитать граф из файла
    private void read2s(InputStream in, Vertex vertex) throws IOException {
        // Прочитать состояние
        vertex.state = (byte) in.read();

        // Прочитать количество ходов
        int size = in.read();
        if (size <= 0) {
            // Не требуется продолжать чтение
            return;
        }
        vertex.edges = new Edge[size];
        for (int i = 0; i < size; i++) {

            // Прочитать ход и создать грань
            int square = in.read();
            // System.out.println("H                                               ".substring(0, vertex.count)
            //        + "[" + (square >> 4) + ":" + (square & 0xf) + "]");

            Vertex p = vertex.makeMove(square);
            Vertex v = get(p);
            if (v == null) {
                put(p);
                // Прочитать дочернюю вершину
                read2s(in, p);
                v = p;
            } else {
                if (v.state == -128) {
                    // Неподготовленный узел
                    v.prepare(true);
                    v.edges = null;
                }
                // Пропустить вложенные ходы
                skip2s(in);
            }

            vertex.edges[i] = new Edge(square, v);
        }
    }

    // Пропуск чтения страницы
    private void skip2s(InputStream in) throws IOException {
        // Прочитать состояние
        in.read();

        // Прочитать количество ходов
        int size = in.read();
        if (size <= 0) {
            // Не требуется продолжать чтение
            return;
        }
        for (int i = 0; i < size; i++) {

            // Прочитать ход 
            in.read();

            // Пропустить вложенные ходы
            skip2s(in);
        }
    }

    private JSONObject toJSON(Vertex vertex, int transCode) {
        JSONObject object = new JSONObject();
        if (vertex.edges != null) {
            boolean subExists = false;
            for (Edge e : vertex.edges) {

                // Обратная трансформация хода
                int square = Layout.transSquare(e.square, transCode);

                // Ключ узла
                String key = "" + (char) ('a' + (square & 0xf)) + (15 - (square >> 4));
                if (e.vertex.edges == null) {
                    // Записать состояние
                    object.put(key, Math.abs(e.vertex.state));
                    continue;
                }

                // Код трансформации
                int nextTransCode = Layout.transMultiple(
                        e.vertex.transCode(vertex.makeMove(e.square)), transCode);

                // Записать следующий узел
                subExists = true;
                JSONObject child = toJSON(e.vertex, nextTransCode);
                if (child != null) {
                    object.put(key, child);
                } else {
                    object.put(key, Math.abs(e.vertex.state));
                }

                // Для черных только один ход
                if (vertex.hand == Layout.BLACK) {
                    break;
                }
            }
            if (vertex.hand == Layout.WHITE && !subExists) {
                return null;
            }

        }
        return object;
    }

    private void fromJSON(JSONObject object, Vertex vertex) {
        int size = object.length();
        vertex.edges = new Edge[size];
        int i = 0;
        for (String key : object.keySet()) {
            int square = 0;
            if (key != null && key.length() > 1) {
                square = ((15 - Integer.parseInt(key.substring(1))) << 4)
                        | ((int) key.charAt(0) - 'a');
            }
            Edge e = vertex.makeEdge(square, this, true);
            Object item = object.get(key);
            if (item instanceof JSONObject) {
                fromJSON((JSONObject) item, e.vertex);
            } else {
                e.vertex.state = object.optInt(key);
                if (e.vertex.hand == Layout.WHITE) {
                    e.vertex.state = -e.vertex.state;
                }
            }
            vertex.edges[i] = e;
            i++;
        }
    }

    // Поместить вершину в коллекцию
    public Vertex put(Vertex e) {
        return vertexes.put(e, e);
    }

    // Получить вершину из коллекции, если есть
    public Vertex get(Vertex e) {
        return vertexes.get(e);
    }

    // Размер коллекции
    @Override
    public int size() {
        return vertexes.size();
    }

    // Пустая ли коллекция
    @Override
    public boolean isEmpty() {
        return vertexes.isEmpty();
    }

    // Содержиться ли объект в коллекции
    @Override
    public boolean contains(Object o) {
        return vertexes.containsKey((Vertex) o);
    }

    // Перебор элементов коллекции
    @Override
    public Iterator<Vertex> iterator() {
        return vertexes.keySet().iterator();
    }

    // Добавить элемент в коллекцию
    @Override
    public boolean add(Vertex e) {
        return vertexes.put(e, e) == null;
    }

    // Удалить элемент из коллекции
    @Override
    public boolean remove(Object o) {
        return vertexes.remove((Vertex) o) == o;
    }

    // Очистить коллекций
    @Override
    public void clear() {
        vertexes.clear();
    }

}
