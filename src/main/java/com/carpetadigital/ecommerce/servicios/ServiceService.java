package com.carpetadigital.ecommerce.servicios;

import com.carpetadigital.ecommerce.Repository.ServiciosRepository;
import com.carpetadigital.ecommerce.entity.Servicios;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ServiceService {

    private final ServiciosRepository serviciosRepository;

    @Autowired
    public ServiceService(ServiciosRepository categoryRepository) {
        this.serviciosRepository = categoryRepository;
    }

    public List<Servicios> getAllServicios() {
        return serviciosRepository.findAll();
    }
}
