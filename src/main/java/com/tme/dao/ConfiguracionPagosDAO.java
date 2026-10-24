package com.tme.dao;

import java.sql.*;
import com.tme.service.ReglasTutoria;

public final class ConfiguracionPagosDAO {
    public record Configuracion(String medio,String titular,String destino,String instrucciones,Integer correccion) {
        public boolean completa(){return medio!=null&&!medio.isBlank()&&titular!=null&&!titular.isBlank()&&destino!=null&&!destino.isBlank()&&instrucciones!=null&&!instrucciones.isBlank()&&correccion!=null;}
    }
    private final Connection conn;
    public ConfiguracionPagosDAO(Connection conn){this.conn=conn;}
    public Configuracion obtener(){
        try(PreparedStatement s=conn.prepareStatement("select * from configuracion_pagos where id=1");ResultSet r=s.executeQuery()){
            if(!r.next())return new Configuracion(null,null,null,null,null);
            return new Configuracion(r.getString("medio"),r.getString("titular"),r.getString("destino"),r.getString("instrucciones"),(Integer)r.getObject("correccion_minutos"));
        }catch(SQLException e){throw new IllegalStateException("No se pudo consultar la configuración de cobro.",e);}
    }
    public Configuracion condiciones(int id){
        try(PreparedStatement s=conn.prepareStatement("select cobro_medio,cobro_titular,cobro_destino,cobro_instrucciones,plazo_correccion_minutos from tutoria where id=?")){
            s.setInt(1,id);try(ResultSet r=s.executeQuery()){if(!r.next())return new Configuracion(null,null,null,null,null);return new Configuracion(r.getString(1),r.getString(2),r.getString(3),r.getString(4),(Integer)r.getObject(5));}
        }catch(SQLException e){throw new IllegalStateException("No se pudieron consultar las instrucciones de esta reserva.",e);}
    }
    public void guardar(String medio,String titular,String destino,String instrucciones,int correccion){
        String m=OfertaDAO.texto(medio,80,"Medio de cobro"),t=OfertaDAO.texto(titular,150,"Titular"),d=OfertaDAO.texto(destino,200,"Cuenta o número de destino"),i=OfertaDAO.texto(instrucciones,1000,"Instrucciones");
        if(correccion<15||correccion>10080)throw new IllegalArgumentException("La corrección debe tener un plazo de 15 minutos a 7 días.");
        new TutoriaDAO(conn).transaction(()->{
            try(PreparedStatement s=conn.prepareStatement("update configuracion_pagos set medio=?,titular=?,destino=?,instrucciones=?,correccion_minutos=? where id=1")){
                s.setString(1,m);s.setString(2,t);s.setString(3,d);s.setString(4,i);s.setInt(5,correccion);if(s.executeUpdate()!=1)throw new IllegalStateException("Falta la configuración de cobro.");
            }
            GestionAsesoriasDAO.evento(conn,null,"ADMIN",null,"Configuración de cobro","Se actualizaron las instrucciones de cobro y el plazo de corrección. Las reservas existentes conservan sus condiciones.",ReglasTutoria.ahora());return true;
        });
    }
    void copiar(int id)throws SQLException{
        Configuracion c=obtener();if(!c.completa())throw new IllegalArgumentException("El administrador debe completar las instrucciones de cobro y el plazo de corrección.");
        try(PreparedStatement s=conn.prepareStatement("update tutoria set cobro_medio=?,cobro_titular=?,cobro_destino=?,cobro_instrucciones=?,plazo_correccion_minutos=? where id=? and cobro_medio is null")){
            s.setString(1,c.medio());s.setString(2,c.titular());s.setString(3,c.destino());s.setString(4,c.instrucciones());s.setInt(5,c.correccion());s.setInt(6,id);s.executeUpdate();
        }
    }
}
