package com.example.egresos.service;

import com.example.egresos.dto.EgresoRequestDTO;
import com.example.egresos.dto.EgresoResponseDTO;
import com.example.egresos.model.Egreso;
import com.example.egresos.repository.EgresoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EgresoService {

    @Autowired
    private EgresoRepository repository;

    @Autowired
    private WebClient webClientProductos;

    @Autowired
    private WebClient webClientKardex;

    public EgresoResponseDTO crear(EgresoRequestDTO dto) {
        validarExterno(
                webClientProductos,
                "/api/productos/" + dto.getProductoId(),
                "Producto " + dto.getProductoId() + " no existe en ms-productos"
        );

        Integer stockDisponible = obtenerStock(dto.getProductoId(), dto.getBodegaId());

        if (stockDisponible < dto.getCantidad()) {
            throw new RuntimeException("Stock insuficiente. Disponible: " + stockDisponible +
                    ", solicitado: " + dto.getCantidad());
        }

        Egreso egreso = new Egreso();
        egreso.setProductoId(dto.getProductoId());
        egreso.setBodegaId(dto.getBodegaId());
        egreso.setCantidad(dto.getCantidad());
        egreso.setFechaEgreso(LocalDate.now());
        egreso.setDestinatario(dto.getDestinatario());
        egreso.setNumeroDocumento(dto.getNumeroDocumento());
        egreso.setEstado("PENDIENTE");
        egreso.setObservaciones(dto.getObservaciones());

        return toResponse(repository.save(egreso));
    }

    public EgresoResponseDTO confirmar(Long id) {
        Egreso egreso = buscarEntidadPorId(id);

        if (!"PENDIENTE".equals(egreso.getEstado())) {
            throw new RuntimeException("Solo se pueden confirmar egresos en estado PENDIENTE");
        }

        try {
            Map<String, Object> movimiento = new HashMap<>();
            movimiento.put("productoId", egreso.getProductoId());
            movimiento.put("bodegaId", egreso.getBodegaId());
            movimiento.put("cantidad", egreso.getCantidad());
            movimiento.put("tipoMovimiento", "EGRESO");

            if (egreso.getNumeroDocumento() != null) {
                movimiento.put("referencia", egreso.getNumeroDocumento());
            }

            webClientKardex.post()
                    .uri("/api/kardex/movimiento")
                    .bodyValue(movimiento)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .timeout(Duration.ofSeconds(3))
                    .block();

            log.info("Movimiento EGRESO registrado en Kardex para productoId={}", egreso.getProductoId());
        } catch (Exception e) {
            log.warn("No se pudo registrar en Kardex: {}", e.getMessage());
            throw new RuntimeException("No se pudo registrar el movimiento en Kardex");
        }

        egreso.setEstado("CONFIRMADO");
        return toResponse(repository.save(egreso));
    }

    public EgresoResponseDTO anular(Long id) {
        Egreso egreso = buscarEntidadPorId(id);

        if ("CONFIRMADO".equals(egreso.getEstado())) {
            throw new RuntimeException("No se puede anular un egreso ya CONFIRMADO");
        }

        egreso.setEstado("ANULADO");
        return toResponse(repository.save(egreso));
    }

    public List<EgresoResponseDTO> listarTodos() {
        return repository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    public EgresoResponseDTO buscarPorId(Long id) {
        return toResponse(buscarEntidadPorId(id));
    }

    public List<EgresoResponseDTO> listarPendientes() {
        return repository.findByEstado("PENDIENTE").stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<EgresoResponseDTO> listarPorProducto(Long productoId) {
        return repository.findByProductoId(productoId).stream().map(this::toResponse).collect(Collectors.toList());
    }

    private Egreso buscarEntidadPorId(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Egreso no encontrado con ID: " + id));
    }

    private void validarExterno(WebClient client, String uri, String errorMsg) {
        try {
            client.get()
                    .uri(uri)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .timeout(Duration.ofSeconds(3))
                    .block();
        } catch (Exception e) {
            throw new RuntimeException(errorMsg);
        }
    }

    private Integer obtenerStock(Long productoId, Long bodegaId) {
        try {
            Map<String, Object> stock = webClientKardex.get()
                    .uri("/api/kardex/stock/producto/" + productoId + "/bodega/" + bodegaId)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(3))
                    .block();

            if (stock != null && stock.get("cantidad") != null) {
                return ((Number) stock.get("cantidad")).intValue();
            }
        } catch (Exception e) {
            log.warn("No se pudo obtener stock desde Kardex: {}", e.getMessage());
        }
        return 0;
    }

    private EgresoResponseDTO toResponse(Egreso egreso) {
        EgresoResponseDTO dto = new EgresoResponseDTO();
        dto.setId(egreso.getId());
        dto.setProductoId(egreso.getProductoId());
        dto.setBodegaId(egreso.getBodegaId());
        dto.setCantidad(egreso.getCantidad());
        dto.setFechaEgreso(egreso.getFechaEgreso());
        dto.setDestinatario(egreso.getDestinatario());
        dto.setNumeroDocumento(egreso.getNumeroDocumento());
        dto.setEstado(egreso.getEstado());
        dto.setObservaciones(egreso.getObservaciones());
        return dto;
    }
}