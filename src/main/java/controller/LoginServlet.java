package controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.User;
import service.UserService;
import util.JsonUtil;
import util.SessionUtil;
import util.ValidationException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/** POST /api/login - authenticates with username/email + password and starts a session. */
@WebServlet("/api/login")
public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(LoginServlet.class.getName());

    private final transient UserService userService = new UserService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            User user = userService.authenticate(req.getParameter("identifier"), req.getParameter("password"));
            if (user == null) {
                JsonUtil.error(resp, HttpServletResponse.SC_UNAUTHORIZED,
                        "Invalid username/email or password.", null);
                return;
            }
            SessionUtil.login(req, user);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("fullName", user.getFullName());
            data.put("firstName", user.getFirstName());
            JsonUtil.success(resp, "Welcome back, " + user.getFirstName()
                    + "! You have successfully logged into Net Banking.", data);

        } catch (ValidationException e) {
            JsonUtil.error(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage(), e.getFieldErrors());
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Login failed", e);
            JsonUtil.error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Login is temporarily unavailable. Please try again later.", null);
        }
    }
}
