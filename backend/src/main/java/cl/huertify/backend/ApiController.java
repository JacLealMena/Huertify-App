package cl.huertify.backend;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ApiController {
    private final UserRepository users;
    private final SectorRepository sectors;
    private final CropRepository crops;
    private final TaskRepository tasks;

    public ApiController(UserRepository users, SectorRepository sectors, CropRepository crops, TaskRepository tasks) {
        this.users = users; this.sectors = sectors; this.crops = crops; this.tasks = tasks;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<Models.UserResponse> login(@RequestBody LoginRequest request) {
        Models.User user = request.email() == null ? null : users.findByEmailIgnoreCase(request.email().trim());
        if (user == null || request.password() == null || !constantTimeEquals(user.passwordHash, sha256(request.password()))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(Models.UserResponse.from(user));
    }

    @GetMapping("/users")
    public List<Models.UserResponse> getUsers() {
        return users.findAll().stream().map(Models.UserResponse::from).toList();
    }

    @GetMapping("/sectors")
    public List<Models.Sector> getSectors() { return sectors.findAll(); }

    @PostMapping("/sectors")
    public Models.Sector createSector(@RequestBody Models.Sector sector) {
        long now = System.currentTimeMillis();
        sector.id = null; sector.createdAt = now; sector.updatedAt = now;
        return sectors.save(sector);
    }

    @PutMapping("/sectors/{id}")
    public Models.Sector updateSector(@PathVariable Long id, @RequestBody Models.Sector input) {
        Models.Sector existing = require(sectors.findById(id).orElse(null));
        input.id = id;
        input.createdAt = existing.createdAt;
        input.updatedAt = System.currentTimeMillis();
        return sectors.save(input);
    }

    @DeleteMapping("/sectors/{id}")
    public ResponseEntity<Void> deleteSector(@PathVariable Long id) {
        require(sectors.findById(id).orElse(null));
        for (Models.Task task : tasks.findBySectorId(id)) {
            task.sectorId = null;
            tasks.save(task);
        }
        for (Models.Crop crop : crops.findBySectorId(id)) {
            for (Models.Task task : tasks.findByCropId(crop.id)) {
                task.cropId = null;
                tasks.save(task);
            }
            crops.delete(crop);
        }
        sectors.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/crops")
    public List<Models.Crop> getCrops(@RequestParam(required = false) Long sectorId) {
        return sectorId == null ? crops.findAll() : crops.findBySectorId(sectorId);
    }

    @PostMapping("/crops")
    public Models.Crop createCrop(@RequestBody Models.Crop crop) { crop.id = null; return crops.save(crop); }

    @PutMapping("/crops/{id}")
    public Models.Crop updateCrop(@PathVariable Long id, @RequestBody Models.Crop crop) {
        require(crops.findById(id).orElse(null)); crop.id = id; return crops.save(crop);
    }

    @DeleteMapping("/crops/{id}")
    public ResponseEntity<Void> deleteCrop(@PathVariable Long id) {
        require(crops.findById(id).orElse(null));
        for (Models.Task task : tasks.findByCropId(id)) {
            task.cropId = null;
            tasks.save(task);
        }
        crops.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tasks")
    public List<Models.Task> getTasks(@RequestParam(required = false) Long assigneeId) {
        return assigneeId == null ? tasks.findAll() : tasks.findByAssigneeId(assigneeId);
    }

    @PostMapping("/tasks")
    public Models.Task createTask(@RequestBody Models.Task task) {
        long now = System.currentTimeMillis();
        task.id = null;
        if (task.createdAt == null) task.createdAt = now;
        if (task.updatedAt == null) task.updatedAt = now;
        return tasks.save(task);
    }

    @PutMapping("/tasks/{id}")
    public Models.Task updateTask(@PathVariable Long id, @RequestBody Models.Task task) {
        Models.Task existing = require(tasks.findById(id).orElse(null));
        task.id = id;
        if (task.createdAt == null) task.createdAt = existing.createdAt;
        task.updatedAt = System.currentTimeMillis();
        return tasks.save(task);
    }

    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        require(tasks.findById(id).orElse(null)); tasks.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Void> notFound() { return ResponseEntity.notFound().build(); }

    private static <T> T require(T value) {
        if (value == null) throw new EntityNotFoundException();
        return value;
    }

    private static String sha256(String password) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(password.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        return expected != null && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    public record LoginRequest(String email, String password) {}
}
