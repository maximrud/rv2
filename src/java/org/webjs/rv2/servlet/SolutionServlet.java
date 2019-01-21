package org.webjs.rv2.servlet;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.stream.Stream;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONArray;
import org.json.JSONObject;
import org.webjs.data.JsonServlet;
import org.webjs.rv2.Config;
import org.webjs.rv2.Edge;
import org.webjs.rv2.Figure;
import org.webjs.rv2.Layout;
import org.webjs.rv2.Line;
import org.webjs.rv2.Pattern;
import org.webjs.rv2.Position;
import org.webjs.rv2.Store;
import org.webjs.rv2.Vertex;

/**
 * Возвращате позицию
 *
 * @author rmr
 */
public class SolutionServlet extends JsonServlet {

    protected byte[] movesFromJson(JSONArray array) {
        byte[] r = new byte[array.length()];
        for (int i = 0; i < r.length; i++) {
            r[i] = (byte) array.getInt(i);
        }
        return r;
    }

    protected Config configFromJson(JSONObject obj) {
        Config config = new Config();
        config.computetarget = obj.optInt("computetarget", config.computetarget);
        config.estimatedepth = obj.optInt("estimatedepth", config.estimatedepth);
        config.estimatesize = obj.optInt("estimatesize", config.estimatesize);
        config.computedepth = obj.optInt("computedepth", config.computedepth);
        config.computesize = obj.optInt("computesize", config.computesize);
        config.computeedges = obj.optInt("computeedges", config.computeedges);
        config.computebrute = obj.optInt("computebrute", config.computebrute);
        return config;
    }

    protected JSONArray movesToJson(byte[] moves) {
        if (moves != null) {
            JSONArray array = new JSONArray();
            for (int i = 0; i < moves.length; i++) {
                array.put(moves[i] & 0xff);
            }
            return array;
        }
        return null;
    }

    protected JSONArray movesToJson(Set<Integer> moves) {
        if (moves != null) {
            JSONArray array = new JSONArray();
            moves.forEach((move) -> {
                array.put(move);
            });
            return array;
        }
        return null;
    }

    protected JSONArray movesToJson(Stream<Integer> moves) {
        if (moves != null) {
            JSONArray array = new JSONArray();
            moves.forEach(move -> {
                array.put(move);
            });
            return array;
        }
        return null;
    }

    protected JSONObject patternToJson(Pattern pattern) {
        JSONObject obj = new JSONObject();
        obj.put("type", pattern.type);
        obj.put("rating", pattern.rating);
        obj.put("length", pattern.length);
        return obj;
    }

    protected JSONObject figureToJson(Figure figure) {
        if (figure == null) {
            return null;
        }
        JSONObject obj = new JSONObject();
        obj.put("hand", figure.hand);
        obj.put("number", figure.number);
        obj.put("offset", figure.offset);
        obj.put("pattern", patternToJson(figure.pattern));
        obj.put("moves", movesToJson(figure.moves()));
        obj.put("gains", movesToJson(figure.gains()));
        obj.put("downs", movesToJson(figure.downs()));
        obj.put("rifts", movesToJson(figure.rifts()));
        return obj;
    }

    protected JSONArray figuresToJson(Figure[] figures) {
        JSONArray array = new JSONArray();
        for (Figure figure : figures) {
            array.put(figureToJson(figure));
        }
        return array;
    }

    protected JSONObject lineToJson(Line line) {
        JSONObject obj = new JSONObject();
        obj.put("number", line.number);
        obj.put("blen", line.blen);
        obj.put("wlen", line.wlen);
        obj.put("stroke", line.stroke);
        return obj;
    }

    protected JSONArray linesToJson(Line[] lines) {
        JSONArray obj = new JSONArray();
        for (Line line : lines) {
            obj.put(lineToJson(line));
        }
        return obj;
    }

    protected JSONObject layoutToJson(Layout layout) {
        JSONObject obj = new JSONObject();
        obj.put("hand", layout.hand);
        obj.put("lines", linesToJson(layout.lines));
        obj.put("figures", figuresToJson(layout.figures));
        obj.put("top", figureToJson(layout.top()));
        return obj;
    }

    protected JSONObject positionToJson(Position position) {
        JSONObject obj = new JSONObject();
        obj.put("blacks", movesToJson(position.blacks));
        obj.put("whites", movesToJson(position.whites));
        return obj;
    }

    protected JSONObject graphToJson(Vertex dag) {
        JSONObject obj = new JSONObject();
        obj.put("state", dag.state);
        obj.put("moves", movesToJson(dag.moves()));
        obj.put("rating", dag.rating);
        return obj;
    }

    protected Vertex processLayout(JSONObject req, Layout p) {
        Vertex v = new Vertex(p, p.hand);
        Vertex v0 = store != null ? store.get(v) : null;
        if (v0 == null || v0.edges == null || v0.state == -128) {
            Config config = getConfig(req);
            v = Vertex.estimate(p, p.hand, true, config);
            if (v.state == 0) {
                v = Vertex.estimate(p,
                        p.hand == Layout.BLACK ? Layout.WHITE : Layout.BLACK,
                        true, config);
            }
        } else {
            v.state = v0.state;
            v.edges = new Edge[v0.edges.length];
            int transCode = v0.transCode(v);
            for (int i = 0; i < v.edges.length; i++) {
                v.edges[i] = new Edge(Layout.transSquare(v0.edges[i].square, 
                        transCode), v0.edges[i].vertex);
            }
            System.out.println("transCode=" + transCode);
        } 
        return v;
    }

    protected Layout processPosition(JSONObject req, Position p) {
        return new Layout(p);
    }

    protected Position processRecord(JSONObject req, byte moves[]) {
        return new Position(moves);
    }

    @Override
    protected JSONObject processRequest(HttpServletRequest request)
            throws ServletException, IOException {
        
//        if (Pattern.PRECALCED_PATTERNS == null) {
//            String precalcFileName = request.getServletContext().getRealPath("lines.dat");
//            try (InputStream in = new FileInputStream(precalcFileName)) {
//                Pattern.PRECALCED_PATTERNS = Pattern.readPrecalc(in);
//            } catch(IOException e) {
//                Pattern.PRECALCED_PATTERNS = Pattern.precalc();
//                try (OutputStream out = new FileOutputStream(precalcFileName)) {
//                    Pattern.writePrecalc(out, Pattern.PRECALCED_PATTERNS);
//                }
//            }
//        }
//            
        if (storeFileName == null) {
            storeFileName = storeFileName(request);
        }
        readStore();
        long start = System.currentTimeMillis();
        JSONObject req = requestContent(request);
        byte[] moves = movesFromJson(req.getJSONArray("record"));
        Position p = processRecord(req, moves);
        Layout l = processPosition(req, p);
        Vertex v = processLayout(req, l);
        JSONObject obj = new JSONObject();
        obj.put("layout", layoutToJson(l));
        obj.put("graph", graphToJson(v));
        System.out.println("Elapsed time: " + (System.currentTimeMillis() - start) + " ms");
        return obj;
    }
    
    protected Config getConfig(JSONObject req) {
        if (req.has("config")) {
            return configFromJson(req.getJSONObject("config"));
        } else
            return new Config();
    }

    protected String storeFileName(HttpServletRequest request) {
        return request.getServletContext().getRealPath("store.dat");
    }

    protected void readStore() {
        if (store == null) {
            try (InputStream in = new FileInputStream(storeFileName)) {
                store = new Store();
                store.read(in);
            } catch (IOException e) {
                store = null;
            }
        }
    }

    protected void writeStore() {
        try (OutputStream out = new FileOutputStream(storeFileName)) {
            store.write(out);
        } catch (IOException e) {
        }
    }
    
    protected static Store store = null;

    protected static String storeFileName = null;

}
