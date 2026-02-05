package com.example.demo.controllers;

import com.example.demo.models.Juego;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/v1/juegos_pendientes")
@CrossOrigin(origins = "https://backend-gameboxd-1.onrender.com")
@Tag(name = "Moderación", description = "Gestión de propuestas de juegos pendientes de aprobación por administradores")
public class JuegosSinAprobarController {

    @Autowired
    private EmpresaController empresaController;

    private Map<String, Object> response = new HashMap<>();


    @Operation(summary = "Listar propuestas", description = "Obtiene todos los juegos sugeridos que aún no han sido aprobados para el catálogo oficial.")
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> buscarPorTitulo(
            @Parameter(description = "Buscar propuesta por título") @RequestParam(required = false) String titulo,
            @Parameter(description = "Resultados por página") @RequestParam(required = false, defaultValue = "10") Integer limit,
            @Parameter(description = "Número de página") @RequestParam(required = false, defaultValue = "1") Integer page,
            @Parameter(description = "Orden cronológico", schema = @io.swagger.v3.oas.annotations.media.Schema(allowableValues = {"asc", "desc"})) @RequestParam(required = false) String order
    ) throws ExecutionException, InterruptedException {
        response.clear();
        Firestore db = FirestoreClient.getFirestore();
        List<QueryDocumentSnapshot> documents = db.collection("juegosSinAprobacion").get().get().getDocuments();

        ArrayList<Juego> juegos = new ArrayList<>();
        for (QueryDocumentSnapshot document : documents) {
            Juego juego = document.toObject(Juego.class);
            juego.setId(document.getId());
            juegos.add(juego);
        }

        Stream<Juego> stream = juegos.stream();

        if (titulo != null && !titulo.isEmpty()) {
            stream = stream.filter(j -> j.getTitulo().toLowerCase().contains(titulo.toLowerCase()));
        }

        List<Juego> filtrados = stream.collect(Collectors.toList());

        for (int i = 0; i < filtrados.size() - 1; i++) {
            int indiceMejor = i;
            for (int j = i + 1; j < filtrados.size(); j++) {
                boolean esMejor = false;
                Juego actual = filtrados.get(j);
                Juego mejor = filtrados.get(indiceMejor);

                if (actual.getFechaCreacion() != null && mejor.getFechaCreacion() != null) {
                    esMejor = actual.getFechaCreacion().before(mejor.getFechaCreacion());
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

        int total = filtrados.size();
        int start = Math.min(Math.max(0, (page - 1) * limit), total);
        int end = Math.min(start + limit, total);

        List<Juego> paginaJuegos = filtrados.subList(start, end);

        response.put("status", "OK");
        response.put("page", page);
        response.put("totalResults", total);
        response.put("data", paginaJuegos);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "Detalle de propuesta", description = "Recupera la información completa de un juego pendiente mediante su ID.")
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable String id) throws ExecutionException, InterruptedException {
        response.clear();
        Firestore db = FirestoreClient.getFirestore();
        DocumentSnapshot document = db.collection("juegosSinAprobacion").document(id).get().get();

        if (document.exists()) {
            Juego juego = document.toObject(Juego.class);
            juego.setId(document.getId());
            response.put("status", "OK");
            response.put("data", juego);

            return new ResponseEntity<>(response, HttpStatus.OK);
        }

        response.put("status", "ERROR");
        response.put("message", "Can't find pending game with id: " + id);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @Operation(summary = "Sugerir nuevo juego", description = "Envía una propuesta de juego a la cola de moderación. Requiere estar autenticado.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Propuesta enviada con éxito"),
            @ApiResponse(responseCode = "400", description = "El título es obligatorio o la empresa vinculada no existe")
    })
    @PostMapping("/")
    public ResponseEntity<Map<String, Object>> nuevoJuego(@RequestBody Juego juego, @AuthenticationPrincipal String uid) throws ExecutionException, InterruptedException {
        response.clear();

        if (uid == null) {
            response.put("status", "ERROR");
            response.put("message", "Valid Token is required");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        if (juego == null || juego.getTitulo() == null || juego.getTitulo().isEmpty()) {
            response.put("status", "ERROR");
            response.put("message", "Título obligatorio");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }



        Firestore db = FirestoreClient.getFirestore();

        if (juego.getIdDesarrolladora() != null && !juego.getIdDesarrolladora().isEmpty()) {
            if (!empresaController.insertaJuegoDesarollado(juego.getId(), juego.getIdDesarrolladora())) {
                response.put("status", "ERROR");
                response.put("message", "Can't find developer with id: " + juego.getIdDesarrolladora());
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
        }
        if (juego.getIdPublisher() != null && !juego.getIdPublisher().isEmpty()) {
            if (!empresaController.insertaJuegoPublicado(juego.getId(), juego.getIdPublisher())) {
                response.put("status", "ERROR");
                response.put("message", "Can't find publisher with id: " + juego.getIdPublisher());
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
        }

        Date ahora = new Date();
        juego.setFechaCreacion(ahora);
        juego.setFechaActualizacion(ahora);


        if (juego.getNotas() == null) juego.setNotas(new ArrayList<>());
        if (juego.getPlataformas() == null) juego.setPlataformas(new ArrayList<>());
        if (juego.getGeneros() == null) juego.setGeneros(new ArrayList<>());
        if (juego.getTags() == null) juego.setTags(new ArrayList<>());

        ApiFuture<DocumentReference> docRef = db.collection("juegosSinAprobacion").add(juego);
        juego.setId(docRef.get().getId());

        response.put("status", "OK");
        response.put("data", juego);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Editar propuesta", description = "Permite a un moderador o al usuario corregir datos de la sugerencia antes de su aprobación final.", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> actualizaJuego(@PathVariable String id, @RequestBody Juego juego, @AuthenticationPrincipal String uid) throws ExecutionException, InterruptedException {
        response.clear();

        if (uid == null) {
            response.put("status", "ERROR");
            response.put("message", "Valid Token is required");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        if (juego == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        Firestore db = FirestoreClient.getFirestore();
        DocumentReference docRef = db.collection("juegosSinAprobacion").document(id);
        DocumentSnapshot snapshot = docRef.get().get();

        if (snapshot.exists()) {
            Juego juegoExistente = snapshot.toObject(Juego.class);

            juego.setFechaCreacion(juegoExistente.getFechaCreacion());
            juego.setFechaActualizacion(new Date());
            juego.setId(id);



            docRef.set(juego);

            if (juego.getIdDesarrolladora() != null && !juego.getIdDesarrolladora().equals(juegoExistente.getIdDesarrolladora())) {
                if (!empresaController.insertaJuegoDesarollado(juego.getId(), juego.getIdDesarrolladora())) {
                    response.put("warning", "Can't find developer assigned");
                }
            }
            if (juego.getIdPublisher() != null && !juego.getIdPublisher().equals(juegoExistente.getIdPublisher())) {
                if (!empresaController.insertaJuegoPublicado(juego.getId(), juego.getIdPublisher())) {
                    response.put("warning", "Can't find publisher assigned");
                }
            }

            response.put("status", "OK");
            response.put("data", juego);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }

        response.put("status", "ERROR");
        response.put("message", "Can't find pending game with id: " + id);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @Operation(summary = "Rechazar propuesta", description = "Elimina el juego de la cola de moderación. Se usa para descartar sugerencias duplicadas o incorrectas.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Propuesta eliminada correctamente"),
            @ApiResponse(responseCode = "404", description = "No se encontró la propuesta")
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
        DocumentReference docRef = db.collection("juegosSinAprobacion").document(id);

        if (docRef.get().get().exists()) {
            docRef.delete();
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        response.put("status", "ERROR");
        response.put("message", "Can't find pending game with id: " + id);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
}