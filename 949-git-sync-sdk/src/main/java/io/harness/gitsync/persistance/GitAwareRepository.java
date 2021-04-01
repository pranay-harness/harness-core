package io.harness.gitsync.persistance;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import com.mongodb.client.result.DeleteResult;
import org.springframework.data.repository.Repository;

@OwnedBy(DX)
public interface GitAwareRepository<T, Y, ID> extends Repository<T, ID> {
  T save(T entity, Y yaml);

  DeleteResult delete(T entity, Y yaml);
}
