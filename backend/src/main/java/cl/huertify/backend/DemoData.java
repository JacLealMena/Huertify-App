package cl.huertify.backend;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZoneId;

@Configuration
class DemoData {
    @Bean
    CommandLineRunner seedDemoData(UserRepository users, SectorRepository sectors, CropRepository crops, TaskRepository tasks) {
        return args -> {
            if (users.count() > 0) return;
            Models.User docente = users.save(user("Prof. Carla Muñoz", "docente@huertify.cl", "admin123", Models.Role.ADMIN, null));
            Models.User ana = users.save(user("Ana Torres", "ana@huertify.cl", "huerto123", Models.Role.STUDENT, "Taller 5° Básico"));
            Models.User benjamin = users.save(user("Benjamín Rojas", "benjamin@huertify.cl", "huerto123", Models.Role.STUDENT, "Taller 5° Básico"));
            Models.User sofia = users.save(user("Sofía Pérez", "sofia@huertify.cl", "huerto123", Models.Role.STUDENT, "Taller 7° Básico"));

            Models.Sector bancal = sector("S-01", "Bancal Norte", "Patio central, junto al invernadero", 12.0,
                    "Bancal elevado para hortalizas de hoja", Models.SectorStatus.EN_USO, docente.id);
            Models.Sector invernadero = sector("S-02", "Invernadero", "Costado del gimnasio", 20.5,
                    "Almácigos y cultivos de temporada", Models.SectorStatus.EN_USO, ana.id);
            Models.Sector compostera = sector("S-03", "Compostera", "Fondo del patio", 4.0,
                    "Zona de compostaje y lombricultura", Models.SectorStatus.EN_MANTENIMIENTO, sofia.id);
            Models.Sector sur = sector("S-04", "Bancal Sur", "Junto a la sala de 1° básico", 8.0,
                    "Bancal libre para la próxima siembra", Models.SectorStatus.DISPONIBLE, null);
            sectors.save(bancal); sectors.save(invernadero); sectors.save(compostera); sectors.save(sur);

            LocalDate today = LocalDate.now();
            Models.Crop lechuga = crop(bancal.id, "Lechuga", "Escarola", "Hortaliza de hoja de ciclo corto",
                    today.minusDays(20).toEpochDay(), today.minusDays(12).toEpochDay(), today.plusDays(5).toEpochDay(),
                    2, Models.CropStatus.EN_CRECIMIENTO, "Regar temprano");
            Models.Crop acelga = crop(bancal.id, "Acelga", "Penca blanca", "Hoja verde de cosecha escalonada",
                    today.minusDays(30).toEpochDay(), null, today.plusDays(30).toEpochDay(), 3,
                    Models.CropStatus.EN_CRECIMIENTO, "");
            Models.Crop tomate = crop(invernadero.id, "Tomate", "Cherry", "Fruto pequeño, requiere tutorado",
                    today.minusDays(10).toEpochDay(), null, today.plusDays(80).toEpochDay(), 1,
                    Models.CropStatus.SEMBRADO, "Requiere tutor");
            crops.save(lechuga); crops.save(acelga); crops.save(tomate);
            crops.save(crop(invernadero.id, "Albahaca", "Genovesa", "Aromática para el almácigo",
                    today.plusDays(7).toEpochDay(), null, today.plusDays(60).toEpochDay(), 2,
                    Models.CropStatus.PLANIFICADO, ""));

            task(tasks, "Regar lechugas", "Riego de las lechugas del bancal norte", "Regar a ras de suelo.\nNo mojar las hojas.",
                    Models.TaskType.RIEGO, bancal.id, lechuga.id, ana.id, docente.id, today.minusDays(3), Models.TaskStatus.VALIDADA);
            task(tasks, "Regar acelgas", "Riego de las acelgas del bancal norte", "Revisar que el agua no se acumule.",
                    Models.TaskType.RIEGO, bancal.id, acelga.id, benjamin.id, docente.id, today.minusDays(1), Models.TaskStatus.VALIDADA);
            task(tasks, "Regar tomates", "Riego diario de los tomates del invernadero", "Regar en la base de cada planta.",
                    Models.TaskType.RIEGO, invernadero.id, tomate.id, sofia.id, docente.id, today.minusDays(1), Models.TaskStatus.VALIDADA);
            task(tasks, "Regar el bancal norte", "Riego completo de lechugas y acelgas", "Usar regadera y evitar mojar las hojas.",
                    Models.TaskType.RIEGO, bancal.id, null, ana.id, docente.id, today, Models.TaskStatus.PENDIENTE);
            task(tasks, "Desmalezar invernadero", "Retirar malezas entre las hileras de tomate", "Sacar la maleza con raíz.",
                    Models.TaskType.DESMALEZADO, invernadero.id, tomate.id, benjamin.id, docente.id, today.minusDays(2), Models.TaskStatus.PENDIENTE);
            task(tasks, "Voltear compost", "Airear la pila de compost", "Medir la humedad del compost.",
                    Models.TaskType.MANTENCION, compostera.id, null, sofia.id, docente.id, today.plusDays(3), Models.TaskStatus.EN_DESARROLLO);
            task(tasks, "Sembrar rabanitos", "Siembra directa en el bancal norte", "Hacer surcos, sembrar y regar suavemente.",
                    Models.TaskType.SIEMBRA, bancal.id, null, ana.id, docente.id, today.plusDays(5), Models.TaskStatus.PENDIENTE);
        };
    }

    private static Models.User user(String name, String email, String password, Models.Role role, String workshop) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(password.getBytes(StandardCharsets.UTF_8));
            return new Models.User(name, email, java.util.HexFormat.of().formatHex(hash), role, workshop);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static Models.Sector sector(String code, String name, String location, double area, String description,
                                        Models.SectorStatus status, Long responsibleId) {
        Models.Sector sector = new Models.Sector();
        sector.code = code; sector.name = name; sector.location = location; sector.areaM2 = area;
        sector.description = description; sector.status = status; sector.responsibleId = responsibleId;
        sector.createdAt = System.currentTimeMillis(); sector.updatedAt = sector.createdAt;
        return sector;
    }

    private static Models.Crop crop(Long sectorId, String name, String variety, String description,
                                    Long planting, Long transplant, Long harvest, int wateringDays,
                                    Models.CropStatus status, String notes) {
        Models.Crop crop = new Models.Crop();
        crop.sectorId = sectorId; crop.name = name; crop.variety = variety; crop.description = description;
        crop.plantingDate = planting; crop.transplantDate = transplant; crop.harvestDate = harvest;
        crop.wateringFrequencyDays = wateringDays; crop.status = status; crop.notes = notes;
        return crop;
    }

    private static void task(TaskRepository repo, String title, String description, String instructions,
                             Models.TaskType type, Long sectorId, Long cropId, Long assigneeId, Long creatorId,
                             LocalDate scheduled, Models.TaskStatus status) {
        Models.Task task = new Models.Task();
        task.title = title; task.description = description; task.instructions = instructions; task.type = type;
        task.sectorId = sectorId; task.cropId = cropId; task.assigneeId = assigneeId; task.creatorId = creatorId;
        task.scheduledDate = scheduled.toEpochDay(); task.status = status;
        task.createdAt = System.currentTimeMillis(); task.updatedAt = task.createdAt;
        if (status == Models.TaskStatus.VALIDADA) {
            task.completedAt = scheduled.atTime(10, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            task.reviewerId = creatorId; task.reviewedAt = task.completedAt;
        }
        repo.save(task);
    }
}
