package com.example.demo.controllers;

import com.example.demo.models.Empresa;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/v1/empresas")
public class EmpresaController {

    // Variable de respuesta a nivel de clase
    private Map<String, Object> response = new HashMap<>();

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> getAll(
            @RequestParam(required = false) String generico,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String nacionalidad,
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String order
    ) throws ExecutionException, InterruptedException {
        response.clear();
        Firestore db = FirestoreClient.getFirestore();
        List<QueryDocumentSnapshot> documents = db.collection("empresas").get().get().getDocuments();

        ArrayList<Empresa> empresas = new ArrayList<>();
        for (QueryDocumentSnapshot document : documents) {
            Empresa empresa = document.toObject(Empresa.class);
            empresa.setId(document.getId());
            empresas.add(empresa);
        }

        Stream<Empresa> stream = empresas.stream();

        if (generico != null) {
            String busqueda = generico.toLowerCase();
            stream = stream.filter(e ->
                    (e.getNombre() != null && e.getNombre().toLowerCase().contains(busqueda)) ||
                            (e.getNacionalidad() != null && e.getNacionalidad().toLowerCase().contains(busqueda))
            );
        } else {
            if (nombre != null) stream = stream.filter(e -> e.getNombre().toLowerCase().contains(nombre.toLowerCase()));
            if (nacionalidad != null) stream = stream.filter(e -> e.getNacionalidad().toLowerCase().contains(nacionalidad.toLowerCase()));
        }

        List<Empresa> filtrados = stream.collect(Collectors.toList());

        if (sort != null) {
            for (int i = 0; i < filtrados.size() - 1; i++) {
                int indiceMejor = i;
                for (int j = i + 1; j < filtrados.size(); j++) {
                    boolean esMejor = false;
                    Empresa actual = filtrados.get(j);
                    Empresa mejor = filtrados.get(indiceMejor);

                    switch (sort.toLowerCase()) {
                        case "date":
                            if (actual.getFechaCreacion() != null && mejor.getFechaCreacion() != null)
                                esMejor = actual.getFechaCreacion().before(mejor.getFechaCreacion());
                            break;
                        case "foundation":
                            if (actual.getFechaFundacion() != null && mejor.getFechaFundacion() != null)
                                esMejor = actual.getFechaFundacion().before(mejor.getFechaFundacion());
                            break;
                        case "name":
                        default:
                            if (actual.getNombre() != null && mejor.getNombre() != null)
                                esMejor = actual.getNombre().compareToIgnoreCase(mejor.getNombre()) < 0;
                            break;
                    }
                    if (esMejor) indiceMejor = j;
                }
                Empresa temp = filtrados.get(indiceMejor);
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

        List<Empresa> paginaEmpresas = filtrados.subList(start, end);

        response.put("status", "OK");
        response.put("page", page);
        response.put("limit", limit);
        response.put("totalResults", total);
        response.put("data", paginaEmpresas);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable String id) throws ExecutionException, InterruptedException {
        response.clear();
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
    public ResponseEntity<Map<String, Object>> nuevaEmpresa(@RequestBody Empresa empresa, @AuthenticationPrincipal String uid) throws ExecutionException, InterruptedException {
        response.clear();

        if (uid == null) {
            response.put("status", "ERROR");
            response.put("message", "Valid Token is required");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        if (empresa == null ||
                empresa.getNombre() == null || empresa.getNombre().isEmpty() ||
                empresa.getFechaFundacion() == null
        ) {
            response.put("status", "ERROR");
            response.put("message", "Name and foundation date are required");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
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
    public ResponseEntity<Map<String, Object>> actualizaEmpresa(@PathVariable String id, @RequestBody Empresa empresa, @AuthenticationPrincipal String uid) throws ExecutionException, InterruptedException {
        response.clear();

        if (uid == null) {
            response.put("status", "ERROR");
            response.put("message", "Valid Token is required");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

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

    // Métodos de ayuda (Helper) para ser llamados desde otros controladores
    public boolean insertaJuegoDesarollado(String idJuego, String idDesarrolladora) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        DocumentSnapshot document = db.collection("empresas").document(idDesarrolladora).get().get();

        if (!document.exists()) return false;

        Empresa empresa = document.toObject(Empresa.class);
        empresa.setId(idDesarrolladora);

        if (empresa.getDesarrollados() == null) empresa.setDesarrollados(new ArrayList<>());

        if (!empresa.getDesarrollados().contains(idJuego))
            empresa.getDesarrollados().add(idJuego);

        db.collection("empresas").document(idDesarrolladora).set(empresa);
        return true;
    }

    public boolean insertaJuegoPublicado(String idJuego, String idPublisher) {
        Firestore db = FirestoreClient.getFirestore();
        try {
            DocumentSnapshot document = db.collection("empresas").document(idPublisher).get().get();
            if (!document.exists()) return false;

            Empresa empresa = document.toObject(Empresa.class);
            empresa.setId(idPublisher);

            if (empresa.getPublicados() == null) empresa.setPublicados(new ArrayList<>());

            if (!empresa.getPublicados().contains(idJuego))
                empresa.getPublicados().add(idJuego);

            db.collection("empresas").document(idPublisher).set(empresa);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}