package controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.RegistrationResult;
import service.UserService;
import util.JsonUtil;
import util.ValidationException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/** POST /api/signup - registers a user and opens their bank account. */
@WebServlet("/api/signup")
public class SignupServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(SignupServlet.class.getName());

    private final transient UserService userService = new UserService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            RegistrationResult result = userService.register(
                    req.getParameter("fullName"),
                    req.getParameter("email"),
                    req.getParameter("mobile"),
                    req.getParameter("username"),
                    req.getParameter("password"),
                    req.getParameter("confirmPassword"),
                    req.getParameter("address"));

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("fullName", result.getUser().getFullName());
            data.put("username", result.getUser().getUsername());
            data.put("accountNumber", result.getAccount().getAccountNumber());
            data.put("accountType", result.getAccount().getAccountType());
            data.put("balance", result.getAccount().getBalance());
            JsonUtil.success(resp, "Welcome to Net Banking! Your account has been successfully created.", data);

        } catch (ValidationException e) {
            JsonUtil.error(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage(), e.getFieldErrors());
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Signup failed", e);
            JsonUtil.error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "We could not create your account right now. Please try again later.", null);
        }
    }
}
