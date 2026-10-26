package com.tme.dao;

import java.sql.*;
import java.time.LocalDateTime;
import com.tme.service.ReglasTutoria;

public final class ConfiguracionReservasDAO {
    public record Configuracion(Integer respuesta, Integer pago, Integer antelacion) {
        public boolean completa() { return pago != null && antelacion != null; }
    }
    private final Connection conn;
    public ConfiguracionReservasDAO(Connection conn) { this.conn=conn; }
    public Configuracion obtener() {
        try(PreparedStatement s=conn.prepareStatement("select respuesta_minutos,pago_minutos,antelacion_minutos from configuracion_reservas where id=1");ResultSet r=s.executeQuery()) {
            if(!r.next())return new Configuracion(null,null,null);
            return new Configuracion((Integer)r.getObject(1),(Integer)r.getObject(2),(Integer)r.getObject(3));
        }catch(SQLException e){throw new IllegalStateException("No se pudo consultar la configuración de reservas.",e);}
    }
    public void guardar(int pago,int antelacion) {
        if(pago<15||pago>10080||antelacion<1440||antelacion<pago||antelacion>43200)
            throw new IllegalArgumentException("El pago debe tener un plazo de 15 minutos a 7 días. La antelación debe ser de al menos 24 horas, cubrir el plazo de pago y no superar 30 días.");
        new TutoriaDAO(conn).transaction(()->{
            try(PreparedStatement s=conn.prepareStatement("update configuracion_reservas set respuesta_minutos=?,pago_minutos=?,antelacion_minutos=? where id=1")) {
                s.setInt(1,0);s.setInt(2,pago);s.setInt(3,antelacion);if(s.executeUpdate()!=1)throw new IllegalStateException("Falta la configuración de reservas.");
            }
            GestionAsesoriasDAO.evento(conn,null,"ADMIN",null,"Configuración","Reserva directa sobre disponibilidad publicada; pago: "+pago+" min; antelación: "+antelacion+" min.",ReglasTutoria.ahora());
            return true;
        });
    }
    public Configuracion exigir(LocalDateTime inicio,LocalDateTime ahora) {
        Configuracion c=obtener();
        if(!c.completa())throw new IllegalArgumentException("El administrador debe configurar los plazos antes de habilitar nuevas reservas.");
        if(!new ConfiguracionPagosDAO(conn).obtener().completa())throw new IllegalArgumentException("El administrador debe configurar el cobro antes de habilitar nuevas reservas.");
        if(inicio.isBefore(ahora.plusMinutes(c.antelacion())))throw new IllegalArgumentException("Elige un horario con al menos "+c.antelacion()+" minutos de antelación.");
        return c;
    }
}
