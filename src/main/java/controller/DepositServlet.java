package controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.TransactionResult;
import service.AccessDeniedException;
import service.BankingService;
import util.JsonUtil;
import util.SessionUtil;
import util.ValidationException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/** POST /api/deposit - credits money to the logged-in user's own account. */
@WebServlet("/api/deposit")
public class DepositServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(DepositServlet.class.getName());

    private final transient BankingService bankingService = new BankingService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long userId = SessionUtil.getUserId(req);
        try {
            TransactionResult result = bankingService.deposit(userId,
                    req.getParameter("accountNumber"),
                    req.getParameter("amount"),
                    req.getParameter("description"));
            JsonUtil.success(resp, "Amount deposited successfully.", result);
        } catch (ValidationException e) {
            JsonUtil.error(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage(), e.getFieldErrors());
        } catch (AccessDeniedException e) {
            JsonUtil.error(resp, HttpServletResponse.SC_FORBIDDEN, e.getMessage(), null);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Deposit failed", e);
            JsonUtil.error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Deposit failed due to a server error. No money was moved.", null);
        }
    }
}
