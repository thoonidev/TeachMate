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
            int dias=0;
            String[] seleccion=q.getParameterValues("dias");
            if(seleccion!=null)for(String dia:seleccion){int numero=Integer.parseInt(dia);if(numero<1||numero>7)throw new IllegalArgumentException("Día de atención inválido.");dias|=1<<(numero-1);}
            java.time.LocalTime desde,hasta;
            try { desde=java.time.LocalTime.parse(q.getParameter("desde"));hasta=java.time.LocalTime.parse(q.getParameter("hasta")); }
            catch(java.time.DateTimeException|NullPointerException e){throw new IllegalArgumentException("Indica las horas de inicio y fin de atención.");}
            new ConfiguracionReservasDAO(DBConnection.getConn()).guardar(Integer.parseInt(q.getParameter("pago")),Integer.parseInt(q.getParameter("antelacion")),
                dias,desde,hasta,Integer.parseInt(q.getParameter("revision")));
            q.getSession().setAttribute("successMsg","Plazos guardados. Las solicitudes anteriores conservan sus vencimientos.");
        }catch(IllegalArgumentException e){q.getSession().setAttribute("errorMsg",e instanceof NumberFormatException?"Indica el plazo de pago, la antelación y el margen de revisión en minutos enteros.":e.getMessage());}
        catch(IllegalStateException e){r.sendError(500);return;}
        r.sendRedirect(q.getContextPath()+"/admin/configuracion.jsp");
    }
}
