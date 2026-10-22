package com.tme.health;

import com.tme.db.DBConnection;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/health")
public class HealthServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        try {
            Connection connection = DBConnection.getConn();
            // Check the schema too: an empty database must not be declared ready.
            for (String table : new String[] {"user_details", "docente", "especialidad", "tutoria"}) {
                try (PreparedStatement statement = connection.prepareStatement("select id from " + table + " limit 1")) {
                    statement.setQueryTimeout(2);
                    try (ResultSet result = statement.executeQuery()) { result.next(); }
                }
            }
            response.getWriter().write("{\"status\":\"ok\"}");
        } catch (SQLException | IllegalStateException exception) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.getWriter().write("{\"status\":\"unavailable\"}");
        }
    }
}
