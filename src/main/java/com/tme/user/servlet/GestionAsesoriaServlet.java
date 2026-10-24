package com.tme.user.servlet;

import com.tme.dao.GestionAsesoriasDAO;
import com.tme.db.DBConnection;
import com.tme.modal.*;
import com.tme.service.ReglasTutoria.Rol;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/gestionarAsesoria")
public final class GestionAsesoriaServlet extends HttpServlet {
    @Override protected void doPost(HttpServletRequest q,HttpServletResponse r)throws IOException {
        int id;
        try{id=Integer.parseInt(q.getParameter("id"));if(id<1)throw new NumberFormatException();}catch(RuntimeException e){r.sendError(400);return;}
        HttpSession session=q.getSession(false);
        Rol rol=session.getAttribute("adminObj")!=null?Rol.ADMIN:session.getAttribute("docenteObj")!=null?Rol.DOCENTE:Rol.ALUMNO;
        int actor=rol==Rol.ADMIN?0:rol==Rol.DOCENTE?((Docente)session.getAttribute("docenteObj")).getId():((User)session.getAttribute("userObj")).getId();
        try {
            int version=q.getParameter("version")==null?0:Integer.parseInt(q.getParameter("version"));
            if(!new GestionAsesoriasDAO(DBConnection.getConn()).gestionar(id,actor,rol,q.getParameter("accion"),q.getParameter("motivo"),q.getParameter("valor"),q.getParameter("fecha"),q.getParameter("hora"),version)){r.sendError(404);return;}
            session.setAttribute("successMsg","La asesoría se actualizó. Consulta su estado y el historial.");
        }catch(IllegalArgumentException e){
            session.setAttribute("errorMsg",e.getMessage());
            java.util.Map<String,String> form=new java.util.HashMap<>();form.put("id",Integer.toString(id));
            for(String field:java.util.List.of("accion","motivo","valor","fecha","hora")){String value=q.getParameter(field);if(value!=null&&value.length()<=1000)form.put(field,value);}
            session.setAttribute("asesoriaForm",form);
        }catch(IllegalStateException e){q.getServletContext().log("No se pudo gestionar la asesoría.",e);r.sendError(500);return;}
        r.sendRedirect(q.getContextPath()+"/asesoria.jsp?id="+id);
    }
}
