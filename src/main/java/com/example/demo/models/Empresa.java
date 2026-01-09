package com.example.demo.models;

import java.util.ArrayList;
import java.util.Date;

public class Empresa {
    private String id;
    private String nombre;
    private String nacionalidad;
    private String urlLogo;
    private ArrayList<String> publicados;
    private ArrayList<String> desarrollados;
    private Date fechaFundacion;
    private Date fechaCreacion;
    private Date fechaActualizacion;

    public Empresa() {

    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getNacionalidad() {
        return nacionalidad;
    }

    public void setNacionalidad(String nacionalidad) {
        this.nacionalidad = nacionalidad;
    }

    public String getUrlLogo() {
        return urlLogo;
    }

    public void setUrlLogo(String urlLogo) {
        this.urlLogo = urlLogo;
    }

    public ArrayList<String> getPublicados() {
        return publicados;
    }

    public void setPublicados(ArrayList<String> publicados) {
        this.publicados = publicados;
    }

    public ArrayList<String> getDesarrollados() {
        return desarrollados;
    }

    public void setDesarrollados(ArrayList<String> desarrollados) {
        this.desarrollados = desarrollados;
    }

    public Date getFechaFundacion() {
        return fechaFundacion;
    }

    public void setFechaFundacion(Date fechaFundacion) {
        this.fechaFundacion = fechaFundacion;
    }

    public Date getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(Date fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Date getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(Date fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
}