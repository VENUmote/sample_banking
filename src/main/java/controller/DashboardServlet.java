package controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.DashboardSummary;
import service.AccessDeniedException;
import service.BankingService;
import util.JsonUtil;
import util.SessionUtil;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** GET /api/dashboard - balance, totals, counts and recent transactions. */
@WebServlet("/api/dashboard")
public class DashboardServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(DashboardServlet.class.getName());

    private final transient BankingService bankingService = new BankingService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long userId = SessionUtil.getUserId(req);
        try {
            DashboardSummary summary = bankingService.getDashboard(userId);
            JsonUtil.success(resp, "OK", summary);
        } catch (AccessDeniedException e) {
            JsonUtil.error(resp, HttpServletResponse.SC_FORBIDDEN, e.getMessage(), null);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Loading dashboard failed", e);
            JsonUtil.error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unable to load the dashboard. Please try again.", null);
        }
    }
}
