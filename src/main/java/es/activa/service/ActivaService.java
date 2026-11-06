package es.activa.service;

import es.activa.domain.Actividad;
import es.activa.domain.Cliente;
import es.activa.domain.Monitor;
import es.activa.domain.Reserva;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public class ActivaService {
    private final EntityManagerFactory entityManagerFactory;

    public ActivaService(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    public Long crearMonitor(String nombre, String especialidad) {
        requireText(nombre, "El nombre");
        requireText(especialidad, "La especialidad");
        return inTransaction(entityManager -> {
            Monitor monitor = new Monitor(nombre.trim(), especialidad.trim());
            entityManager.persist(monitor);
            return monitor.getId();
        });
    }

    public void modificarMonitor(long monitorId, String nombre, String especialidad) {
        requireText(nombre, "El nombre");
        requireText(especialidad, "La especialidad");
        inTransaction(entityManager -> {
            Monitor monitor = entityManager.find(Monitor.class, monitorId);
            if (monitor == null) {
                throw new IllegalArgumentException("No existe un monitor con ese ID.");
            }
            monitor.actualizar(nombre.trim(), especialidad.trim());
            return null;
        });
    }

    public void eliminarMonitor(long monitorId) {
        inTransaction(entityManager -> {
            Monitor monitor = entityManager.find(Monitor.class, monitorId);
            if (monitor == null) {
                throw new IllegalArgumentException("No existe un monitor con ese ID.");
            }
            Long actividades = entityManager.createQuery(
                            "select count(a) from Actividad a where a.monitor.id = :monitorId", Long.class)
                    .setParameter("monitorId", monitorId)
                    .getSingleResult();
            if (actividades > 0) {
                throw new IllegalArgumentException(
                        "No se puede eliminar el monitor porque tiene actividades asignadas.");
            }
            entityManager.remove(monitor);
            return null;
        });
    }

    public Long crearActividad(String nombre, String localidad, LocalDateTime fechaHora,
                               int plazasTotales, long monitorId) {
        requireText(nombre, "El nombre");
        requireText(localidad, "La localidad");
        if (fechaHora == null) {
            throw new IllegalArgumentException("La fecha y hora son obligatorias.");
        }
        if (plazasTotales <= 0) {
            throw new IllegalArgumentException("El número de plazas debe ser positivo.");
        }
        return inTransaction(entityManager -> {
            Monitor monitor = entityManager.find(Monitor.class, monitorId);
            if (monitor == null) {
                throw new IllegalArgumentException("No existe un monitor con ese ID.");
            }
            Actividad actividad = new Actividad(
                    nombre.trim(), localidad.trim(), fechaHora, plazasTotales, monitor);
            monitor.addActividad(actividad);
            entityManager.persist(actividad);
            return actividad.getId();
        });
    }

    public void modificarActividad(long actividadId, String nombre, String localidad,
                                   LocalDateTime fechaHora, int plazasTotales, long monitorId) {
        requireText(nombre, "El nombre");
        requireText(localidad, "La localidad");
        if (fechaHora == null) {
            throw new IllegalArgumentException("La fecha y hora son obligatorias.");
        }
        if (plazasTotales <= 0) {
            throw new IllegalArgumentException("El número de plazas debe ser positivo.");
        }
        inTransaction(entityManager -> {
            Actividad actividad = entityManager.find(Actividad.class, actividadId);
            if (actividad == null) {
                throw new IllegalArgumentException("No existe una actividad con ese ID.");
            }
            Monitor monitor = entityManager.find(Monitor.class, monitorId);
            if (monitor == null) {
                throw new IllegalArgumentException("No existe un monitor con ese ID.");
            }
            actividad.actualizar(nombre.trim(), localidad.trim(), fechaHora, plazasTotales, monitor);
            return null;
        });
    }

    public void eliminarActividad(long actividadId) {
        inTransaction(entityManager -> {
            Actividad actividad = entityManager.find(Actividad.class, actividadId);
            if (actividad == null) {
                throw new IllegalArgumentException("No existe una actividad con ese ID.");
            }
            Long reservas = entityManager.createQuery(
                            "select count(r) from Reserva r where r.actividad.id = :actividadId", Long.class)
                    .setParameter("actividadId", actividadId)
                    .getSingleResult();
            if (reservas > 0) {
                throw new IllegalArgumentException(
                        "No se puede eliminar la actividad porque tiene reservas. Cancélalas primero.");
            }
            actividad.desvincularMonitorAlEliminar();
            entityManager.remove(actividad);
            return null;
        });
    }

    public List<Actividad> buscarDisponibles(String localidad, LocalDate fecha, int plazasNecesarias) {
        requireText(localidad, "La localidad");
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha es obligatoria.");
        }
        if (plazasNecesarias <= 0) {
            throw new IllegalArgumentException("El número de plazas debe ser positivo.");
        }
        String localidadNormalizada = localidad.trim().toLowerCase(Locale.ROOT);
        LocalDateTime inicio = fecha.atStartOfDay();
        LocalDateTime fin = fecha.plusDays(1).atStartOfDay();
        return inEntityManager(entityManager -> entityManager.createQuery(
                        "select a from Actividad a join fetch a.monitor "
                                + "where lower(a.localidad) = :localidad "
                                + "and a.fechaHora >= :inicio and a.fechaHora < :fin "
                                + "and a.plazasDisponibles >= :plazas "
                                + "order by a.fechaHora",
                        Actividad.class)
                .setParameter("localidad", localidadNormalizada)
                .setParameter("inicio", inicio)
                .setParameter("fin", fin)
                .setParameter("plazas", plazasNecesarias)
                .getResultList());
    }

    public Long crearCliente(String nombre, String email) {
        requireText(nombre, "El nombre");
        String emailNormalizado = validarEmail(email);
        String emailBusqueda = emailNormalizado.toLowerCase(Locale.ROOT);
        return inTransaction(entityManager -> {
            Long duplicados = entityManager.createQuery(
                            "select count(c) from Cliente c where lower(trim(c.email)) = :email", Long.class)
                    .setParameter("email", emailBusqueda)
                    .getSingleResult();
            if (duplicados > 0) {
                throw new IllegalArgumentException("Ya existe un cliente con ese correo electrónico.");
            }
            Cliente cliente = new Cliente(nombre.trim(), emailNormalizado);
            entityManager.persist(cliente);
            return cliente.getId();
        });
    }

    public void modificarCliente(long clienteId, String nombre, String email) {
        requireText(nombre, "El nombre");
        String emailNormalizado = validarEmail(email);
        String emailBusqueda = emailNormalizado.toLowerCase(Locale.ROOT);
        inTransaction(entityManager -> {
            Cliente cliente = entityManager.find(Cliente.class, clienteId);
            if (cliente == null) {
                throw new IllegalArgumentException("No existe un cliente con ese ID.");
            }
            Long duplicados = entityManager.createQuery(
                            "select count(c) from Cliente c "
                                    + "where c.id <> :clienteId and lower(trim(c.email)) = :email",
                            Long.class)
                    .setParameter("clienteId", clienteId)
                    .setParameter("email", emailBusqueda)
                    .getSingleResult();
            if (duplicados > 0) {
                throw new IllegalArgumentException("Ya existe un cliente con ese correo electrónico.");
            }
            cliente.actualizar(nombre.trim(), emailNormalizado);
            return null;
        });
    }

    public void eliminarCliente(long clienteId) {
        inTransaction(entityManager -> {
            Cliente cliente = entityManager.find(Cliente.class, clienteId);
            if (cliente == null) {
                throw new IllegalArgumentException("No existe un cliente con ese ID.");
            }
            Long reservas = entityManager.createQuery(
                            "select count(r) from Reserva r where r.cliente.id = :clienteId", Long.class)
                    .setParameter("clienteId", clienteId)
                    .getSingleResult();
            if (reservas > 0) {
                throw new IllegalArgumentException(
                        "No se puede eliminar el cliente porque tiene reservas. Cancélalas primero.");
            }
            entityManager.remove(cliente);
            return null;
        });
    }

    public ReservaCreada crearReserva(long clienteId, long actividadId, int numeroPlazas) {
        if (numeroPlazas <= 0) {
            throw new IllegalArgumentException("El número de plazas debe ser positivo.");
        }
        return inTransaction(entityManager -> {
            Cliente cliente = entityManager.find(Cliente.class, clienteId);
            if (cliente == null) {
                throw new IllegalArgumentException("No existe un cliente con ese ID.");
            }
            Actividad actividad = entityManager.find(Actividad.class, actividadId);
            if (actividad == null) {
                throw new IllegalArgumentException("No existe una actividad con ese ID.");
            }
            if (actividad.getPlazasDisponibles() < numeroPlazas) {
                throw new IllegalArgumentException("No hay plazas suficientes para esta reserva.");
            }
            actividad.reservar(numeroPlazas);
            Reserva reserva = new Reserva(LocalDateTime.now(), numeroPlazas, actividad, cliente);
            entityManager.persist(reserva);
            entityManager.flush();
            return new ReservaCreada(reserva.getId(), actividad.getPlazasDisponibles());
        });
    }

    public CancelacionRealizada cancelarReserva(long reservaId) {
        return inTransaction(entityManager -> {
            Reserva reserva = entityManager.find(Reserva.class, reservaId);
            if (reserva == null) {
                throw new IllegalArgumentException("No existe una reserva con ese ID.");
            }
            Actividad actividad = reserva.getActividad();
            int plazasDevueltas = reserva.getNumeroPlazas();
            actividad.liberar(plazasDevueltas);
            entityManager.remove(reserva);
            return new CancelacionRealizada(plazasDevueltas, actividad.getPlazasDisponibles());
        });
    }

    public List<Actividad> listarActividades() {
        return inEntityManager(entityManager -> entityManager.createQuery(
                        "select a from Actividad a join fetch a.monitor order by a.fechaHora",
                        Actividad.class)
                .getResultList());
    }

    private <T> T inTransaction(Function<EntityManager, T> operation) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        EntityTransaction transaction = entityManager.getTransaction();
        try {
            transaction.begin();
            T result = operation.apply(entityManager);
            transaction.commit();
            return result;
        } catch (RuntimeException exception) {
            if (transaction.isActive()) {
                try {
                    transaction.rollback();
                } catch (RuntimeException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
            }
            throw exception;
        } finally {
            entityManager.close();
        }
    }

    private <T> T inEntityManager(Function<EntityManager, T> operation) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        try {
            return operation.apply(entityManager);
        } finally {
            entityManager.close();
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " no puede estar vacío.");
        }
    }

    private static String validarEmail(String email) {
        requireText(email, "El email");
        String emailNormalizado = email.trim();
        if (!emailNormalizado.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalArgumentException("El formato del correo electrónico no es válido.");
        }
        return emailNormalizado;
    }

    public record ReservaCreada(Long id, int plazasDisponibles) {
    }

    public record CancelacionRealizada(int plazasDevueltas, int plazasDisponibles) {
    }
}
