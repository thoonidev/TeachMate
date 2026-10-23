package com.tme.user.servlet;

import com.tme.dao.TutoriaDAO;
import com.tme.db.DBConnection;
import com.tme.modal.*;
import com.tme.service.ReglasTutoria.Rol;
import java.io.IOException;
import java.time.LocalTime;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

/** Requests and workflow actions share the same business rules and error handling. */
@WebServlet({"/addTutoria", "/cambiarEstadoTutoria", "/cancelarTutoria", "/adminCancelarTutoria"})
public class TutoriaServlet extends HttpServlet {
    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getServletPath();
        String destino = switch (path) {
            case "/cambiarEstadoTutoria" -> "/docente/alumno.jsp";
            case "/cancelarTutoria" -> "/list_tutoria.jsp";
            case "/adminCancelarTutoria" -> "/admin/alumno.jsp";
            default -> "/reg_tutoria.jsp";
        };
        HttpSession session = request.getSession(false);
        try {
            TutoriaDAO dao = new TutoriaDAO(DBConnection.getConn());
            if (path.equals("/addTutoria")) {
                int userId = ((User) session.getAttribute("userObj")).getId();
                Tutoria tutoria = new Tutoria(userId, request.getParameter("nombre"), request.getParameter("genero"),
                        request.getParameter("edad"), request.getParameter("ftutoria"), request.getParameter("email"),
                        request.getParameter("telefono"), request.getParameter("experiencia"),
                        Integer.parseInt(request.getParameter("docenteNameSelect")), request.getParameter("direccion"), "Pendiente");
                tutoria.setHoraInicio(LocalTime.parse(request.getParameter("horaInicio")));
                tutoria.setDuracionMinutos(Integer.parseInt(request.getParameter("duracionMinutos")));
                if (!dao.addTutoria(tutoria)) throw new IllegalArgumentException("El docente seleccionado ya no está disponible. Elige otro docente.");
                session.setAttribute("successMsg", "Solicitud registrada. El horario queda reservado mientras el docente la revisa.");
                session.removeAttribute("tutoriaForm");
                destino = "/list_tutoria.jsp";
            } else {
                Rol rol = path.equals("/cambiarEstadoTutoria") ? Rol.DOCENTE
                        : path.equals("/cancelarTutoria") ? Rol.ALUMNO : Rol.ADMIN;
                int actorId = rol == Rol.DOCENTE ? ((Docente) session.getAttribute("docenteObj")).getId()
                        : rol == Rol.ALUMNO ? ((User) session.getAttribute("userObj")).getId() : 0;
                EstadoTutoria estado = rol == Rol.DOCENTE ? EstadoTutoria.desdeValor(request.getParameter("estado")) : EstadoTutoria.CANCELADA;
                if (!dao.cambiarEstado(Integer.parseInt(request.getParameter("id")), actorId, rol, estado)) {
                    response.sendError(404); return;
                }
                session.setAttribute("successMsg", "Estado de la tutoría actualizado: " + estado.getValor() + ".");
            }
        } catch (IllegalArgumentException exception) {
            session.setAttribute("errorMsg", exception.getMessage());
            if (path.equals("/addTutoria")) {
                java.util.Map<String, String> datos = new java.util.HashMap<>();
                for (String campo : java.util.List.of("nombre", "genero", "edad", "ftutoria", "email", "telefono",
                        "experiencia", "docenteNameSelect", "direccion", "horaInicio", "duracionMinutos")) {
                    datos.put(campo, request.getParameter(campo));
                }
                session.setAttribute("tutoriaForm", datos);
            }
        } catch (IllegalStateException exception) {
            request.getServletContext().log("No se pudo completar la operación de tutorías.");
            response.sendError(500); return;
        }
        response.sendRedirect(request.getContextPath() + destino);
    }
}
