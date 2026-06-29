package com.example.egresos.service;

import com.example.egresos.dto.EgresoRequestDTO;
import com.example.egresos.dto.EgresoResponseDTO;
import com.example.egresos.model.Egreso;
import com.example.egresos.repository.EgresoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class EgresosServiceTest {

    @Mock EgresoRepository repository;
    @Mock WebClient webClientProductos;
    @Mock WebClient webClientKardex;
    @InjectMocks EgresoService service;

    @Mock WebClient.RequestHeadersUriSpec getUriSpec;
    @Mock WebClient.RequestHeadersSpec headersSpec;
    @Mock WebClient.ResponseSpec responseSpec;
    @Mock WebClient.RequestBodyUriSpec postUriSpec;
    @Mock WebClient.RequestBodySpec bodySpec;

    private void mockProductoExiste() {
        when(webClientProductos.get()).thenReturn(getUriSpec);
        when(getUriSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Object.class)).thenReturn(Mono.just(new Object()));
    }

    private void mockStockSuficiente(int cantidad) {
        when(webClientKardex.get()).thenReturn(getUriSpec);
        when(getUriSpec.uri(contains("stock"))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.just(Map.of("cantidad", cantidad)));
    }

    private void mockPostKardex(boolean success) {
        when(webClientKardex.post()).thenReturn(postUriSpec);
        when(postUriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.bodyValue(any())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        Mono<Object> mono = success ? Mono.just(new Object()) : Mono.error(new RuntimeException());
        when(responseSpec.bodyToMono(Object.class)).thenReturn(mono);
    }

    private EgresoRequestDTO dtoBase() {
        EgresoRequestDTO dto = new EgresoRequestDTO();
        dto.setProductoId(1L);
        dto.setBodegaId(2L);
        dto.setCantidad(5);
        dto.setDestinatario("Cliente A");
        dto.setNumeroDocumento("DOC-001");
        dto.setObservaciones("Test");
        return dto;
    }

    private Egreso egresoMock(Long id, String estado) {
        Egreso e = new Egreso();
        e.setId(id);
        e.setProductoId(1L);
        e.setBodegaId(2L);
        e.setCantidad(5);
        e.setFechaEgreso(LocalDate.now());
        e.setDestinatario("Cliente A");
        e.setNumeroDocumento("DOC-001");
        e.setEstado(estado);
        e.setObservaciones("Test");
        return e;
    }

    @Test
    void crear_exito() {
        mockProductoExiste();
        mockStockSuficiente(10);
        when(repository.save(any())).thenReturn(egresoMock(100L, "PENDIENTE"));

        EgresoResponseDTO resp = service.crear(dtoBase());

        assertThat(resp.getId()).isEqualTo(100L);
        assertThat(resp.getEstado()).isEqualTo("PENDIENTE");
    }

    @Test
    void crear_productoNoExiste_lanzaExcepcion() {
        when(webClientProductos.get()).thenReturn(getUriSpec);
        when(getUriSpec.uri(anyString())).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(Object.class)).thenReturn(Mono.error(new RuntimeException()));

        assertThatThrownBy(() -> service.crear(dtoBase()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("no existe en ms-productos");
    }

    @Test
    void crear_stockInsuficiente_lanzaExcepcion() {
        mockProductoExiste();
        mockStockSuficiente(2);

        assertThatThrownBy(() -> service.crear(dtoBase()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Stock insuficiente");
    }

    @Test
    void crear_stockNoDisponible_asumeCero_lanzaExcepcion() {
        mockProductoExiste();
        when(webClientKardex.get()).thenReturn(getUriSpec);
        when(getUriSpec.uri(contains("stock"))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(ParameterizedTypeReference.class)))
                .thenReturn(Mono.error(new RuntimeException()));

        assertThatThrownBy(() -> service.crear(dtoBase()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Stock insuficiente");
    }

    @Test
    void confirmar_exito() {
        Egreso pendiente = egresoMock(10L, "PENDIENTE");
        when(repository.findById(10L)).thenReturn(Optional.of(pendiente));
        when(repository.save(any())).thenReturn(pendiente);
        mockPostKardex(true);

        EgresoResponseDTO resp = service.confirmar(10L);

        assertThat(resp.getEstado()).isEqualTo("CONFIRMADO");
        verify(webClientKardex).post();
    }

    @Test
    void confirmar_falloKardex_lanzaExcepcion() {
        Egreso pendiente = egresoMock(11L, "PENDIENTE");
        when(repository.findById(11L)).thenReturn(Optional.of(pendiente));
        mockPostKardex(false);

        assertThatThrownBy(() -> service.confirmar(11L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No se pudo registrar el movimiento en Kardex");

        verify(repository, never()).save(any());
    }

    @Test
    void confirmar_estadoNoPendiente_lanzaExcepcion() {
        when(repository.findById(12L)).thenReturn(Optional.of(egresoMock(12L, "CONFIRMADO")));

        assertThatThrownBy(() -> service.confirmar(12L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Solo se pueden confirmar egresos en estado PENDIENTE");
    }

    @Test
    void anular_desdePendiente_exito() {
        when(repository.findById(20L)).thenReturn(Optional.of(egresoMock(20L, "PENDIENTE")));
        when(repository.save(any())).thenReturn(egresoMock(20L, "ANULADO"));

        assertThat(service.anular(20L).getEstado()).isEqualTo("ANULADO");
    }

    @Test
    void anular_confirmado_lanzaExcepcion() {
        when(repository.findById(21L)).thenReturn(Optional.of(egresoMock(21L, "CONFIRMADO")));

        assertThatThrownBy(() -> service.anular(21L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("No se puede anular un egreso ya CONFIRMADO");
    }

    @Test
    void listarTodos_retornaLista() {
        when(repository.findAll()).thenReturn(List.of(
                egresoMock(1L, "PENDIENTE"),
                egresoMock(2L, "CONFIRMADO")
        ));

        assertThat(service.listarTodos()).hasSize(2);
    }

    @Test
    void buscarPorId_noEncontrado_lanzaExcepcion() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorId(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Egreso no encontrado");
    }

    @Test
    void listarPendientes_soloPendientes() {
        when(repository.findByEstado("PENDIENTE")).thenReturn(List.of(egresoMock(1L, "PENDIENTE")));

        assertThat(service.listarPendientes()).hasSize(1);
    }

    @Test
    void listarPorProducto_retornaLista() {
        when(repository.findByProductoId(1L)).thenReturn(List.of(egresoMock(1L, "PENDIENTE")));

        assertThat(service.listarPorProducto(1L)).hasSize(1);
    }
}