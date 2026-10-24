package com.tme.dao;
import java.sql.*;
import java.time.*;
import com.tme.service.ReglasTutoria;

public final class ConfiguracionLiquidacionesDAO {
    public record Configuracion(Integer corte,Integer pago){
        public boolean completa(){return corte!=null&&pago!=null;}
        public LocalDate ultimoCorte(LocalDate hoy){if(!completa())return null;int dias=Math.floorMod(hoy.getDayOfWeek().getValue()-corte,7);return hoy.minusDays(dias==0?7:dias);}
        public LocalDate pagoPrevisto(LocalDate fecha){int dias=Math.floorMod(pago-fecha.getDayOfWeek().getValue(),7);return fecha.plusDays(dias==0?7:dias);}
    }
    private final Connection conn;
    public ConfiguracionLiquidacionesDAO(Connection c){conn=c;}
    public Configuracion obtener(){try(PreparedStatement s=conn.prepareStatement("select corte_dia,pago_dia from configuracion_liquidaciones where id=1");ResultSet r=s.executeQuery()){return r.next()?new Configuracion((Integer)r.getObject(1),(Integer)r.getObject(2)):new Configuracion(null,null);}catch(SQLException e){throw new IllegalStateException("No se pudo consultar el calendario de liquidaciones.",e);}}
    public void guardar(int corte,int pago){
        if(corte<1||corte>7||pago<1||pago>7||corte==pago)throw new IllegalArgumentException("Elige dos días distintos para el corte y el pago semanal.");
        new TutoriaDAO(conn).transaction(()->{try(PreparedStatement s=conn.prepareStatement("update configuracion_liquidaciones set corte_dia=?,pago_dia=? where id=1")){s.setInt(1,corte);s.setInt(2,pago);if(s.executeUpdate()!=1)throw new IllegalStateException("Falta configurar las liquidaciones.");}LiquidacionesDAO.evento(conn,null,null,null,"ADMIN","Calendario","Corte semanal: "+corte+"; día de pago: "+pago+". Los cierres anteriores conservan su fecha prevista.",ReglasTutoria.ahora());return true;});
    }
}
