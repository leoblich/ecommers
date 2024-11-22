package com.carpetadigital.ecommerce.document.controller;

import com.carpetadigital.ecommerce.entity.dto.Document.DocumentDto;
import com.carpetadigital.ecommerce.Repository.DocumentsRepository;
import com.carpetadigital.ecommerce.utils.handler.ResponseHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;


@RestController
@RequestMapping("/api/v1/document")
public class DocumentsController {

//    https://ecommers-9tn0.onrender.com

    private static final Logger logger = LoggerFactory.getLogger(DocumentsService.class);

    @Value("${script.path}")
    private String scriptPath;


    @Autowired
    public DocumentsService documentsService;

    private final DocumentsRepository documentsRepository;

    public DocumentsController(DocumentsRepository documentsRepository) {
        this.documentsRepository = documentsRepository;
    }

    // buscar todos los documento
    @GetMapping()
    public ResponseEntity<Object> getAllDocuments() {
        return ResponseHandler.generateResponse(
                HttpStatus.OK,
                documentsService.buscarTodosDocuments(),
                true
        );
    }

    // buscar un documento por ID
    @GetMapping("/{id}")
    public ResponseEntity<Object> buscarPorId(@PathVariable Long id) {
        return ResponseHandler.generateResponse(
                HttpStatus.OK,
                documentsService.buscarPorId(id),
                true
        );
    }

    // buscar un elemento por key: value (búsqueda dinámica)
    @GetMapping("/searchBy")
    public ResponseEntity<Object> searchDocumentBy(@RequestParam String key, @RequestParam String value) {
        return ResponseHandler.generateResponse(
                HttpStatus.OK,
                documentsService.buscarPor(key, value),
                true
        );
    }

    // buscar los más recientes guardados
    @GetMapping("/recientes")
    public Object buscarMasReciente(
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "6") int cantElementos
    ) {
        return ResponseHandler.generateResponse(
                HttpStatus.OK,
                documentsService.buscarMasReciente(pagina - 1, cantElementos),
                true
        );
    }

    // buscar los más vistos (countPreView)
    @GetMapping("/masvistos")
    public Object buscarLosMasVistos(
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "6") int cantElementos
    ) {
        return ResponseHandler.generateResponse(
                HttpStatus.OK,
                documentsService.buscarLosMasVistos(pagina - 1, cantElementos),
                true
        );
    }

    // buscar los más vendidos
    @GetMapping("/masvendidos")
    public Object masvendidos(
            @RequestParam(defaultValue = "1") int pagina,
            @RequestParam(defaultValue = "6") int cantElementos
    ) {
        return ResponseHandler.generateResponse(
                HttpStatus.OK,
                documentsService.masvendidos(pagina - 1, cantElementos),
                true
        );
    }


    // guardado de un documento
    @PostMapping()
    public Object postDocument(@ModelAttribute @Validated DocumentDto documentDto) throws GeneralSecurityException, IOException {
        if (scriptPath == null || scriptPath.isEmpty()) {
            logger.error("La propiedad script.path no está configurada");
            throw new IllegalStateException();
        }
        File scriptFile = new File(scriptPath);
        if (!scriptFile.exists()) {
            logger.error("El archivo o directorio {} no existe", scriptPath);
            throw new IllegalStateException("El archivo o directorio configurado en script.path no existe");
        }
        return ResponseHandler.generateResponse(
                HttpStatus.CREATED,
                documentsService.guardarDocument(documentDto, new File(scriptPath)),
                true
                 );
   }

    // borrado lógico del documento
    @DeleteMapping("/{id}")
    public ResponseEntity<Object> borradoLogicoDocument(@PathVariable Long id) {
        documentsService.borradoLogicoDocument(id);
        return ResponseHandler.generateResponse(
                HttpStatus.OK,
                null,
                true
        );
    }

    // elimina documento físicamente (base de datos y google drive)
    @DeleteMapping("/fisico/{id}")
    public ResponseEntity<Object> borradoFisicoDocument(@PathVariable Long id) {
        documentsService.borradoFisicoDocument(id);
        return ResponseHandler.generateResponse(
                HttpStatus.OK,
                null,
                true
        );
    }

    // actualización de un documento
    @PutMapping("/{id}")
    public ResponseEntity<Object> actualizacionDocument(@PathVariable Long id, @ModelAttribute DocumentDto documentDto) throws Exception {
        if (scriptPath == null || scriptPath.isEmpty()) {
            throw new IllegalStateException("La propiedad script.path no está configurada");
        }
        return ResponseHandler.generateResponse(
                HttpStatus.OK,
                documentsService.actualizacionDocument(id, documentDto, new File(scriptPath)),
                true
        );
    }

    // sumar un likes
    @PutMapping("/likes/{id}")
    public ResponseEntity<Object> sumarUnLike(@PathVariable Long id) {
        return ResponseHandler.generateResponse(
                HttpStatus.OK,
                documentsService.sumarUnLike(id),
                true
        );
    }
}
