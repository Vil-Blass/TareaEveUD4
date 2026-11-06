package es.activa.main;

import es.activa.config.PersistenceConfig;
import es.activa.domain.Actividad;
import es.activa.service.ActivaService;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public class ActivaApplication {
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm").withResolverStyle(ResolverStyle.STRICT);
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private final Scanner scanner;
    private final ActivaService service;

    private ActivaApplication(ActivaService service, Scanner scanner) {
        this.service = service;
        this.scanner = scanner;
    }

    public static void main(String[] args) {
        try (EntityManagerFactory factory = PersistenceConfig.createEntityManagerFactory();
             Scanner scanner = new Scanner(System.in)) {
            new ActivaApplication(new ActivaService(factory), scanner).run();
        } catch (PersistenceException | IllegalStateException exception) {
            System.err.println("No se pudo iniciar o utilizar la base de datos: " + usefulMessage(exception));
        }
    }

    private void run() {
        boolean running = true;
        while (running) {
            printMenu();
            String option = readLine("Opción: ");
            if (option == null) {
                break;
            }
            try {
                switch (option.trim()) {
                    case "1" -> crearMonitor();
                    case "2" -> crearActividad();
                    case "3" -> buscarDisponibles();
                    case "4" -> crearCliente();
                    case "5" -> crearReserva();
                    case "6" -> cancelarReserva();
                    case "7" -> listarActividades();
                    case "8" -> modificarRegistro();
                    case "9" -> eliminarRegistro();
                    case "10" -> running = false;
                    default -> System.out.println("Opción incorrecta. Elige un número del 1 al 10.");
                }
            } catch (IllegalArgumentException exception) {
                System.out.println("Operación no realizada: " + usefulMessage(exception));
            } catch (RuntimeException exception) {
                System.out.println("Error al realizar la operación: " + usefulMessage(exception));
            }
        }
        System.out.println("Hasta pronto.");
    }

    private void crearMonitor() {
        String nombre = readRequiredText("Nombre: ");
        String especialidad = readRequiredText("Especialidad: ");
        Long id = service.crearMonitor(nombre, especialidad);
        System.out.println("Monitor creado. ID: " + id);
    }

    private void crearActividad() {
        String nombre = readRequiredText("Nombre: ");
        String localidad = readRequiredText("Localidad: ");
        LocalDateTime fechaHora = readDateTime("Fecha y hora (dd/MM/yyyy HH:mm): ");
        int plazas = readPositiveInt("Plazas totales: ");
        long monitorId = readPositiveLong("ID del monitor: ");
        Long id = service.crearActividad(nombre, localidad, fechaHora, plazas, monitorId);
        System.out.println("Actividad creada. ID: " + id);
    }

    private void buscarDisponibles() {
        String localidad = readRequiredText("Localidad: ");
        LocalDate fecha = readDate("Día (dd/MM/yyyy): ");
        int plazas = readPositiveInt("Plazas necesarias: ");
        var actividades = service.buscarDisponibles(localidad, fecha, plazas);
        if (actividades.isEmpty()) {
            System.out.println("No hay actividades disponibles que coincidan.");
            return;
        }
        actividades.forEach(this::printActividadDisponible);
    }

    private void crearCliente() {
        String nombre = readRequiredText("Nombre: ");
        String email = readRequiredText("Email: ");
        Long id = service.crearCliente(nombre, email);
        System.out.println("Cliente creado. ID: " + id);
    }

    private void crearReserva() {
        long clienteId = readPositiveLong("ID del cliente: ");
        long actividadId = readPositiveLong("ID de la actividad: ");
        int plazas = readPositiveInt("Número de plazas: ");
        ActivaService.ReservaCreada reserva = service.crearReserva(clienteId, actividadId, plazas);
        System.out.println("Reserva confirmada. ID: " + reserva.id()
                + ". Plazas disponibles: " + reserva.plazasDisponibles());
    }

    private void cancelarReserva() {
        long reservaId = readPositiveLong("ID de la reserva: ");
        ActivaService.CancelacionRealizada cancelacion = service.cancelarReserva(reservaId);
        System.out.println("Reserva cancelada. Plazas recuperadas: " + cancelacion.plazasDevueltas()
                + ". Plazas disponibles: " + cancelacion.plazasDisponibles());
    }

    private void listarActividades() {
        var actividades = service.listarActividades();
        if (actividades.isEmpty()) {
            System.out.println("No hay actividades registradas.");
            return;
        }
        actividades.forEach(actividad -> System.out.printf(
                "ID: %d | %s | %s | %s | Monitor: %s | Plazas totales: %d | Disponibles: %d%n",
                actividad.getId(), actividad.getNombre(), actividad.getLocalidad(),
                actividad.getFechaHora().format(DATE_TIME_FORMAT), actividad.getMonitor().getNombre(),
                actividad.getPlazasTotales(), actividad.getPlazasDisponibles()));
    }

    private void modificarRegistro() {
        String tipo = readRecordType("modificar");
        long id = readPositiveLong("ID: ");
        switch (tipo) {
            case "1" -> {
                service.modificarMonitor(id, readRequiredText("Nombre: "),
                        readRequiredText("Especialidad: "));
                System.out.println("Monitor modificado.");
            }
            case "2" -> {
                String nombre = readRequiredText("Nombre: ");
                String localidad = readRequiredText("Localidad: ");
                LocalDateTime fechaHora = readDateTime("Fecha y hora (dd/MM/yyyy HH:mm): ");
                int plazas = readPositiveInt("Plazas totales: ");
                long monitorId = readPositiveLong("ID del monitor: ");
                service.modificarActividad(id, nombre, localidad, fechaHora, plazas, monitorId);
                System.out.println("Actividad modificada.");
            }
            case "3" -> {
                service.modificarCliente(id, readRequiredText("Nombre: "), readRequiredText("Email: "));
                System.out.println("Cliente modificado.");
            }
            default -> throw new IllegalStateException("Tipo de registro no reconocido.");
        }
    }

    private void eliminarRegistro() {
        String tipo = readRecordType("eliminar");
        long id = readPositiveLong("ID: ");
        if (!confirmar("¿Confirmas la eliminación? (S/N): ")) {
            System.out.println("Eliminación cancelada.");
            return;
        }
        switch (tipo) {
            case "1" -> {
                service.eliminarMonitor(id);
                System.out.println("Monitor eliminado.");
            }
            case "2" -> {
                service.eliminarActividad(id);
                System.out.println("Actividad eliminada.");
            }
            case "3" -> {
                service.eliminarCliente(id);
                System.out.println("Cliente eliminado.");
            }
            default -> throw new IllegalStateException("Tipo de registro no reconocido.");
        }
    }

    private String readRecordType(String action) {
        while (true) {
            System.out.println("Selecciona el tipo de registro que quieres " + action + ":");
            System.out.println("1. Monitor");
            System.out.println("2. Actividad");
            System.out.println("3. Cliente");
            String type = readLine("Opción: ");
            if (type == null) {
                throw new IllegalArgumentException("Entrada finalizada.");
            }
            if (type.equals("1") || type.equals("2") || type.equals("3")) {
                return type;
            }
            System.out.println("Opción incorrecta. Elige 1, 2 o 3.");
        }
    }

    private boolean confirmar(String prompt) {
        String respuesta = readLine(prompt);
        return respuesta != null && respuesta.trim().equalsIgnoreCase("s");
    }

    private void printActividadDisponible(Actividad actividad) {
        System.out.printf("ID: %d | %s | %s | Monitor: %s | Plazas disponibles: %d%n",
                actividad.getId(), actividad.getNombre(), actividad.getFechaHora().format(DATE_TIME_FORMAT),
                actividad.getMonitor().getNombre(), actividad.getPlazasDisponibles());
    }

    private String readRequiredText(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (value == null) {
                throw new IllegalArgumentException("Entrada finalizada.");
            }
            if (!value.isBlank()) {
                return value.trim();
            }
            System.out.println("Este campo no puede estar vacío.");
        }
    }

    private int readPositiveInt(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (value == null) {
                throw new IllegalArgumentException("Entrada finalizada.");
            }
            try {
                int number = Integer.parseInt(value);
                if (number > 0) {
                    return number;
                }
            } catch (NumberFormatException ignored) {
                // Show the same clear validation message for non-numeric and non-positive values.
            }
            System.out.println("Introduce un número entero positivo.");
        }
    }

    private long readPositiveLong(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (value == null) {
                throw new IllegalArgumentException("Entrada finalizada.");
            }
            try {
                long number = Long.parseLong(value);
                if (number > 0) {
                    return number;
                }
            } catch (NumberFormatException ignored) {
                // Show the same clear validation message for non-numeric and non-positive values.
            }
            System.out.println("Introduce un identificador numérico positivo.");
        }
    }

    private LocalDateTime readDateTime(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (value == null) {
                throw new IllegalArgumentException("Entrada finalizada.");
            }
            try {
                return LocalDateTime.parse(value, DATE_TIME_FORMAT);
            } catch (DateTimeParseException exception) {
                System.out.println("Fecha u hora no válida. Usa el formato dd/MM/yyyy HH:mm.");
            }
        }
    }

    private LocalDate readDate(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (value == null) {
                throw new IllegalArgumentException("Entrada finalizada.");
            }
            try {
                return LocalDate.parse(value, DATE_FORMAT);
            } catch (DateTimeParseException exception) {
                System.out.println("Fecha no válida. Usa el formato dd/MM/yyyy.");
            }
        }
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        return scanner.hasNextLine() ? scanner.nextLine() : null;
    }

    private static String usefulMessage(Throwable exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    private static void printMenu() {
        System.out.println("""
                === ACTIVA · Gestión de actividades ===
                1. Crear monitor
                2. Crear actividad
                3. Buscar actividades disponibles
                4. Crear cliente
                5. Crear reserva
                6. Cancelar reserva
                7. Listar actividades
                8. Modificar monitor, actividad o cliente
                9. Eliminar monitor, actividad o cliente
                10. Salir
                """);
    }
}
