package io.harness.repositories.fullSync;

import io.harness.gitsync.core.fullsync.entity.GitFullSyncJob;

import com.mongodb.client.result.UpdateResult;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

public interface FullSyncJobRepositoryCustom {
  UpdateResult update(Criteria criteria, Update update);

  GitFullSyncJob find(Criteria criteria);
}
