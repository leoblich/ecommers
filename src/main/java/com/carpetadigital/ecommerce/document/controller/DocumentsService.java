package com.carpetadigital.ecommerce.document.controller;

import com.carpetadigital.ecommerce.Firebase.FirebaseServiceMio;
import com.carpetadigital.ecommerce.Firebase.ResFirebase;
import com.carpetadigital.ecommerce.entity.DocumentsEntity;
import com.carpetadigital.ecommerce.entity.dto.Document.*;
import com.carpetadigital.ecommerce.Repository.DocumentsRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RestController
public class DocumentsService {

    private static final Logger logger = LoggerFactory.getLogger(DocumentsService.class);

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private FirebaseServiceMio firebaseServiceMio;

    private final DocumentsRepository documentsRepository;

    public DocumentsService(DocumentsRepository documentsRepository) {
        this.documentsRepository = documentsRepository;
    }

    // servicio para guardar un documento en base de datos
    @Transactional
    public Object guardarDocument(DocumentDto documento, File scriptResource) throws GeneralSecurityException, IOException {

        MultipartFile file = documento.getFile();
        String nombreArchivo = baseNameFile(file);

        // valido el archivo
        validarArchivo(file, documento);

        // busco el archivo en base de datos
        Optional<DocumentsEntity> tituloEncontrado = documentsRepository.findByTitle(documento.getTitle());

        if (tituloEncontrado.isPresent()) {
            throw new IllegalArgumentException("documento existente");
        }

        File tempFileGuargar = null;
        String outputFilePath = String.valueOf(scriptResource.getParentFile());
        int cantidadPaginas = 0;
        ResFirebase responseFirebaseFile = new ResFirebase();
        byte[] imagenWord = new byte[0];
        DocumentsEntity respuesta = new DocumentsEntity();

        String fileType = file.getContentType();
        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        String baseName = fileName != null ? fileName.substring(0, fileName.lastIndexOf(".")) : "file";

        byte[] fileParaTrabajar = file.getBytes();
        try {
            if (documento.getFormat().equals("pdf")) {

                logger.info("cuento páginas del archivo");
                CompletableFuture<Integer> cantidadPaginasPdf = contarPaginasPdf(fileParaTrabajar);
                cantidadPaginas = cantidadPaginasPdf.join();

                logger.info("saco imagen pre-vista");
                CompletableFuture<byte[]> imagenPdfAsync = sacarPrimeraPaginaPdfAImagen(fileParaTrabajar);
                imagenWord = imagenPdfAsync.join();
            }

            if (documento.getFormat().equals("docx")) {

                String scriptPath = scriptResource.getAbsolutePath();

                logger.info("llamo script de python");
                CompletableFuture<DocumentPdfCovertidoDto> respuestaConversionAsinc = llamadaScriptPythonConversion(fileParaTrabajar, scriptPath, outputFilePath, nombreArchivo);
                DocumentPdfCovertidoDto respuestaConversion = respuestaConversionAsinc.join();
                logger.info("volviendo del script de python");

                if (respuestaConversion.isExitoConversionPython()) {

                    cantidadPaginas = respuestaConversion.getNumeroDePagina();

                    // buscar el archivo en el directorio indicado
                    byte[] pdfData = obtenerPdfDesdeDirectorio(outputFilePath, nombreArchivo);

                    logger.info("saco imagen pre-vista");
                    CompletableFuture<byte[]> imagenPdfAsync = sacarPrimeraPaginaPdfAImagen(pdfData);
                    imagenWord = imagenPdfAsync.join();
                }

            }

            logger.info("guardo archivo");
            CompletableFuture<ResFirebase> responseFirebaseAsync = firebaseServiceMio.guardarArchivoEnFirebase(fileParaTrabajar, fileType, documento.getCategory(), baseName);
            responseFirebaseFile = responseFirebaseAsync.join();

            logger.info("guardo imagen");
            CompletableFuture<ResFirebase> responseFirebaseImagenAsync = firebaseServiceMio.guardarImagenEnFirebase(imagenWord, nombreArchivo);
            ResFirebase responseFirebaseImagen = responseFirebaseImagenAsync.join();

            DocumentsEntity datosIngreso = prepararRespuesta(documento, cantidadPaginas, responseFirebaseFile, responseFirebaseImagen);

            logger.info("guardo en base de datos ");
            respuesta = documentsRepository.save(datosIngreso);

        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (tempFileGuargar != null && tempFileGuargar.exists() && !tempFileGuargar.delete()) {
                logger.error("No se pudo eliminar el archivo temporal.");
            }
            if (respuesta.getFormat().equals("docx")) {
                String ruta = outputFilePath;
                String nombre = nombreArchivo;
                String pdfFilename = nombre + "_first_page.pdf";
                Path pdfPath = Paths.get(ruta, pdfFilename);
                File borrar = new File(String.valueOf(pdfPath));
                if (borrar.exists()) {
                    boolean eliminado = borrar.delete();
                    if (eliminado) {
                        logger.error("El archivo fue eliminado PDF correctamente: ");
                    } else {
                        logger.error("No se pudo eliminar el archivo: ");
                    }
                } else {
                    logger.error("El archivo no existe: ");
                }
            }
        }
        return respuesta;

    }

    // servicio para actualizar un documento
    @Transactional
    public DocumentsEntity actualizacionDocument(Long id, DocumentDto documentoUpdate, File scriptResource) throws Exception {

        // busco el archivo en base de datos
        DocumentsEntity existeDocumentoBaseDeDatos = buscarDocumentoDB(id);

        MultipartFile fileUpdate = documentoUpdate.getFile();

        String category = documentoUpdate.getCategory() != null ? documentoUpdate.getCategory() : existeDocumentoBaseDeDatos.getCategory();

        if (fileUpdate != null) {
            validarArchivo(fileUpdate, documentoUpdate);

            String outputFilePath = String.valueOf(scriptResource.getParentFile());
            int cantidadPaginasFileUpdate = 0;
            ResFirebase responseFireArchivoUpdate = new ResFirebase();
            byte[] imagenArchivoUpdate = new byte[0];

            String fileType = fileUpdate.getContentType();
            String fileNameUpdate = UUID.randomUUID() + "_" + fileUpdate.getOriginalFilename();
            String baseNameUpdate = baseNameFile(fileUpdate);
            String extension = fileNameUpdate.substring(fileNameUpdate.lastIndexOf(".") + 1);

            byte[] fileUpdateParaTrabajar = fileUpdate.getBytes();

            if (extension.equals("pdf")) {
                logger.info("cuento páginas del archivo");
                CompletableFuture<Integer> cantidadPaginasPdf = contarPaginasPdf(fileUpdateParaTrabajar);
                cantidadPaginasFileUpdate = cantidadPaginasPdf.join();

                logger.info("saco imagen pre-vista");
                CompletableFuture<byte[]> imagenPdfAsync = sacarPrimeraPaginaPdfAImagen(fileUpdateParaTrabajar);
                imagenArchivoUpdate = imagenPdfAsync.join();
            }

            if (extension.equals("docx")) {
                try {
                    // ruta al script de python para la conversión
                    String scriptPath = scriptResource.getAbsolutePath();

                    logger.info("llamo script de python");
                    CompletableFuture<DocumentPdfCovertidoDto> respuestaConversionAsyncUpdate = llamadaScriptPythonConversion(fileUpdateParaTrabajar, scriptPath, outputFilePath, baseNameUpdate);
                    DocumentPdfCovertidoDto respuestaConversionUpdate = respuestaConversionAsyncUpdate.join();
                    logger.info("volviendo del script de python");

                    if (respuestaConversionUpdate.isExitoConversionPython()) {

                        cantidadPaginasFileUpdate = respuestaConversionUpdate.getNumeroDePagina();

                        // buscar el archivo en el directorio indicado
                        byte[] pdfData = obtenerPdfDesdeDirectorio(outputFilePath, baseNameUpdate);

                        logger.info("saco imagen pre-vista update");
                        CompletableFuture<byte[]> imagenPdfAsyncUpdate = sacarPrimeraPaginaPdfAImagen(pdfData);
                        imagenArchivoUpdate = imagenPdfAsyncUpdate.join();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    if (extension.equals("docx")) {
                        String pdfFilename = baseNameUpdate + "_first_page.pdf";
                        Path pdfPath = Paths.get(outputFilePath, pdfFilename);
                        File borrar = new File(String.valueOf(pdfPath));
                        if (borrar.exists()) {
                            boolean eliminado = borrar.delete();
                            if (eliminado) {
                                logger.error("El archivo fue eliminado PDF correctamente: ");
                            } else {
                                logger.error("No se pudo eliminar el archivo: ");
                            }
                        } else {
                            logger.error("El archivo no existe: ");
                        }
                    }
                }
            }

            CompletableFuture<Boolean> existeArchivoAsync = firebaseServiceMio.buscarArchivoFirebase(existeDocumentoBaseDeDatos.getFileNameId());
            boolean existeArchivo = existeArchivoAsync.join();

            if (existeArchivo) {
                logger.info("borrando archivo existente");
                firebaseServiceMio.borrarArchivo(existeDocumentoBaseDeDatos.getFileNameId());
                logger.info("borrando imagen existente");
                firebaseServiceMio.borrarArchivo(existeDocumentoBaseDeDatos.getImagenNameId());
            }
            logger.info("guardo archivo");
            CompletableFuture<ResFirebase> responseFirebaseAsyncUpdate = firebaseServiceMio.guardarArchivoEnFirebase(fileUpdateParaTrabajar, fileType, category, fileNameUpdate);
            responseFireArchivoUpdate = responseFirebaseAsyncUpdate.join();

            logger.info("guardo imagen");
            CompletableFuture<ResFirebase> responseFirebaseImagenAsync = firebaseServiceMio.guardarImagenEnFirebase(imagenArchivoUpdate, baseNameUpdate);
            ResFirebase responseFirebaseImagenUpdate = responseFirebaseImagenAsync.join();

            prepararRespuestaFileUpdate(existeDocumentoBaseDeDatos, documentoUpdate.getFormat(), cantidadPaginasFileUpdate, responseFireArchivoUpdate, responseFirebaseImagenUpdate);

        }
        if (documentoUpdate.getTitle() != null || documentoUpdate.getPrice() != null || documentoUpdate.getDescription() != null || documentoUpdate.getCategory() != null) {
            prepararParaGuardarDatosUpdate(documentoUpdate, existeDocumentoBaseDeDatos);
        }
        logger.info("guardando en BASE DE DATOS");
        return documentsRepository.save(existeDocumentoBaseDeDatos);

    }

    // buscar documento en base de datos
    private DocumentsEntity buscarDocumentoDB(Long id) {
        Optional<DocumentsEntity> datosBaseDeDatos = documentsRepository.findById(id);
        if (datosBaseDeDatos.isPresent()) {
            return datosBaseDeDatos.get();
        } else {
            throw new IllegalArgumentException("error al encontrar el archivo a actualizar");
        }
    }

    // sumar un like al documento
    public DocumentCountLikesDto sumarUnLike(Long id) {
        Optional<DocumentsEntity> documentoEncontrado = documentsRepository.findById(id);
        DocumentCountLikesDto likeDelDocumento = new DocumentCountLikesDto();
        if (documentoEncontrado.isPresent()) {
            DocumentsEntity documentoLike = documentoEncontrado.get();
            documentoLike.setCountLikes(documentoLike.getCountLikes() + 1);
            documentsRepository.save(documentoLike);

            likeDelDocumento.setId(documentoLike.getId());
            likeDelDocumento.setCountLikes(documentoLike.getCountLikes());
        }
        return likeDelDocumento;
    }

    // servicio para buscar uno o todos los elementos que coinciden con los valores que vienen en key: value
    public List<DocumentsEntity> buscarPor(String key, String value) {

        // se crea una lista con todas las posibles key, para evitar inyección
        List<String> validKeys = Arrays.asList("title", "format", "category", "id", "fileUrl", "borradoLogico");

        if (validKeys.contains(key)) {
            // si la key está en la lista entonces armo la petición sql personalizada
            CriteriaBuilder cb = entityManager.getCriteriaBuilder();
            CriteriaQuery<DocumentsEntity> query = cb.createQuery(DocumentsEntity.class);
            Root<DocumentsEntity> root = query.from(DocumentsEntity.class);

            Predicate condition = cb.equal(root.get(key), value);
            query.select(root).where(condition);

            //retorno los resultados en un array []
            return entityManager.createQuery(query).getResultList();
        } else {
            throw new IllegalArgumentException("pedido no válido");
        }
    }

    // servicio para buscar un documento por id
    public DocumentInfoPorIdDto buscarPorId(Long id) {
        Optional<DocumentsEntity> respuesta = documentsRepository.findById(id);
        DocumentInfoPorIdDto datosAEnviar = new DocumentInfoPorIdDto();

        if (respuesta.isPresent()) {
            DocumentsEntity documentoEncontrado = respuesta.get();

            documentoEncontrado.setCountPreView(documentoEncontrado.getCountPreView() + 1);
            documentsRepository.save(documentoEncontrado);

            return prepararRespuestaPorId(documentoEncontrado);

        } else {
            throw new IllegalArgumentException("pedido no válido");
        }
    }

    // servicio para buscar todos los documentos
    public List<DocumentsEntity> buscarTodosDocuments() {
        try {
            // retorno solo los elementos que no están borrados (valor borradoLogico = false)
            return documentsRepository.findByBorradoLogicoFalse();
        } catch (Exception exception) {
            throw new IllegalArgumentException("error en el servidor", exception);
        }
    }

    //servicio para borrar un documento en forma lógica
    public void borradoLogicoDocument(Long id) {
        Optional<DocumentsEntity> documentoEncontrado = documentsRepository.findById(id);

        if (documentoEncontrado.isPresent()) {
            DocumentsEntity documentoAModificar = documentoEncontrado.get();
            documentoAModificar.setBorradoLogico(!documentoAModificar.getBorradoLogico());
            documentsRepository.save(documentoAModificar);
        } else {
            throw new IllegalArgumentException("error de consistencia de datos");
        }
    }

    // servicio para borrar un documento en forma permanente (fisica)
    public void borradoFisicoDocument(Long id) {
        Optional<DocumentsEntity> documentoEncontrado = documentsRepository.findById(id);

        try {
            if (documentoEncontrado.isPresent()) {
                logger.info("BORRANDO archivo");
                firebaseServiceMio.borrarArchivo(documentoEncontrado.get().getFileNameId());
                logger.info("borrando imagen");
                firebaseServiceMio.borrarArchivo(documentoEncontrado.get().getImagenNameId());
                logger.info("BORRANDO registro");
                documentsRepository.deleteById(id);
            } else {
                throw new IllegalArgumentException("error de consistencia de datos");
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("error de borrado");
        }
    }

    public List<DocumentLosMasDto> buscarMasReciente(int pagina, int cantElementos) {
        Pageable pageable = PageRequest.of(pagina, cantElementos);
        Page<DocumentsEntity> masRecientes = documentsRepository.findAllByOrderByFileCreateTimeDesc(pageable);
        return masRecientes.stream()
                .distinct()
                .map(this:: prepararRespuestaLosMas)
                .collect(Collectors.toList());
    }


    public List<DocumentLosMasDto> buscarLosMasVistos(int pagina, int cantElementos) {
        Pageable pageable = PageRequest.of(pagina, cantElementos);
        Page<DocumentsEntity> masVistos = documentsRepository.findAllByOrderByCountPreViewDesc(pageable);
        return masVistos.stream()
                .distinct()
                .map(this::prepararRespuestaLosMas)
                .collect(Collectors.toList());
    }

    public List<DocumentLosMasDto> masvendidos(int pagina, int cantElementos) {
        Pageable pageable = PageRequest.of(pagina, cantElementos);
        Page<DocumentsEntity> masVendidos = documentsRepository.findDocumentsOrderedBySalesCount(pageable);

        return masVendidos.stream()
                .distinct()
                .map(this:: prepararRespuestaLosMas)
                .collect(Collectors.toList());
    }



    /*  ------------    MÉTODOS     ---------------  */

    private CompletableFuture<byte[]> sacarPrimeraPaginaPdfAImagen(byte[] file) throws IOException {

        return CompletableFuture.supplyAsync(() -> {
            try (PDDocument document = PDDocument.load(file)) {
                PDFRenderer pdfRenderer = new PDFRenderer(document);
                BufferedImage firstPageImage = pdfRenderer.renderImageWithDPI(0, 72);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(firstPageImage, "png", baos);
                return baos.toByteArray();
            } catch (IOException e) {
                logger.error("Error al procesar el PDF", e);
                throw new RuntimeException("Error al procesar el PDF", e);
            }
        });

    }

    // lista de MIME types permitidos
    public static final Set<String> ALLOWED_MIME_TYPES = Set.of("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "image/jpeg", "image/png", "image/gif");

    // Sacar número de páginas del archivo
    public CompletableFuture<Integer> contarPaginasPdf(byte[] file) throws IOException {
        return CompletableFuture.supplyAsync(() -> {
            try (PDDocument document = PDDocument.load(file)) {
                return document.getNumberOfPages();
            } catch (IOException e) {
                logger.error("Error al procesar el archivo ", e);
                throw new RuntimeException("Error al procesar el archivo ", e);
            }
        });
    }

    // Método para llamar al script de conversión en python
    private CompletableFuture<DocumentPdfCovertidoDto> llamadaScriptPythonConversion(byte[] file, String scriptPath, String outputFilePath, String baseName) throws IOException {

        return CompletableFuture.supplyAsync(() -> {
            DocumentPdfCovertidoDto respuestaConversion = new DocumentPdfCovertidoDto();

            if (file.length == 0) {
                logger.error("El archivo Word es vacío.");
                throw new RuntimeException("El archivo Word es vacío.");
            }

            File tempFile = null;
            try {
                tempFile = File.createTempFile("temp", ".docx");
                tempFile.deleteOnExit();
                try (FileOutputStream fileOutputStream = new FileOutputStream(tempFile)) {
                    fileOutputStream.write((file));
                }
            } catch (IOException e) {
                logger.error(String.valueOf(e));
                throw new RuntimeException(e);
            }

//            log.info("Archivo temporal creado: {}", tempFile.getAbsolutePath() + "scriptPath = " + scriptPath + "outputFilePath = " + outputFilePath + "baseName = " + baseName);

            try {
                // Llama al script de Python para convertir el archivo
                ProcessBuilder processBuilder = new ProcessBuilder("python3", scriptPath, tempFile.getAbsolutePath(), outputFilePath, baseName);
                Process process = processBuilder.start();

                CompletableFuture<Void> outputFuture = CompletableFuture.runAsync(() -> {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            System.out.println("Salida del script: " + line);

                            // Verifica si la línea contiene solo un número (número de páginas)
                            if (line.matches("\\d+")) { // Este patrón verifica si la línea es un número
                                try {
                                    int numPages = Integer.parseInt(line.trim()); // Convertir el número
                                    respuestaConversion.setNumeroDePagina(numPages); // Asignar al DTO
                                } catch (NumberFormatException e) {
                                    logger.info("Error al convertir el número de páginas: {}", line);
                                }
                            }
                        }
                    } catch (IOException e) {
                        logger.error("Error al leer la salida estándar del script.", e);
                        throw new RuntimeException("Error al leer la salida estándar del script.", e);
                    }
                });
                CompletableFuture<Void> errorFuture = CompletableFuture.runAsync(() -> {
                    try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = errorReader.readLine()) != null) {
                            if (line.contains("%|") || line.contains("[00:")) {
                                continue; // Ignorar barras de progreso
                            }
                            logger.info("Error del script: " + line);
                        }
                    } catch (IOException e) {
                        logger.error("Error al leer la salida de error del script.", e);
                        throw new RuntimeException("Error al leer la salida de error del script.", e);
                    }
                });

                // Espera a que el proceso termine
                int exitCode = process.waitFor();

                // Asegúrate de que se procesen ambas salidas antes de continuar
                CompletableFuture.allOf(outputFuture, errorFuture).join();

                if (exitCode != 0) {
                    logger.error("Error al ejecutar el script de Python: ");
                    throw new RuntimeException("Error al ejecutar el script de Python: ");
                }
                respuestaConversion.setExitoConversionPython(true);

            } catch (IOException | InterruptedException e) {
                logger.error("Error al ejecutar el script de Python: ", e);
                throw new RuntimeException("Error al ejecutar el script de Python.", e);
            } finally {
                // Eliminar el archivo temporal si fue creado
                if (tempFile.exists() && tempFile != null && !tempFile.delete()) {
                    logger.info("error al borrar archivo temporal");
                } else {
                    logger.info("Archivo temporal eliminado exitosamente.");
                }
            }
            return respuestaConversion;
        });
    }

    // Método para obtener el archivo convertido a pdf del directorio
    public byte[] obtenerPdfDesdeDirectorio(String outputDir, String nombre) throws IOException {

        // Generar el nombre del archivo PDF
        String pdfFilename = nombre + "_first_page.pdf";
        Path pdfPath = Paths.get(outputDir, pdfFilename);

        // Comprobar si el archivo existe
        if (!Files.exists(pdfPath)) {
            logger.error("El archivo PDF no se encontró: ");
            throw new FileNotFoundException("El archivo PDF no se encontró: ");
        }

        // Leer el archivo PDF en un arreglo de bytes
        return Files.readAllBytes(pdfPath);
    }

    //sacar nombre base del archivo
    private String baseNameFile(MultipartFile file) {
        // saco el nombre del archivo
        String fileName = file.getOriginalFilename();

        // saco nombre del archivo
        return fileName.substring(0, fileName.lastIndexOf("."));
    }

    // validaciones del archivo que ingresa
    private boolean validarArchivo(MultipartFile file, DocumentDto documento) {

        // compruebo si el archivo viene vacío o no viene
        if (file == null || file.isEmpty()) {
            logger.error("error de recepción de archivo");
            throw new IllegalArgumentException("error de recepción de archivo");
        }

        // saco el nombre del archivo
        String fileName = file.getOriginalFilename();
        if (fileName == null || !fileName.contains(".")) {
            logger.error("el nombre del archivo no tiene una extensión válida");
            throw new IllegalArgumentException("el nombre del archivo no tiene una extensión válida");
        }

        // saco el tipo de archivo
        String fileType = file.getContentType();

        // Usa URLConnection para detectar el tipo MIME
        String mimeType = URLConnection.guessContentTypeFromName(file.getOriginalFilename());

        String baseName = fileName != null ? fileName.substring(0, fileName.lastIndexOf(".")) : "file";

        String extension = fileName.substring(fileName.lastIndexOf(".") + 1);


        // compruebo si el archivo corresponde con el formato (format)
        String format = documento.getFormat() != null ? documento.getFormat().trim().toLowerCase() : null;
        if (format == null || !extension.equalsIgnoreCase(format)) {
            logger.error("no corresponde el tipo de archivo con su formato");
            throw new IllegalArgumentException("no corresponde el tipo de archivo con su formato");
        }

        // compruebo que el contenido del archivo es de tipo pdf, word o imágen
        if (!(fileType.equals("application/pdf") || fileType.equals("application/msword") || fileType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document") || fileType.equals("image/jpeg") || fileType.equals("image/png") || fileType.equals("image/gif"))) {
            logger.error("el tipo de archivo no es permitido");
            throw new IllegalArgumentException("el tipo de archivo no es permitido");
        }
        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            logger.error("el tipo de archivo no es permitido");
            throw new IllegalArgumentException("el tipo de archivo no es permitido");
        }

        return true;
    }

    // preparar la respuesta
    private DocumentsEntity prepararRespuesta(DocumentDto documento, int cantidadPaginas, ResFirebase responseFirebase, ResFirebase responseFirebaseImagen) {

        DocumentsEntity datosIngreso = new DocumentsEntity();

        datosIngreso.setTitle(documento.getTitle());
        datosIngreso.setDescription(documento.getDescription());
        datosIngreso.setFormat(documento.getFormat());
        datosIngreso.setPrice(documento.getPrice());
        datosIngreso.setCategory(documento.getCategory());
        datosIngreso.setNumeroDePaginas(cantidadPaginas);

        datosIngreso.setFileNameId(responseFirebase.getFileName());
        datosIngreso.setFileUrlPublic(responseFirebase.getPublicaLink());
        datosIngreso.setFileUrlPrivate(responseFirebase.getPrivadaLink());
        datosIngreso.setFileDownLoadToken(responseFirebase.getDownLoadToken());
        datosIngreso.setFileCreateTime(responseFirebase.getCreateTime());

        datosIngreso.setImagenNameId(responseFirebaseImagen.getFileName());
        datosIngreso.setImagenUrlPublic(responseFirebaseImagen.getPublicaLink());
        datosIngreso.setImagenUrlprivate(responseFirebaseImagen.getPrivadaLink());
        datosIngreso.setImageDownLoadToken(responseFirebaseImagen.getDownLoadToken());
        datosIngreso.setImageCreateTime(responseFirebase.getCreateTime());

        return datosIngreso;
    }

    // preparar la entidad para actualizar el documento (referido al archivo)
    private DocumentsEntity prepararRespuestaFileUpdate(DocumentsEntity documento, String format, int cantidadPaginas, ResFirebase responseFirebase, ResFirebase responseFirebaseImagen) {

        documento.setFormat(format);
        documento.setNumeroDePaginas(cantidadPaginas);

        documento.setFileNameId(responseFirebase.getFileName());
        documento.setFileUrlPublic(responseFirebase.getPublicaLink());
        documento.setFileUrlPrivate(responseFirebase.getPrivadaLink());
        documento.setFileDownLoadToken(responseFirebase.getDownLoadToken());
        documento.setFileCreateTime(responseFirebase.getCreateTime());

        documento.setImagenNameId(responseFirebaseImagen.getFileName());
        documento.setImagenUrlPublic(responseFirebaseImagen.getPublicaLink());
        documento.setImagenUrlprivate(responseFirebaseImagen.getPrivadaLink());
        documento.setImageDownLoadToken(responseFirebaseImagen.getDownLoadToken());
        documento.setImageCreateTime(responseFirebase.getCreateTime());

        return documento;
    }

    // preparar la entidad para actualizar la base de datos (referido a los datos del documento)
    private DocumentsEntity prepararParaGuardarDatosUpdate(DocumentDto documentoUpdate, DocumentsEntity documentoAActualizar) {

        // verifico y actualizo solo los campos que se reciban por body
        if (documentoUpdate.getTitle() != null && !documentoUpdate.getTitle().equals(documentoAActualizar.getTitle())) {
            documentoAActualizar.setTitle(documentoUpdate.getTitle());
        } else if (documentoUpdate.getPrice() != null && !documentoUpdate.getPrice().equals(documentoAActualizar.getPrice())) {
            documentoAActualizar.setPrice(documentoUpdate.getPrice());
        } else if (documentoUpdate.getDescription() != null && !documentoUpdate.getDescription().equals(documentoAActualizar.getDescription())) {
            documentoAActualizar.setDescription(documentoUpdate.getDescription());
        } else if (documentoUpdate.getCategory() != null && !documentoUpdate.getCategory().equals(documentoAActualizar.getCategory())) {
            documentoAActualizar.setCategory(documentoUpdate.getCategory());
        }

        return documentoAActualizar;
    }

    private DocumentInfoPorIdDto prepararRespuestaPorId(DocumentsEntity documento) {
        DocumentInfoPorIdDto documentInfoPorIdDto = new DocumentInfoPorIdDto();

        documentInfoPorIdDto.setId(documento.getId());
        documentInfoPorIdDto.setFormat(documento.getFormat());
        documentInfoPorIdDto.setTitle(documento.getTitle());
        documentInfoPorIdDto.setPrice(documento.getPrice());
        documentInfoPorIdDto.setCategory(documento.getCategory());
        documentInfoPorIdDto.setDescription(documento.getDescription());
        documentInfoPorIdDto.setNumeroDePaginas(documento.getNumeroDePaginas());
        documentInfoPorIdDto.setCountLikes(documento.getCountLikes());
        documentInfoPorIdDto.setCountPreView(documento.getCountPreView());

        documentInfoPorIdDto.setUrlImagenPublic(documento.getImagenUrlPublic());
        documentInfoPorIdDto.setUrlImagenPrivate(documento.getFileUrlPrivate());

        return documentInfoPorIdDto;
    }

    private DocumentLosMasDto prepararRespuestaLosMas(DocumentsEntity documento) {

        LocalDateTime date = Instant.ofEpochMilli(documento.getFileCreateTime() / 1000).atZone(ZoneId.systemDefault()).toLocalDateTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        String formatterdDate = date.format(formatter);

        DocumentLosMasDto documentLosMasDto = new DocumentLosMasDto();

        documentLosMasDto.setId(documento.getId());
        documentLosMasDto.setTitle(documento.getTitle());
        documentLosMasDto.setFormat(documento.getFormat());
        documentLosMasDto.setCategory(documento.getCategory());
        documentLosMasDto.setDescription(documento.getDescription());
        documentLosMasDto.setPrice(documento.getPrice());
        documentLosMasDto.setNumeroDePaginas(documento.getNumeroDePaginas());
        documentLosMasDto.setBorradoLogico(documento.getBorradoLogico());
        documentLosMasDto.setCountLikes(documento.getCountLikes());
        documentLosMasDto.setCountPreview(documento.getCountPreView());

        documentLosMasDto.setUrlImagenPublic(documento.getImagenUrlPublic());
        documentLosMasDto.setUrlImagenPrivate(documento.getFileUrlPrivate());
        documentLosMasDto.setFilecreateTime(formatterdDate);

        return documentLosMasDto;
    }
}
