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
            new ConfiguracionReservasDAO(DBConnection.getConn()).guardar(com.tme.service.TiempoVisible.minutos(q.getParameter("pago"),q.getParameter("pagoUnidad")),com.tme.service.TiempoVisible.minutos(q.getParameter("antelacion"),q.getParameter("antelacionUnidad")),
                dias,desde,hasta,com.tme.service.TiempoVisible.minutos(q.getParameter("revision"),q.getParameter("revisionUnidad")));
            q.getSession().setAttribute("successMsg","Plazos guardados. Se aplican a las nuevas reservas.");
        }catch(IllegalArgumentException e){q.getSession().setAttribute("errorMsg",e instanceof NumberFormatException?"Revisa los plazos y los días de atención.":e.getMessage());}
        catch(IllegalStateException e){r.sendError(500);return;}
        r.sendRedirect(q.getContextPath()+"/admin/configuracion.jsp");
    }
}
