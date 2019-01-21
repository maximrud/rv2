package org.webjs.rv2.servlet;

import javax.servlet.annotation.WebServlet;
import org.json.JSONObject;
import org.webjs.rv2.Layout;
import org.webjs.rv2.Position;

/**
 *
 * @author rmr
 */
@WebServlet(name = "rotate", urlPatterns = {"/rotate"})
public class RotateServlet extends SolutionServlet {

    @Override
    protected Layout processPosition(JSONObject req, Position p) {
        p.rotate();
        return super.processPosition(req, p);
    }
    
}
