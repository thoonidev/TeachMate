package com.tme.db;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class DatabaseConnectionFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            chain.doFilter(request, response);
        } finally {
            try {
                DBConnection.closeCurrentConnection();
            } catch (IllegalStateException exception) {
                request.getServletContext().log("No se pudo cerrar la conexión con MySQL.");
            }
        }
    }
}
