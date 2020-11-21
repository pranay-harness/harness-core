package io.harness.gitsync.core.dao.api.repositories.YamlChangeSet;

import io.harness.gitsync.common.beans.YamlChangeSet;

import com.mongodb.client.result.UpdateResult;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

public interface YamlChangeSetRepositoryCustom {
  UpdateResult updateYamlChangeSetStatus(YamlChangeSet.Status status, String yamlChangeSetId);

  UpdateResult updateYamlChangeSetsStatus(
      YamlChangeSet.Status oldStatus, YamlChangeSet.Status newStatus, String accountId);

  UpdateResult updateYamlChangeSetsToNewStatusWithMessageCodeAndCreatedAtLessThan(
      YamlChangeSet.Status oldStatus, YamlChangeSet.Status newStatus, long cutOffCreatedAt, String message);

  UpdateResult update(Query query, Update update);

  Page<YamlChangeSet> findAll(Criteria criteria, Pageable pageable);

  <C> AggregationResults aggregate(Aggregation aggregation, Class<C> castClass);

  List<String> findDistinctAccountIdByStatus(YamlChangeSet.Status status);
}
