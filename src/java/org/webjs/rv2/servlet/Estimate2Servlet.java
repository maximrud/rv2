package org.webjs.rv2.servlet;

import javax.servlet.annotation.WebServlet;
import org.json.JSONObject;
import org.webjs.rv2.Apex;
import org.webjs.rv2.Config;
import org.webjs.rv2.Edge;
import org.webjs.rv2.Layout;
import org.webjs.rv2.Vertex;
import static org.webjs.rv2.servlet.SolutionServlet.store;

/**
 *
 * @author rmr
 */
@WebServlet(name = "estimate2", urlPatterns = {"/estimate2"})
public class Estimate2Servlet extends SolutionServlet {

    @Override
    protected Vertex processLayout(JSONObject req, Layout p) {
        Vertex v = new Vertex(p, p.hand);
        Vertex v0 = store != null ? store.get(v) : null;
        if (v0 == null || v0.edges == null || v0.state == -128) {
            v = Apex.estimate(p, p.hand, true, Config.defaults());
            if (v.state == 0) {
                v = Apex.estimate(p,
                        p.hand == Layout.BLACK ? Layout.WHITE : Layout.BLACK,
                        true, getConfig(req));
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
    
}
