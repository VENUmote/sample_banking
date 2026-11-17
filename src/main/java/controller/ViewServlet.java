package controller;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.SessionUtil;

import java.io.IOException;
import java.util.Set;

/**
 * Serves the HTML pages stored in /WEB-INF/views/. Files under WEB-INF cannot be requested
 * directly by a browser, so every page must pass through this servlet (and, for protected
 * pages, through AuthFilter first).
 *
 * <p>URL -> view: /login -> login.html, /dashboard -> dashboard.html, etc.</p>
 */
@WebServlet(urlPatterns = {"/login", "/signup", "/forgot-password",
        "/dashboard", "/deposit", "/withdraw", "/transactions"})
public class ViewServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /** Pages that only make sense for logged-out visitors. */
    private static final Set<String> PUBLIC_ONLY = Set.of("/login", "/signup", "/forgot-password");

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getServletPath();

        if (PUBLIC_ONLY.contains(path) && SessionUtil.isLoggedIn(req)) {
            resp.sendRedirect(req.getContextPath() + "/dashboard");
            return;
        }
        resp.setContentType("text/html;charset=UTF-8");
        String view = "/WEB-INF/views" + path + ".html";
        req.getRequestDispatcher(view).forward(req, resp);
    }
}
