package io.harness.gitsync.persistance;

import com.mongodb.client.result.DeleteResult;
import org.springframework.data.repository.Repository;

public interface GitAwareRepository<T, Y, ID> extends Repository<T, ID> {
  T save(T entity, Y yaml);

  DeleteResult delete(T entity, Y yaml);
}
