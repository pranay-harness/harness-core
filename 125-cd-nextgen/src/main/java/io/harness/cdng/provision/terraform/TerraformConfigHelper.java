/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.provision.terraform;

import io.harness.cdng.provision.terraform.TerraformConfig.TerraformConfigKeys;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import io.harness.pms.contracts.ambiance.Ambiance;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.mongodb.morphia.query.Sort;

@Singleton
public class TerraformConfigHelper {
  @Inject private HPersistence persistence;

  public HIterator<TerraformConfig> getIterator(Ambiance ambiance, String entityId) {
    return new HIterator(persistence.createQuery(TerraformConfig.class)
                             .filter(TerraformConfigKeys.accountId, AmbianceHelper.getAccountId(ambiance))
                             .filter(TerraformConfigKeys.orgId, AmbianceHelper.getOrgIdentifier(ambiance))
                             .filter(TerraformConfigKeys.projectId, AmbianceHelper.getProjectIdentifier(ambiance))
                             .filter(TerraformConfigKeys.entityId, entityId)
                             .order(Sort.descending(TerraformConfigKeys.createdAt))
                             .fetch());
  }
}
