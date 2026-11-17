package controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import service.UserService;
import util.JsonUtil;
import util.ValidationException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * POST /api/forgot-password - resets the password after the user proves identity with
 * username + registered email + registered mobile number.
 * (A production bank would send an OTP / e-mail link instead.)
 */
@WebServlet("/api/forgot-password")
public class ForgotPasswordServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(ForgotPasswordServlet.class.getName());

    private final transient UserService userService = new UserService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            userService.resetPassword(
                    req.getParameter("username"),
                    req.getParameter("email"),
                    req.getParameter("mobile"),
                    req.getParameter("newPassword"),
                    req.getParameter("confirmPassword"));
            JsonUtil.success(resp, "Your password has been reset. Please log in with your new password.", null);
        } catch (ValidationException e) {
            JsonUtil.error(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage(), e.getFieldErrors());
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Password reset failed", e);
            JsonUtil.error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Password reset is temporarily unavailable.", null);
        }
    }
}
