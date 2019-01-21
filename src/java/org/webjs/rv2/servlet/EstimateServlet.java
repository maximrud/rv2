package org.webjs.rv2.servlet;

import javax.servlet.annotation.WebServlet;
import org.json.JSONObject;
import org.webjs.rv2.Layout;
import org.webjs.rv2.Vertex;

/**
 *
 * @author rmr
 */
@WebServlet(name = "estimate", urlPatterns = {"/estimate"})
public class EstimateServlet extends SolutionServlet {

    @Override
    protected Vertex processLayout(JSONObject req, Layout p) {
        return super.processLayout(req, p);
    }
    
}
