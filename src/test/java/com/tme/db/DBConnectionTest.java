package com.tme.db;

import static org.junit.Assert.*;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import org.junit.*;

public class DBConnectionTest {
    private static Driver driver;

    @BeforeClass
    public static void installDriver() throws Exception {
        driver = new Driver() {
            public boolean acceptsURL(String url) { return url.startsWith("jdbc:mysql://test.invalid/"); }
            public Connection connect(String url, Properties properties) {
                if (!acceptsURL(url)) return null;
                AtomicBoolean closed = new AtomicBoolean();
                return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[] {Connection.class}, (proxy, method, args) -> {
                        switch (method.getName()) {
                            case "close": closed.set(true); return null;
                            case "isClosed": return closed.get();
                            default: throw new UnsupportedOperationException(method.getName());
                        }
                    });
            }
            public DriverPropertyInfo[] getPropertyInfo(String url, Properties p) { return new DriverPropertyInfo[0]; }
            public int getMajorVersion() { return 1; }
            public int getMinorVersion() { return 0; }
            public boolean jdbcCompliant() { return false; }
            public Logger getParentLogger() { return Logger.getGlobal(); }
        };
        DriverManager.registerDriver(driver);
    }

    @AfterClass
    public static void removeDriver() throws Exception { DriverManager.deregisterDriver(driver); }

    @After
    public void cleanup() { DBConnection.closeCurrentConnection(); }

    @Test
    public void reusesConnectionAndReopensAfterCleanup() throws Exception {
        Connection first = DBConnection.getConn();
        assertSame(first, DBConnection.getConn());
        DBConnection.closeCurrentConnection();
        assertTrue(first.isClosed());
        assertNotSame(first, DBConnection.getConn());
    }

    @Test
    public void isolatesConcurrentRequests() throws Exception {
        Connection main = DBConnection.getConn();
        java.util.concurrent.FutureTask<Connection> task = new java.util.concurrent.FutureTask<>(() -> {
            try { return DBConnection.getConn(); }
            finally { DBConnection.closeCurrentConnection(); }
        });
        new Thread(task).start();
        Connection other = task.get(5, java.util.concurrent.TimeUnit.SECONDS);
        assertNotSame(main, other);
        assertTrue(other.isClosed());
        assertFalse(main.isClosed());
    }

    @Test
    public void filterClosesConnectionWhenRequestFails() throws Exception {
        Connection connection = DBConnection.getConn();
        try {
            new DatabaseConnectionFilter().doFilter(null, null, (request, response) -> {
                throw new javax.servlet.ServletException("test failure");
            });
            fail("Expected request failure");
        } catch (javax.servlet.ServletException expected) {
            assertEquals("test failure", expected.getMessage());
        }
        assertTrue(connection.isClosed());
    }
}
