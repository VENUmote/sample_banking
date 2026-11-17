package controller;

import com.mysql.cj.jdbc.AbandonedConnectionCleanupThread;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Application start/stop hook. On shutdown/redeploy it de-registers the MySQL JDBC driver
 * and stops its cleanup thread, preventing Tomcat "memory leak" warnings.
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(AppContextListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOG.info("Net Banking application started at context path '"
                + sce.getServletContext().getContextPath() + "'");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ClassLoader appLoader = Thread.currentThread().getContextClassLoader();
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            if (driver.getClass().getClassLoader() == appLoader) {
                try {
                    DriverManager.deregisterDriver(driver);
                } catch (SQLException e) {
                    LOG.log(Level.WARNING, "Could not deregister JDBC driver", e);
                }
            }
        }
        AbandonedConnectionCleanupThread.checkedShutdown();
        LOG.info("Net Banking application stopped.");
    }
}
