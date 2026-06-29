package com.example.egresos.repository;

import com.example.egresos.model.Egreso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EgresoRepository extends JpaRepository<Egreso, Long> {
    List<Egreso> findByEstado(String estado);
    List<Egreso> findByProductoId(Long productoId);
    List<Egreso> findByBodegaId(Long bodegaId);
}