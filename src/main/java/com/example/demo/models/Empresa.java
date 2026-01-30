package com.example.demo.models;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.Date;

@Schema(description = "Representa a una compañía de la industria (Desarrolladora o Publisher)")
public class Empresa {

    @Schema(description = "ID único de la empresa en Firestore", example = "comp_nintendo_01")
    private String id;

    @Schema(description = "Nombre oficial de la empresa", example = "Nintendo EPD", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nombre;

    @Schema(description = "País de origen de la sede principal", example = "Japón")
    private String nacionalidad;

    @Schema(description = "URL del logotipo oficial", example = "https://logo.com/nintendo.png")
    private String urlLogo;

    @Schema(description = "Lista de IDs de juegos publicados por esta empresa")
    private ArrayList<String> publicados;

    @Schema(description = "Lista de IDs de juegos desarrollados por esta empresa")
    private ArrayList<String> desarrollados;

    @Schema(description = "Fecha en la que se fundó la compañía")
    private Date fechaFundacion;

    @Schema(description = "Fecha de registro en la plataforma")
    private Date fechaCreacion;

    @Schema(description = "Última modificación de los datos")
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