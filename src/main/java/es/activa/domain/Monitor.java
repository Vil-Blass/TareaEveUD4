package es.activa.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "monitor")
public class Monitor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "especialidad", nullable = false)
    private String especialidad;

    @OneToMany(mappedBy = "monitor")
    private List<Actividad> actividades = new ArrayList<>();

    protected Monitor() {
    }

    public Monitor(String nombre, String especialidad) {
        this.nombre = nombre;
        this.especialidad = especialidad;
    }

    public void addActividad(Actividad actividad) {
        actividad.setMonitor(this);
    }

    void addActividadReference(Actividad actividad) {
        if (!actividades.contains(actividad)) {
            actividades.add(actividad);
        }
    }

    void removeActividadReference(Actividad actividad) {
        actividades.remove(actividad);
    }

    public void actualizar(String nombre, String especialidad) {
        this.nombre = nombre;
        this.especialidad = especialidad;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public List<Actividad> getActividades() {
        return List.copyOf(actividades);
    }
}
