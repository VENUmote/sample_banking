package controller;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import model.Transaction;
import model.TransactionFilter;
import service.AccessDeniedException;
import service.BankingService;
import util.JsonUtil;
import util.SessionUtil;
import util.ValidationException;
import util.ValidationUtil;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GET /api/transactions?type=CREDIT|DEBIT&from=yyyy-MM-dd&to=yyyy-MM-dd&q=text&sort=desc|asc
 * Returns the filtered ledger of the logged-in user's account.
 */
@WebServlet("/api/transactions")
public class TransactionHistoryServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(TransactionHistoryServlet.class.getName());

    private final transient BankingService bankingService = new BankingService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        long userId = SessionUtil.getUserId(req);
        try {
            List<Transaction> list = bankingService.getHistory(userId, buildFilter(req));
            JsonUtil.success(resp, list.size() + " transaction(s) found.", list);
        } catch (ValidationException e) {
            JsonUtil.error(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage(), e.getFieldErrors());
        } catch (AccessDeniedException e) {
            JsonUtil.error(resp, HttpServletResponse.SC_FORBIDDEN, e.getMessage(), null);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "Loading history failed", e);
            JsonUtil.error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "Unable to load transactions. Please try again.", null);
        }
    }

    /** Converts query parameters into a validated TransactionFilter. */
    private TransactionFilter buildFilter(HttpServletRequest req) throws ValidationException {
        TransactionFilter f = new TransactionFilter();

        String type = ValidationUtil.clean(req.getParameter("type")).toUpperCase(Locale.ROOT);
        if (type.equals(Transaction.CREDIT) || type.equals(Transaction.DEBIT)) {
            f.setType(type);
        } else if (!type.isEmpty() && !type.equals("ALL")) {
            throw new ValidationException("Invalid transaction type.", Map.of("type", "Choose CREDIT, DEBIT or All."));
        }

        f.setFromDate(ValidationUtil.parseOptionalDate(req.getParameter("from"), "from"));
        f.setToDate(ValidationUtil.parseOptionalDate(req.getParameter("to"), "to"));
        if (f.getFromDate() != null && f.getToDate() != null && f.getFromDate().isAfter(f.getToDate())) {
            throw new ValidationException("'From' date must be on or before 'To' date.",
                    Map.of("from", "Must be on or before the 'To' date."));
        }

        String q = ValidationUtil.clean(req.getParameter("q"));
        if (q.length() > 100) {
            q = q.substring(0, 100);
        }
        f.setKeyword(q.isEmpty() ? null : q);
        f.setAscending("asc".equalsIgnoreCase(req.getParameter("sort")));
        return f;
    }
}
