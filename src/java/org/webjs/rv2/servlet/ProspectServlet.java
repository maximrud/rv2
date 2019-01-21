package org.webjs.rv2.servlet;

import javax.servlet.annotation.WebServlet;
import org.json.JSONObject;
import org.webjs.rv2.Config;
import org.webjs.rv2.Vertex;
import org.webjs.rv2.Layout;
import org.webjs.rv2.Position;
import org.webjs.rv2.Store;
import static org.webjs.rv2.servlet.SolutionServlet.store;

/**
 * Предожение по продолжению
 * @author rmr
 */
@WebServlet(name = "prospect", urlPatterns = {"/prospect"})
public class ProspectServlet extends SolutionServlet {

    @Override
    protected Position processRecord(JSONObject req, byte moves[]) {
        if (store == null) {
            store = new Store();
        }
        store.merge(moves);
        return super.processRecord(req, moves);
    }

    
    
    @Override
    protected Vertex processLayout(JSONObject req, Layout p) {
        Vertex v = Vertex.compute(p, p.hand, true, getConfig(req), store);
        writeStore();
        return v;
    }
    
}
