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
            for (String sql : new String[] {"select id from user_details limit 1", "select id,activo from docente limit 1",
                    "select id from especialidad limit 1", "select id,estado,comentario,hora_inicio,duracion_minutos from tutoria limit 1"}) {
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
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
