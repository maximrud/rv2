package org.webjs.data;

import java.io.IOException;
import java.sql.SQLException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONObject;

/**
 * Json VTBI Servlet
 *
 * @author ryabochkin_mr
 */
public abstract class DataServlet extends JsonServlet {

    protected String[] primaryKey;
    protected String methodsAllowed;

    /**
     * Initialize servle config
     *
     * @param config
     * @throws ServletException
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        String value = config.getInitParameter("primaryKey");
        if (value != null) {
            primaryKey = value.replace(" ", "").split(",");
        } else {
            primaryKey = new String[]{"id"};
        }
        methodsAllowed = config.getInitParameter("methodsAllowed");
        if (methodsAllowed != null) {
            methodsAllowed = methodsAllowed.toUpperCase();
        } else {
            methodsAllowed = "ALL";
        }
    }

    /**
     * Make response status text
     *
     * @param status
     * @param error
     * @param message
     * @param confirm
     * @param tranid
     * @return
     */
    protected JSONObject responseStatus(int status, int error, String message, String confirm, String tranid) {
        JSONObject obj = new JSONObject();
        obj.put("status", status);
        obj.put("error", error);
        if (message != null) {
            obj.put("message", message);
        }
        if (confirm != null) {
            obj.put("confirm", confirm);
        }
        obj.put("tranid", tranid);
        return obj;
    }

    /**
     * Make response status text
     *
     * @param status
     * @param error
     * @param message
     * @param confirm
     * @return
     */
    protected JSONObject responseStatus(int status, int error, String message, String confirm) {
        JSONObject obj = new JSONObject();
        obj.put("status", status);
        obj.put("error", error);
        if (message != null) {
            obj.put("message", message);
        }
        if (confirm != null) {
            obj.put("confirm", confirm);
        }
        return obj;
    }

    /**
     * Добавить статус к объекту
     *
     * @param obj
     * @param status
     * @param message
     * @return
     */
    protected JSONObject appendStatus(JSONObject obj, int status, String message) {
        obj.put("status", status);
        obj.put("message", message);
        return obj;
    }

    /**
     * Добавить статус к объекту
     *
     * @param obj
     * @param status
     * @return
     */
    protected JSONObject appendStatus(JSONObject obj, int status) {
        obj.put("status", status);
        obj.put("message", responseStatus(status));
        return obj;
    }

    /**
     * Добавить положительный статус
     *
     * @param obj
     * @return
     */
    protected JSONObject appendStatus(JSONObject obj) {
        int status = HttpServletResponse.SC_OK;
        obj.put("status", status);
        obj.put("message", responseStatus(status));
        return obj;
    }

    /**
     * Разрешен ли указанные метод
     *
     * @param method
     * @return
     */
    protected boolean isMethodAllowed(String method) {
        return methodsAllowed.equalsIgnoreCase("ALL")
                || methodsAllowed.contains(method.toUpperCase());
    }

    /**
     * Получение полей первичного ключа
     *
     * @return
     */
    protected String[] getPrimaryKey() {
        return primaryKey;
    }

    /**
     * Get id from params
     *
     * @param params
     * @return
     */
    protected Object[] getPKParams(JSONObject params) {
        String[] pk = getPrimaryKey();
        Object[] result = new Object[pk.length];
        for (int i = 0, l = pk.length; i < l; i++) {
            result[i] = params.opt(pk[i]);
        }
        return result;
    }

    /**
     * Get true if params has not null primary key
     *
     * @param params
     * @return
     */
    protected boolean hasPKParams(JSONObject params) {
        Object[] obj = getPKParams(params);
        for (int i = 0, l = obj.length; i < l; i++) {
            if (obj[i] == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Скопировать данные первичного ключа 
     *
     * @param data
     * @param params
     */
    protected void putPKParams(JSONObject data, JSONObject params) {
        String[] pk = getPrimaryKey();
        for (int i = 0, l = pk.length; i < l; i++) {
            if (params.has(pk[i])) {
                data.put(pk[i], params.get(pk[i]));
            }
        }
    }

    /**
     * Эхо выбранные параметры
     *
     * @param dest
     * @param names
     * @param values
     * @return
     */
    protected JSONObject echo(JSONObject dest, String[] names, JSONObject values) {
        for (String name : names) {
            Object value = values.opt(name);
            if (value != null) {
                dest.put(name, value);
            }
        }
        return dest;
    }

    /**
     * Эхо все параметры
     *
     * @param dest
     * @param params
     * @return
     */
    protected JSONObject echo(JSONObject dest, JSONObject params) {
        for (String name : params.keySet()) {
            dest.put(name, params.get(name));
        }
        return dest;
    }

    /**
     * Get record
     *
     * @param request
     * @param params
     * @return
     * @throws java.sql.SQLException
     */
    protected JSONObject getData(HttpServletRequest request, JSONObject params) throws SQLException {
        return null;
    }

    /**
     * Update record
     *
     * @param request
     * @param params
     * @param data
     * @return
     * @throws java.sql.SQLException
     */
    protected JSONObject putData(HttpServletRequest request, JSONObject params, JSONObject data) throws SQLException {
        return null;
    }

    /**
     * Delete record
     *
     * @param request
     * @param params
     * @return
     * @throws java.sql.SQLException
     */
    protected JSONObject deleteData(HttpServletRequest request, JSONObject params) throws SQLException {
        return null;
    }

    /**
     * Insert record
     *
     * @param request
     * @param data
     * @return
     * @throws java.sql.SQLException
     */
    protected JSONObject postData(HttpServletRequest request, JSONObject data) throws SQLException {
        return null;
    }

    /**
     * Method allowed
     *
     * @param request
     * @return
     */
    protected JSONObject optionsData(HttpServletRequest request) {
        JSONObject result = new JSONObject();
        result.put("name", this.getServletName());
        result.put("primaryKey", this.primaryKey);
        result.put("allow", this.methodsAllowed.toLowerCase());
        return result;
    }
    
    /**
     * Process JSON request
     *
     * @return
     * @throws javax.servlet.ServletException
     * @throws java.io.IOException
     */
    @Override
    protected JSONObject processRequest(HttpServletRequest request) throws ServletException, IOException {
        try {
            String method = request.getMethod();
            if (isMethodAllowed(method)) {
                JSONObject keys = new JSONObject();
                String info = request.getPathInfo();
                if (info != null) {
                    String[] pk = getPrimaryKey();
                    String[] values = info.substring(1).split("/");
                    for (int i = 0, l = Math.min(pk.length, values.length); i < l; i++) {
                        keys.put(pk[i], values[i]);
                    }
                }
                if ("GET".equalsIgnoreCase(method)) {
                    return getData(request, echo(requestParams(request), keys));
                } else if ("PUT".equalsIgnoreCase(method)) {
                    return putData(request, echo(requestParams(request), keys), requestContent(request));
                } else if ("DELETE".equalsIgnoreCase(method)) {
                    return deleteData(request, echo(requestParams(request), keys));
                } else if ("POST".equalsIgnoreCase(method)) {
                    return postData(request, echo(requestContent(request), keys));
                } else if ("OPTIONS".equalsIgnoreCase(method)) {
                    return optionsData(request);
                }
            }
            return responseStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        } catch (SQLException e) {
            // Ошибка SQL. Сообщение возращаем
            return responseStatus(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } catch (IOException | RuntimeException e) {
            // Внутренние ошибки сервера. Сообщение не возращаем
            return responseStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

}
