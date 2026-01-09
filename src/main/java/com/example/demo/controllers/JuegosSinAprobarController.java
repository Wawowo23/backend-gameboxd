package com.example.demo.controllers;

import com.example.demo.models.Juego;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/v1/juegos_pendientes")
public class JuegosSinAprobarController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> getAll() throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        List<QueryDocumentSnapshot> documents = db.collection("juegosSinAprobacion").get().get().getDocuments();

        ArrayList<Juego> juegos = new ArrayList<>();
        for (QueryDocumentSnapshot document : documents) {
            Juego juego = document.toObject(Juego.class);
            juego.setId(document.getId());
            juegos.add(juego);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("page", 1);
        response.put("data", juegos);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable String id) throws ExecutionException, InterruptedException {
        Map<String, Object> response = new HashMap<>();
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

    //TODO si viene sin una URL hay que poner un placeholder
    @PostMapping("/")
    public ResponseEntity<Map<String, Object>> nuevoJuego(@RequestBody Juego juego) throws ExecutionException, InterruptedException {
        Map<String, Object> response = new HashMap<>();

        if (juego == null || juego.getTitulo() == null || juego.getTitulo().isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Firestore db = FirestoreClient.getFirestore();

        EmpresaController empresaController = new EmpresaController();

        if (juego.getIdDesarrolladora() != null && !juego.getIdDesarrolladora().isEmpty()) {
            if (!empresaController.insertaJuegoDesarollado(juego.getId(),juego.getIdDesarrolladora())) {
                response.put("status","ERROR");
                response.put("message","Can't find developer with id: " + juego.getIdDesarrolladora());
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
        }
        if (juego.getIdPublisher() != null && !juego.getIdPublisher().isEmpty()) {
            if (!empresaController.insertaJuegoPublicado(juego.getId(),juego.getIdDesarrolladora())) {
                response.put("status","ERROR");
                response.put("message","Can't find publisher with id: " + juego.getIdDesarrolladora());
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }
        }

        Date ahora = new Date();
        juego.setFechaCreacion(ahora);
        juego.setFechaActualizacion(ahora);

        juego.setNotaMedia(0.0f);
        juego.setNumeroReviews(0);

        if (juego.getPlataformas() == null) juego.setPlataformas(new ArrayList<>());
        if (juego.getGeneros() == null) juego.setGeneros(new ArrayList<>());
        if (juego.getTags() == null) juego.setTags(new ArrayList<>());

        ApiFuture<DocumentReference> docRef = db.collection("juegosSinAprobacion").add(juego);
        juego.setId(docRef.get().getId());

        response.put("status", "OK");
        response.put("data", juego);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> actualizaJuego(@PathVariable String id, @RequestBody Juego juego) throws ExecutionException, InterruptedException {
        Map<String, Object> response = new HashMap<>();
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
            response.put("status", "OK");
            response.put("data", juego);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }

        response.put("status", "ERROR");
        response.put("message", "Can't find pending game with id: " + id);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> borraJuego(@PathVariable String id) throws ExecutionException, InterruptedException {
        Map<String, Object> response = new HashMap<>();
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