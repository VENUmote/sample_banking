package controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.TransactionResult;
import service.AccessDeniedException;
import service.BankingService;
import service.InsufficientBalanceException;
import util.JsonUtil;
import util.SessionUtil;
import util.ValidationException;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/** POST /api/withdraw - debits money; refuses with 422 when the balance is insufficient. */
@WebServlet("/api/withdraw")
public class WithdrawServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(WithdrawServlet.class.getName());
    private static final int SC_UNPROCESSABLE = 422;

    private final transient BankingService bankingService = new BankingService();

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long userId = SessionUtil.getUserId(req);
        try {
            TransactionResult result = bankingService.withdraw(userId,
                    req.getParameter("accountNumber"),
                    req.getParameter("amount"),
                    req.getParameter("description"));
            JsonUtil.success(resp, "Amount withdrawn successfully.", result);
        } catch (InsufficientBalanceException e) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("availableBalance", e.getAvailableBalance());
            data.put("requestedAmount", e.getRequestedAmount());
            JsonUtil.error(resp, SC_UNPROCESSABLE, e.getMessage(),
                    Map.of("amount", "Amount exceeds your available balance."), data);
        } catch (ValidationException e) {
            JsonUtil.error(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage(), e.getFieldErrors());
        } catch (AccessDeniedException e) {
            JsonUtil.error(resp, HttpServletResponse.SC_FORBIDDEN, e.getMessage(), null);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Withdrawal failed", e);
            JsonUtil.error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Withdrawal failed due to a server error. No money was moved.", null);
        }
    }
}
