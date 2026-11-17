package controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import util.JsonUtil;
import util.SessionUtil;

import java.io.IOException;

/** POST /api/logout - destroys the session (protected by AuthFilter + CSRF token). */
@WebServlet("/api/logout")
public class LogoutServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        SessionUtil.logout(req);
        JsonUtil.success(resp, "You have been logged out successfully.", null);
    }
}
