package com.example.demo.models;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.Date;

@Schema(description = "Representa una lista personalizada de juegos creada por un usuario")
public class Coleccion {

    @Schema(description = "ID único de la colección", example = "col_556677")
    private String id;

    @Schema(description = "ID del creador de la colección", example = "user_12345", requiredMode = Schema.RequiredMode.REQUIRED)
    private String idUsuario;

    @Schema(description = "Nombre de la lista", example = "Must Play: RPGs de los 90", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nombre;

    @Schema(description = "Breve explicación del propósito de la colección", example = "Una selección de los mejores títulos de la era dorada.")
    private String descripcion;

    @Schema(description = "Lista de IDs de juegos incluidos en esta colección")
    private ArrayList<String> juegos;

    @Schema(description = "Contador de 'Me gusta' recibidos por otros usuarios", example = "42")
    private int cantidadMeGusta;

    @Schema(description = "Fecha de creación de la lista")
    private Date fechaCreacion;

    @Schema(description = "Fecha de la última vez que se añadieron o quitaron juegos")
    private Date fechaActualizacion;

    public Coleccion() {}

    public String getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(String idUsuario) {
        this.idUsuario = idUsuario;
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

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public ArrayList<String> getJuegos() {
        return juegos;
    }

    public void setJuegos(ArrayList<String> juegos) {
        this.juegos = juegos;
    }

    public int getCantidadMeGusta() {
        return cantidadMeGusta;
    }

    public void setCantidadMeGusta(int cantidadMeGusta) {
        this.cantidadMeGusta = cantidadMeGusta;
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