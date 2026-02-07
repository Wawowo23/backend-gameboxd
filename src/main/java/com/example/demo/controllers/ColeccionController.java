package com.example.demo.controllers;

import com.example.demo.models.Coleccion;
import com.example.demo.models.Usuario;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/v1/colecciones")
@CrossOrigin(origins = "https://backend-gameboxd-1.onrender.com")
@Tag(name = "Colecciones", description = "Gestión de listas personalizadas de juegos y curación de contenido")
public class ColeccionController {

    @Autowired
    private UsuarioController usuarioController;

    private Map<String, Object> response = new HashMap<>();

    @Operation(summary = "Obtener todas las colecciones", description = "Lista paginada de colecciones. Usa caché para mejorar el rendimiento.")
    @Cacheable(value = "colecciones",key = "{#generico, #page, #limit, #sort}")
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> getAll(
            @Parameter(description = "Búsqueda por nombre o descripción") @RequestParam(required = false) String generico,
            @Parameter(description = "Filtrar por el creador de la lista") @RequestParam(required = false) String idUsuario,
            @Parameter(description = "Cantidad por página") @RequestParam(required = false, defaultValue = "10") Integer limit,
            @Parameter(description = "Número de página") @RequestParam(required = false, defaultValue = "1") Integer page,
            @Parameter(description = "Criterio de ordenación", schema = @Schema(allowableValues = {"likes", "date", "name"})) @RequestParam(required = false) String sort,
            @Parameter(description = "Orden (asc/desc)") @RequestParam(required = false) String order
    ) throws ExecutionException, InterruptedException {
        response.clear();
        Firestore db = FirestoreClient.getFirestore();
        List<QueryDocumentSnapshot> documents = db.collection("colecciones").get().get().getDocuments();

        ArrayList<Coleccion> colecciones = new ArrayList<>();
        for (QueryDocumentSnapshot document : documents) {
            Coleccion coleccion = document.toObject(Coleccion.class);
            coleccion.setId(document.getId());
            colecciones.add(coleccion);
        }

        Stream<Coleccion> stream = colecciones.stream();

        if (generico != null  && !generico.isEmpty()) {
            String busqueda = generico.toLowerCase();
            stream = stream.filter(c ->
                    (c.getNombre() != null && c.getNombre().toLowerCase().contains(busqueda)) ||
                            (c.getDescripcion() != null && c.getDescripcion().toLowerCase().contains(busqueda))
            );
        } else {
            if (idUsuario != null)
                stream = stream.filter(c ->
                        (c.getIdUsuario() != null && c.getIdUsuario().equals(idUsuario)));
        }

        List<Coleccion> filtrados = stream.collect(Collectors.toList());

        if (sort != null) {
            for (int i = 0; i < filtrados.size() - 1; i++) {
                int indiceMejor = i;
                for (int j = i + 1; j < filtrados.size(); j++) {
                    boolean esMejor = false;
                    Coleccion actual = filtrados.get(j);
                    Coleccion mejor = filtrados.get(indiceMejor);

                    switch (sort.toLowerCase()) {
                        case "likes":
                            esMejor = actual.getCantidadMeGusta() > mejor.getCantidadMeGusta();
                            break;
                        case "date":
                            if (actual.getFechaCreacion() != null && mejor.getFechaCreacion() != null)
                                esMejor = actual.getFechaCreacion().before(mejor.getFechaCreacion());
                            break;
                        case "name":
                        default:
                            if (actual.getNombre() != null && mejor.getNombre() != null)
                                esMejor = actual.getNombre().compareToIgnoreCase(mejor.getNombre()) < 0;
                            break;
                    }
                    if (esMejor) indiceMejor = j;
                }
                Coleccion temp = filtrados.get(indiceMejor);
                filtrados.set(indiceMejor, filtrados.get(i));
                filtrados.set(i, temp);
            }
        }

        if ("desc".equalsIgnoreCase(order)) {
            Collections.reverse(filtrados);
        }

        int total = filtrados.size();
        int start = Math.min(Math.max(0, (page - 1) * limit), total);
        int end = Math.min(start + limit, total);

        List<Coleccion> paginaColecciones = filtrados.subList(start, end);

        ArrayList<Map<String, Object>> listaColecciones = new ArrayList<>();
        for (Coleccion coleccion : paginaColecciones) {
            listaColecciones.add(hidratarColeccion(coleccion));
        }

        response.put("status", "OK");
        response.put("page", page);
        response.put("totalResults", total);
        response.put("data", listaColecciones);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable String id) throws ExecutionException, InterruptedException {
        response.clear();
        Firestore db = FirestoreClient.getFirestore();
        DocumentSnapshot document = db.collection("colecciones").document(id).get().get();

        if (document.exists()) {
            Coleccion coleccion = document.toObject(Coleccion.class);
            coleccion.setId(document.getId());
            response.put("status", "OK");
            response.put("data", coleccion);

            return new ResponseEntity<>(response, HttpStatus.OK);
        }

        response.put("status", "ERROR");
        response.put("message", "Can't find collection with id: " + id);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @Operation(summary = "Crear nueva colección", description = "Crea una lista vacía o con juegos. Actualiza automáticamente el perfil del usuario.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Colección creada correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o usuario no encontrado"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    @PostMapping("/")
    public ResponseEntity<Map<String, Object>> nuevaColeccion(@RequestBody Coleccion coleccion, @AuthenticationPrincipal String uid) throws ExecutionException, InterruptedException {
        response.clear();

        if (uid == null) {
            response.put("status", "ERROR");
            response.put("message", "Valid Token is required");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        if (coleccion == null || coleccion.getNombre() == null || coleccion.getNombre().isEmpty()) {
            response.put("status", "ERROR");
            response.put("message", "Nombre de colección obligatorio");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        if (usuarioController.getById(coleccion.getIdUsuario()).getStatusCode() == HttpStatus.NOT_FOUND) {
            response.put("status", "ERROR");
            response.put("message","Can't find user with Id: " + coleccion.getIdUsuario());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        Firestore db = FirestoreClient.getFirestore();

        Date ahora = new Date();
        coleccion.setCantidadMeGusta(0);
        coleccion.setFechaCreacion(ahora);
        coleccion.setFechaActualizacion(ahora);

        if (coleccion.getJuegos() == null) coleccion.setJuegos(new ArrayList<>());

        ApiFuture<DocumentReference> docRef = db.collection("colecciones").add(coleccion);
        coleccion.setId(docRef.get().getId());

        usuarioController.insertaColeccion(coleccion);

        response.put("status", "OK");
        response.put("data", coleccion);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar colección", description = "Modifica los detalles de la colección y limpia la caché de juegos.", security = @SecurityRequirement(name = "bearerAuth"))
    @CacheEvict(value = "colecciones", allEntries = true)
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> actualizaColeccion(@PathVariable String id, @RequestBody Coleccion coleccion, @AuthenticationPrincipal String uid) throws ExecutionException, InterruptedException {
        response.clear();

        if (uid == null) {
            response.put("status", "ERROR");
            response.put("message", "Valid Token is required");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        if (coleccion == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        Firestore db = FirestoreClient.getFirestore();
        DocumentReference docRef = db.collection("colecciones").document(id);
        DocumentSnapshot snapshot = docRef.get().get();

        if (snapshot.exists()) {
            Coleccion coleccionExistente = snapshot.toObject(Coleccion.class);

            coleccion.setFechaCreacion(coleccionExistente.getFechaCreacion());
            coleccion.setFechaActualizacion(new Date());
            coleccion.setId(id);

            docRef.set(coleccion);

            response.put("status", "OK");
            response.put("data", coleccion);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }

        response.put("status", "ERROR");
        response.put("message", "Can't find collection with id: " + id);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @Operation(summary = "Borrar colección", description = "Elimina la colección de forma permanente y desvincula la referencia del usuario.", security = @SecurityRequirement(name = "bearerAuth"))
    @CacheEvict(value = "juegos", allEntries = true)
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> borraColeccion(@PathVariable String id, @AuthenticationPrincipal String uid) throws ExecutionException, InterruptedException {
        response.clear();



        if (uid == null) {
            response.put("status", "ERROR");
            response.put("message", "Valid Token is required");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        Firestore db = FirestoreClient.getFirestore();
        DocumentReference docRef = db.collection("colecciones").document(id);
        DocumentSnapshot snapshot = docRef.get().get();

        if (snapshot.exists()) {
            Coleccion coleccionExistente = snapshot.toObject(Coleccion.class);

            docRef.delete();
            usuarioController.eliminaColeccion(coleccionExistente);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        response.put("status", "ERROR");
        response.put("message", "Can't find collection with id: " + id);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    public void eliminaJuegoDeColecciones(String idJuego) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        List<QueryDocumentSnapshot> documents = db.collection("colecciones").get().get().getDocuments();

        for (QueryDocumentSnapshot document : documents) {
            Coleccion coleccion = document.toObject(Coleccion.class);
            coleccion.setId(document.getId());
            DocumentReference docRef = db.collection("colecciones").document(coleccion.getId());

            if (coleccion.getJuegos() != null) {
                boolean encontrado = coleccion.getJuegos().removeIf(juegoId -> juegoId.equals(idJuego));

                if (encontrado) {
                    docRef.set(coleccion);
                }
            }
        }
    }

    public void eliminaColeccionesUsuario(Usuario usuario) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        for (String coleccionId : usuario.getColecciones()) {
            DocumentReference docRef = db.collection("colecciones").document(coleccionId);
            docRef.delete();
        }
    }

    private Map<String, Object> hidratarColeccion(Coleccion coleccion) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        Map<String, Object> map = new HashMap<>();

        map.put("id", coleccion.getId());
        map.put("nombre", coleccion.getNombre());
        map.put("descripcion", coleccion.getDescripcion());
        map.put("idUsuario", coleccion.getIdUsuario());
        map.put("cantidadMeGusta", coleccion.getCantidadMeGusta());

        // Hidratar la lista de JUEGOS
        if (coleccion.getJuegos() != null && !coleccion.getJuegos().isEmpty()) {
            List<QueryDocumentSnapshot> gameDocs = db.collection("videojuegos")
                    .whereIn(FieldPath.documentId(), coleccion.getJuegos())
                    .get().get().getDocuments();

            List<Map<String, Object>> juegosCompletos = gameDocs.stream().map(d -> {
                Map<String, Object> j = d.getData();
                j.put("id", d.getId());
                return j;
            }).collect(Collectors.toList());

            map.put("juegos", juegosCompletos);
        } else {
            map.put("juegos", new ArrayList<>());
        }

        return map;
    }
}