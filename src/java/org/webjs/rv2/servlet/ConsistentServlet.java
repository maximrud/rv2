package org.webjs.rv2.servlet;

import javax.servlet.annotation.WebServlet;
import org.json.JSONObject;
import org.webjs.rv2.Layout;
import org.webjs.rv2.Vertex;
import static org.webjs.rv2.servlet.SolutionServlet.store;

/**
 *
 * @author rmr
 */
@WebServlet(name = "consistent", urlPatterns = {"/consistent"})
public class ConsistentServlet extends SolutionServlet {

    @Override
    protected Vertex processLayout(JSONObject req, Layout p) {
        Vertex v = new Vertex(p, p.hand);
        Vertex v0 = store != null ? store.get(v) : null;
        if (v0 != null && v0.state != -128) {
            v0.consistent(getConfig(req)); 
        } 
        return v0;
    }
    
}
