package com.tme.user.servlet;

import com.tme.dao.PagosDAO;
import com.tme.db.DBConnection;
import com.tme.modal.User;
import com.tme.service.ReglasTutoria.Rol;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/comprobantePago")
public final class ComprobanteServlet extends HttpServlet {
    @Override protected void doGet(HttpServletRequest q,HttpServletResponse r)throws IOException{
        HttpSession s=q.getSession(false);if(s==null||(s.getAttribute("adminObj")==null&&s.getAttribute("userObj")==null)){r.sendError(403);return;}
        int id;try{id=Integer.parseInt(q.getParameter("id"));if(id<1)throw new NumberFormatException();}catch(RuntimeException e){r.sendError(400);return;}
        Rol rol=s.getAttribute("adminObj")!=null?Rol.ADMIN:Rol.ALUMNO;int actor=rol==Rol.ADMIN?0:((User)s.getAttribute("userObj")).getId();
        var file=new PagosDAO(DBConnection.getConn()).descargar(id,actor,rol);if(file==null){r.sendError(404);return;}
        String ext=file.mime().equals("application/pdf")?"pdf":file.mime().equals("image/png")?"png":"jpg";
        r.setHeader("Cache-Control","no-store, private");r.setHeader("Content-Security-Policy","sandbox; default-src 'none'");r.setHeader("Content-Disposition","attachment; filename=\"comprobante-"+id+"."+ext+"\"");r.setContentType(file.mime());r.setContentLength(file.datos().length);r.getOutputStream().write(file.datos());
    }
}
