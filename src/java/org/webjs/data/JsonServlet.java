/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.webjs.data;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;

/**
 * JSON Servelt
 *
 * @author ryabochkin_mr
 */
public abstract class JsonServlet extends HttpServlet {

    /**
     * Make response status and message
     *
     * @param status
     * @param message
     * @return
     */
    protected JSONObject responseStatus(int status, String message) {
        JSONObject obj = new JSONObject();
        obj.put("status", status);
        if (message != null) {
            obj.put("message", message);
        }
        return obj;
    }

    /**
     * Make response status with default message
     *
     * @param status
     * @return
     */
    protected JSONObject responseStatus(int status) {
        String message = "Error (Ошибка)";
        switch (status) {
            case 200:
                message = "Success (Успешно)";
                break;
            case 201:
                message = "Created (Создано)";
                break;
            case 202:
                message = "Accepted (Принято)";
                break;
            case 204:
                message = "No Content (Нет содержимого)";
                break;
            case 205:
                message = "Reset Content (Сбросить содержимое)";
                break;

            case 300:
                message = "Multiple Choices (Множественный выбор)";
                break;
            case 301:
                message = "Moved Permanently (Перемещено навсегда)";
                break;
            case 302:
                message = "Moved Temporarily (Перемещено временно)";
                break;
            case 304:
                message = "Not Modified (Не изменялось)";
                break;
            case 307:
                message = "Temporary Redirect (Временное перенаправление)";
                break;

            case 401:
                message = "Unauthorized (Неавторизован)";
                break;
            case 402:
                message = "Payment Required (Необходима оплата)";
                break;
            case 403:
                message = "Forbidden (Запрещено)";
                break;
            case 404:
                message = "Not Found (Не найдено)";
                break;
            case 405:
                message = "Method Not Allowed (Метод не поддерживается)";
                break;
            case 406:
                message = "Not Acceptable (Неприемлемо)";
                break;
            case 408:
                message = "Request Timeout (Истекло время ожидания)";
                break;
            case 410:
                message = "Gone (Удалён)";
                break;
            case 412:
                message = "Precondition Failed (Условие ложно)";
                break;

            case 500:
                message = "Internal Server Error (Внутренняя ошибка сервера)";
                break;
            case 501:
                message = "Not Implemented (Не реализовано)";
                break;
        }
        return responseStatus(status, message);
    }

    /**
     * Make response success status
     *
     * @return
     */
    protected JSONObject responseStatus() {
        return responseStatus(HttpServletResponse.SC_OK);
    }

    /**
     * Добавить заголовки в данные
     *
     * @param request
     * @param params
     */
    protected void appendHeaders(HttpServletRequest request, JSONObject params) {
        for (Enumeration<String> e = request.getHeaderNames(); e.hasMoreElements();) {
            String key = e.nextElement();
            if (!"accept".equals(key) && !"accept-encoding".equals(key)
                    && !"connection".equals(key) && !"cookie".equals(key)
                    && !"if-none-match".equals(key) && !"content-length".equals(key)
                    && !"content-type".equals(key) && !"cache-control".equals(key)) {
                String name = key.replace("-", "");
                if ("accept-language".equals(key)) {
                    name = "language";
                }
                params.put(name, request.getHeader(key));
            }
        }
    }

    /**
     * Read request parameters
     *
     * @param request
     * @return
     */
    protected JSONObject requestParams(HttpServletRequest request) {
        JSONObject params = new JSONObject();
        for (Enumeration<java.lang.String> e = request.getParameterNames(); e.hasMoreElements();) {
            String name = e.nextElement();
            params.put(name, request.getParameter(name));
        }
        appendHeaders(request, params);
        return params;
    }

    /**
     * Read content from request
     *
     * @param request
     * @return
     * @throws IOException
     */
    protected JSONObject requestContent(HttpServletRequest request) throws IOException {
        String content = request.getContentType();
        int type = content != null
                ? (content.contains("application/json") ? 1
                : (content.contains("application/xml") ? 2
                : (content.contains("application/x-www-form-urlencoded") ? 3 : 0))) : 0;
        String encoding = request.getCharacterEncoding();
        if (encoding == null) {
            encoding = "UTF-8";
            request.setCharacterEncoding(encoding);
        }
        if (type > 0
                && (type != 3 || !"POST".equalsIgnoreCase(request.getMethod()))) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4 * 1024];
            try (Reader in = request.getReader()) {
                int len;
                while ((len = in.read(buf, 0, buf.length)) != -1) {
                    sb.append(buf, 0, len);
                }
            }
            String s = sb.toString();
            if (type == 1 || type == 2 || type == 3) {
                JSONObject result;
                switch (type) {
                    case 1:
                        result = new JSONObject(s);
                        break;
                    case 2:
                        result = XML.toJSONObject(s);
                        break;
                    default:
                        result = new JSONObject();
                        String[] pairs = s.split("&");
                        for (String pair : pairs) {
                            String[] fields = pair.split("=");
                            String name = URLDecoder.decode(fields[0], encoding);
                            String value = URLDecoder.decode(fields[1], encoding);
                            result.put(name, value);
                        }
                        break;
                }
                appendHeaders(request, result);
                return result;
            }
        }
        return requestParams(request);
    }

    /**
     * Process JSON request
     *
     * @param request
     * @return
     * @throws ServletException
     * @throws IOException
     */
    protected abstract JSONObject processRequest(HttpServletRequest request) throws ServletException, IOException;

    private String getMd5Digest(byte data[]) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException exception) {
            throw new RuntimeException("MD5 cryptographic algorithm is not available.", exception);
        }
        byte result[] = md.digest(data);
        BigInteger bi = new BigInteger(1, result);
        return bi.toString(16);
    }

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
        response.setHeader("Pragma", "no-cache"); // HTTP 1.0.
        response.setDateHeader("Expires", 0); // Proxies.            
        String allowOrigin = request.getServletContext().getInitParameter("AccessControlAllowOrigin");
        if (allowOrigin != null && !allowOrigin.isEmpty()) {
            response.setHeader("Access-Control-Allow-Origin", "*".equals(allowOrigin) ? request.getHeader("Origin") : allowOrigin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Accept, Accept-Encoding, Accept-Language, Cache-Control, Cookie, Content-Type, ETag, If-Modified-Since, If-None-Match, Last-Modified, Set-Cookie, User-Agent, X-Requested-With, DynamicPassword, PrevTranId, ThisTranId");
            response.setHeader("Access-Control-Expose-Headers", "Cache-Control, Content-Type, Date, ETag, Expires, Last-Modified, Pragma, Server");
        }

        String accept = request.getHeader("Accept");
        int type;
        type = accept != null
                ? (accept.contains("application/json") ? 1
                : (accept.contains("application/xml") ? 2
                : (accept.contains("text/html") ? 3
                : (accept.contains("text/plain") ? 4 : 0)))) : 0;

        String encoding = request.getCharacterEncoding();
        if (encoding == null) {
            encoding = "UTF-8";
        }
        if (type == 0 && accept != null && accept.equals("*/*")) {
            type = 1;
        }
        if (type > 0) {
            switch (type) {
                case 1:
                    response.setContentType("application/json");
                    break;
                case 2:
                    response.setContentType("application/xml");
                    break;
                case 3:
                    response.setContentType("text/html");
                    break;
                default:
                    response.setContentType("text/plain");
                    break;
            }
            response.setCharacterEncoding(encoding);
            JSONObject obj;
            try {
                obj = processRequest(request);
                String originalETag = request.getHeader("If-None-Match");
                String method = request.getMethod();
                if (obj == null) {
                    obj = responseStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
                } else if ("GET".equalsIgnoreCase(method)) {
                    String newETag = "W/\"" + getMd5Digest(obj.toString().getBytes("UTF-8")) + "\"";
                    response.setHeader("ETag", newETag);
                    if (originalETag != null && originalETag.equals(newETag)) {
                        response.setStatus(obj.optInt("status", HttpServletResponse.SC_NOT_MODIFIED));
                        return;
                    }
                } else if ("OPTIONS".equalsIgnoreCase(method)) {
                    response.setHeader("Allow", "GET, POST, PUT, DELETE, OPTIONS");
                }

            } catch (IOException | ServletException | JSONException e) {
                obj = responseStatus(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            }
            int status = obj.optInt("status", HttpServletResponse.SC_OK);
            if (status >= 200 && status < 600) {
                response.setStatus(status);
            } else {
                response.setStatus(HttpServletResponse.SC_OK);
            }
            try (PrintWriter out = response.getWriter()) {
                switch (type) {
                    case 1:
                    case 4:
                        obj.write(out);
                        break;
                    case 2:
                        out.write(XML.toString(obj, response.getStatus() < 300
                                ? getServletName() : "result"));
                        break;
                    default:
                        out.write("<!DOCTYPE html><html><head>");
                        out.write("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=" + encoding + "\">");
                        out.write("<title>" + getServletName() + "</title>");
                        obj.write(out);
                        out.write("<body>");
                        out.write("</body></html>");
                        break;
                }
            } catch (IOException e) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
        }

    }

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>DELETE</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>PUT</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>OPTIONS</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doOptions(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Servlet " + this.getServletName();
    }

}
