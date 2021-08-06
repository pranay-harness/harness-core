package io.harness.repositories;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.timeout.TimeoutInstance;

import java.util.List;
import org.springframework.data.repository.CrudRepository;

@OwnedBy(CDC)
@HarnessRepo
public interface TimeoutInstanceRepository extends CrudRepository<TimeoutInstance, String> {
  void deleteByUuidIn(List<String> ids);
}
