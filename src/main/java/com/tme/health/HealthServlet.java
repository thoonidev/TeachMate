package com.tme.health;

import com.tme.db.DBConnection;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/health")
public class HealthServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        try {
            Connection connection = DBConnection.getConn();
            // One round trip validates all required tables/columns, without scanning customer data.
            String sql = "select u.id,u.telefono,d.activo,d.presentacion,d.niveles,e.descripcion,e.activo,"
                    + "t.estado,t.comentario,t.hora_inicio,t.duracion_minutos,t.curso_id,t.tarifa_id,t.precio,t.porcentaje_docente,"
                    + "t.nivel,t.vence_respuesta,t.vence_pago,t.enlace_clase,t.asistencia,t.cancelacion_solicitada,t.pago_revision,t.cobro_medio,t.plazo_correccion_minutos,t.revision_dias,t.revision_desde,t.revision_hasta,t.revision_minutos,t.revision_prevista,"
                    + "dc.curso_id,tc.precio,tc.porcentaje_docente,di.fecha,di.hora_desde,di.hora_hasta,cr.respuesta_minutos,cr.pago_minutos,cr.antelacion_minutos,cr.atencion_dias,cr.atencion_desde,cr.atencion_hasta,cr.revision_minutos,"
                    + "ce.accion,ca.version,cp.medio,cp.correccion_minutos,p.estado,p.version,pc.mime,pc.huella,pi.tipo,pi.clave,pe.accion,pd.importe,pd.ejecutada_en,"
                    + "cl.corte_dia,cl.pago_dia,l.total,l.estado,ll.importe,ls.liquidacion_id,la.estado,la.importe,laa.liquidacion_id,le.ajuste_id,co.clave,co.estado,co.token,co.programado_en,co.incierto "
                    + "from user_details u left join docente d on 1=0 left join especialidad e on 1=0 left join tutoria t on 1=0 "
                    + "left join docente_curso dc on 1=0 left join tarifa_curso tc on 1=0 left join disponibilidad di on 1=0 "
                    + "left join configuracion_reservas cr on 1=0 left join tutoria_evento ce on 1=0 left join tutoria_cambio ca on 1=0 "
                    + "left join configuracion_pagos cp on 1=0 left join pago_alumno p on 1=0 left join pago_comprobante pc on 1=0 "
                    + "left join pago_identificador pi on 1=0 left join pago_evento pe on 1=0 left join pago_devolucion pd on 1=0 "
                    + "left join configuracion_liquidaciones cl on 1=0 left join liquidacion l on 1=0 left join liquidacion_linea ll on 1=0 "
                    + "left join liquidacion_sesion ls on 1=0 left join liquidacion_ajuste la on 1=0 "
                    + "left join liquidacion_ajuste_asignacion laa on 1=0 left join liquidacion_evento le on 1=0 left join correo_pendiente co on 1=0 where 1=0";
            try (PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setQueryTimeout(3);
                try (ResultSet result = statement.executeQuery()) { result.next(); }
            }
            response.getWriter().write("{\"status\":\"ok\"}");
        } catch (SQLException | IllegalStateException exception) {
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.getWriter().write("{\"status\":\"unavailable\"}");
        }
    }
}
