package org.webjs.rv2;

/**
 * Ребра графа
 *
 * @author rmr
 */
public class Edge implements Comparable {

    public Edge(int square, Vertex vertex) {
        this.square = square;
        this.vertex = vertex;
    }

    // Ход
    public int square;

    // Следующая вершина
    public Vertex vertex;

    @Override
    public int compareTo(Object o) {
        return vertex.compareTo(((Edge) o).vertex);
    }

}
