package com.tme.dao;

import java.sql.*;
import java.time.LocalDateTime;
import com.tme.service.ReglasTutoria;
import com.tme.service.CalendarioAtencion;
import java.time.LocalTime;

public final class ConfiguracionReservasDAO {
    public record Configuracion(Integer respuesta, Integer pago, Integer antelacion,
            Integer dias, LocalTime desde, LocalTime hasta, Integer revision) {
        public boolean completa() { return pago != null && antelacion != null && dias != null
            && desde != null && hasta != null && revision != null; }
        public CalendarioAtencion calendario() { return new CalendarioAtencion(dias,desde,hasta,revision); }
        public LocalDateTime minimo(LocalDateTime ahora) {
            LocalDateTime anticipacion=ahora.plusMinutes(antelacion);
            LocalDateTime revisado=calendario().sumarRevision(ahora.plusMinutes(pago));
            return anticipacion.isAfter(revisado)?anticipacion:revisado;
        }
    }
    private final Connection conn;
    public ConfiguracionReservasDAO(Connection conn) { this.conn=conn; }
    public Configuracion obtener() {
        try(PreparedStatement s=conn.prepareStatement("select respuesta_minutos,pago_minutos,antelacion_minutos,atencion_dias,atencion_desde,atencion_hasta,revision_minutos from configuracion_reservas where id=1");ResultSet r=s.executeQuery()) {
            if(!r.next())return new Configuracion(null,null,null,null,null,null,null);
            return new Configuracion((Integer)r.getObject(1),(Integer)r.getObject(2),(Integer)r.getObject(3),
                (Integer)r.getObject(4),r.getObject(5,LocalTime.class),r.getObject(6,LocalTime.class),(Integer)r.getObject(7));
        }catch(SQLException e){throw new IllegalStateException("No se pudo consultar la configuración de reservas.",e);}
    }
    public void guardar(int pago,int antelacion,int dias,LocalTime desde,LocalTime hasta,int revision) {
        if(pago<15||pago>10080||antelacion<1440||antelacion<pago||antelacion>43200)
            throw new IllegalArgumentException("El pago debe tener un plazo de 15 minutos a 7 días. La antelación debe ser de al menos 24 horas, cubrir el plazo de pago y no superar 30 días.");
        CalendarioAtencion calendario=new CalendarioAtencion(dias,desde,hasta,revision);
        new TutoriaDAO(conn).transaction(()->{
            try(PreparedStatement s=conn.prepareStatement("update configuracion_reservas set respuesta_minutos=0,pago_minutos=?,antelacion_minutos=?,atencion_dias=?,atencion_desde=?,atencion_hasta=?,revision_minutos=? where id=1")) {
                s.setInt(1,pago);s.setInt(2,antelacion);s.setInt(3,dias);s.setObject(4,desde);s.setObject(5,hasta);s.setInt(6,revision);
                if(s.executeUpdate()!=1)throw new IllegalStateException("Falta la configuración de reservas.");
            }
            GestionAsesoriasDAO.evento(conn,null,"ADMIN",null,"Configuración","Pago: "+com.tme.service.TiempoVisible.duracion(pago)+"; anticipación: "+com.tme.service.TiempoVisible.duracion(antelacion)+". Revisión: "+com.tme.service.TiempoVisible.duracion(revision)+" de atención; "+calendario.descripcion()+".",ReglasTutoria.ahora());
            return true;
        });
    }
    public Configuracion exigir(LocalDateTime inicio,LocalDateTime ahora) {
        Configuracion c=obtener();
        if(!c.completa())throw new IllegalArgumentException("El administrador debe configurar los plazos antes de habilitar nuevas reservas.");
        if(!new ConfiguracionPagosDAO(conn).obtener().completa())throw new IllegalArgumentException("El administrador debe configurar el cobro antes de habilitar nuevas reservas.");
        if(inicio.isBefore(c.minimo(ahora)))throw new IllegalArgumentException("Elige un horario desde el "+c.minimo(ahora).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))+" (Lima). Necesitamos tiempo para revisar el pago.");
        return c;
    }
}
