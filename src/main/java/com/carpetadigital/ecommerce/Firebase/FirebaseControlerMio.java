package com.carpetadigital.ecommerce.Firebase;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/firebase")
public class FirebaseControlerMio {

    private final FirebaseServiceMio firebaseServiceMio;

    public FirebaseControlerMio(FirebaseServiceMio firebaseService) {
        this.firebaseServiceMio = firebaseService;
    }

    @PostMapping()
    public CompletableFuture<ResFirebase> guardarArchivo(@RequestParam("file") MultipartFile file) throws IOException {

        byte[] fileByte = file.getBytes();
        String fileType = file.getContentType();
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        CompletableFuture<ResFirebase> resultadoGuardado = firebaseServiceMio.guardarArchivoEnFirebase(fileByte, fileType, "matemáticas", fileName);
        return resultadoGuardado;
    }

    @DeleteMapping("/image/{fileName}")
    public String eliminarImagen(@PathVariable String fileName) {
        boolean borrado = firebaseServiceMio.borrarArchivo(fileName);
        if (borrado) {
            return "eliminación del archivo exitosamente";
        } else {
            return "error en la eliminación del archivo";
        }
    }

    @DeleteMapping("/archivo")
    public String eliminarArchivo(@RequestParam String fileNameIdConRuta) {
        boolean borrado = firebaseServiceMio.borrarArchivo(fileNameIdConRuta);
        if (borrado) {
            return "eliminación del archivo exitosamente";
        } else {
            return "error en la eliminación del archivo";
        }
    }

    // Endpoint para obtener la URL de descarga pública
    @GetMapping("/download/{filePath}")
    public ResponseEntity<String> getDownloadUrl(@PathVariable String filePath) {
        String downloadUrl = firebaseServiceMio.getDownloadURL(filePath);

        if (downloadUrl != null) {
            return ResponseEntity.ok(downloadUrl);  // Retorna la URL de descarga
        } else {
            return ResponseEntity.status(404).body("Archivo no encontrado.");
        }
    }

    @GetMapping()
    public String url() throws UnsupportedEncodingException {
        return firebaseServiceMio.url();
    }

    // buscar por fileName (por query)
    @GetMapping("/bucarArchivo")
    public CompletableFuture<Boolean> buscarArchivo(@RequestParam String fileName) {
//        String filename = "imagenes" + "/" + fileName;
        return firebaseServiceMio.buscarArchivoFirebase(fileName);
    }

    @GetMapping("/todos")
    public List<String> buscarTodos() {
        return firebaseServiceMio.buscarTodosArchivosFirebase();
    }

}
