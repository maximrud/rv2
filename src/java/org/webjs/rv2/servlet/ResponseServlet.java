package org.webjs.rv2.servlet;

import javax.servlet.annotation.WebServlet;
import org.json.JSONObject;
import org.webjs.rv2.Config;
import org.webjs.rv2.Vertex;
import org.webjs.rv2.Layout;
import org.webjs.rv2.Position;
import org.webjs.rv2.Store;

/**
 * Предожение по продолжению
 *
 * @author rmr
 */
@WebServlet(name = "response", urlPatterns = {"/response"})
public class ResponseServlet extends SolutionServlet {

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
        Vertex v = Vertex.compute(p,
                p.hand == Layout.BLACK ? Layout.WHITE : Layout.BLACK,
                true, getConfig(req), store);
        writeStore();
        return v;
    }

}
