package com.example.demo.controllers;

import com.example.demo.models.Coleccion;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/v1/colecciones")
public class ColeccionController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> getAll() throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        List<QueryDocumentSnapshot> documents = db.collection("colecciones").get().get().getDocuments();

        ArrayList<Coleccion> colecciones = new ArrayList<>();
        for (QueryDocumentSnapshot document : documents) {
            Coleccion coleccion = document.toObject(Coleccion.class);
            coleccion.setId(document.getId());
            colecciones.add(coleccion);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("page", 1);
        response.put("data", colecciones);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable String id) throws ExecutionException, InterruptedException {
        Map<String, Object> response = new HashMap<>();
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

    @PostMapping("/")
    public ResponseEntity<Map<String, Object>> nuevaColeccion(@RequestBody Coleccion coleccion) throws ExecutionException, InterruptedException {
        Map<String, Object> response = new HashMap<>();

        if (coleccion == null || coleccion.getNombre() == null || coleccion.getNombre().isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Firestore db = FirestoreClient.getFirestore();

        Date ahora = new Date();
        coleccion.setCantidadMeGusta(0);
        coleccion.setFechaCreacion(ahora);
        coleccion.setFechaActualizacion(ahora);

        if (coleccion.getJuegos() == null) coleccion.setJuegos(new ArrayList<>());

        ApiFuture<DocumentReference> docRef = db.collection("colecciones").add(coleccion);
        coleccion.setId(docRef.get().getId());

        response.put("status", "OK");
        response.put("data", coleccion);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> actualizaColeccion(@PathVariable String id, @RequestBody Coleccion coleccion) throws ExecutionException, InterruptedException {
        Map<String, Object> response = new HashMap<>();
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> borraColeccion(@PathVariable String id) throws ExecutionException, InterruptedException {
        Map<String, Object> response = new HashMap<>();
        Firestore db = FirestoreClient.getFirestore();
        DocumentReference docRef = db.collection("colecciones").document(id);

        if (docRef.get().get().exists()) {
            docRef.delete();
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        response.put("status", "ERROR");
        response.put("message", "Can't find collection with id: " + id);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
}