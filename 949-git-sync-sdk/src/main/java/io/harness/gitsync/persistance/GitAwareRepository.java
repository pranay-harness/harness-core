package io.harness.gitsync.persistance;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import org.springframework.data.repository.Repository;

@OwnedBy(DX)
// Setting string as a convention
public interface GitAwareRepository<T, Y> extends Repository<T, String> {
  T save(T entity, Y yaml);
}
