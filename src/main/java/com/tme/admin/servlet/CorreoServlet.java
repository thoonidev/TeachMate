package com.tme.admin.servlet;

import com.tme.dao.CorreoDAO;
import com.tme.db.DBConnection;
import com.tme.service.ReglasTutoria;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/gestionarCorreo")
public final class CorreoServlet extends HttpServlet {
    @Override protected void doPost(HttpServletRequest q,HttpServletResponse r)throws IOException {
        if(q.getSession(false)==null||q.getSession(false).getAttribute("adminObj")==null){r.sendError(403);return;}
        try {
            long id=Long.parseLong(q.getParameter("id"));if(id<1)throw new NumberFormatException();
            String accion=q.getParameter("accion");if(accion==null)throw new IllegalArgumentException("Indica una acción.");
            boolean cambiado=new CorreoDAO(DBConnection.getConn()).gestionar(id,accion,"true".equals(q.getParameter("confirmar")),ReglasTutoria.ahora());
            q.getSession().setAttribute(cambiado?"successMsg":"errorMsg",cambiado?"Aviso actualizado.":"El aviso ya no existe.");
        }catch(IllegalArgumentException e){q.getSession().setAttribute("errorMsg",e instanceof NumberFormatException?"Aviso inválido.":e.getMessage());}
        catch(IllegalStateException e){r.sendError(500);return;}
        r.sendRedirect(q.getContextPath()+"/admin/correos.jsp");
    }
}
