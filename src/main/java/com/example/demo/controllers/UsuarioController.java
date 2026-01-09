package com.example.demo.controllers;

import com.example.demo.models.Usuario;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> getAll() throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        List<QueryDocumentSnapshot> documents = db.collection("usuarios").get().get().getDocuments();

        ArrayList<Usuario> usuarios = new ArrayList<>();
        for (QueryDocumentSnapshot document : documents) {
            Usuario usuario = document.toObject(Usuario.class);
            usuario.setId(document.getId());
            usuarios.add(usuario);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("page", 1);
        response.put("data", usuarios);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable String id) throws ExecutionException, InterruptedException {
        Map<String, Object> response = new HashMap<>();
        Firestore db = FirestoreClient.getFirestore();
        DocumentSnapshot document = db.collection("usuarios").document(id).get().get();

        if (document.exists()) {
            Usuario usuario = document.toObject(Usuario.class);
            usuario.setId(document.getId());
            response.put("status", "OK");
            response.put("data", usuario);

            return new ResponseEntity<>(response, HttpStatus.OK);
        }

        response.put("status", "ERROR");
        response.put("message", "Can't find user with id: " + id);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @PostMapping("/")
    public ResponseEntity<Map<String, Object>> registro(@RequestBody Usuario usuario) throws ExecutionException, InterruptedException {
        Map<String, Object> response = new HashMap<>();

        if (usuario == null ||
                usuario.getNombre() == null || usuario.getNombre().isEmpty() ||
                usuario.getEmail() == null || usuario.getEmail().isEmpty() ||
                usuario.getPass() == null || usuario.getPass().isEmpty() ||
                usuario.getFechaNacimiento() == null
        ) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Firestore db = FirestoreClient.getFirestore();

        // Inicialización de listas por defecto
        if (usuario.getFavoritos() == null) usuario.setFavoritos(new ArrayList<>());
        if (usuario.getDeseados() == null) usuario.setDeseados(new ArrayList<>());
        if (usuario.getReviews() == null) usuario.setReviews(new ArrayList<>());

        Date ahora = new Date();
        usuario.setFechaCreacion(ahora);
        usuario.setFechaActualizacion(ahora);

        ApiFuture<DocumentReference> docRef = db.collection("usuarios").add(usuario);
        usuario.setId(docRef.get().getId());

        response.put("status", "OK");
        response.put("data", usuario);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> actualizaUsuario(@PathVariable String id, @RequestBody Usuario usuario) throws ExecutionException, InterruptedException {
        Map<String, Object> response = new HashMap<>();
        if (usuario == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        Firestore db = FirestoreClient.getFirestore();
        DocumentReference docRef = db.collection("usuarios").document(id);
        DocumentSnapshot snapshot = docRef.get().get();

        if (snapshot.exists()) {
            Usuario usuarioExistente = snapshot.toObject(Usuario.class);

            // Mantenemos la fecha de creación original y actualizamos la de modificación
            usuario.setFechaCreacion(usuarioExistente.getFechaCreacion());
            usuario.setFechaActualizacion(new Date());
            usuario.setId(id);

            docRef.set(usuario);

            response.put("status", "OK");
            response.put("data", usuario);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }

        response.put("status", "ERROR");
        response.put("message", "Can't find user with id: " + id);
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> borraUsuario(@PathVariable String id) throws ExecutionException, InterruptedException {
        Map<String, Object> response = new HashMap<>();
        Firestore db = FirestoreClient.getFirestore();
        DocumentReference docRef = db.collection("usuarios").document(id);

        if (docRef.get().get().exists()) {
            docRef.delete();
            // Siguiendo tu patrón, el borrado exitoso devuelve NO_CONTENT (sin cuerpo)
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        response.put("status", "ERROR");
        response.put("message", "Can't find user with id: " + id);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
}