package com.example.demo.controllers;

import com.example.demo.config.JwtUtil;
import com.example.demo.config.SecurityConfig;
import com.example.demo.models.Coleccion;
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
@CrossOrigin(origins = "https://backend-gameboxd-1.onrender.com")
@Tag(name = "Usuarios", description = "Gestión de perfiles, autenticación (Login/Registro) y listas personales")
public class UsuarioController {

    @Autowired
    JwtUtil jwtUtil = new JwtUtil();


    Map<String,Object> response = new HashMap<>();

    @Operation(summary = "Listar usuarios", description = "Devuelve una lista paginada de todos los usuarios registrados. Permite buscar por nombre o email.")
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> getAll(
            @Parameter(description = "Búsqueda por nombre o email") @RequestParam(required = false) String generico,
            @Parameter(description = "Usuarios por página") @RequestParam(required = false, defaultValue = "10") Integer limit,
            @Parameter(description = "Página actual") @RequestParam(required = false, defaultValue = "1") Integer page,
            @Parameter(description = "Campo de ordenación", schema = @Schema(allowableValues = {"date", "birth", "name"})) @RequestParam(required = false, defaultValue = "name") String sort,
            @Parameter(description = "Dirección", schema = @Schema(allowableValues = {"asc", "desc"})) @RequestParam(required = false) String order
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

        if (generico != null  && generico.isEmpty()) {
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


    @Operation(summary = "Obtener usuario por ID", description = "Recupera los datos públicos y listas de un usuario concreto.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
            @ApiResponse(responseCode = "404", description = "Usuario no existe")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable String id) throws ExecutionException, InterruptedException {
        response.clear();
        Firestore db = FirestoreClient.getFirestore();
        DocumentSnapshot document = db.collection("usuarios").document(id).get().get();

        if (document.exists()) {
            Usuario usuario = document.toObject(Usuario.class);
            usuario.setId(document.getId());
            response.put("status", "OK");
            response.put("data", hidratarUsuario(usuario));

            return new ResponseEntity<>(response, HttpStatus.OK);
        }

        response.put("status", "ERROR");
        response.put("message", "Can't find user with id: " + id);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }


    @Operation(summary = "Login de usuario", description = "Autentica al usuario mediante email/password y devuelve un token JWT.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Autenticación exitosa"),
            @ApiResponse(responseCode = "401", description = "Credenciales incorrectas"),
            @ApiResponse(responseCode = "400", description = "Formato de email o password no válido")
    })
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

        if (!validaEmail(email)) {
            response.put("status", "ERROR");
            response.put("message", "Email is not valid");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        if (!validaPass(pass)) {
            response.put("status", "ERROR");
            response.put("message", "Password is not valid");
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
        response.put("data", hidratarUsuario(usuario));
        return new ResponseEntity<>(response, HttpStatus.OK);
    }


    @Operation(summary = "Registro de nuevo usuario", description = "Crea una cuenta nueva en el sistema. No requiere token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Usuario creado"),
            @ApiResponse(responseCode = "400", description = "Email ya existe o validaciones fallidas")
    })
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

        if (!validaEmail(usuario.getEmail())) {
            response.put("status", "ERROR");
            response.put("message", "Email is not valid");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        if (!validaPass(usuario.getPass())) {
            response.put("status", "ERROR");
            response.put("message", "Password is not valid");
            return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
        }

        ArrayList<Usuario> usuarios = new ArrayList<>();
        for (QueryDocumentSnapshot document : documents) {
            Usuario usuarioAux = document.toObject(Usuario.class);
            if (usuarioAux.getEmail().equals(usuario.getEmail())) {
                response.put("status", "ERROR");
                response.put("message", "The email already exists");
                return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
            }

        }


        if (usuario.getFavoritos() == null) usuario.setFavoritos(new ArrayList<>());
        if (usuario.getDeseados() == null) usuario.setDeseados(new ArrayList<>());
        if (usuario.getReviews() == null) usuario.setReviews(new ArrayList<>());
        usuario.setAdmin(false);

        Date ahora = new Date();
        usuario.setFechaCreacion(ahora);
        usuario.setFechaActualizacion(ahora);
        SecurityConfig securityConfig = new SecurityConfig();
        usuario.setPass(securityConfig.passwordEncoder().encode(usuario.getPass()));
        usuario.setToken(jwtUtil.generateToken(usuario.getId(), usuario.getNombre()));
        ApiFuture<DocumentReference> docRef = db.collection("usuarios").add(usuario);
        usuario.setId(docRef.get().getId());

        response.put("status", "OK");
        response.put("data", hidratarUsuario(usuario));

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Actualizar perfil", description = "Modifica los datos del usuario. Requiere token de sesión.", security = @SecurityRequirement(name = "bearerAuth"))
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

    @Operation(summary = "Borrar cuenta", description = "Elimina al usuario y todas sus reviews y colecciones asociadas.", security = @SecurityRequirement(name = "bearerAuth"))
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

    private boolean validaEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        return email != null && email.matches(emailRegex);
    }

    private boolean validaPass(String pass) {
        return pass != null && pass.length() >= 8  && pass.matches(".*[0-9].*");
    }


    private Map<String, Object> hidratarUsuario(Usuario usuario) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        Map<String, Object> uMap = new HashMap<>();
        uMap.put("id", usuario.getId());
        uMap.put("nombre", usuario.getNombre());
        uMap.put("email", usuario.getEmail());
        uMap.put("token", usuario.getToken());

        // 1. Hidratar REVIEWS + sus JUEGOS
        List<QueryDocumentSnapshot> revDocs = db.collection("reviews")
                .whereEqualTo("idUsuario", usuario.getId()).get().get().getDocuments();

        List<Map<String, Object>> reviewsConJuego = new ArrayList<>();
        for (QueryDocumentSnapshot doc : revDocs) {
            Review r = doc.toObject(Review.class);
            r.setId(doc.getId());

            // Buscamos el juego de esta review
            Map<String, Object> rMap = new HashMap<>();
            rMap.put("id", r.getId());
            rMap.put("nota", r.getNota());
            rMap.put("comentario", r.getComentario());
            rMap.put("idUsuario", r.getIdUsuario());
            rMap.put("idJuego", r.getIdJuego());
            rMap.put("likes", r.getLikes() != null ? r.getLikes() : new ArrayList<>());
            rMap.put("fechaCreacion", r.getFechaCreacion());
            rMap.put("fechaActualizacion", r.getFechaActualizacion());

            DocumentSnapshot gDoc = db.collection("videojuegos").document(r.getIdJuego()).get().get();
            if (gDoc.exists()) {
                Map<String, Object> jData = gDoc.getData();
                jData.put("id", gDoc.getId());
                rMap.put("juego", jData);
            }
            reviewsConJuego.add(rMap);
        }
        uMap.put("reviews", reviewsConJuego);

        // 2. Hidratar COLECCIONES + sus JUEGOS
        List<QueryDocumentSnapshot> colDocs = db.collection("colecciones")
                .whereEqualTo("idUsuario", usuario.getId()).get().get().getDocuments();

        List<Map<String, Object>> coleccionesConJuegos = new ArrayList<>();
        for (QueryDocumentSnapshot doc : colDocs) {
            Coleccion c = doc.toObject(Coleccion.class);
            c.setId(doc.getId());

            // Reutilizamos la lógica de hidratar juegos de la colección
            Map<String, Object> cMap = new HashMap<>();
            cMap.put("id", c.getId());
            cMap.put("idUsuario", c.getIdUsuario());
            cMap.put("nombre", c.getNombre());
            cMap.put("descripcion", c.getDescripcion());
            cMap.put("cantidadMeGusta", c.getCantidadMeGusta());
            cMap.put("fechaCreacion", c.getFechaCreacion());
            cMap.put("fechaActualizacion", c.getFechaActualizacion());


            if (c.getJuegos() != null && !c.getJuegos().isEmpty()) {
                // Consulta whereIn para traer todos los juegos de la colección de golpe
                List<QueryDocumentSnapshot> jDocs = db.collection("videojuegos")
                        .whereIn(FieldPath.documentId(), c.getJuegos()).get().get().getDocuments();
                cMap.put("juegos", jDocs.stream().map(d -> d.getData()).collect(Collectors.toList()));
            }
            coleccionesConJuegos.add(cMap);
        }
        uMap.put("colecciones", coleccionesConJuegos);

        // 4. Hidratar FAVORITOS (Lista de juegos completa)
        uMap.put("favoritos", hidratarListaJuegos(usuario.getFavoritos(), db));

        // 5. Hidratar DESEADOS (Lista de juegos completa)
        uMap.put("deseados", hidratarListaJuegos(usuario.getDeseados(), db));

        return uMap;
    }

    // Método auxiliar para no repetir código en favoritos y deseados
    private List<Map<String, Object>> hidratarListaJuegos(List<String> ids, Firestore db) throws ExecutionException, InterruptedException {
        if (ids == null || ids.isEmpty()) return new ArrayList<>();

        // Firestore permite un máximo de 30 IDs en una consulta 'whereIn'
        // Si tus listas son más grandes, habría que paginar esto o partir la lista
        List<QueryDocumentSnapshot> jDocs = db.collection("videojuegos")
                .whereIn(FieldPath.documentId(), ids).get().get().getDocuments();

        return jDocs.stream().map(d -> {
            Map<String, Object> m = d.getData();
            m.put("id", d.getId());
            return m;
        }).collect(Collectors.toList());
    }

}