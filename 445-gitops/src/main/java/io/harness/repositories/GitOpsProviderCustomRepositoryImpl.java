package io.harness.repositories;

import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.gitopsprovider.entity.GitOpsProvider;

import com.google.inject.Inject;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.repository.support.PageableExecutionUtils;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@OwnedBy(HarnessTeam.GITOPS)
public class GitOpsProviderCustomRepositoryImpl implements GitOpsProviderCustomRepository {
  private MongoTemplate mongoTemplate;

  @Override
  public GitOpsProvider get(
      String providerIdentifier, String projectIdentifier, String orgIdentifier, String accountIdentifier) {
    Criteria criteria = new Criteria();
    criteria.and("accountIdentifier").is(accountIdentifier);
    criteria.and("orgIdentifier").is(orgIdentifier);
    criteria.and("projectIdentifier").is(projectIdentifier);
    criteria.and("identifier").is(providerIdentifier);
    Query query = new Query(criteria);
    return mongoTemplate.findOne(query, GitOpsProvider.class);
  }

  @Override
  public boolean delete(
      String providerIdentifier, String projectIdentifier, String orgIdentifier, String accountIdentifier) {
    Criteria criteria = new Criteria();
    criteria.and("accountIdentifier").is(accountIdentifier);
    criteria.and("orgIdentifier").is(orgIdentifier);
    criteria.and("projectIdentifier").is(projectIdentifier);
    criteria.and("identifier").is(providerIdentifier);
    Query query = new Query(criteria);
    return mongoTemplate.remove(query, GitOpsProvider.class).wasAcknowledged();
  }

  @Override
  public Page<GitOpsProvider> findAll(
      Criteria criteria, Pageable pageable, String projectIdentifier, String orgIdentifier, String accountIdentifier) {
    Query query = new Query(criteria).with(pageable);
    List<GitOpsProvider> gitOpsProviders = mongoTemplate.find(query, GitOpsProvider.class);
    return PageableExecutionUtils.getPage(
        gitOpsProviders, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), GitOpsProvider.class));
  }

  @Override
  public GitOpsProvider save(GitOpsProvider gitopsProvider) {
    try {
      return mongoTemplate.save(gitopsProvider);
    } catch (DuplicateKeyException de) {
      throw new InvalidRequestException("Creation of duplicate provider not allowed", USER);
    }
  }

  @Override
  public GitOpsProvider update(GitOpsProvider gitopsProvider) {
    Criteria criteria = new Criteria();
    criteria.and("accountIdentifier").is(gitopsProvider.getAccountIdentifier());
    criteria.and("orgIdentifier").is(gitopsProvider.getOrgIdentifier());
    criteria.and("projectIdentifier").is(gitopsProvider.getProjectIdentifier());
    criteria.and("identifier").is(gitopsProvider.getIdentifier());
    Query query = new Query(criteria);
    return mongoTemplate.findAndReplace(query, gitopsProvider, FindAndReplaceOptions.empty().returnNew());
  }
}
