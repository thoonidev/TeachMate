package com.tme.admin.servlet;

import com.tme.dao.OfertaDAO;
import com.tme.db.DBConnection;
import com.tme.modal.Docente;
import java.io.IOException;
import java.util.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet({"/guardarCurso", "/guardarTarifa", "/retirarTarifa", "/asignarCursos", "/adminDisponibilidad", "/docenteDisponibilidad"})
public final class OfertaServlet extends HttpServlet {
    @Override protected void doPost(HttpServletRequest q, HttpServletResponse r)throws IOException {
        String path=q.getServletPath(), destino="/admin/cursos.jsp";
        try {
            OfertaDAO dao=new OfertaDAO(DBConnection.getConn());
            switch(path) {
                case "/guardarCurso" -> dao.guardarCurso(nonnegative(q.getParameter("id")),q.getParameter("nombre"),q.getParameter("descripcion"),"on".equals(q.getParameter("activo")));
                case "/guardarTarifa", "/retirarTarifa" -> {
                    int curso=id(q.getParameter("cursoId")); destino+="?cursoId="+curso;
                    if(path.equals("/guardarTarifa"))dao.guardarTarifa(curso,id(q.getParameter("minutos")),q.getParameter("precio"),q.getParameter("porcentaje"));
                    else dao.retirarTarifa(curso,id(q.getParameter("tarifaId")));
                }
                case "/asignarCursos" -> {
                    int docente=id(q.getParameter("docenteId"));destino="/admin/oferta.jsp?docenteId="+docente;
                    Set<Integer> cursos=new HashSet<>();String[] values=q.getParameterValues("cursoId");
                    if(values!=null){if(values.length>200)throw new IllegalArgumentException("Demasiados cursos seleccionados.");for(String value:values)cursos.add(id(value));}
                    dao.asignar(docente,cursos);
                }
                default -> {
                    boolean profesor=path.equals("/docenteDisponibilidad");
                    int docente=profesor?((Docente)q.getSession(false).getAttribute("docenteObj")).getId():id(q.getParameter("docenteId"));
                    destino=profesor?"/docente/disponibilidad.jsp":"/admin/oferta.jsp?docenteId="+docente;
                    String actor=profesor?"DOCENTE:"+docente:"ADMIN";
                    if("retirar".equals(q.getParameter("accion")))dao.retirarFranja(docente,id(q.getParameter("franjaId")),actor);
                    else if("agregar".equals(q.getParameter("accion")))dao.agregarFranja(docente,q.getParameter("fecha"),q.getParameter("desde"),q.getParameter("hasta"),actor);
                    else throw new IllegalArgumentException("Acción inválida.");
                }
            }
            q.getSession().setAttribute("successMsg","Cambios guardados. Las asesorías existentes conservan su historial.");
        }catch(IllegalArgumentException e){q.getSession().setAttribute("errorMsg",e.getMessage());}
        catch(IllegalStateException e){q.getServletContext().log("No se pudo actualizar la oferta académica.");r.sendError(500);return;}
        r.sendRedirect(q.getContextPath()+destino);
    }
    public static int id(String text){int n=nonnegative(text);if(n==0)throw new IllegalArgumentException("Selecciona un registro válido.");return n;}
    private static int nonnegative(String text){try{int n=Integer.parseInt(text);if(n>=0)return n;}catch(RuntimeException ignored){}throw new IllegalArgumentException("Identificador inválido.");}
}
