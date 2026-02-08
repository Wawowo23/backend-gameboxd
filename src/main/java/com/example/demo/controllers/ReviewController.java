package com.example.demo.controllers;

import com.example.demo.models.Review;
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
@RequestMapping("/api/v1/reviews")
@Tag(name = "Reviews", description = "Gestión de reseñas, valoraciones y sistema de likes")
public class ReviewController {

    @Autowired
    private JuegoController juegoController;

    @Autowired
    private UsuarioController usuarioController;

    Map<String, Object> response = new HashMap<>();

    @Cacheable(value = "reviews",key = "{#generico, #page, #limit, #sort}")
    @Operation(summary = "Listado de reviews", description = "Obtiene reviews con filtros por juego, usuario o búsqueda de texto.")
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> getAll(
            @Parameter(description = "Búsqueda por texto en el contenido del comentario") @RequestParam(required = false) String generico,
            @Parameter(description = "Filtrar todas las reviews de un juego específico") @RequestParam(required = false) String idJuego,
            @Parameter(description = "Filtrar todas las reviews de un usuario específico") @RequestParam(required = false) String idUsuario,
            @Parameter(description = "Cantidad de resultados") @RequestParam(required = false, defaultValue = "10") Integer limit,
            @Parameter(description = "Número de página") @RequestParam(required = false, defaultValue = "1") Integer page,
            @Parameter(description = "Criterio de ordenación", schema = @Schema(allowableValues = {"likes", "date", "rating"})) @RequestParam(required = false) String sort,
            @Parameter(description = "Orden", schema = @Schema(allowableValues = {"asc", "desc"})) @RequestParam(required = false) String order
    ) throws ExecutionException, InterruptedException {
        response.clear();
        Firestore db = FirestoreClient.getFirestore();
        List<QueryDocumentSnapshot> documents = db.collection("reviews").get().get().getDocuments();

        ArrayList<Review> reviews = new ArrayList<>();
        for (QueryDocumentSnapshot document : documents) {
            Review review = document.toObject(Review.class);
            review.setId(document.getId());
            reviews.add(review);
        }

        Stream<Review> stream = reviews.stream();

        //TODO arreglasr
        if (generico != null && !generico.isEmpty()) {
            stream = stream.filter(r -> r.getComentario() != null &&
                    r.getComentario().toLowerCase().contains(generico.toLowerCase()));
        } else {
            if (idJuego != null) stream = stream.filter(r -> r.getIdJuego().equals(idJuego));
            if (idUsuario != null) stream = stream.filter(r -> r.getIdUsuario().equals(idUsuario));
        }

        List<Review> filtrados = stream.collect(Collectors.toList());

        if (sort != null) {
            for (int i = 0; i < filtrados.size() - 1; i++) {
                int indiceMejor = i;
                for (int j = i + 1; j < filtrados.size(); j++) {
                    boolean esMejor = false;
                    Review actual = filtrados.get(j);
                    Review mejor = filtrados.get(indiceMejor);

                    switch (sort.toLowerCase()) {
                        case "likes":
                            esMejor = actual.getLikes().size() > mejor.getLikes().size();
                            break;
                        case "date":
                            if (actual.getFechaCreacion() != null && mejor.getFechaCreacion() != null)
                                esMejor = actual.getFechaCreacion().before(mejor.getFechaCreacion());
                            break;
                        case "rating":
                            esMejor = actual.getNota() > mejor.getNota();
                            break;
                        default:
                            esMejor = false;
                            break;
                    }
                    if (esMejor) indiceMejor = j;
                }
                Review temp = filtrados.get(indiceMejor);
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

        List<Review> paginaReviews = filtrados.subList(start, end);

        ArrayList<Map<String, Object>> listaReviews = new ArrayList<>();
        for (Review review : paginaReviews) {
            listaReviews.add(hidratarReview(review));
        }

        response.put("status", "OK");
        response.put("page", page);
        response.put("totalResults", total);
        response.put("data", listaReviews);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @Operation(summary = "Publicar una nueva review", description = "Crea una reseña y actualiza automáticamente la nota media del juego y el perfil del usuario.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Review creada y vinculada correctamente"),
            @ApiResponse(responseCode = "400", description = "ID de juego o usuario no válidos"),
            @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    @PostMapping("/")
    public ResponseEntity<Map<String, Object>> nuevaReview(@RequestBody Review review, @AuthenticationPrincipal String uid) throws ExecutionException, InterruptedException {
        response.clear();

        if (uid == null) {
            response.put("status", "ERROR");
            response.put("message", "Valid Token is required");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        if (review == null || review.getIdJuego() == null || review.getIdUsuario() == null) {
            response.put("status", "ERROR");
            response.put("message", "User Id and Game Id are required");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        if (juegoController.getById(review.getIdJuego()).getStatusCode() == HttpStatus.NOT_FOUND) {
            response.put("status", "ERROR");
            response.put("message", "Can't find game with Id: " + review.getIdJuego());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        if (usuarioController.getById(review.getIdUsuario()).getStatusCode() == HttpStatus.NOT_FOUND) {
            response.put("status", "ERROR");
            response.put("message", "Can't find user with Id: " + review.getIdUsuario());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        Firestore db = FirestoreClient.getFirestore();

        if (review.getLikes() == null) review.setLikes(new ArrayList<>());
        Date ahora = new Date();
        review.setFechaCreacion(ahora);
        review.setFechaActualizacion(ahora);

        String id = db.collection("reviews").add(review).get().getId();
        review.setId(id);

        usuarioController.insertaReview(review);
        juegoController.nuevaReview(review);

        response.put("status", "OK");
        response.put("data", review);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Dar o quitar Like", description = "Añade el ID del usuario a la lista de likes si no estaba, o lo elimina si ya existía (Toggle).",security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Like procesado correctamente"),
            @ApiResponse(responseCode = "404", description = "La review no existe")
    })
    @PatchMapping("/like/{id}")
    public ResponseEntity<Map<String, Object>> nuevoLike(
            @PathVariable String id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal String uid
    ) throws ExecutionException, InterruptedException {

        response.clear();
        String userId = body.get("idUsuario");

        if (userId == null || userId.isEmpty()) {
            response.put("status", "ERROR");
            response.put("message", "idUsuario is required in the request body");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        Firestore db = FirestoreClient.getFirestore();
        DocumentReference docRef = db.collection("reviews").document(id);
        DocumentSnapshot document = docRef.get().get();

        if (document.exists()) {
            Review review = document.toObject(Review.class);

            if (review.getLikes() == null) {
                review.setLikes(new ArrayList<>());
            }

            ArrayList<String> likes = review.getLikes();


            if (likes.contains(userId)) {
                likes.remove(userId);
            } else {
                likes.add(userId);
            }

            review.setLikes(likes);
            review.setFechaActualizacion(new Date());

            docRef.set(review);

            response.put("status", "OK");
            response.put("message", likes.contains(userId) ? "Like removed" : "Like added");
            response.put("data", review);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }

        response.put("status", "ERROR");
        response.put("message", "Review not found");
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }


    public ResponseEntity<Map<String, Object>> eliminaReviewsDeJuego(@PathVariable String idJuego) throws ExecutionException, InterruptedException {
        response.clear();
        Firestore db = FirestoreClient.getFirestore();

        ApiFuture<QuerySnapshot> future = db.collection("reviews").whereEqualTo("idJuego", idJuego).get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        if (documents.isEmpty()) {
            response.put("status", "OK");
            response.put("message", "No se encontraron reviews para el juego: " + idJuego);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }

        WriteBatch batch = db.batch();
        for (QueryDocumentSnapshot document : documents) {
            batch.delete(document.getReference());
        }

        batch.commit().get();

        response.put("status", "OK");
        response.put("message", "Se han eliminado " + documents.size() + " reviews asociadas al juego.");

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    public void eliminaReviewsUsuario(Usuario usuario) throws ExecutionException, InterruptedException {
        for (String id : usuario.getReviews()) {
            Firestore db = FirestoreClient.getFirestore();
            DocumentReference docRef = db.collection("reviews").document(id);
            docRef.delete();
        }
    }

    private Map<String, Object> hidratarReview(Review review) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        Map<String, Object> map = new HashMap<>();

        // Copiamos datos básicos
        map.put("id", review.getId());
        map.put("nota", review.getNota());
        map.put("comentario", review.getComentario());
        map.put("idUsuario", review.getIdUsuario());
        map.put("fechaCreacion", review.getFechaCreacion());
        map.put("likes", review.getLikes());

        // Hidratamos el JUEGO
        DocumentSnapshot gameDoc = db.collection("videojuegos").document(review.getIdJuego()).get().get();
        if (gameDoc.exists()) {
            Map<String, Object> juego = gameDoc.getData();
            juego.put("id", gameDoc.getId());
            map.put("juego", juego); // Añadimos el objeto completo
        }

        return map;
    }
}