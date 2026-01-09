package com.example.demo.controllers;

import com.example.demo.models.Review;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> getAll() throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        List<QueryDocumentSnapshot> documents = db.collection("reviews").get().get().getDocuments();

        ArrayList<Review> reviews = new ArrayList<>();
        for (QueryDocumentSnapshot document : documents) {
            Review review = document.toObject(Review.class);
            review.setId(document.getId());
            reviews.add(review);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("page", 1);
        response.put("data", reviews);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    //TODO hacer que los videojuegos tengan un atributo que sea added y cuente cada vez que se crea una review de ese juego entonces se actualiza aqui
    @PostMapping("/")
    public ResponseEntity<Map<String, Object>> nuevaReview(@RequestBody Review review) throws ExecutionException, InterruptedException {
        Map<String, Object> response = new HashMap<>();

        if (review == null || review.getIdJuego() == null || review.getIdUsuario() == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        JuegoController juegoController = new JuegoController();
        if (juegoController.getById(review.getIdJuego()).getStatusCode() == HttpStatus.NOT_FOUND) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        UsuarioController usuarioController = new UsuarioController();
        if (usuarioController.getById(review.getIdUsuario()).getStatusCode() == HttpStatus.NOT_FOUND) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Firestore db = FirestoreClient.getFirestore();

        Date ahora = new Date();
        review.setFechaCreacion(ahora);
        review.setFechaActualizacion(ahora);

        String id = db.collection("reviews").add(review).get().getId();
        review.setId(id);

        response.put("status", "OK");
        response.put("data", review);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> borraReview(@PathVariable String id) throws ExecutionException, InterruptedException {
        Map<String, Object> response = new HashMap<>();
        Firestore db = FirestoreClient.getFirestore();
        DocumentReference docRef = db.collection("reviews").document(id);

        if (docRef.get().get().exists()) {
            docRef.delete();
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        response.put("status", "ERROR");
        response.put("message", "Can't find review with id: " + id);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
}