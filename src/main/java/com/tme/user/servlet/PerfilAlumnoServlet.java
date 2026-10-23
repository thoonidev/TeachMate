package com.tme.user.servlet;
import com.tme.dao.UserDAO;
import com.tme.db.DBConnection;
import com.tme.modal.User;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

@WebServlet("/guardarPerfilAlumno")
public final class PerfilAlumnoServlet extends HttpServlet {
    @Override protected void doPost(HttpServletRequest q,HttpServletResponse r)throws IOException {
        int id=((User)q.getSession(false).getAttribute("userObj")).getId();
        try {
            UserDAO dao=new UserDAO(DBConnection.getConn());dao.guardarPerfil(id,q.getParameter("nombre"),q.getParameter("telefono"));
            q.getSession().setAttribute("userObj",dao.getById(id));q.getSession().setAttribute("successMsg","Perfil actualizado.");
        }catch(IllegalArgumentException e){q.getSession().setAttribute("errorMsg",e.getMessage());}
        catch(IllegalStateException e){r.sendError(500);return;}
        r.sendRedirect(q.getContextPath()+"/perfil.jsp");
    }
}
