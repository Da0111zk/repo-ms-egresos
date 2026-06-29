package com.example.egresos.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "EGRESOS")
public class Egreso {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_egresos")
    @SequenceGenerator(name = "seq_egresos", sequenceName = "SEQ_EGRESOS", allocationSize = 1)
    private Long id;

    @Column(name = "PRODUCTO_ID", nullable = false)
    private Long productoId;

    @Column(name = "BODEGA_ID", nullable = false)
    private Long bodegaId;

    @Column(name = "CANTIDAD", nullable = false)
    private Integer cantidad;

    @Column(name = "FECHA_EGRESO", nullable = false)
    private LocalDate fechaEgreso;

    @Column(name = "DESTINATARIO", nullable = false, length = 100)
    private String destinatario;

    @Column(name = "NUMERO_DOCUMENTO", length = 50)
    private String numeroDocumento;

    @Column(name = "ESTADO", nullable = false, length = 20)
    private String estado = "PENDIENTE";

    @Column(name = "OBSERVACIONES", length = 300)
    private String observaciones;
}