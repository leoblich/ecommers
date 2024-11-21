package com.carpetadigital.ecommerce.servicios;




import com.carpetadigital.ecommerce.utils.handler.ResponseHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sevicios")
public class ServiceController {

    private final ServiceService serviceService;

    @Autowired
    public ServiceController(ServiceService categoryService) {
        this.serviceService = categoryService;
    }

    @GetMapping
    public ResponseEntity<Object> getAllServicios() {
        return ResponseHandler.generateResponse(
                HttpStatus.OK,
                serviceService.getAllServicios(),
                true);
    }
}
