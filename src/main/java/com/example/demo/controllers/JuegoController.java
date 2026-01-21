package com.example.demo.controllers;

import com.example.demo.models.Juego;
import com.example.demo.models.Review;
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
@RequestMapping("/api/v1/juegos")
public class JuegoController {

    Map<String, Object> response = new HashMap<>();

    @CrossOrigin(origins = "https://backend-gameboxd-1.onrender.com")
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> getAll(
            @RequestParam(required = false) String generico,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String subtitulo,
            @RequestParam(required = false) String genero,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false, defaultValue = "10") Integer limit,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "popularity") String sort,
            @RequestParam(required = false, defaultValue = "asc") String order
    ) throws ExecutionException, InterruptedException {
        response.clear();
        Firestore db = FirestoreClient.getFirestore();
        List<QueryDocumentSnapshot> documents = db.collection("videojuegos").get().get().getDocuments();

        ArrayList<Juego> juegos = new ArrayList<>();
        for (QueryDocumentSnapshot document : documents) {
            Juego juego = document.toObject(Juego.class);
            juego.setId(document.getId());
            juegos.add(juego);
        }

        Stream<Juego> stream = juegos.stream();

        if (generico != null) {
            stream = stream.filter(j ->
                    j.getTitulo().toLowerCase().contains(generico.toLowerCase()) ||
                            j.getSubtitulo().toLowerCase().contains(generico.toLowerCase()) ||
                            j.getTags().stream().anyMatch(t -> t.toLowerCase().contains(generico.toLowerCase())) ||
                            j.getGeneros().stream().anyMatch(g -> g.toLowerCase().contains(generico.toLowerCase()))
            );
        } else {
            if (nombre != null) stream = stream.filter(j -> j.getTitulo().toLowerCase().contains(nombre.toLowerCase()));
            if (subtitulo != null)
                stream = stream.filter(j -> j.getSubtitulo().toLowerCase().contains(subtitulo.toLowerCase()));
            if (genero != null)
                stream = stream.filter(j -> j.getGeneros().stream().anyMatch(g -> g.toLowerCase().contains(genero.toLowerCase())));
            if (tags != null)
                stream = stream.filter(j -> j.getTags().stream().anyMatch(t -> t.toLowerCase().contains(tags.toLowerCase())));
        }

        List<Juego> filtrados = stream.collect(Collectors.toList());

        for (int i = 0; i < filtrados.size() - 1; i++) {
            int indiceMejor = i;
            for (int j = i + 1; j < filtrados.size(); j++) {

                boolean esMejor = false;
                Juego actual = filtrados.get(j);
                Juego mejor = filtrados.get(indiceMejor);

                switch (sort.toLowerCase()) {
                    case "date":
                        if (actual.getFechaLanzamiento() != null && mejor.getFechaLanzamiento() != null)
                            esMejor = actual.getFechaLanzamiento().before(mejor.getFechaLanzamiento());
                        break;
                    case "popularity":
                        esMejor = (actual.getNotas() != null ? actual.getNotas().size() : 0) > (mejor.getNotas() != null ? mejor.getNotas().size() : 0);
                        break;
                    case "duration":
                        esMejor = actual.getMinutosDuracion() > mejor.getMinutosDuracion();
                        break;
                    case "completion":
                        esMejor = actual.getMinutosDuracionCompleto() > mejor.getMinutosDuracionCompleto();
                        break;
                    case "rating":
                        esMejor = actual.getNotaMedia() > mejor.getNotaMedia();
                        break;
                    case "title":
                    default:
                        esMejor = actual.getTitulo().compareToIgnoreCase(mejor.getTitulo()) < 0;
                        break;
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

        int totalJuegos = filtrados.size();
        int start = Math.min(Math.max(0, (page - 1) * limit), totalJuegos);
        int end = Math.min(start + limit, totalJuegos);

        List<Juego> paginaJuegos = filtrados.subList(start, end);
        List<Map<String, Object>> dataResponse = paginaJuegos.stream()
                .map(this::mapJuegoResponse)
                .collect(Collectors.toList());

        response.put("status", "OK");
        response.put("page", page);
        // Nota: He mantenido tu lógica de totalJuegos pero filtrados.size() es el conteo real.
        response.put("total", totalJuegos);
        response.put("limit", limit);
        response.put("data", dataResponse);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @CrossOrigin(origins = "https://backend-gameboxd-1.onrender.com")
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable String id) throws ExecutionException, InterruptedException {
        response.clear();
        Firestore db = FirestoreClient.getFirestore();
        DocumentSnapshot document = db.collection("videojuegos").document(id).get().get();

        if (document.exists()) {
            Juego juego = document.toObject(Juego.class);
            juego.setId(document.getId());
            response.put("status", "OK");
            response.put("data", mapJuegoResponse(juego));

            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        response.put("status", "ERROR");
        response.put("message", "Can't find videogame with id: " + id);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    //TODO si viene sin una URL hay que poner un placeholder
    @PostMapping("/")
    public ResponseEntity<Map<String, Object>> nuevoJuego(@RequestBody Juego juego, @AuthenticationPrincipal String uid) throws ExecutionException, InterruptedException {
        response.clear();

        if (uid == null) {
            response.put("status", "ERROR");
            response.put("message", "Valid Token is required");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        if (juego == null ||
                juego.getTitulo() == null || juego.getTitulo().isEmpty() ||
                juego.getSubtitulo() == null || juego.getSubtitulo().isEmpty() ||
                juego.getDescripcion() == null || juego.getDescripcion().isEmpty() ||
                juego.getFechaLanzamiento() == null
        ) {
            response.put("status", "ERROR");
            response.put("message", "Title, subtitle, description and release date are required");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        Firestore db = FirestoreClient.getFirestore();

        Date ahora = new Date();
        juego.setFechaCreacion(ahora);
        juego.setFechaActualizacion(ahora);

        if (juego.getNotas() == null) juego.setNotas(new ArrayList<>());
        if (juego.getPlataformas() == null) juego.setPlataformas(new ArrayList<>());
        if (juego.getGeneros() == null) juego.setGeneros(new ArrayList<>());
        if (juego.getTags() == null) juego.setTags(new ArrayList<>());

        ApiFuture<DocumentReference> docRef = db.collection("videojuegos").add(juego);
        juego.setId(docRef.get().getId());

        EmpresaController empresaController = new EmpresaController();
        if (juego.getIdDesarrolladora() != null && !juego.getIdDesarrolladora().isEmpty()) {
            if (!empresaController.insertaJuegoDesarollado(juego.getId(), juego.getIdDesarrolladora())) {
                response.put("warning", "Can't find developer assigned");
            }
        }
        if (juego.getIdPublisher() != null && !juego.getIdPublisher().isEmpty()) {
            if (!empresaController.insertaJuegoPublicado(juego.getId(), juego.getIdPublisher())) {
                response.put("warning", "Can't find publisher assigned");
            }
        }

        response.put("status", "OK");
        response.put("data", mapJuegoResponse(juego));

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @CrossOrigin(origins = "https://backend-gameboxd-1.onrender.com")
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
        DocumentReference docRef = db.collection("videojuegos").document(id);
        DocumentSnapshot snapshot = docRef.get().get();

        if (snapshot.exists()) {
            Juego juegoExistente = snapshot.toObject(Juego.class);

            juego.setFechaCreacion(juegoExistente.getFechaCreacion());
            juego.setFechaActualizacion(new Date());
            juego.setId(id);

            docRef.set(juego);

            EmpresaController empresaController = new EmpresaController();
            if (juego.getIdDesarrolladora() != juegoExistente.getIdDesarrolladora()) {
                if (!empresaController.insertaJuegoDesarollado(juego.getId(), juego.getIdDesarrolladora())) {
                    response.put("warning", "Can't find developer assigned");
                }
            }
            if (juego.getIdPublisher() != juegoExistente.getIdPublisher()) {
                if (!empresaController.insertaJuegoPublicado(juego.getId(), juego.getIdPublisher())) {
                    response.put("warning", "Can't find publisher assigned");
                }
            }

            response.put("status", "OK");
            response.put("data", mapJuegoResponse(juego));
            return new ResponseEntity<>(response, HttpStatus.OK);
        }
        response.put("status", "ERROR");
        response.put("message", "Can't find videogame with id: " + id);
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @CrossOrigin(origins = "https://backend-gameboxd-1.onrender.com")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> borraJuego(@PathVariable String id, @AuthenticationPrincipal String uid) throws ExecutionException, InterruptedException {
        response.clear();

        if (uid == null) {
            response.put("status", "ERROR");
            response.put("message", "Valid Token is required");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        Firestore db = FirestoreClient.getFirestore();
        DocumentReference docRef = db.collection("videojuegos").document(id);

        if (docRef.get().get().exists()) {
            docRef.delete();
            ColeccionController coleccionController = new ColeccionController();
            ReviewController reviewController = new ReviewController();
            UsuarioController usuarioController = new UsuarioController();
            usuarioController.eliminaJuego(id);
            reviewController.eliminaReviewsDeJuego(id);
            coleccionController.eliminaJuegoDeColecciones(id);
            response.put("status", "OK");
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }
        response.put("status", "ERROR");
        response.put("message", "Can't find videogame with id: " + id);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    public void nuevaReview(Review review) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        DocumentReference docRef = db.collection("videojuegos").document(review.getIdJuego());
        DocumentSnapshot snapshot = docRef.get().get();

        Juego juego = snapshot.toObject(Juego.class);
        if (juego.getNotas() == null) juego.setNotas(new ArrayList<>());

        juego.getNotas().add(review.getNota());
        docRef.set(juego);
    }

    private Map<String, Object> mapJuegoResponse(Juego juego) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", juego.getId());
        map.put("titulo", juego.getTitulo());
        map.put("subtitulo", juego.getSubtitulo());
        map.put("descripcion", juego.getDescripcion());
        map.put("fechaLanzamiento", juego.getFechaLanzamiento());
        map.put("urlPortada", juego.getUrlPortada());
        map.put("urlFondo", juego.getUrlFondo());
        map.put("minutosDuracion", juego.getMinutosDuracion());
        map.put("minutosDuracionCompleto", juego.getMinutosDuracionCompleto());
        map.put("plataformas", juego.getPlataformas());
        map.put("generos", juego.getGeneros());
        map.put("tags", juego.getTags());
        map.put("idDesarrolladora", juego.getIdDesarrolladora());
        map.put("idPublisher", juego.getIdPublisher());
        map.put("fechaCreacion", juego.getFechaCreacion());
        map.put("fechaActualizacion", juego.getFechaActualizacion());

        int numNotas = (juego.getNotas() != null) ? juego.getNotas().size() : 0;
        map.put("numNotas", numNotas);
        map.put("notaMedia", numNotas > 0 ? juego.getNotaMedia() : 0.0f);

        return map;
    }
}