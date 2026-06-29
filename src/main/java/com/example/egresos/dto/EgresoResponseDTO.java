package com.example.egresos.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class EgresoResponseDTO {
    private Long id;
    private Long productoId;
    private Long bodegaId;
    private Integer cantidad;
    private LocalDate fechaEgreso;
    private String destinatario;
    private String numeroDocumento;
    private String estado;
    private String observaciones;
}