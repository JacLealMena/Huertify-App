package cl.huertify.backend;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

public final class Models {
    private Models() {}

    public enum Role { ADMIN, STUDENT }
    public enum SectorStatus { DISPONIBLE, EN_USO, EN_MANTENIMIENTO, FUERA_DE_USO }
    public enum CropStatus { PLANIFICADO, SEMBRADO, EN_CRECIMIENTO, LISTO_COSECHA, COSECHADO, CON_PROBLEMAS, FINALIZADO }
    public enum TaskType { PREPARACION, SIEMBRA, TRASPLANTE, RIEGO, LIMPIEZA, DESMALEZADO, OBSERVACION, MANTENCION, REVISION_SUELO, REGISTRO_CRECIMIENTO, COSECHA, OTRO }
    public enum TaskStatus { PENDIENTE, EN_DESARROLLO, PENDIENTE_VALIDACION, VALIDADA, RECHAZADA }

    @Entity
    @Table(name = "users")
    public static class User {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
        public String name;
        public String email;
        public String passwordHash;
        @Enumerated(EnumType.STRING) public Role role;
        public String workshop;
        public User() {}
        User(String name, String email, String passwordHash, Role role, String workshop) {
            this.name = name; this.email = email; this.passwordHash = passwordHash; this.role = role; this.workshop = workshop;
        }
    }

    @Entity
    @Table(name = "sectors")
    public static class Sector {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
        public String code;
        public String name;
        public String location;
        public Double areaM2;
        public String description;
        @Enumerated(EnumType.STRING) public SectorStatus status;
        public Long responsibleId;
        public Long createdAt;
        public Long updatedAt;
        public Sector() {}
    }

    @Entity
    @Table(name = "crops")
    public static class Crop {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
        public Long sectorId;
        public String name;
        public String variety;
        public String description;
        public Long plantingDate;
        public Long transplantDate;
        public Long harvestDate;
        public Integer wateringFrequencyDays;
        @Enumerated(EnumType.STRING) public CropStatus status;
        public String photoUri;
        public String notes;
        public Crop() {}
    }

    @Entity
    @Table(name = "tasks")
    public static class Task {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
        public String title;
        public String description;
        public String instructions;
        @Enumerated(EnumType.STRING) public TaskType type;
        public Long sectorId;
        public Long cropId;
        public Long assigneeId;
        public String groupName;
        public Long creatorId;
        public Long scheduledDate;
        @Enumerated(EnumType.STRING) public TaskStatus status;
        public String studentComment;
        public String evidenceUri;
        public Long completedAt;
        public String rejectionReason;
        public String reviewComment;
        public Long reviewerId;
        public Long reviewedAt;
        public Long createdAt;
        public Long updatedAt;
        public Task() {}
    }

    public record UserResponse(Long id, String name, String email, Role role, String workshop) {
        static UserResponse from(User user) {
            return new UserResponse(user.id, user.name, user.email, user.role, user.workshop);
        }
    }
}
