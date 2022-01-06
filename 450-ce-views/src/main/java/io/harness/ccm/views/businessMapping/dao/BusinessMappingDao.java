/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.views.businessMapping.dao;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.persistence.HQuery.excludeValidate;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.views.businessMapping.entities.BusinessMapping;
import io.harness.ccm.views.businessMapping.entities.BusinessMapping.BusinessMappingKeys;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Slf4j
@Singleton
@OwnedBy(CE)
public class BusinessMappingDao {
  @Inject private HPersistence hPersistence;

  public boolean save(BusinessMapping businessMapping) {
    return hPersistence.save(businessMapping) != null;
  }

  public BusinessMapping update(BusinessMapping businessMapping) {
    Query query = hPersistence.createQuery(BusinessMapping.class)
                      .field(BusinessMappingKeys.accountId)
                      .equal(businessMapping.getAccountId())
                      .field(BusinessMappingKeys.uuid)
                      .equal(businessMapping.getUuid());

    UpdateOperations<BusinessMapping> updateOperations =
        hPersistence.createUpdateOperations(BusinessMapping.class)
            .set(BusinessMappingKeys.name, businessMapping.getName())
            .set(BusinessMappingKeys.accountId, businessMapping.getAccountId())
            .set(BusinessMappingKeys.costTargets, businessMapping.getCostTargets())
            .set(BusinessMappingKeys.sharedCosts, businessMapping.getSharedCosts());

    hPersistence.update(query, updateOperations);
    return (BusinessMapping) query.asList().get(0);
  }

  public boolean delete(String uuid, String accountId) {
    Query query = hPersistence.createQuery(BusinessMapping.class)
                      .field(BusinessMappingKeys.accountId)
                      .equal(accountId)
                      .field(BusinessMappingKeys.uuid)
                      .equal(uuid);

    return hPersistence.delete(query);
  }

  public BusinessMapping get(String uuid, String accountId) {
    return hPersistence.createQuery(BusinessMapping.class, excludeValidate)
        .filter(BusinessMappingKeys.uuid, uuid)
        .filter(BusinessMappingKeys.accountId, accountId)
        .get();
  }

  public BusinessMapping get(String uuid) {
    return hPersistence.createQuery(BusinessMapping.class, excludeValidate)
        .filter(BusinessMappingKeys.uuid, uuid)
        .get();
  }

  public List<BusinessMapping> findByAccountId(String accountId) {
    return hPersistence.createQuery(BusinessMapping.class).filter(BusinessMappingKeys.accountId, accountId).asList();
  }
}
