package io.harness.gitsync.persistance;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.git.model.ChangeType;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

@OwnedBy(DX)
// Setting string as a convention
@NoRepositoryBean
//@Repository
public interface GitAwareRepository<T, Y> extends Repository<T, String> {
  T save(T entity, Y yaml);

  T save(T entity, Y yaml, ChangeType changeType);

  T save(T entity, ChangeType changeType);
}
