package com.example.demo.models;

import java.util.ArrayList;
import java.util.Date;

public class Usuario {
    private String id;
    private String nombre;
    private String email;
    private String pass;
    private String token;
    private boolean isAdmin;
    private ArrayList<String> colecciones;
    private ArrayList<String> favoritos;
    private ArrayList<String> deseados;
    private ArrayList<String> reviews;
    private Date fechaNacimiento;
    private Date fechaCreacion;
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