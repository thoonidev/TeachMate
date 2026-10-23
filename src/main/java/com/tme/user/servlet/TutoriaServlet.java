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
@WebServlet("/addTutoria")
public class TutoriaServlet extends HttpServlet {
    @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String destino="/reg_tutoria.jsp";
        HttpSession session = request.getSession(false);
        try {
            TutoriaDAO dao = new TutoriaDAO(DBConnection.getConn());
            {
                User alumno = new com.tme.dao.UserDAO(DBConnection.getConn()).getById(((User) session.getAttribute("userObj")).getId());
                if (alumno.getTelefono().isBlank()) throw new IllegalArgumentException("Completa tu teléfono en Mi perfil antes de solicitar.");
                Tutoria tutoria = new Tutoria(alumno.getId(), alumno.getNombre(), "", "", request.getParameter("ftutoria"),
                        alumno.getEmail(), alumno.getTelefono(), request.getParameter("experiencia"),
                        Integer.parseInt(request.getParameter("docenteNameSelect")), "", "Pendiente");
                tutoria.setHoraInicio(LocalTime.parse(request.getParameter("horaInicio")));
                tutoria.setDuracionMinutos(Integer.parseInt(request.getParameter("duracionMinutos")));
                tutoria.setNivel(request.getParameter("nivel"));
                tutoria.setCursoId(Integer.parseInt(request.getParameter("cursoId")));
                tutoria.setTarifaId(Integer.parseInt(request.getParameter("tarifaId")));
                if (!dao.addTutoria(tutoria)) throw new IllegalArgumentException("El docente seleccionado ya no está disponible. Elige otro docente.");
                session.setAttribute("successMsg", "Solicitud registrada. El horario queda reservado mientras el docente la revisa.");
                session.removeAttribute("tutoriaForm");
                session.removeAttribute("reservaRetorno");
                destino = "/asesoria.jsp?id="+tutoria.getId();
            }
        } catch (IllegalArgumentException exception) {
            session.setAttribute("errorMsg", exception.getMessage());
            {
                java.util.Map<String, String> datos = new java.util.HashMap<>();
                for (String campo : java.util.List.of("cursoId", "tarifaId", "docenteNameSelect", "ftutoria", "experiencia", "horaInicio", "duracionMinutos", "nivel")) {
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
