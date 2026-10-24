package com.tme.admin.servlet;
import com.tme.dao.*;
import com.tme.db.DBConnection;
import com.tme.service.ReglasTutoria.Rol;
import java.io.IOException;
import java.time.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/gestionarLiquidacion")
public final class LiquidacionServlet extends HttpServlet {
    @Override protected void doPost(HttpServletRequest q,HttpServletResponse r)throws IOException{
        if(q.getSession(false)==null||q.getSession().getAttribute("adminObj")==null){r.sendError(403);return;}
        int id=0,docente=0;String accion=q.getParameter("accion");
        try{
            if("configurar".equals(accion)){
                new ConfiguracionLiquidacionesDAO(DBConnection.getConn()).guardar(Integer.parseInt(q.getParameter("corteDia")),Integer.parseInt(q.getParameter("pagoDia")));
            }else if("crear".equals(accion)){
                docente=Integer.parseInt(q.getParameter("docente"));if(docente<1)throw new NumberFormatException();
                id=new LiquidacionesDAO(DBConnection.getConn()).crear(docente,q.getParameter("corte"),q.getParameter("huella"),Rol.ADMIN);
            }else{
                id=Integer.parseInt(q.getParameter("id"));if(id<1)throw new NumberFormatException();
                LocalDateTime fecha=null;String valor=q.getParameter("fecha");if(valor!=null&&!valor.isBlank())try{fecha=LocalDateTime.parse(valor);}catch(DateTimeException e){throw new IllegalArgumentException("Indica una fecha y hora válidas en Lima.");}
                if(!new LiquidacionesDAO(DBConnection.getConn()).gestionar(id,Integer.parseInt(q.getParameter("version")),Rol.ADMIN,accion,q.getParameter("importe"),q.getParameter("referencia"),q.getParameter("destino"),fecha,q.getParameter("motivo"))){r.sendError(404);return;}
            }
            q.getSession().setAttribute("successMsg","Operación registrada. Consulta el estado y el detalle de la liquidación.");
        }catch(IllegalArgumentException e){q.getSession().setAttribute("errorMsg",e instanceof NumberFormatException?"Revisa los datos del formulario.":e.getMessage());}
        catch(IllegalStateException e){q.getServletContext().log("No se pudo gestionar la liquidación.",e);r.sendError(500);return;}
        r.sendRedirect(q.getContextPath()+(id>0?"/liquidacion.jsp?id="+id:"/admin/liquidaciones.jsp"+(docente>0?"?docente="+docente:"")));
    }
}
