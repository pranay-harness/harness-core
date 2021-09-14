/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.migrations.all;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.PageRequest.UNLIMITED;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.beans.InfrastructureMappingBlueprint.NodeFilteringType.AWS_INSTANCE_FILTER;

import io.harness.beans.PageRequest;
import io.harness.exception.ExceptionUtils;
import io.harness.migrations.Migration;

import software.wings.beans.InfrastructureMappingBlueprint;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InfraProvisionerFilteringTypeMigration implements Migration {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public void migrate() {
    // We could implement a more intelligent logic like batching etc.
    // But it is probably not needed right now, as we have very few provisioners created
    log.info("Getting all provisioners to migrate blue prints");
    PageRequest<InfrastructureProvisioner> request = aPageRequest().withLimit(UNLIMITED).build();
    List<InfrastructureProvisioner> provisioners =
        wingsPersistence.query(InfrastructureProvisioner.class, request).getResponse();
    if (isNotEmpty(provisioners)) {
      provisioners.forEach(provisioner -> {
        try {
          List<InfrastructureMappingBlueprint> blueprints = provisioner.getMappingBlueprints();
          if (isNotEmpty(blueprints)) {
            blueprints.forEach(blueprint -> { blueprint.setNodeFilteringType(AWS_INSTANCE_FILTER); });
            wingsPersistence.saveAndGet(InfrastructureProvisioner.class, provisioner);
          }
        } catch (Exception ex) {
          log.error(ExceptionUtils.getMessage(ex), ex);
        }
      });
    }
    log.info("Completed update of blue prints");
  }
}
