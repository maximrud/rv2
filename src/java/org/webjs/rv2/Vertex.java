package org.webjs.rv2;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Вершины графа
 *
 * @author rmr
 */
public class Vertex extends Layout implements Comparable {

    // Состояние решения
    // Ноль - неизвестно, положительное - выигрышь, отрицательно - проигрышь
    // Колиство ходов до проигрыша или выигрыша на один меньше состояния
    public int state;

    // Ходы решения
    public Edge[] edges;

    // Перспективность позиции
    public int rating;

    // Атакующая сторона
    public int attacker;

    // Провексти оценку позиции
    public static Vertex estimate(Layout layout, int attacker, boolean alledges, Config config) {
        Vertex root = new Vertex(layout.alignType(Pattern.OPEN_TWO), attacker);
        root.prepare(false);
        Store store = new Store();
        int target = config.computetarget - root.count + 1;
        int count = Math.max(Math.min(target - 2, config.estimatedepth), 0);
        for (int level = 0; level <= count; level++) {
            root.estimate(level, store, alledges, config);
            // System.out.println("Level: " + level + " Size: " + store.size());
            if (root.state != 0 || store.size() > config.estimatesize) {
                break;
            }
        }
        if ((attacker == layout.hand && root.state < 0)
                || (attacker != layout.hand && root.state > 0)) {
            root.state = 0;
        }
        if (Math.abs(root.state) > target) {
            root.state = 0;
        }
        // System.out.println("Store size " + store.size());
        store.clear();
        return root;
    }

    // Посчитать простые ходы
    public static Vertex compute(Layout layout, int attacker, boolean alledges, Config config) {
        Store store = new Store();
        Vertex v = compute(layout, attacker, alledges, config, store);
        store.clear();
        return v;
    }

    // Посчитать простые ходы
    public static Vertex compute(Layout layout, int attacker, boolean alledges, Config config, Store store) {
        Vertex root = new Vertex(layout, attacker);
        Vertex v = store.get(root);
        if (v != null) {
            root = v;
            root.edges = null;
        } else {
            store.put(root);
        }
        root.prepare(true);
        int start = attacker == layout.hand ? 1 : 2; // По два хода вглубину
        for (int level = start; level <= config.computedepth; level = level + 2) {
            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
            root.compute(level, store, alledges, config);
            System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
            System.out.println("Level [" + level + "] Rating [" + root.state + ":"
                    + (root.state == 0 ? (root.rating >> 8) + ":" + (root.rating & 0xff) : "0") + "]");
            if (root.state != 0 || store.size() > config.computesize) {
                break;
            }
        }
        if ((attacker == layout.hand && root.state < 0)
                || (attacker != layout.hand && root.state > 0)) {
            root.state = 0;
        }
        store.normalize();
        return root;
    }

    // Создание вершины
    public Vertex(Layout layout, int attacker) {
        super(layout.lines, layout.hand, layout.figures, layout.type, layout.count);
        this.attacker = attacker;
    }

    // Создание вершины
    public Vertex(Position position, int type, int attacker) {
        super(position, type);
        this.attacker = attacker;
    }

    // Создание вершины
    public Vertex(Position position, int attacker) {
        super(position);
        this.attacker = attacker;
    }

    // Создание вершины
    public Vertex(byte[] moves, int type, int attacker) {
        super(moves, type);
        this.attacker = attacker;
    }

    // Создание вершины
    public Vertex(byte[] moves, int attacker) {
        super(moves);
        this.attacker = attacker;
    }

    // Создание вершины
    public Vertex(Line[] lines, int hand, Figure[] figures, int type, int count, int attacker) {
        super(lines, hand, figures, type, count);
        this.attacker = attacker;
    }

    // Получить посчитанные ходы
    public Set<Integer> moves() {
        if (edges == null) {
            if (state > 0) {
                if (state < 4) {
                    return gains(Pattern.FOUR);
                } else if (state < 6) {
                    return gains(Pattern.OPEN_THREE);
                }
            } else if (state < 0) {
                if (state > -5) {
                    return downs(Pattern.OPEN_FOUR);
                }
            }
        } else {
            Set<Integer> r = new HashSet<>();
            if (edges.length > 0) {
                Vertex top = edges[0].vertex;
                for (Edge e : edges) {
                    if (e.vertex.state != top.state) {
                        break;
                    }
                    r.add(e.square);
                }
            }
            return r;
        }
        return null;
    }

    // Подготовка оценки позиции
    public void prepare(boolean compute) {
        Figure top = top();
        if (top != null) {
            // Свой ход: пять, четыре или открытая три
            if (top.hand == hand) {
                switch (top.pattern.type) {
                    // Победа
                    case Pattern.FIVE:
                        state = 1;
                        break;
                    // Победа одним ходом
                    case Pattern.OPEN_FOUR:
                    case Pattern.FOUR:
                        state = 2;
                        break;
                    // Победа в три хода
                    case Pattern.OPEN_THREE:
                        state = 4;
                        break;
                    case Pattern.THREE:
                    case Pattern.OPEN_TWO:
                        // Мы атакуем - будет продолжение
                        state = attacker == hand || compute ? 0 : 1;
                        break;
                    // Нет фигур для продолжения
                    default:
                        state = compute ? 0 : attacker == hand ? -1 : 1;
                }
            } else {
                // Чужеой ход
                switch (top.pattern.type) {
                    // Проигрышь
                    case Pattern.FIVE:
                        state = -1;
                        break;
                    // Проигрышь через два хода
                    case Pattern.OPEN_FOUR:
                        state = -3;
                        break;
                    // Будем защищаться
                    case Pattern.FOUR:
                    case Pattern.OPEN_THREE:
                        state = 0;
                        break;
                    // Нет фигур для продолжения
                    default:
                        state = compute ? 0 : attacker == hand ? -1 : 1;
                }
            }
        } else {
            // Нет фигур для продолжения
            state = compute ? 0 : attacker == hand ? -1 : 1;
        }
        // Рассчитаем рейтинг
        rating = rate() << 8;
    }

    // Построить дочерние узлы
    public void expand(Store store, boolean alledges, Config config, boolean compute) {
        if (compute) {
            // Пробуем атаку на своем ходе
            Vertex va = estimate(this, hand, false, config);
            if (va.state > 0) {
                state = va.state;
                return;
            }
        }
        // Рассчитать возможные ходы
        Set<Integer> moves;
        Figure top = top();
        // По уровню угрозы
        switch (top.pattern.type) {
            // Пробуем делать вынужденные ходы и защищаться
            case Pattern.FOUR:
                moves = downs(Pattern.FOUR);
                break;
            // Пробуем атаковать и/или защищаться
            case Pattern.OPEN_THREE:
                moves = gains(Pattern.THREE);
                moves.addAll(downs(Pattern.OPEN_THREE));
                break;
            default:
                if (compute) {
                    if (attacker == hand) {
                        // Подбираем простые входы для скрытой атаки
                        moves = gains(Pattern.ONE);
                        moves.addAll(downs(Pattern.TWO));
                    } else {
                        // Определяем перечень допустимых ходов
                        // методом пропуска хода
                        Layout l = swapHand(Pattern.OPEN_TWO);
                        // Ищем только одно решение
                        Vertex v = estimate(l, l.hand, false, config);
                        if (v.state > 0) {
                            // Определение области, зависимых ходов
                            moves = v.relations();
                            // Добавить ходы повышения независимых фигур
                            moves.addAll(gains(Pattern.TWO));
                        } else {
                            // Не выигрыша - ход ответ может быть любой
                            if (config.computebrute > 0) {
                                moves = availables();
                            } else {
                                state = 1;
                                return;
                            }
                        }
                    }
                } else {
                    // Пробуем атаковать - мы атакующая сторона
                    moves = gains(Pattern.OPEN_TWO);
                }
        }
        edges = new Edge[moves.size()];
        int size = 0;
        for (int square : moves) {
            // Создать ребро с вершиной 
            Edge e = makeEdge(square, store, compute);
            // Проверим достаточную глубину поиска
            int target = config.computetarget - count + 1;
            if (Math.abs(e.vertex.state) > target) {
                e.vertex.state = 0;
                e.vertex.edges = null;
            }
            // Выигрышный ход - достаточно
            if (!alledges && e.vertex.state < 0) {
                edges = new Edge[]{e};
                break;
            }
            edges[size] = e;
            size++;
        }
        check();
        // Обрезать число ходов для продолжения поиска атаки
        if (compute && attacker == hand && edges != null
                && edges.length > config.computeedges) {
            edges = Arrays.copyOf(edges, config.computeedges);
        }
    }

    // Создать вершину
    @Override
    public Vertex makeMove(int square) {
        return new Vertex(super.makeMove(square), attacker);
    }

    // Создать ребро с вершиной 
    public Edge makeEdge(int square, Store store, boolean compute) {
        Vertex p = makeMove(square);
        Vertex v = store.get(p);
        if (v == null) {
            p.prepare(compute);
            store.put(p);
            v = p;
        } else if (v.state == -128) {
            // Неподготовленный узел
            v.prepare(compute);
            v.edges = null;
        }
        return new Edge(square, v);
    }

    // Оценка выполнения вынужденных ходов
    public void estimate(int level, Store store, boolean alledges, Config config) {
        // Построение дочернего уровня 
        if (state == 0 && edges == null && level > 0 && (store.size() >> 1) <= config.estimatesize) {
            expand(store, alledges, config, false);
        }
        // Итерационный вызов дочернего уровня
        if (state == 0 && edges != null && level > 1) {
            // Вызов оценки позции на уровень ниже в порядке рейтинга
            for (Edge e : edges) {
                Vertex v = e.vertex;
                // На уровень ниже нужен только один ход
                v.estimate(level - 1, store, false, config);
                // Найден победный ход или для защиты найден не проигранный вариант
                if (!alledges && (v.state < 0
                        // Здесь быстрее ищет победу если != WIN и быстрее защиту 
                        || (attacker != hand && v.state == 0))) {
                    break;
                }
            }
            check();
        }
    }

    // Расчет ходов без условия обязательности
    public void compute(int level, Store store, boolean alledges, Config config) {
        if (state == 0 && edges == null && level > 0 && (store.size() >> 1) <= config.computesize) {
            expand(store, alledges, config, true);
        }
        if (state == 0 && edges != null && level > 1) {
            // Для печати
            int n = count + 1;
            StringBuilder sb = new StringBuilder(n);
            for (int i = 0; i < n; ++i) {
                sb.append(">>");
            }
            String indent = sb.toString();
            for (Edge e : edges) {
                Vertex v = e.vertex;
                if (v.state == 0 && n <= 18) {
                    System.out.println(indent + " Hand [" + n + ":" + hand
                            + "] Square [" + (e.square >> 4) + ":" + (e.square & 0xf)
                            + "] Rating [" + v.state + ":" + (v.rating >> 8) + ":" + (v.rating & 0xff) + "] Size [" + store.size() + "]");
                }

                // На уровень ниже нужно только одно решение
                v.compute(level - 1, store, false, config);

                if (n <= 18) {
                    System.out.println(indent.replace('>', '<') + " Hand [" + n + ":" + hand
                            + "] Square [" + (e.square >> 4) + ":" + (e.square & 0xf)
                            + "] Rating [" + v.state + ":" + (v.state == 0 ? (v.rating >> 8) + ":" + (v.rating & 0xff) : "0")
                            + "] Size [" + store.size() + "]");
                }

                // Найден победный ход или для защиты найден не проигранный вариант
                if (!alledges && (v.state < 0
                        // Здесь быстрее ищет победу если != WIN и быстрее защиту == DRAW
                        || (attacker != hand && v.state == 0))) {
                    break;
                }
            }
            check();
            // Пересчитаем рейтинг
            if (attacker != hand && edges != null) {
                rating = rating & 0xffffff00;
                // Рассчитать число непросчитанных дочерних позиций
                // Следующий атакующий ход с наименьшим числом продолжений
                for (Edge e : edges) {
                    if (e.vertex.state == 0) {
                        rating++;
                    }
                }
            }
        }
    }

    // Ответные ходы
    public Set<Integer> relations() {
        Set<Integer> r = new HashSet<>();
        // Пока есть ходы 
        if ((attacker == hand ? state > 0 : state < 0)
                && (edges != null && edges.length > 0)) {
            if (attacker == hand) {
                // По лучшему ходы
                Edge e = edges[0];
                r.add(e.square);
                // Ходы блокировки фигур атаки, образовавшихся после хода
                for (Figure f : e.vertex.figures) {
                    if (hand == f.hand
                            && f.pattern.type <= Pattern.OPEN_THREE
                            && f.contains(e.square)) {
                        r.addAll(f.rifts());
                    }
                }
                // Рекурсивное добавление
                r.addAll(e.vertex.relations());
            } else {
                // По всем ходам защиты 
                for (Edge e : edges) {
                    r.add(e.square);
                    // Ходы повыешение своих фигур, образующихся после хода
                    for (Figure f : e.vertex.figures) {
                        if (hand == f.hand
                                && f.pattern.type <= Pattern.TWO
                                && f.contains(e.square)) {
                            r.addAll(f.gains());
                        }
                    }
                    // Рекурсивное добавление
                    r.addAll(e.vertex.relations());
                }
            }
        }
        return r;
    }

    // Проверка состояния дочерних узлов
    public void check() {
        if (edges != null && edges.length > 0 && state != -128) {
            // Сортировка дочерних узлов по рейтингу
            Arrays.sort(edges);
            // Выбираем лучший узел из дочерних
            Vertex top = edges[0].vertex;
            if (attacker == hand) {
                if (top.state < 0 && top.state != -128) {
                    // Найден победный ход
                    state = -top.state + 1;
                    // Оставляем позиции с лучним ходом
                    int size = 0;
                    for (Edge edge : edges) {
                        if (edge.vertex.state != top.state) {
                            break;
                        }
                        size++;
                    }
                    if (size != edges.length) {
                        edges = Arrays.copyOf(edges, size);
                    }
                } else if (top.state > 0) {
                    // Перебор вариантов завершен, выигрыша нет
                    state = -1;
                    edges = null;
                }
            } else {
                if (top.state < 0 && top.state != -128) {
                    // Найдена ничья                    
                    state = 1;
                    edges = null;
                } else if (top.state > 0) {
                    // Перебор ходов завершен, позиция проиграна
                    state = -top.state - 1;
                    // Оставляем позиции с лучшим ходом и имеющие дочерние 
                    int size = 0;
                    Edge[] es = new Edge[edges.length];
                    for (Edge edge : edges) {
                        if (edge.vertex.edges != null
                                || edge.vertex.state == top.state) {
                            es[size] = edge;
                            size++;
                        }
                    }
                    if (size != edges.length) {
                        edges = Arrays.copyOf(es, size);
                    }
                }
            }
        }
    }

    // Проверка консистентности данных по вершине
    public boolean consistent(Config config) {
        // Для печати
        int n = count + 1;
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; ++i) {
            sb.append(">>");
        }
        String indent = sb.toString();
        if (hand == attacker) {
            // Атакующий должен выиграть
            if (state <= 0) {
                System.out.print("Attacker state <= 0: ");
                return false;
            }
            // Если нет дочерних элементов, выигрыш должен быть найден
            if (edges == null || edges.length == 0) {
                Vertex va = estimate(this, hand, false, config);
                if (va.state != state) {
                    System.out.print("Estimate state != state: ");
                    return false;
                }
            } else {
                // Проверяем все победные ходы
                for (Edge edge : edges) {
                    if (n <= 16) {
                        System.out.println(indent + " Hand [" + n + ":" + hand
                                + "] Square [" + (edge.square >> 4) + ":" + (edge.square & 0xf)
                                + "] Rating [" + edge.vertex.state + ":" + (edge.vertex.rating >> 8) + ":" + (edge.vertex.rating & 0xff) + "]");
                    }
                    Vertex top = edge.vertex;
                    if (state != -top.state + 1) {
                        System.out.print("Top child state + 1 != state: ["
                                + (edge.square >> 4) + ":"
                                + (edge.square & 0xf) + "] ");
                        return false;
                    }
                    if (!top.consistent(config)) {
                        System.out.print("["
                                + (edge.square >> 4) + ":"
                                + (edge.square & 0xf) + "] ");
                        return false;
                    }
                }
            }
        } else {
            // Защищающийся должен проиграть
            if (state >= 0) {
                System.out.print("Defender state >= 0: ");
                return false;
            }
            // Проверяем дочерние узлы
            if (edges == null) {
                // Пробуем сразу проверить на проигрышь
                Vertex va = estimate(this, hand == BLACK ? WHITE : BLACK, false, config);
                if (va.state != state) {
                    System.out.print("Estimate state != state: ");
                    return false;
                }
            } else {
                // Преверим самый успешный ход
                Vertex top = edges[0].vertex;
                if (state != -top.state - 1) {
                    System.out.print("Top child state + 1 != state: ["
                            + (edges[0].square >> 4) + ":"
                            + (edges[0].square & 0xf) + "] ");
                    return false;
                }
                // Проверяем все доступные ходы
                Set<Integer> moves = availables();
                int size = 0;
                for (int square : moves) {
                    // Ищем ход в списке граней
                    Edge edge = null;
                    for (Edge edge1 : edges) {
                        if (edge1.square == square) {
                            edge = edge1;
                            break;
                        }
                    }
                    if (edge != null) {
                        // Проверяем только при отсутствии трансформации
                        if (edge.vertex.transCode(makeMove(edge.square)) == 0) {
                            if (n <= 16) {
                                System.out.println(indent + " Hand [" + n + ":" + hand
                                        + "] Square [" + (edge.square >> 4) + ":" + (edge.square & 0xf)
                                        + "] Rating [" + edge.vertex.state + ":" + (edge.vertex.rating >> 8) + ":" + (edge.vertex.rating & 0xff) + "]");
                            }
                            // Проверка консистентности дочернего хода
                            if (!edge.vertex.consistent(config)) {
                                System.out.print("["
                                        + (edge.square >> 4) + ":"
                                        + (edge.square & 0xf) + "] ");
                                return false;
                            }
                        }
                        size++;
                    } else {
                        // Проверка позиции после хода
                        Vertex va = estimate(makeMove(square), hand == BLACK ? WHITE : BLACK, false, config);
                        if (state > -va.state - 1) {
                            System.out.print("Estimate child state + 1 > state: ["
                                    + (square >> 4) + ":"
                                    + (square & 0xf) + "] ");
                            return false;
                        }
                    }
                }
                // Не все грани доступны для хода
                if (size != edges.length) {
                    for (Edge edge : edges) {
                        if (!moves.contains(edge.square)) {
                            System.out.print("Edges square not availables: ["
                                    + (edge.square >> 4) + ":"
                                    + (edge.square & 0xf) + "] ");
                            return false;
                        }
                    }
                    System.out.print("Unknown edges square not availables: ");
                    return false;
                }
            }
        }
        return true;
    }

    // Сравнение состояний
    // Меньшее значение соответствует наиболее короткому выигрышу или
    // неизвестности с большим рейтингом или наиболее длинному проигрышу
    @Override
    public int compareTo(Object o) {
        Vertex v = (Vertex) o;
        if (v == null) {
            return -1;
        }
        if (v.state == 0 && state == 0) {
            return rating - v.rating;
        }
        if ((v.state >= 0 && state <= 0)
                || (v.state <= 0 && state >= 0)) {
            return state - v.state;
        } else {
            return v.state - state;
        }
    }
}
