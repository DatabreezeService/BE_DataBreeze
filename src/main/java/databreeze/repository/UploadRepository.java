package databreeze.repository;

import databreeze.entity.Upload;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface UploadRepository extends JpaRepository<Upload, UUID> {}
