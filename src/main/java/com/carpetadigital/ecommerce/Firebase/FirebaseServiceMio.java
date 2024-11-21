package com.carpetadigital.ecommerce.Firebase;

import com.google.cloud.storage.Acl;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.cloud.StorageClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class FirebaseServiceMio {

    @Async
    public CompletableFuture<ResFirebase> guardarArchivoEnFirebase(byte[] file, String fileType, String categoria, String baseName) throws IOException {
        ResFirebase datosDeGuardado = new ResFirebase();

        // Crear un InputStream a partir del byte[] (esto es necesario porque el método bucket.create espera un InputStream)
        InputStream fileInputStream = new ByteArrayInputStream(file);

        String rutaName = categoria + "/" + baseName;

        return CompletableFuture.supplyAsync(() -> {

            // Obtén una referencia al bucket
            Bucket bucket = StorageClient.getInstance().bucket();

            Blob blob = bucket.create(rutaName, fileInputStream, fileType);

            blob.createAcl(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER));

            datosDeGuardado.setFileName(blob.getName());
            datosDeGuardado.setFileSize(blob.getSize());
            datosDeGuardado.setContentType(blob.getContentType());
            datosDeGuardado.setPublicaLink(blob.getMediaLink());
            datosDeGuardado.setPrivadaLink(blob.getSelfLink());
            datosDeGuardado.setDownLoadToken(blob.getMd5());
            datosDeGuardado.setCreateTime(blob.getBlobId().getGeneration());
            datosDeGuardado.setFileId(blob.getBlobId());

            return datosDeGuardado;
        });
    }

    @Async
    public CompletableFuture<ResFirebase> guardarImagenEnFirebase(byte[] file, String nombreImagen) throws IOException {
        ResFirebase datosDeGuardado = new ResFirebase();
        return CompletableFuture.supplyAsync(() -> {
            String fileName = UUID.randomUUID() + nombreImagen;
            String rutaName = "imagenes" + "/" + fileName;

            try {
                // Obtén una referencia al bucket
                Bucket bucket = StorageClient.getInstance().bucket();
                Blob blob = bucket.create(rutaName, file, "image/png");

                blob.createAcl(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER));

                datosDeGuardado.setFileName(blob.getName());
                datosDeGuardado.setFileSize(blob.getSize());
                datosDeGuardado.setContentType(blob.getContentType());
                datosDeGuardado.setPublicaLink(blob.getMediaLink());
                datosDeGuardado.setPrivadaLink(blob.getSelfLink());
                datosDeGuardado.setDownLoadToken(blob.getMd5());
                datosDeGuardado.setFileId(blob.getBlobId());
                datosDeGuardado.setCreateTime(blob.getBlobId().getGeneration());

            } catch (RuntimeException e) {
                throw new RuntimeException(e);
            }
            return datosDeGuardado;
        });

    }

    public boolean borrarArchivo(String fileNameIdConRuta) {

        // usa el bucket predeterminado configurado en Firebase
        Bucket bucket = StorageClient.getInstance().bucket();

        if (bucket != null) {
            // Obtén el archivo (Blob) dentro del bucket
            Blob blob = bucket.get(fileNameIdConRuta);
            if (blob != null) {
                // Elimina el archivo
                return blob.delete();
            } else {
                System.out.println("El archivo no existe");
            }
        } else {
            System.out.println("El bucket no se encuentra.");
        }
        return false;
    }

    public CompletableFuture<Boolean> buscarArchivoFirebase(String fileNameIdConRuta) {

        return CompletableFuture.supplyAsync(() -> {
            Bucket bucket = StorageClient.getInstance().bucket();
            if (bucket != null) {
                Blob blob = bucket.get(fileNameIdConRuta);
                if (blob != null) {
                    return true;
                }
            }
            return false;
        });

    }

    public List<String> buscarTodosArchivosFirebase() {

        List<String> filesNames = new ArrayList<>();
        Bucket bucket = StorageClient.getInstance().bucket();

        for (Blob blob : bucket.list().iterateAll()) {
            filesNames.add(blob.getName());
        }
        return filesNames;
    }

    // Método para obtener la URL de descarga pública de un archivo en Firebase Storage
    public String getDownloadURL(String filePath) {
        // Obtén el bucket de Firebase
        Bucket bucket = StorageClient.getInstance().bucket();

        if (bucket != null) {
            Blob blob = bucket.get(filePath);  // Obtén el blob del archivo

            if (blob != null) {
                // Retorna el enlace público de descarga
                return blob.getMediaLink();
            }
        }
        return null;  // Retorna null si el archivo no existe o no se puede acceder
    }

    public String url() throws UnsupportedEncodingException {
        // Obtiene el bucket y luego el archivo (Blob)
        Bucket bucket = StorageClient.getInstance().bucket();
//        Blob blob = bucket.get("imagenes/255880c4-e0df-4f8b-b938-3c1083a7c4b0PROGRAMACIÓN ANUAL - V CICLO");  // Asegúrate de que el nombre del archivo esté correcto

        if (bucket == null) {
            return "Bucket no encontrado.";
        }
        // Obtiene el archivo (Blob) en el bucket
        Blob blob = bucket.get("matemáticas/3fc70fd2-0fc2-43a9-8754-45746c694630PROGRAMACIÓN ANUAL - V CICLO");  // Asegúrate de que el nombre del archivo sea correcto
//        Blob blob = bucket.get("imagenes/255880c4-e0df-4f8b-b938-3c1083a7c4b0PROGRAMACIÓN ANUAL - V CICLO");  // Asegúrate de que el nombre del archivo sea correcto
//        Blob blob = bucket.get("matemáticas/f92c42bd-d494-4d7b-9830-d97817891c07PROGRAMACIÓN ANUAL - V CICLO");  // Asegúrate de que el nombre del archivo sea correcto

        if (blob == null) {
            return "Archivo no encontrado en el bucket.";
        }

        // Verifica o configura permisos públicos de lectura
        if (blob.getAcl(Acl.User.ofAllUsers()) == null) {
            blob.createAcl(Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER));
        }

        Acl acl = blob.getAcl(Acl.User.ofAllUsers());


//        // Devuelve la URL de previsualización
//        return String.format("https://storage.googleapis.com/%s/%s", bucket.getName(), blob.getName());
        // Codifica la ruta del archivo para usarla en la URL
        String encodedFilePath = URLEncoder.encode(blob.getName(), "UTF-8").replaceAll("\\+", "%20");

        // Obtiene los metadatos del archivo
        Map<String, String> metadata = blob.getMetadata();
        System.out.println(metadata);
        // Obtiene el token, si existe
        String token = Objects.requireNonNull(blob.getMetadata()).get("firebaseStorageDownloadTokens");
//        token = metadata.get("firebaseStorageDownloadTokens");
//        if (metadata != null) {
        // Actualizar metadatos para agregar el token de descarga
        Map<String, String> newMetadata = new HashMap<>();
        newMetadata.put("firebaseStorageDownloadTokens", "");
        blob.toBuilder().setMetadata(newMetadata).build().update();
//        }
//
//// Obtener el token después de actualizar

//        }
//
//        if (token == null) {
//            return "El archivo no tiene un token de descarga configurado.";
//        }

        try {
            // Verifica si el archivo tiene el token

        } catch (Exception e) {
            // Si no tiene un token, lo dejamos como null
            token = null;
        }
//            // Construir la URL de previsualización
//            String bucketName = bucket.getName(); // Ejemplo: "cd-store-529c3"
//            String encodedFileName = URLEncoder.encode(blob.getName(), StandardCharsets.UTF_8);
//            String previewUrl = "https://firebasestorage.googleapis.com/v0/b/" + bucketName + "/o/" + encodedFileName + "?alt=media&token=" + token;
//        System.out.println(previewUrl + "-----");
//        return previewUrl;

//        // Construye la URL de previsualización
//        return String.format(
//                "https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media&token=%s",
//                bucket.getName(),
//                encodedFilePath,
//                token
//        );
//        // Obtén el token (opcional si el archivo es público ya)
//        String token = blob.getMetadata().toString();
//
//        // Construye la URL de previsualización
//        return String.format(
//                "https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media&token=%s",
//                bucket.getName(),
//                encodedFilePath,
//                token);
        return "token";
    }

}

