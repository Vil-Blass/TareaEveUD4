package es.activa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "actividad")
public class Actividad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "localidad", nullable = false)
    private String localidad;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "plazas_totales", nullable = false)
    private int plazasTotales;

    @Column(name = "plazas_disponibles", nullable = false)
    private int plazasDisponibles;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "monitor_id", nullable = false)
    private Monitor monitor;

    protected Actividad() {
    }

    public Actividad(String nombre, String localidad, LocalDateTime fechaHora, int plazasTotales, Monitor monitor) {
        this.nombre = nombre;
        this.localidad = localidad;
        this.fechaHora = fechaHora;
        this.plazasTotales = plazasTotales;
        this.plazasDisponibles = plazasTotales;
        setMonitor(monitor);
    }

    public void setMonitor(Monitor monitor) {
        Objects.requireNonNull(monitor, "La actividad debe tener un monitor.");
        if (this.monitor == monitor) {
            monitor.addActividadReference(this);
            return;
        }
        if (this.monitor != null) {
            this.monitor.removeActividadReference(this);
        }
        this.monitor = monitor;
        monitor.addActividadReference(this);
    }

    public void desvincularMonitorAlEliminar() {
        if (monitor != null) {
            Monitor monitorAnterior = monitor;
            monitor = null;
            monitorAnterior.removeActividadReference(this);
        }
    }

    public void actualizar(String nombre, String localidad, LocalDateTime fechaHora,
                           int plazasTotales, Monitor monitor) {
        int plazasReservadas = this.plazasTotales - this.plazasDisponibles;
        if (plazasTotales < plazasReservadas) {
            throw new IllegalArgumentException(
                    "Las plazas totales no pueden ser inferiores a las plazas ya reservadas.");
        }
        this.nombre = nombre;
        this.localidad = localidad;
        this.fechaHora = fechaHora;
        this.plazasTotales = plazasTotales;
        this.plazasDisponibles = plazasTotales - plazasReservadas;
        setMonitor(monitor);
    }

    public void reservar(int numeroPlazas) {
        plazasDisponibles -= numeroPlazas;
    }

    public void liberar(int numeroPlazas) {
        plazasDisponibles += numeroPlazas;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getLocalidad() {
        return localidad;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public int getPlazasTotales() {
        return plazasTotales;
    }

    public int getPlazasDisponibles() {
        return plazasDisponibles;
    }

    public Monitor getMonitor() {
        return monitor;
    }
}
