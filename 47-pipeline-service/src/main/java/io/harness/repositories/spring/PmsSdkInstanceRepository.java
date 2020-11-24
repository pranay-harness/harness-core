package io.harness.repositories.spring;

import io.harness.annotation.HarnessRepo;
import io.harness.pms.beans.entities.PmsSdkInstance;
import io.harness.repositories.custom.PmsSdkInstanceRepositoryCustom;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

@HarnessRepo
public interface PmsSdkInstanceRepository
    extends CrudRepository<PmsSdkInstance, String>, PmsSdkInstanceRepositoryCustom {
  Optional<PmsSdkInstance> findByName(String name);
}
