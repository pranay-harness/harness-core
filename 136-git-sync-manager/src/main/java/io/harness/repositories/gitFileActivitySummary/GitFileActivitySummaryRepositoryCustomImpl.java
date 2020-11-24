package io.harness.repositories.gitFileActivitySummary;

import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.gitsync.gitfileactivity.beans.GitFileActivitySummary;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivitySummary.GitFileActivitySummaryKeys;

import com.google.inject.Inject;
import com.mongodb.client.result.DeleteResult;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@AllArgsConstructor(onConstructor = @__({ @Inject }))

public class GitFileActivitySummaryRepositoryCustomImpl implements GitFileActivitySummaryRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public DeleteResult deleteByIds(List<String> ids) {
    Query query = query(Criteria.where(GitFileActivitySummaryKeys.uuid).in(ids));
    return mongoTemplate.remove(query, GitFileActivitySummary.class);
  }
}
