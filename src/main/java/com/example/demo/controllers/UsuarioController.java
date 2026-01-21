package com.example.demo.controllers;

import com.example.demo.config.JwtUtil;
import com.example.demo.config.SecurityConfig;
import com.example.demo.models.Coleccion;
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
@RequestMapping("/api/v1/usuarios")
public class UsuarioController {

    @Autowired
    JwtUtil jwtUtil = new JwtUtil();


    Map<String,Object> response = new HashMap<>();

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> getAll(
            @RequestParam(required = false) String generico,

            @RequestParam(required = false, defaultValue = "10") Integer limit,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "name") String sort,
            @RequestParam(required = false) String order
    ) throws ExecutionException, InterruptedException {
        response.clear();
        Firestore db = FirestoreClient.getFirestore();
        List<QueryDocumentSnapshot> documents = db.collection("usuarios").get().get().getDocuments();

        ArrayList<Usuario> usuarios = new ArrayList<>();
        for (QueryDocumentSnapshot document : documents) {
            Usuario usuario = document.toObject(Usuario.class);
            usuario.setId(document.getId());
            usuarios.add(usuario);
        }

        Stream<Usuario> stream = usuarios.stream();

        if (generico != null) {
            String busqueda = generico.toLowerCase();
            stream = stream.filter(u ->
                    (u.getNombre() != null && u.getNombre().toLowerCase().contains(busqueda)) ||
                            (u.getEmail() != null && u.getEmail().toLowerCase().contains(busqueda))
            );
        }

        List<Usuario> filtrados = stream.collect(Collectors.toList());

        if (sort != null) {
            for (int i = 0; i < filtrados.size() - 1; i++) {
                int indiceMejor = i;
                for (int j = i + 1; j < filtrados.size(); j++) {
                    boolean esMejor = false;
                    Usuario actual = filtrados.get(j);
                    Usuario mejor = filtrados.get(indiceMejor);

                    switch (sort.toLowerCase()) {
                        case "date":
                            if (actual.getFechaCreacion() != null && mejor.getFechaCreacion() != null)
                                esMejor = actual.getFechaCreacion().before(mejor.getFechaCreacion());
                            break;
                        case "birth":
                            if (actual.getFechaNacimiento() != null && mejor.getFechaNacimiento() != null)
                                esMejor = actual.getFechaNacimiento().before(mejor.getFechaNacimiento());
                            break;
                        case "name":
                        default:
                            if (actual.getNombre() != null && mejor.getNombre() != null)
                                esMejor = actual.getNombre().compareToIgnoreCase(mejor.getNombre()) < 0;
                            break;
                    }
                    if (esMejor) indiceMejor = j;
                }
                Usuario temp = filtrados.get(indiceMejor);
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

        List<Usuario> paginaUsuarios = filtrados.subList(start, end);

        response.put("status", "OK");
        response.put("page", page);
        response.put("limit", limit);
        response.put("totalResults", total);
        response.put("data", paginaUsuarios);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable String id) throws ExecutionException, InterruptedException {
        response.clear();
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
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String,String> params) throws ExecutionException, InterruptedException {
        response.clear();
        String email = params.get("email");
        String pass = params.get("pass");
        if ((email == null || email.isEmpty()) || (pass == null || pass.isEmpty())) {
            response.put("status", "ERROR");
            response.put("message", "Email and password required");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        Firestore db = FirestoreClient.getFirestore();
        List<QueryDocumentSnapshot> documents = db.collection("usuarios").whereEqualTo("email",email).get().get().getDocuments();
        if (documents.isEmpty()) {
            response.put("status", "ERROR");
            response.put("message", "User not found");
            return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
        }
        QueryDocumentSnapshot snapshot = documents.get(0);
        Usuario usuario = snapshot.toObject(Usuario.class);
        usuario.setId(snapshot.getId());
        SecurityConfig securityConfig = new SecurityConfig();

        if (!securityConfig.passwordEncoder().matches(pass, usuario.getPass())) {
            response.put("status", "ERROR");
            response.put("message", "Wrong password");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }
        usuario.setToken(jwtUtil.generateToken(usuario.getId(), usuario.getNombre()));
        db.collection("usuarios").document(usuario.getId()).set(usuario);
        response.put("status", "OK");
        response.put("data", usuario);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PostMapping("/new")
    public ResponseEntity<Map<String, Object>> registro(@RequestBody Usuario usuario) throws ExecutionException, InterruptedException {
        response.clear();



        if (usuario == null ||
                usuario.getNombre() == null || usuario.getNombre().isEmpty() ||
                usuario.getEmail() == null || usuario.getEmail().isEmpty() ||
                usuario.getPass() == null || usuario.getPass().isEmpty() ||
                usuario.getFechaNacimiento() == null
        ) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        Firestore db = FirestoreClient.getFirestore();
        List<QueryDocumentSnapshot> documents = db.collection("usuarios").get().get().getDocuments();

        ArrayList<Usuario> usuarios = new ArrayList<>();
        for (QueryDocumentSnapshot document : documents) {
            Usuario usuarioAux = document.toObject(Usuario.class);
            if (usuarioAux.getEmail().equals(usuario.getEmail())) {
                response.put("status", "ERROR");
                response.put("message", "The email already exists");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

        }


        // Inicialización de listas por defecto
        if (usuario.getFavoritos() == null) usuario.setFavoritos(new ArrayList<>());
        if (usuario.getDeseados() == null) usuario.setDeseados(new ArrayList<>());
        if (usuario.getReviews() == null) usuario.setReviews(new ArrayList<>());

        Date ahora = new Date();
        usuario.setFechaCreacion(ahora);
        usuario.setFechaActualizacion(ahora);
        SecurityConfig securityConfig = new SecurityConfig();
        usuario.setPass(securityConfig.passwordEncoder().encode(usuario.getPass()));
        usuario.setToken(jwtUtil.generateToken(usuario.getId(), usuario.getNombre()));
        ApiFuture<DocumentReference> docRef = db.collection("usuarios").add(usuario);
        usuario.setId(docRef.get().getId());

        response.put("status", "OK");
        response.put("data", usuario);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> actualizaUsuario(@PathVariable String id, @RequestBody Usuario usuario, @AuthenticationPrincipal String uid) throws ExecutionException, InterruptedException {
        response.clear();

        if (uid == null) {
            response.put("status", "ERROR");
            response.put("message", "Valid Token is required");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        if (usuario == null) return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

        Firestore db = FirestoreClient.getFirestore();
        DocumentReference docRef = db.collection("usuarios").document(id);
        DocumentSnapshot snapshot = docRef.get().get();

        if (snapshot.exists()) {
            Usuario usuarioExistente = snapshot.toObject(Usuario.class);

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
    public ResponseEntity<Map<String, Object>> borraUsuario(@PathVariable String id, @AuthenticationPrincipal String uid) throws ExecutionException, InterruptedException {
        response.clear();

        if (uid == null) {
            response.put("status", "ERROR");
            response.put("message", "Valid Token is required");
            return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
        }

        Firestore db = FirestoreClient.getFirestore();
        DocumentReference docRef = db.collection("usuarios").document(id);
        DocumentSnapshot snapshot = docRef.get().get();
        if (docRef.get().get().exists()) {
            Usuario usuario = snapshot.toObject(Usuario.class);
            docRef.delete();
            ReviewController reviewController = new ReviewController();
            reviewController.eliminaReviewsUsuario(usuario);
            ColeccionController coleccionController = new ColeccionController();
            coleccionController.eliminaColeccionesUsuario(usuario);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        response.put("status", "ERROR");
        response.put("message", "Can't find user with id: " + id);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    public boolean insertaReview(Review review) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        DocumentReference docRef = db.collection("usuarios").document(review.getIdUsuario());
        DocumentSnapshot snapshot = docRef.get().get();

        Usuario usuario = snapshot.toObject(Usuario.class);

        usuario.getReviews().add(review.getId());

        docRef.set(usuario);

        return true;

    }

    public boolean insertaColeccion(Coleccion coleccion) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        DocumentReference docRef = db.collection("usuarios").document(coleccion.getIdUsuario());
        DocumentSnapshot snapshot = docRef.get().get();

        Usuario usuario = snapshot.toObject(Usuario.class);
        if (usuario.getColecciones() == null)
            usuario.setColecciones(new ArrayList<>());
        System.out.println(usuario);
        usuario.getColecciones().add(coleccion.getId());

        System.out.println(usuario);
        docRef.set(usuario);

        return true;

    }

    public void eliminaJuego(String idJuego) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        List<QueryDocumentSnapshot> documents = db.collection("usuarios").get().get().getDocuments();

        ArrayList<Usuario> usuarios = new ArrayList<>();
        for (QueryDocumentSnapshot document : documents) {
            Usuario usuario = document.toObject(Usuario.class);
            usuario.setId(document.getId());
            DocumentReference docRef = db.collection("usuarios").document(usuario.getId());

            for (String juego : usuario.getDeseados()) {
                if (juego.equals(idJuego)) {
                    usuario.getDeseados().remove(juego);
                    docRef.set(usuario);

                }
            }

            for (String juego : usuario.getFavoritos()) {
                if (juego.equals(idJuego)) {
                    usuario.getFavoritos().remove(juego);
                    docRef.set(usuario);

                }
            }



        }
    }

    public void eliminaColeccion(Coleccion coleccionExistente) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        DocumentReference docRef = db.collection("usuarios").document(coleccionExistente.getIdUsuario());
        DocumentSnapshot snapshot = docRef.get().get();

        if (snapshot.exists()) {
            Usuario usuario = snapshot.toObject(Usuario.class);
            usuario.getColecciones().remove(coleccionExistente.getId());

            docRef.set(usuario);

        }
    }


}