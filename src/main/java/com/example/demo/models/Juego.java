package com.example.demo.models;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.Date;

public class Juego {
    @Schema(description = "ID único generado por Firestore", example = "7qXy89BzLp0q")
    private String id;

    @Schema(description = "Título principal del juego", example = "The Legend of Zelda: Breath of the Wild", requiredMode = Schema.RequiredMode.REQUIRED)
    private String titulo;

    @Schema(description = "Frase corta descriptiva", example = "Olvida todo lo que sabes sobre los juegos de Zelda")
    private String subtitulo;

    @Schema(description = "Descripción detallada de la trama o mecánicas", example = "Explora un vasto mundo abierto...")
    private String descripcion;

    @Schema(description = "Fecha de salida oficial al mercado")
    private Date fechaLanzamiento;

    @Schema(description = "Enlace a la imagen de portada", example = "https://link-a-imagen.com/portada.jpg")
    private String urlPortada;

    @Schema(description = "Enlace a la imagen de fondo (Hero)", example = "https://link-a-imagen.com/fondo.png")
    private String urlFondo;

    @Schema(description = "Lista de calificaciones numéricas dadas por los usuarios")
    private ArrayList<Integer> notas;

    @Schema(description = "Duración estimada para la historia principal en minutos", example = "3000")
    private int minutosDuracion;

    @Schema(description = "Duración estimada para el 100% en minutos", example = "11000")
    private int minutosDuracionCompleto;

    @Schema(description = "Plataformas disponibles", example = "[\"Nintendo Switch\", \"Wii U\"]")
    private ArrayList<String> plataformas;

    @Schema(description = "Categorías principales", example = "[\"Aventura\", \"RPG\"]")
    private ArrayList<String> generos;

    @Schema(description = "Etiquetas descriptivas", example = "[\"Mundo Abierto\", \"Fantasía\"]")
    private ArrayList<String> tags;

    @Schema(description = "ID de la empresa desarrolladora", example = "dev_nintendo_123")
    private String idDesarrolladora;

    @Schema(description = "ID de la empresa distribuidora", example = "pub_nintendo_456")
    private String idPublisher;

    @Schema(description = "Fecha en la que se añadió al catálogo de GameBoxd")
    private Date fechaCreacion;

    @Schema(description = "Última vez que se modificó la información del juego")
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

    @Schema(description = "Cálculo en tiempo real de la calificación media del juego", example = "8.5")
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