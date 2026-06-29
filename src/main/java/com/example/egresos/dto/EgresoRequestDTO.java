package com.example.egresos.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EgresoRequestDTO {
    @Schema(description = "ID del producto a egresar", example = "1")
    @NotNull(message = "El ID de producto es obligatorio")
    @Positive(message = "El ID de producto debe ser positivo")
    private Long productoId;

    @Schema(description = "ID de la bodega desde donde se egresa el producto", example = "1")
    @NotNull(message = "El ID de bodega es obligatorio")
    @Positive(message = "El ID de bodega debe ser positivo")
    private Long bodegaId;

    @Schema(description = "Cantidad de unidades a egresar", example = "10")
    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser mayor a 0")
    private Integer cantidad;
    
    @Schema(description = "Nombre del destinatario de la mercancía", example = "Sucursal Norte")
    @NotBlank(message = "El destinatario es obligatorio")
    @Size(max = 100, message = "El destinatario no puede exceder 100 caracteres")
    private String destinatario;

    @Schema(description = "Número de documento asociado al egreso", example = "DOC-2025-045")
    @Size(max = 50, message = "El número de documento no puede exceder 50 caracteres")
    private String numeroDocumento;

    @Schema(description = "Observaciones adicionales del egreso", example = "Despacho urgente")
    @Size(max = 300, message = "Las observaciones no pueden exceder 300 caracteres")
    private String observaciones;

}