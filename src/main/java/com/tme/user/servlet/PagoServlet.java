package com.tme.user.servlet;

import com.tme.dao.PagosDAO;
import com.tme.db.DBConnection;
import com.tme.modal.User;
import com.tme.service.ReglasTutoria.Rol;
import java.io.IOException;
import java.time.LocalDateTime;
import javax.servlet.ServletException;
import javax.servlet.annotation.*;
import javax.servlet.http.*;

@WebServlet("/gestionarPago")
@MultipartConfig(maxFileSize=5242880,maxRequestSize=6291456,fileSizeThreshold=0)
public final class PagoServlet extends HttpServlet {
    @Override protected void doPost(HttpServletRequest q,HttpServletResponse r)throws IOException,ServletException {
        HttpSession session=q.getSession(false);
        if(session==null||(session.getAttribute("adminObj")==null&&session.getAttribute("userObj")==null)){r.sendError(403);return;}
        Rol rol=session.getAttribute("adminObj")!=null?Rol.ADMIN:Rol.ALUMNO;
        int actor=rol==Rol.ADMIN?0:((User)session.getAttribute("userObj")).getId(),id;
        try{id=Integer.parseInt(q.getParameter("id"));if(id<1)throw new NumberFormatException();}catch(RuntimeException e){r.sendError(400);return;}
        try{
            PagosDAO dao=new PagosDAO(DBConnection.getConn());int version=Integer.parseInt(q.getParameter("version"));String accion=q.getParameter("accion");boolean ok;
            if("registrar".equals(accion)){
                Part part=q.getPart("comprobante");if(part==null||part.getSize()>5242880)throw new IllegalArgumentException("Adjunta un comprobante de hasta 5 MB.");
                byte[] bytes;try(var stream=part.getInputStream()){bytes=stream.readNBytes(5242881);}finally{part.delete();}
                ok=dao.registrar(id,actor,rol,version,q.getParameter("importe"),q.getParameter("referencia"),fecha(q.getParameter("fecha")),bytes,"si".equals(q.getParameter("tardio")),q.getParameter("motivo"));
            }else{
                int devolucion=q.getParameter("devolucion")==null?0:Integer.parseInt(q.getParameter("devolucion"));
                ok=dao.gestionar(id,actor,rol,version,accion,q.getParameter("motivo"),q.getParameter("importe"),q.getParameter("referencia"),fecha(q.getParameter("fecha")),devolucion);
            }
            if(!ok){r.sendError(404);return;}
            session.setAttribute("successMsg","Operación registrada. Revisa el estado del pago y de la asesoría.");
        }catch(IllegalArgumentException e){
            session.setAttribute("errorMsg",e instanceof NumberFormatException?"El formulario no es válido. Revisa los datos.":e.getMessage());
            java.util.Map<String,String> form=new java.util.HashMap<>();form.put("id",Integer.toString(id));
            for(String field:java.util.List.of("accion","motivo","importe","referencia","fecha","tardio")){String value=q.getParameter(field);if(value!=null&&value.length()<=1000)form.put(field,value);}
            session.setAttribute("pagoForm",form);
        }catch(IllegalStateException e){q.getServletContext().log("No se pudo registrar la operación de pago.",e);r.sendError(500);return;}
        r.sendRedirect(q.getContextPath()+"/pago.jsp?id="+id);
    }
    private LocalDateTime fecha(String texto){if(texto==null||texto.isBlank())return null;try{return LocalDateTime.parse(texto);}catch(java.time.DateTimeException e){throw new IllegalArgumentException("Indica una fecha y hora válidas (Lima).");}}
}
