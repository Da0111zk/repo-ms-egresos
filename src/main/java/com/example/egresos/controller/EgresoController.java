package com.example.egresos.controller;

import com.example.egresos.dto.EgresoRequestDTO;
import com.example.egresos.dto.EgresoResponseDTO;
import com.example.egresos.service.EgresoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Egresos", description = "Registro de salidas de mercancía del sistema de bodega")
@RestController
@RequestMapping("/api/egresos")
public class EgresoController {

    @Autowired
    private EgresoService service;


    @Operation(summary = "Registrar un nuevo egreso", description = "Valida producto y stock disponible en kardex antes de crear el egreso en estado PENDIENTE")
    @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Egreso registrado correctamente"),
    @ApiResponse(responseCode = "400", description = "Stock insuficiente o datos inválidos")
    })
    @PostMapping
    public ResponseEntity<EgresoResponseDTO> crear(@Valid @RequestBody EgresoRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(dto));
    }


    @Operation(summary = "Listar todos los egresos", description = "Retorna el historial completo de egresos registrados")
    @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente")
    @GetMapping
    public ResponseEntity<List<EgresoResponseDTO>> listarTodos() {
        return ResponseEntity.ok(service.listarTodos());
    }



    @Operation(summary = "Buscar egreso por ID", description = "Retorna los detalles de un egreso específico")
    @ApiResponse(responseCode = "200", description = "Egreso encontrado correctamente")
    @ApiResponse(responseCode = "404", description = "Egreso no encontrado")
    @GetMapping("/{id}")
    public ResponseEntity<EgresoResponseDTO> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }

    @Operation(summary = "Listar egresos en estado PENDIENTE")
    @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente")
    @GetMapping("/pendientes")
    public ResponseEntity<List<EgresoResponseDTO>> listarPendientes() {
        return ResponseEntity.ok(service.listarPendientes());
    }

    @Operation(summary = "Listar egresos de un producto específico")
    @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente")
    @GetMapping("/producto/{productoId}")
    public ResponseEntity<List<EgresoResponseDTO>> listarPorProducto(@PathVariable Long productoId) {
        return ResponseEntity.ok(service.listarPorProducto(productoId));
    }

    @Operation(summary = "Confirmar un egreso pendiente", description = "Descuenta el stock en kardex al confirmar")
    @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Egreso confirmado correctamente"),
    @ApiResponse(responseCode = "400", description = "El egreso no está en estado PENDIENTE"),
    @ApiResponse(responseCode = "404", description = "Egreso no encontrado")
    })
    @PutMapping("/{id}/confirmar")
    public ResponseEntity<EgresoResponseDTO> confirmar(@PathVariable Long id) {
        return ResponseEntity.ok(service.confirmar(id));
    }

    @Operation(summary = "Anular un egreso")
    @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Egreso anulado correctamente"),
    @ApiResponse(responseCode = "400", description = "No se puede anular un egreso ya confirmado"),
    @ApiResponse(responseCode = "404", description = "Egreso no encontrado")
    })
    @PutMapping("/{id}/anular")
    public ResponseEntity<EgresoResponseDTO> anular(@PathVariable Long id) {
        return ResponseEntity.ok(service.anular(id));
    }
}