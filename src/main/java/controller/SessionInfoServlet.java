package controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.BankAccount;
import service.BankingService;
import util.JsonUtil;
import util.SessionUtil;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GET /api/me - returns the logged-in user's name, own account number, balance and the
 * CSRF token. Every protected page calls this first to fill the navbar and pre-fill forms.
 */
@WebServlet("/api/me")
public class SessionInfoServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(SessionInfoServlet.class.getName());

    private final transient BankingService bankingService = new BankingService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long userId = SessionUtil.getUserId(req);
        try {
            BankAccount account = bankingService.getAccountForUser(userId);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("fullName", SessionUtil.getString(req, SessionUtil.FULL_NAME));
            data.put("username", SessionUtil.getString(req, SessionUtil.USERNAME));
            data.put("csrfToken", SessionUtil.getString(req, SessionUtil.CSRF_TOKEN));
            data.put("accountNumber", account == null ? null : account.getAccountNumber());
            data.put("accountType", account == null ? null : account.getAccountType());
            data.put("balance", account == null ? null : account.getBalance());
            JsonUtil.success(resp, "OK", data);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Loading session info failed", e);
            JsonUtil.error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to load your profile.", null);
        }
    }
}
