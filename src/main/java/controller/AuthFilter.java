package controller;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.JsonUtil;
import util.SessionUtil;

import java.io.IOException;

/**
 * Guards every protected page and API (URL patterns are listed in web.xml).
 *
 * <ul>
 *   <li>No session: pages redirect to /login, APIs return 401 JSON.</li>
 *   <li>State-changing API calls (POST) must carry the session's CSRF token header.</li>
 *   <li>Protected pages are sent with no-cache headers so the Back button cannot show
 *       account data after logout.</li>
 * </ul>
 */
public class AuthFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        String path = req.getServletPath();
        boolean isApi = path.startsWith("/api/");

        resp.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        resp.setHeader("Pragma", "no-cache");
        resp.setDateHeader("Expires", 0);

        if (!SessionUtil.isLoggedIn(req)) {
            if (isApi) {
                JsonUtil.error(resp, HttpServletResponse.SC_UNAUTHORIZED,
                        "Your session has expired. Please log in again.", null);
            } else {
                resp.sendRedirect(req.getContextPath() + "/login?expired=1");
            }
            return;
        }

        if (isApi && "POST".equalsIgnoreCase(req.getMethod()) && !SessionUtil.isValidCsrf(req)) {
            JsonUtil.error(resp, HttpServletResponse.SC_FORBIDDEN,
                    "Security token missing or invalid. Please refresh the page.", null);
            return;
        }

        chain.doFilter(request, response);
    }
}
