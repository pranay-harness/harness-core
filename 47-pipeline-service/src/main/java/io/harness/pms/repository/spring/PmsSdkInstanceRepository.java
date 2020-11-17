package io.harness.pms.repository.spring;

import io.harness.annotation.HarnessRepo;
import io.harness.pms.beans.entities.PmsSdkInstance;
import io.harness.pms.repository.custom.PmsSdkInstanceRepositoryCustom;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

@HarnessRepo
public interface PmsSdkInstanceRepository
    extends CrudRepository<PmsSdkInstance, String>, PmsSdkInstanceRepositoryCustom {
  Optional<PmsSdkInstance> findByName(String name);
}
