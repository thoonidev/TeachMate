package com.tme.admin.servlet;
import com.tme.dao.ConfiguracionPagosDAO;
import com.tme.db.DBConnection;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/guardarConfiguracionPagos")
public final class ConfiguracionPagosServlet extends HttpServlet {
    @Override protected void doPost(HttpServletRequest q,HttpServletResponse r)throws IOException{
        try{
            new ConfiguracionPagosDAO(DBConnection.getConn()).guardar(q.getParameter("medio"),q.getParameter("titular"),q.getParameter("destino"),q.getParameter("instrucciones"),com.tme.service.TiempoVisible.minutos(q.getParameter("correccion"),q.getParameter("correccionUnidad")));
            q.getSession().setAttribute("successMsg","Instrucciones de cobro guardadas. Las reservas existentes conservan sus condiciones.");
        }catch(IllegalArgumentException e){q.getSession().setAttribute("errorMsg",e instanceof NumberFormatException?"Revisa el plazo de corrección.":e.getMessage());}
        catch(IllegalStateException e){q.getServletContext().log("No se pudo guardar la configuración de cobro.",e);r.sendError(500);return;}
        r.sendRedirect(q.getContextPath()+"/admin/configuracion.jsp#cobros");
    }
}
