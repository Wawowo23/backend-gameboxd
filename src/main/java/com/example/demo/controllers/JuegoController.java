package com.example.demo.controllers;

import com.example.demo.models.Juego;
import com.example.demo.models.Review;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/v1/juegos")
@Tag(name = "Videojuegos", description = "Catálogo completo de videojuegos, filtrado, ordenación y gestión")
public class JuegoController {

    Map<String, Object> response = new HashMap<>();

    @Operation(summary = "Listar y filtrar juegos", description = "Obtiene una lista paginada de juegos con filtros por género, etiquetas y ordenación personalizada.")
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> getAll(
            @Parameter(description = "Búsqueda general en título, subtítulo, tags o géneros") @RequestParam(required = false) String generico,
            @Parameter(description = "Filtrar por título exacto") @RequestParam(required = false) String nombre,
            @Parameter(description = "Filtrar por subtítulo") @RequestParam(required = false) String subtitulo,
            @Parameter(description = "Filtrar por género") @RequestParam(required = false) String genero,
            @Parameter(description = "Filtrar por etiqueta (tag)") @RequestParam(required = false) String tags,
            @Parameter(description = "Cantidad de resultados por página") @RequestParam(required = false, defaultValue = "10") Integer limit,
            @Parameter(description = "Número de página (empezando en 1)") @RequestParam(required = false, defaultValue = "1") Integer page,
            @Parameter(description = "Campo por el que ordenar", schema = @Schema(allowableValues = {"date", "popularity", "duration", "completion", "rating", "title"})) @RequestParam(required = false, defaultValue = "popularity") String sort,
            @Parameter(description = "Sentido de la ordenación", schema = @Schema(allowableValues = {"asc", "desc"})) @RequestParam(required = false, defaultValue = "asc") String order
    ) throws ExecutionException, InterruptedException {
        response.clear();
        Firestore db = FirestoreClient.getFirestore();
        List<QueryDocumentSnapshot> documents = db.collection("videojuegos").get().get().getDocuments();

        ArrayList<Juego> juegos = new ArrayList<>();
        for (QueryDocumentSnapshot document : documents) {
            Juego juego = document.toObject(Juego.class);
            juego.setId(document.getId());
            juegos.add(juego);
        }

        Stream<Juego> stream = juegos.stream();

        if (generico != null && !generico.isEmpty()) {
            stream = stream.filter(j ->
                    j.getTitulo().toLowerCase().contains(generico.toLowerCase()) ||
                            j.getSubtitulo().toLowerCase().contains(generico.toLowerCase()) ||
                            j.getTags().stream().anyMatch(t -> t.toLowerCase().contains(generico.toLowerCase())) ||
                            j.getGeneros().stream().anyMatch(g -> g.toLowerCase().contains(generico.toLowerCase()))
            );
        } else {
            if (nombre != null) stream = stream.filter(j -> j.getTitulo().toLowerCase().contains(nombre.toLowerCase()));
            if (subtitulo != null)
                stream = stream.filter(j -> j.getSubtitulo().toLowerCase().contains(subtitulo.toLowerCase()));
            if (genero != null)
                stream = stream.filter(j -> j.getGeneros().stream().anyMatch(g -> g.toLowerCase().contains(genero.toLowerCase())));
            if (tags != null)
                stream = stream.filter(j -> j.getTags().stream().anyMatch(t -> t.toLowerCase().contains(tags.toLowerCase())));
        }

        List<Juego> filtrados = stream.collect(Collectors.toList());

        for (int i = 0; i < filtrados.size() - 1; i++) {
            int indiceMejor = i;
            for (int j = i + 1; j < filtrados.size(); j++) {

                boolean esMejor = false;
                Juego actual = filtrados.get(j);
                Juego mejor = filtrados.get(indiceMejor);

                switch (sort.toLowerCase()) {
                    case "date":
                        if (actual.getFechaLanzamiento() != null && mejor.getFechaLanzamiento() != null)
                            esMejor = actual.getFechaLanzamiento().before(mejor.getFechaLanzamiento());
                        break;
                    case "popularity":
                        esMejor = (actual.getNotas() != null ? actual.getNotas().size() : 0) > (mejor.getNotas() != null ? mejor.getNotas().size() : 0);
                        break;
                    case "duration":
                        esMejor = actual.getMinutosDuracion() > mejor.getMinutosDuracion();
                        break;
                    case "completion":
                        esMejor = actual.getMinutosDuracionCompleto() > mejor.getMinutosDuracionCompleto();
                        break;
                    case "rating":
                        esMejor = actual.getNotaMedia() > mejor.getNotaMedia();
                        break;
                    case "title":
                    default:
                        esMejor = actual.getTitulo().compareToIgnoreCase(mejor.getTitulo()) < 0;
                        break;
                }

                if (esMejor) indiceMejor = j;
            }
            Juego temp = filtrados.get(indiceMejor);
            filtrados.set(indiceMejor, filtrados.get(i));
            filtrados.set(i, temp);
        }

        if ("desc".equalsIgnoreCase(order)) {
            Collections.reverse(filtrados);
        }

        int totalJuegos = filtrados.indexOf(filtrados.getLast());
        int start = Math.min(Math.max(0, (page - 1) * limit), totalJuegos);
        int end = Math.min(start + limit, totalJuegos);

        List<Juego> paginaJuegos = filtrados.subList(start, end);
        List<Map<String, Object>> dataResponse = paginaJuegos.stream()
                .map(this::mapJuegoResponse)
                .collect(Collectors.toList());

        response.put("status", "OK");
        response.put("page", page);
        response.put("total", totalJuegos);
        response.put("limit", limit);
        response.put("data", dataResponse);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "Obtener un juego aleatorio", description = "Selecciona un videojuego al azar de toda la base de datos.")
    @GetMapping("/random")
    public ResponseEntity<Map<String, Object>> getRandom() throws ExecutionException, InterruptedException {
        response.clear();
        Firestore db = FirestoreClient.getFirestore();

        // Obtenemos todos los documentos de la colección
        List<QueryDocumentSnapshot> documents = db.collection("videojuegos").get().get().getDocuments();

        if (documents.isEmpty()) {
            response.put("status", "ERROR");
            response.put("message", "No se encontraron juegos en la base de datos.");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }

        // Elegimos un índice aleatorio
        int randomIndex = new Random().nextInt(documents.size());
        QueryDocumentSnapshot document = documents.get(randomIndex);

        // Convertimos a objeto Juego e hidratamos el ID
        Juego juego = document.toObject(Juego.class);
        juego.setId(document.getId());

        // Devolvemos la respuesta usando tu método de mapeo existente
        response.put("status", "OK");
        response.put("data", mapJuegoResponse(juego));

        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @Operation(summary = "Obtener detalle de un juego", description = "Devuelve toda la información de un videojuego específico mediante su ID de Firestore.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Juego encontrado"),
            @ApiResponse(responseCode = "404", description = "El ID proporcionado no pertenece a ningún juego")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable String id) throws ExecutionException, InterruptedException {
        response.clear();
        Firestore db = FirestoreClient.getFirestore();
        DocumentSnapshot document = db.collection("videojuegos").document(id).get().get();

        if (document.exists()) {
            Juego juego = document.toObject(Juego.class);
            juego.setId(document.getId());
            response.put("status", "OK");
            response.put("data", mapJuegoResponse(juego));

            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        response.put("status", "ERROR");
        response.put("message", "Can't find videogame with id: " + id);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @Operation(summary = "Crear nuevo videojuego", description = "Añade un nuevo juego a la base de datos. Requiere token de autenticación.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Juego creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Faltan campos obligatorios (título, subtítulo, fecha...)"),
            @ApiResponse(responseCode = "401", description = "No autorizado - Token inválido o ausente")
    })
    @PostMapping("/")
    public ResponseEntity<Map<String, Object>> nuevoJuego(@RequestBody Juego juego, @AuthenticationPrincipal String uid) throws ExecutionException, InterruptedException {
        response.clear();

        if (uid == null) {
            response.put("status", "ERROR");
            response.put("message", "Valid Token is required");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        if (juego == null ||
                juego.getTitulo() == null || juego.getTitulo().isEmpty() ||
                juego.getSubtitulo() == null || juego.getSubtitulo().isEmpty() ||
                juego.getDescripcion() == null || juego.getDescripcion().isEmpty() ||
                juego.getFechaLanzamiento() == null
        ) {
            response.put("status", "ERROR");
            response.put("message", "Title, subtitle, description and release date are required");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        Firestore db = FirestoreClient.getFirestore();

        Date ahora = new Date();
        juego.setFechaCreacion(ahora);
        juego.setFechaActualizacion(ahora);

        if (juego.getNotas() == null) juego.setNotas(new ArrayList<>());
        if (juego.getPlataformas() == null) juego.setPlataformas(new ArrayList<>());
        if (juego.getGeneros() == null) juego.setGeneros(new ArrayList<>());
        if (juego.getTags() == null) juego.setTags(new ArrayList<>());

        ApiFuture<DocumentReference> docRef = db.collection("videojuegos").add(juego);
        juego.setId(docRef.get().getId());

        EmpresaController empresaController = new EmpresaController();
        if (juego.getIdDesarrolladora() != null && !juego.getIdDesarrolladora().isEmpty()) {
            if (!empresaController.insertaJuegoDesarollado(juego.getId(), juego.getIdDesarrolladora())) {
                response.put("warning", "Can't find developer assigned");
            }
        }
        if (juego.getIdPublisher() != null && !juego.getIdPublisher().isEmpty()) {
            if (!empresaController.insertaJuegoPublicado(juego.getId(), juego.getIdPublisher())) {
                response.put("warning", "Can't find publisher assigned");
            }
        }

        response.put("status", "OK");
        response.put("data", mapJuegoResponse(juego));

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }


    @Operation(summary = "Actualizar un juego", description = "Modifica los datos de un juego existente. Limpia la caché automática de juegos.", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> actualizaJuego(@PathVariable String id, @RequestBody Juego juego) throws ExecutionException, InterruptedException {
        response.clear();

        String uid = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (uid == null) {
            response.put("status", "ERROR");
            response.put("message", "Valid Token is required");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        if (juego == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        Firestore db = FirestoreClient.getFirestore();
        DocumentReference docRef = db.collection("videojuegos").document(id);
        DocumentSnapshot snapshot = docRef.get().get();

        if (snapshot.exists()) {
            Juego juegoExistente = snapshot.toObject(Juego.class);

            juego.setFechaCreacion(juegoExistente.getFechaCreacion());
            juego.setFechaActualizacion(new Date());
            juego.setId(id);

            docRef.set(juego);

            EmpresaController empresaController = new EmpresaController();
            if (juego.getIdDesarrolladora() != juegoExistente.getIdDesarrolladora()) {
                if (!empresaController.insertaJuegoDesarollado(juego.getId(), juego.getIdDesarrolladora())) {
                    response.put("warning", "Can't find developer assigned");
                }
            }
            if (juego.getIdPublisher() != juegoExistente.getIdPublisher()) {
                if (!empresaController.insertaJuegoPublicado(juego.getId(), juego.getIdPublisher())) {
                    response.put("warning", "Can't find publisher assigned");
                }
            }

            response.put("status", "OK");
            response.put("data", mapJuegoResponse(juego));
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        response.put("status", "ERROR");
        response.put("message", "Can't find videogame with id: " + id);
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @Operation(summary = "Eliminar un juego", description = "Borra físicamente el juego de Firestore y limpia sus referencias en colecciones, reviews y usuarios.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Juego eliminado correctamente"),
            @ApiResponse(responseCode = "404", description = "No se encontró el juego para eliminar")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> borraJuego(@PathVariable String id, @AuthenticationPrincipal String uid) throws ExecutionException, InterruptedException {
        response.clear();

        if (uid == null) {
            response.put("status", "ERROR");
            response.put("message", "Valid Token is required");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        Firestore db = FirestoreClient.getFirestore();
        DocumentReference docRef = db.collection("videojuegos").document(id);

        if (docRef.get().get().exists()) {
            docRef.delete();
            ColeccionController coleccionController = new ColeccionController();
            ReviewController reviewController = new ReviewController();
            UsuarioController usuarioController = new UsuarioController();
            usuarioController.eliminaJuego(id);
            reviewController.eliminaReviewsDeJuego(id);
            coleccionController.eliminaJuegoDeColecciones(id);
            response.put("status", "OK");
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        response.put("status", "ERROR");
        response.put("message", "Can't find videogame with id: " + id);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    public void nuevaReview(Review review) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        DocumentReference docRef = db.collection("videojuegos").document(review.getIdJuego());
        DocumentSnapshot snapshot = docRef.get().get();

        Juego juego = snapshot.toObject(Juego.class);
        if (juego.getNotas() == null) juego.setNotas(new ArrayList<>());

        juego.getNotas().add(review.getNota());
        docRef.set(juego);
    }

    private Map<String, Object> mapJuegoResponse(Juego juego) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", juego.getId());
        map.put("titulo", juego.getTitulo());
        map.put("subtitulo", juego.getSubtitulo());
        map.put("descripcion", juego.getDescripcion());
        map.put("fechaLanzamiento", juego.getFechaLanzamiento());
        map.put("urlPortada", juego.getUrlPortada());
        map.put("urlFondo", juego.getUrlFondo());
        map.put("minutosDuracion", juego.getMinutosDuracion());
        map.put("minutosDuracionCompleto", juego.getMinutosDuracionCompleto());
        map.put("plataformas", juego.getPlataformas());
        map.put("generos", juego.getGeneros());
        map.put("tags", juego.getTags());

        // HIDRATACIÓN: Aquí pedimos los objetos completos
        map.put("desarrolladora", obtenerDatosEmpresa(juego.getIdDesarrolladora()));
        map.put("publisher", obtenerDatosEmpresa(juego.getIdPublisher()));

        map.put("fechaCreacion", juego.getFechaCreacion());
        map.put("fechaActualizacion", juego.getFechaActualizacion());

        int numNotas = (juego.getNotas() != null) ? juego.getNotas().size() : 0;
        map.put("numNotas", numNotas);
        map.put("notaMedia", numNotas > 0 ? juego.getNotaMedia() : 0.0f);

        return map;
    }


    /*@Operation(summary = "MIGRACIÓN: Inicializar array de notas", description = "Script de un solo uso para actualizar todos los juegos antiguos que no tienen el array de notas inicializado.")
    @PostMapping("/admin/migrar-notas")
    public ResponseEntity<Map<String, Object>> migrarEsquemaNotas(@AuthenticationPrincipal String uid) throws ExecutionException, InterruptedException {
        response.clear();

        Firestore db = FirestoreClient.getFirestore();
        // Obtenemos TODOS los juegos
        List<QueryDocumentSnapshot> documents = db.collection("videojuegos").get().get().getDocuments();

        int actualizados = 0;
        WriteBatch batch = db.batch(); // Usamos batch para ser más eficientes (máx 500 ops por batch)
        int contadorBatch = 0;

        for (QueryDocumentSnapshot document : documents) {
            Juego juego = document.toObject(Juego.class);
            boolean necesitaCambios = false;

            // 1. Si la lista es nula, la inicializamos
            if (juego.getNotas() == null) {
                juego.setNotas(new ArrayList<>());
                necesitaCambios = true;
            }

            *//* 💡 OPCIONAL: MANTENER LA NOTA MEDIA ANTIGUA
               Si el juego tiene una 'notaMedia' antigua guardada en BD pero el array está vacío,
               podemos crear una nota "ficticia" para no perder esa valoración.

               Descomenta esto si quieres conservar el rating antiguo:
            *//*
            *//*
            Double notaMediaAntigua = document.getDouble("notaMedia"); // Leer el campo antiguo crudo
            if (juego.getNotas().isEmpty() && notaMediaAntigua != null && notaMediaAntigua > 0) {
                // Añadimos esa nota media como una nota entera (redondeada) al array
                juego.getNotas().add((int) Math.round(notaMediaAntigua));
                necesitaCambios = true;
            }
            *//*

            // Solo escribimos en la BD si hubo cambios
            if (necesitaCambios) {
                DocumentReference docRef = db.collection("videojuegos").document(document.getId());
                batch.set(docRef, juego); // Sobreescribimos con el nuevo modelo limpio
                actualizados++;
                contadorBatch++;

                // Firestore limita los batchs a 500 operaciones. Si llegamos, comiteamos y reseteamos.
                if (contadorBatch == 490) {
                    batch.commit().get();
                    batch = db.batch();
                    contadorBatch = 0;
                }
            }
        }

        // Commitear los restantes
        if (contadorBatch > 0) {
            batch.commit().get();
        }

        response.put("status", "OK");
        response.put("message", "Migración completada.");
        response.put("juegos_procesados", documents.size());
        response.put("juegos_actualizados", actualizados);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }*/

    private Map<String, Object> obtenerDatosEmpresa(String idEmpresa) {
        if (idEmpresa == null || idEmpresa.isEmpty()) return null;

        try {
            Firestore db = FirestoreClient.getFirestore();
            DocumentSnapshot doc = db.collection("empresas").document(idEmpresa).get().get();

            if (doc.exists()) {
                Map<String, Object> data = doc.getData();
                data.put("id", doc.getId());
                return data;
            }
        } catch (Exception e) {
            // Si hay un error de conexión o lectura, devolvemos null para no romper la respuesta
            System.err.println("Error al obtener empresa: " + e.getMessage());
        }
        return null;
    }
}