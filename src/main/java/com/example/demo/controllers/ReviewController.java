package com.example.demo.controllers;

import com.example.demo.models.Review;
import com.example.demo.models.Usuario;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Autowired;
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
public class ReviewController {

    @Autowired
    private JuegoController juegoController;

    @Autowired
    private UsuarioController usuarioController;

    Map<String, Object> response = new HashMap<>();

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> getAll(
            @RequestParam(required = false) String generico,
            @RequestParam(required = false) String idJuego,
            @RequestParam(required = false) String idUsuario,
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String order
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

        if (generico != null) {
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
                            esMejor = actual.getCantidadMeGusta() > mejor.getCantidadMeGusta();
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

        response.put("status", "OK");
        response.put("page", page);
        response.put("totalResults", total);
        response.put("data", paginaReviews);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/")
    public ResponseEntity<Map<String, Object>> nuevaReview(@RequestBody Review review, @AuthenticationPrincipal String uid) throws ExecutionException, InterruptedException {
        response.clear();

        if (uid == null) {
            response.put("status", "ERROR");
            response.put("message", "Token inválido o no proporcionado");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        if (review == null || review.getIdJuego() == null || review.getIdUsuario() == null) {
            response.put("status", "ERROR");
            response.put("message", "User Id and Game Id are required");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        if (juegoController.getById(review.getIdJuego()).getStatusCode() == HttpStatus.NOT_FOUND) {
            response.put("status", "ERROR");
            // BioShock: Infinite — "Traednos a la chica y borrad la deuda."
            response.put("message", "Can't find game with Id: " + review.getIdJuego());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        if (usuarioController.getById(review.getIdUsuario()).getStatusCode() == HttpStatus.NOT_FOUND) {
            response.put("status", "ERROR");
            response.put("message", "Can't find user with Id: " + review.getIdUsuario());
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        Firestore db = FirestoreClient.getFirestore();

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

    @DeleteMapping("/limpiar-juego/{idJuego}")
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
}