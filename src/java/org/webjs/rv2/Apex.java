package org.webjs.rv2;

import java.util.Arrays;

/**
 * Вершины графаs
 *
 * @author rmr
 */
public class Apex extends Vertex {

    // Вычисление по дереву решений
    public static Vertex compute(Layout layout, int attacker, boolean alledges, Config config) {
        Apex root = new Apex(layout, attacker);
        Store store = new Store();
        root.prepare(true);
        root.expand(store, true, config, true);
        // int startn = root.countStones();
        while (store.size() < config.computesize) {
            if (root.state != 0) {
                break;
            }

            // Получаем самую проверенную вершину
            Apex v = root.next();

            // Для печати
            int n = v.count + 1;
            String nextS = root.nextS();
            System.out.println(">> Moves [" + nextS + "] Hand [" + n + ":" + v.hand
                    + "] State [" + v.state + ":" + v.defenses + ":" + v.attacks
                    + "] Size [" + store.size() + "] Root ["
                    + root.state + ":" + root.defenses + ":" + root.attacks + "]");

            // Расширяем следующий уровень
            v.expand(store, false, config, true);

            // Оцениваем максимальное число ходов вглубину
            // if (v.state == 0 && startn - n >= config.computedepth) {
            //    v.state = v.hand == attacker ? -1 : 1;
            // }

            // Пересчитываем состояние родителей
            v.checkUp(config);

            System.out.println("<< Moves [" + nextS + "] Hand [" + n + ":" + v.hand
                    + "] State [" + v.state + ":" + v.defenses + ":" + v.attacks
                    + "] Size [" + store.size() + "] Root ["
                    + root.state + ":" + root.defenses + ":" + root.attacks + "]");
        }
        if ((attacker == layout.hand && root.state < 0)
                || (attacker != layout.hand && root.state > 0)) {
            root.state = 0;
        }
        store.clear();
        return root;
    }

    // Вычисление по дереву решений
    public static Vertex estimate(Layout layout, int attacker, boolean alledges, Config config) {
        Apex root = new Apex(layout, attacker);
        Store store = new Store();
        root.prepare(true);
        root.expand(store, alledges, config, false);
        int startn = root.count;
        while (store.size() < config.computesize) {
            if (root.state != 0) {
                break;
            }

            // Получаем самую проверенную вершину
            Apex v = root.next();

//            int n = v.countStones() + 1;
//            String nextS = root.nextS();
//            System.out.println(">> Moves [" + nextS + "] Hand [" + n + ":" + v.hand
//                    + "] State [" + v.state + ":" + v.defenses + ":" + v.attacks
//                    + "] Size [" + store.size() + "] Root ["
//                    + root.state + ":" + root.defenses + ":" + root.attacks + "]");

            // Расширяем следующий уровень
            v.expand(store, false, config, false);

            // Оцениваем максимальное число ходов вглубину
            int n = v.count + 1;
             if (v.state == 0 && n - startn >=  config.estimatedepth) {
                v.state = v.hand == attacker ? -1 : 1;
             }

            // Пересчитываем состояние родителей
            v.checkUp(config);

//            System.out.println("<< Moves [" + nextS + "] Hand [" + n + ":" + v.hand
//                    + "] State [" + v.state + ":" + v.defenses + ":" + v.attacks
//                    + "] Size [" + store.size() + "] Root ["
//                    + root.state + ":" + root.defenses + ":" + root.attacks + "]");
        }
        if ((attacker == layout.hand && root.state < 0)
                || (attacker != layout.hand && root.state > 0)) {
            root.state = 0;
        }
        System.out.println("Store size " + store.size());
        store.clear();
        return root;
    }

    // Количество защищающихся позиций
    public int defenses;

    // Количество атакующих позиций
    public int attacks;

    // Родители
    public Apex[] parents;

    // Создание вершины
    public Apex(Layout layout, int attacker) {
        super(layout, attacker);
    }

    // Подготовка оценки позиции
    @Override
    public void prepare(boolean compute) {
        super.prepare(compute);
        defenses = 1;
        attacks = 1;
    }

    // Создать ребро с вершиной 
    @Override
    public Edge makeEdge(int square, Store store, boolean compute) {
        Apex p = new Apex(makeMove(square), attacker);
        Apex v = (Apex) store.get(p);
        if (v == null) {
            p.prepare(compute);
            store.put(p);
            v = p;
        } else if (v.state == -128) {
            v.prepare(compute);
            v.edges = null;
        }
        v.addParent(this);
        return new Edge(square, v);

    }

    // Сравнение состояний
    // В оценке неизвестного состояния участвует число ходов
    @Override
    public int compareTo(Object o) {
        Apex v = (Apex) o;
        if (v == null) {
            return -1;
        }
        if (v.state == 0 && state == 0) {
            if (attacker == hand) {
                if (v.attacks == attacks) {
                    return rating - v.rating;
                } else {
                    return attacks - v.attacks;
                }
            } else {
                if (v.defenses == defenses) {
                    return rating - v.rating;
                } else {
                    return defenses - v.defenses;
                }
            }
        }
        if ((v.state >= 0 && state <= 0)
                || (v.state <= 0 && state >= 0)) {
            return state - v.state;
        } else {
            return v.state - state;
        }
    }

    // Получение наиболее проверенной вершины
    public Apex next() {
        if (edges != null && edges.length > 0) {
            return ((Apex) edges[0].vertex).next();
        } else {
            return this;
        }
    }

    // Ходы до наиболее проверенной вершины
    public String nextS() {
        if (edges != null && edges.length > 0) {
            Edge e = edges[0];
            Vertex v = e.vertex;
            return " " + (e.square >> 4) + ":" + (e.square & 0xf)
                    + ((Apex) v).nextS();
        } else {
            return " ";
        }
    }

    // Расчет показателей
    public void recalc(Config config) {
        if (state == 0 && edges != null && edges.length > 0) {
            Apex top = (Apex) edges[0].vertex;
            if (attacker == hand) {
                // Обновить счетчики
                defenses = top.defenses * config.computeweight;
                attacks = 0;
                for (Edge e : edges) {
                    if (e.vertex.state == 0) {
                        attacks = attacks + ((Apex) e.vertex).attacks;
                    }
                }
            } else {
                attacks = top.attacks * config.computeweight;
                defenses = 0;
                for (Edge e : edges) {
                    if (e.vertex.state == 0) {
                        defenses = defenses + ((Apex) e.vertex).defenses;
                    }
                }
            }
        }
    }
    
    // Пересчет состояния родителей
    public void checkUp(Config config) {
        recalc(config);
        if (parents != null) {
            for (Apex v : parents) {
                v.check();
                v.checkUp(config);
            }
        }
    }

    // Добавить родительскую связь
    public void addParent(Apex vertex) {
        if (parents == null) {
            parents = new Apex[]{vertex};
        } else {
            int size = parents.length;
            parents = Arrays.copyOf(parents, size + 1);
            parents[size] = vertex;
        }
    }

    // Разорвать родительскую связь
    public void removeParent(Apex vertex) {
        if (parents != null) {
            int size = parents.length;
            for (int i = 0; i < size; i++) {
                if (parents[i] == vertex) {
                    if (size == 1) {
                        parents = null;
                    } else {
                        Apex[] r = new Apex[size - 1];
                        System.arraycopy(parents, 0, r, 0, i);
                        System.arraycopy(parents, i + 1, r, i, size - i - 1);
                        parents = r;
                    }
                    return;
                }
            }
        }
    }

}
