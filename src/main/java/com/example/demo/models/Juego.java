package com.example.demo.models;

import java.util.ArrayList;
import java.util.Date;

public class Juego {
    private String id;
    private String titulo;
    private String subtitulo;
    private String descripcion;
    private Date fechaLanzamiento;
    private String urlPortada;
    private String urlFondo;
    private ArrayList<Integer> notas;
    private int minutosDuracion;
    private int minutosDuracionCompleto;
    private ArrayList<String> plataformas;
    private ArrayList<String> generos;
    private ArrayList<String> tags;
    private String idDesarrolladora;
    private String idPublisher;
    private Date fechaCreacion;
    private Date fechaActualizacion;

    public Juego() {}

    public ArrayList<Integer> getNotas() {
        return notas;
    }

    public void setNotas(ArrayList<Integer> notas) {
        this.notas = notas;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getSubtitulo() {
        return subtitulo;
    }

    public void setSubtitulo(String subtitulo) {
        this.subtitulo = subtitulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Date getFechaLanzamiento() {
        return fechaLanzamiento;
    }

    public void setFechaLanzamiento(Date fechaLanzamiento) {
        this.fechaLanzamiento = fechaLanzamiento;
    }

    public String getUrlPortada() {
        return urlPortada;
    }

    public void setUrlPortada(String urlPortada) {
        this.urlPortada = urlPortada;
    }

    public String getUrlFondo() {
        return urlFondo;
    }

    public void setUrlFondo(String urlFondo) {
        this.urlFondo = urlFondo;
    }



    public ArrayList<String> getPlataformas() {
        return plataformas;
    }

    public void setPlataformas(ArrayList<String> plataformas) {
        this.plataformas = plataformas;
    }

    public ArrayList<String> getGeneros() {
        return generos;
    }

    public void setGeneros(ArrayList<String> generos) {
        this.generos = generos;
    }

    public ArrayList<String> getTags() {
        return tags;
    }

    public void setTags(ArrayList<String> tags) {
        this.tags = tags;
    }

    public String getIdDesarrolladora() {
        return idDesarrolladora;
    }

    public void setIdDesarrolladora(String idDesarrolladora) {
        this.idDesarrolladora = idDesarrolladora;
    }

    public String getIdPublisher() {
        return idPublisher;
    }

    public void setIdPublisher(String idPublisher) {
        this.idPublisher = idPublisher;
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

    public int getMinutosDuracion() {
        return minutosDuracion;
    }

    public void setMinutosDuracion(int minutosDuracion) {
        this.minutosDuracion = minutosDuracion;
    }

    public int getMinutosDuracionCompleto() {
        return minutosDuracionCompleto;
    }

    public void setMinutosDuracionCompleto(int minutosDuracionCompleto) {
        this.minutosDuracionCompleto = minutosDuracionCompleto;
    }

    public float getNotaMedia() {
        int cont = 0,sum = 0;
        for (Integer nota : notas) {
            if (nota >= 0) {
                cont++;
                sum += nota;
            }
        }
        return (float) sum / cont;
    }
}