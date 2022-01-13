/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.repositories;

import static io.harness.exception.WingsException.USER;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.GitOpsProviderType;
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
    final Criteria criteria =
        FilterUtils.getCriteria(accountIdentifier, orgIdentifier, projectIdentifier, providerIdentifier);
    final Query query = new Query(criteria);
    return mongoTemplate.findOne(query, GitOpsProvider.class);
  }

  @Override
  public boolean delete(
      String providerIdentifier, String projectIdentifier, String orgIdentifier, String accountIdentifier) {
    final Criteria criteria =
        FilterUtils.getCriteria(accountIdentifier, orgIdentifier, projectIdentifier, providerIdentifier);
    final Query query = new Query(criteria);
    return mongoTemplate.remove(query, GitOpsProvider.class).wasAcknowledged();
  }

  @Override
  public Page<GitOpsProvider> findAll(Pageable pageable, String projectIdentifier, String orgIdentifier,
      String accountIdentifier, String searchTerm, GitOpsProviderType type) {
    final Criteria criteria = FilterUtils.getCriteria(accountIdentifier, orgIdentifier, projectIdentifier);
    FilterUtils.applySearchFilter(criteria, searchTerm);
    FilterUtils.applySearchFilterForType(criteria, type);
    final Query query = new Query(criteria).with(pageable);
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
  public GitOpsProvider update(String accountIdentifier, GitOpsProvider gitopsProvider) {
    final Criteria criteria = FilterUtils.getCriteria(accountIdentifier, gitopsProvider.getOrgIdentifier(),
        gitopsProvider.getProjectIdentifier(), gitopsProvider.getIdentifier());
    Query query = new Query(criteria);
    return mongoTemplate.findAndReplace(query, gitopsProvider, FindAndReplaceOptions.empty().returnNew());
  }
}
