package io.harness.repositories.gitFileLocation;

import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.gitsync.common.beans.GitFileLocation;

import com.google.inject.Inject;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.repository.support.PageableExecutionUtils;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class GitFileLocationRepositoryCustomImpl implements GitFileLocationRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public List<String> getDistinctEntityName(Criteria criteria, String field) {
    Query query = query(criteria);
    return mongoTemplate.findDistinct(query, field, GitFileLocation.class, String.class);
  }

  @Override
  public Page<GitFileLocation> getGitFileLocation(Criteria criteria, Pageable pageable) {
    Query query = query(criteria).with(pageable);
    final List<GitFileLocation> gitFileLocationList = mongoTemplate.find(query, GitFileLocation.class);
    return PageableExecutionUtils.getPage(gitFileLocationList, pageable,
        () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), GitFileLocation.class));
  }
}
