package io.harness.repositories.sdk;

import io.harness.annotation.HarnessRepo;
import io.harness.pms.sdk.PmsSdkInstance;

import java.util.Optional;
import org.springframework.data.repository.CrudRepository;

@HarnessRepo
public interface PmsSdkInstanceRepository
    extends CrudRepository<PmsSdkInstance, String>, PmsSdkInstanceRepositoryCustom {
  Optional<PmsSdkInstance> findByName(String name);
}
