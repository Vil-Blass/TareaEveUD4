# Activa: reservas de actividades guiadas

Aplicación Java de consola para gestionar monitores, actividades, clientes y reservas con JPA y Hibernate.

## Requisitos

- Java 25 (LTS)
- Maven 3.9 o posterior
- MySQL 8 y el esquema `dbactiva` preparado mediante el script `dbactiva.sql` proporcionado por la tarea

El proyecto utiliza Hibernate ORM 6.4.10.Final y el conector MySQL 8.4.0. No incluye ni genera el script de tablas. Hibernate está configurado con `hibernate.hbm2ddl.auto=validate`: comprueba el esquema existente y no lo crea ni lo modifica.

## Conexión

La conexión predeterminada es `jdbc:mysql://localhost:3306/dbactiva`, con el usuario `root`. Configura la contraseña mediante `DB_PASSWORD`; también se pueden cambiar la URL y el usuario. No guardes credenciales reales en archivos versionados:

- `DB_URL`: URL JDBC de la base de datos
- `DB_USER`: usuario
- `DB_PASSWORD`: contraseña

En PowerShell, establece la contraseña solo en la sesión actual antes de ejecutar la aplicación:

```powershell
$secret = Read-Host "Contraseña de MySQL" -AsSecureString
$pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secret)
try {
    $env:DB_PASSWORD = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    mvn exec:java
} finally {
    Remove-Item Env:DB_PASSWORD -ErrorAction SilentlyContinue
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
}
```

## Ejecución

Desde la carpeta del proyecto, ejecuta:

```text
mvn clean compile
mvn exec:java
```

El menú permanece activo hasta elegir la opción 10. Las opciones 8 y 9 permiten modificar y eliminar monitores, actividades y clientes. No se puede borrar un monitor con actividades ni una actividad o cliente con reservas; primero hay que reasignar o eliminar las referencias dependientes, cancelando las reservas desde la opción 6. Para buscar actividades se introduce la fecha como `dd/MM/yyyy`; para crear o modificar una actividad, como `dd/MM/yyyy HH:mm`.

## Ejecución con Docker

Requiere Docker Desktop con Docker Compose. La configuración levanta MySQL 8.4 y la aplicación Java 25. El volumen `mysql-data` conserva la base de datos entre reinicios. Hibernate solo valida el esquema (`validate`); no crea tablas.

1. Crea el fichero local de variables, que está excluido del control de versiones, y ábrelo para reemplazar la contraseña de ejemplo por la contraseña configurada en MySQL. Compose necesita este archivo para interpolar `MYSQL_ROOT_PASSWORD`:

   ```powershell
   Copy-Item .env.example .env
   notepad .env
   ```

   Deja la variable con este formato, usando la contraseña real de MySQL:

   ```text
   MYSQL_ROOT_PASSWORD=tu_contraseña_de_root
   ```

2. Inicia MySQL:

   ```powershell
   docker compose up -d db
   ```

3. Copia el `dbactiva.sql` entregado con la tarea a la carpeta del proyecto e impórtalo una sola vez en la base MySQL del contenedor:

   ```powershell
   docker compose cp .\dbactiva.sql db:/tmp/dbactiva.sql
   docker compose exec db sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" < /tmp/dbactiva.sql'
   ```

4. Construye la imagen y ejecuta la aplicación interactiva:

   ```powershell
   docker compose --profile app run --build --rm app
   ```

Elige la opción 10 para salir. Para volver a iniciar la aplicación, repite el paso 4; no vuelvas a importar el script. `docker compose down` detiene y elimina los contenedores, pero conserva el volumen de datos.

## Organización

- `domain`: entidades JPA `Monitor`, `Actividad`, `Cliente` y `Reserva`.
- `service`: reglas de negocio, consultas JPQL y transacciones.
- `config`: creación de la fábrica `EntityManagerFactory`.
- `main`: menú e interacción con consola.

La reserva y la cancelación usan un único `EntityManager` y una transacción por operación. Si se produce una excepción, se ejecuta rollback antes de cerrar el gestor.

## Uso de IA

Se utilizó un asistente de IA como apoyo para estructurar e implementar el proyecto a partir del enunciado. La persona que entrega el trabajo debe revisar el código, probarlo contra la base de datos proporcionada y explicar las decisiones tomadas; esta nota no sustituye la autoevaluación ni las evidencias requeridas por la tarea.
