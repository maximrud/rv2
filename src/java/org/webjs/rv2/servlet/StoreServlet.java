package org.webjs.rv2.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import org.json.JSONObject;

/**
 * Возвращате позицию
 *
 * @author rmr
 */
@WebServlet(name = "store", urlPatterns = {"/store"})
public class StoreServlet extends SolutionServlet {

    @Override
    protected JSONObject processRequest(HttpServletRequest request)
            throws ServletException, IOException {
        if (storeFileName == null) {
            storeFileName = storeFileName(request);
        }
        readStore();
        return store.toJSON();
    }

}
