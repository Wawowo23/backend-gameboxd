package com.example.demo.models;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.Date;

@Schema(description = "Representa una crítica o reseña realizada por un usuario sobre un videojuego")
public class Review {

    @Schema(description = "ID único de la review en Firestore", example = "rev_987654321")
    private String id;

    @Schema(description = "Puntuación numérica (normalmente de 1 a 10)", example = "9", requiredMode = Schema.RequiredMode.REQUIRED)
    private int nota;

    @Schema(description = "Texto de la reseña", example = "Una obra maestra técnica con una narrativa increíble.")
    private String comentario;

    @Schema(description = "ID del usuario que escribió la review", example = "user_12345", requiredMode = Schema.RequiredMode.REQUIRED)
    private String idUsuario;

    @Schema(description = "ID del juego reseñado", example = "7qXy89BzLp0q", requiredMode = Schema.RequiredMode.REQUIRED)
    private String idJuego;

    @Schema(description = "Lista de IDs de usuarios que han dado 'Like' a esta review")
    private ArrayList<String> likes;

    @Schema(description = "Fecha de publicación")
    private Date fechaCreacion;

    @Schema(description = "Fecha de la última edición del comentario o nota")
    private Date fechaActualizacion;

    public Review() {}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getNota() {
        return nota;
    }

    public void setNota(int nota) {
        this.nota = nota;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public String getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(String idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getIdJuego() {
        return idJuego;
    }

    public void setIdJuego(String idJuego) {
        this.idJuego = idJuego;
    }

    public ArrayList<String> getLikes() {
        return likes;
    }

    public void setLikes(ArrayList<String> likes) {
        this.likes = likes;
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

    @Override
    public String toString() {
        return "Review{" +
                "id='" + id + '\'' +
                ", nota=" + nota +
                ", comentario='" + comentario + '\'' +
                ", idUsuario='" + idUsuario + '\'' +
                ", idJuego='" + idJuego + '\'' +
                ", likes=" + likes +
                ", fechaCreacion=" + fechaCreacion +
                ", fechaActualizacion=" + fechaActualizacion +
                '}';
    }
}