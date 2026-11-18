package cl.huertify.backend;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

interface UserRepository extends JpaRepository<Models.User, Long> {
    Models.User findByEmailIgnoreCase(String email);
}

interface SectorRepository extends JpaRepository<Models.Sector, Long> {}

interface CropRepository extends JpaRepository<Models.Crop, Long> {
    List<Models.Crop> findBySectorId(Long sectorId);
}

interface TaskRepository extends JpaRepository<Models.Task, Long> {
    List<Models.Task> findByAssigneeId(Long assigneeId);
    List<Models.Task> findBySectorId(Long sectorId);
    List<Models.Task> findByCropId(Long cropId);
}
