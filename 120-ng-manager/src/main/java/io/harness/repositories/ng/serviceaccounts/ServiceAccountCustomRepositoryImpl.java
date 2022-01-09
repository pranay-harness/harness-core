/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.repositories.ng.serviceaccounts;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.serviceaccounts.entities.ServiceAccount;

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

@OwnedBy(PL)
@AllArgsConstructor(access = AccessLevel.PROTECTED, onConstructor = @__({ @Inject }))
public class ServiceAccountCustomRepositoryImpl implements ServiceAccountCustomRepository {
  private final MongoTemplate mongoTemplate;

  @Override
  public Page<ServiceAccount> findAll(Criteria criteria, Pageable pageable) {
    Query query = new Query(criteria).with(pageable);
    List<ServiceAccount> serviceAccounts = mongoTemplate.find(query, ServiceAccount.class);
    return PageableExecutionUtils.getPage(
        serviceAccounts, pageable, () -> mongoTemplate.count(Query.of(query).limit(-1).skip(-1), ServiceAccount.class));
  }
}
