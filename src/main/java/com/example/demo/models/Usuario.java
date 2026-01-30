package com.example.demo.models;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.Date;

@Schema(description = "Información detallada del perfil de usuario y sus listas personales")
public class Usuario {

    @Schema(description = "ID único del usuario", example = "user_abc123")
    private String id;

    @Schema(description = "Nombre de usuario o nickname", example = "GeraltDeRivia", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nombre;

    @Schema(description = "Correo electrónico para login", example = "geralt@kaermorhen.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "Contraseña encriptada (BCrypt)", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String pass;

    @Schema(description = "Token JWT generado tras el login")
    private String token;

    @Schema(description = "Indica si el usuario tiene privilegios de administrador", example = "false")
    private boolean isAdmin;

    @Schema(description = "Lista de IDs de las colecciones creadas por el usuario")
    private ArrayList<String> colecciones;

    @Schema(description = "Lista de IDs de juegos marcados como favoritos")
    private ArrayList<String> favoritos;

    @Schema(description = "Lista de IDs de juegos en la lista de deseos")
    private ArrayList<String> deseados;

    @Schema(description = "Lista de IDs de reviews publicadas por el usuario")
    private ArrayList<String> reviews;

    @Schema(description = "Fecha de nacimiento del usuario")
    private Date fechaNacimiento;

    @Schema(description = "Fecha de registro en la plataforma")
    private Date fechaCreacion;

    @Schema(description = "Última modificación del perfil")
    private Date fechaActualizacion;

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public Date getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(Date fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public ArrayList<String> getColecciones() {
        return colecciones;
    }

    public void setColecciones(ArrayList<String> colecciones) {
        this.colecciones = colecciones;
    }

    public ArrayList<String> getFavoritos() {
        return favoritos;
    }

    public void setFavoritos(ArrayList<String> favoritos) {
        this.favoritos = favoritos;
    }

    public ArrayList<String> getDeseados() {
        return deseados;
    }

    public void setDeseados(ArrayList<String> deseados) {
        this.deseados = deseados;
    }

    public ArrayList<String> getReviews() {
        return reviews;
    }

    public void setReviews(ArrayList<String> reviews) {
        this.reviews = reviews;
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

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Usuario() {

    }

    @Override
    public String toString() {
        return "Usuario{" +
                "id='" + id + '\'' +
                ", nombre='" + nombre + '\'' +
                ", email='" + email + '\'' +
                ", pass='" + pass + '\'' +
                ", token='" + token + '\'' +
                ", colecciones=" + colecciones +
                ", favoritos=" + favoritos +
                ", deseados=" + deseados +
                ", reviews=" + reviews +
                ", fechaNacimiento=" + fechaNacimiento +
                ", fechaCreacion=" + fechaCreacion +
                ", fechaActualizacion=" + fechaActualizacion +
                '}';
    }
}