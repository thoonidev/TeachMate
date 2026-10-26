package com.tme.admin.servlet;
import com.tme.dao.ConfiguracionReservasDAO;
import com.tme.db.DBConnection;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/guardarConfiguracionReservas")
public final class ConfiguracionReservasServlet extends HttpServlet {
    @Override protected void doPost(HttpServletRequest q,HttpServletResponse r)throws IOException {
        try {
            new ConfiguracionReservasDAO(DBConnection.getConn()).guardar(Integer.parseInt(q.getParameter("pago")),Integer.parseInt(q.getParameter("antelacion")));
            q.getSession().setAttribute("successMsg","Plazos guardados. Las solicitudes anteriores conservan sus vencimientos.");
        }catch(IllegalArgumentException e){q.getSession().setAttribute("errorMsg",e instanceof NumberFormatException?"Indica el plazo de pago y la antelación en minutos enteros.":e.getMessage());}
        catch(IllegalStateException e){r.sendError(500);return;}
        r.sendRedirect(q.getContextPath()+"/admin/configuracion.jsp");
    }
}
