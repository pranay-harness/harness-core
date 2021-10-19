package io.harness.repositories.gitSyncError;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.gitsyncerror.beans.GitSyncError;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(PL)
public interface GitSyncErrorRepositoryCustom {
  <C> AggregationResults aggregate(Aggregation aggregation, Class<C> castClass);

  DeleteResult deleteByIds(List<String> ids);

  UpdateResult upsertGitError(Criteria criteria, Update update);

  Page<GitSyncError> findAll(Criteria criteria, Pageable pageable);

  GitSyncError find(Criteria criteria);

  long count(Criteria criteria);
}
