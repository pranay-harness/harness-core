package io.harness.repositories.fullSync;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.core.beans.GitFullSyncEntityInfo;

import com.mongodb.client.result.UpdateResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(DX)
public interface GitFullSyncEntityRepositoryCustom {
  UpdateResult update(Criteria criteria, Update update);

  Page<GitFullSyncEntityInfo> findAll(Criteria criteria, Pageable pageable);

  long count(Criteria criteria);
}
