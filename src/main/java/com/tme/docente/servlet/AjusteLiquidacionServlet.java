package com.tme.docente.servlet;
import com.tme.dao.LiquidacionesDAO;
import com.tme.db.DBConnection;
import com.tme.modal.Docente;
import com.tme.service.ReglasTutoria.Rol;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/gestionarAjuste")
public final class AjusteLiquidacionServlet extends HttpServlet {
    @Override protected void doPost(HttpServletRequest q,HttpServletResponse r)throws IOException{
        var session=q.getSession(false);if(session==null||(session.getAttribute("adminObj")==null&&session.getAttribute("docenteObj")==null)){r.sendError(403);return;}
        Rol rol=session.getAttribute("adminObj")!=null?Rol.ADMIN:Rol.DOCENTE;int actor=rol==Rol.ADMIN?0:((Docente)session.getAttribute("docenteObj")).getId();
        try{
            int id=Integer.parseInt(q.getParameter("id")),version=Integer.parseInt(q.getParameter("version"));if(id<1)throw new NumberFormatException();
            if(!new LiquidacionesDAO(DBConnection.getConn()).gestionarAjuste(id,actor,rol,version,q.getParameter("accion"),q.getParameter("importe"),q.getParameter("motivo"))){r.sendError(404);return;}
            session.setAttribute("successMsg","Decisión del ajuste registrada. No modifica transferencias anteriores.");
        }catch(IllegalArgumentException e){session.setAttribute("errorMsg",e instanceof NumberFormatException?"Revisa los datos del ajuste.":e.getMessage());}
        catch(IllegalStateException e){q.getServletContext().log("No se pudo gestionar el ajuste.",e);r.sendError(500);return;}
        r.sendRedirect(q.getContextPath()+(rol==Rol.ADMIN?"/admin/liquidaciones.jsp":"/docente/ingresos.jsp"));
    }
}
