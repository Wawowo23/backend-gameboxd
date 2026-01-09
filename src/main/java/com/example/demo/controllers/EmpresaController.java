package com.example.demo.controllers;

import com.example.demo.models.Empresa;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/v1/empresas")
public class EmpresaController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> getAll() throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        List<QueryDocumentSnapshot> documents = db.collection("empresas").get().get().getDocuments();

        ArrayList<Empresa> empresas = new ArrayList<>();
        for (QueryDocumentSnapshot document : documents) {
            Empresa empresa = document.toObject(Empresa.class);
            empresa.setId(document.getId());
            empresas.add(empresa);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("page", 1);
        response.put("data", empresas);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable String id) throws ExecutionException, InterruptedException {
        Map<String, Object> response = new HashMap<>();
        Firestore db = FirestoreClient.getFirestore();
        DocumentSnapshot document = db.collection("empresas").document(id).get().get();

        if (document.exists()) {
            Empresa empresa = document.toObject(Empresa.class);
            empresa.setId(document.getId());
            response.put("status", "OK");
            response.put("data", empresa);

            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        response.put("status", "ERROR");
        response.put("message", "Can't find company with id: " + id);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @PostMapping("/")
    public ResponseEntity<Map<String, Object>> nuevaEmpresa(@RequestBody Empresa empresa) throws ExecutionException, InterruptedException {
        Map<String, Object> response = new HashMap<>();
        if (empresa == null ||
                empresa.getNombre() == null || empresa.getNombre().isEmpty() ||
                empresa.getFechaFundacion() == null
        ) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Firestore db = FirestoreClient.getFirestore();

        Date ahora = new Date();
        empresa.setFechaCreacion(ahora);
        empresa.setFechaActualizacion(ahora);

        if (empresa.getDesarrollados() == null) empresa.setDesarrollados(new ArrayList<>());
        if (empresa.getPublicados() == null) empresa.setPublicados(new ArrayList<>());

        ApiFuture<DocumentReference> docRef = db.collection("empresas").add(empresa);
        empresa.setId(docRef.get().getId());

        response.put("status", "OK");
        response.put("data", empresa);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> actualizaEmpresa(@PathVariable String id, @RequestBody Empresa empresa) throws ExecutionException, InterruptedException {
        Map<String, Object> response = new HashMap<>();
        if (empresa == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        Firestore db = FirestoreClient.getFirestore();
        DocumentReference docRef = db.collection("empresas").document(id);
        DocumentSnapshot snapshot = docRef.get().get();

        if (snapshot.exists()) {
            Empresa empresaExistente = snapshot.toObject(Empresa.class);

            empresa.setFechaCreacion(empresaExistente.getFechaCreacion());
            empresa.setFechaActualizacion(new Date());
            empresa.setId(id);

            docRef.set(empresa);

            response.put("status", "OK");
            response.put("data", empresa);
            return new ResponseEntity<>(response, HttpStatus.OK);
        }

        response.put("status", "ERROR");
        response.put("message", "Can't find company with id: " + id);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    public boolean insertaJuegoDesarollado(String idJuego, String idDesarrolladora) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        DocumentSnapshot document = db.collection("empresas").document(idDesarrolladora).get().get();

        if (!document.exists()) return false;

        Empresa empresa = document.toObject(Empresa.class);
        empresa.setId(idDesarrolladora);
        empresa.getDesarrollados().add(idJuego);

        DocumentReference docRef = db.collection("empresas").document(idDesarrolladora);

        docRef.set(empresa);

        return true;



    }

    public boolean insertaJuegoPublicado(String idJuego, String idPublisher)  {

        Firestore db = FirestoreClient.getFirestore();
        DocumentSnapshot document = null;
        try {
            document = db.collection("empresas").document(idPublisher).get().get();
        } catch (InterruptedException e) {
            return false;
        } catch (ExecutionException e) {
            return false;
        }

        if (!document.exists()) return false;

        Empresa empresa = document.toObject(Empresa.class);
        empresa.setId(idPublisher);
        empresa.getPublicados().add(idJuego);

        DocumentReference docRef = db.collection("empresas").document(idPublisher);

        docRef.set(empresa);

        return true;
    }
}