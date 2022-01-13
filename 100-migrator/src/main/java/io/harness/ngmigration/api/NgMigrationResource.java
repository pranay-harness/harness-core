/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ngmigration.api;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngmigration.beans.MigrationInputDTO;
import io.harness.ngmigration.service.DiscoveryService;
import io.harness.rest.RestResponse;

import software.wings.ngmigration.DiscoveryResult;
import software.wings.ngmigration.NGMigrationEntityType;
import software.wings.ngmigration.NGYamlFile;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.Scope;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDC)
@Slf4j
@Api("ng-migration")
@Path("/ng-migration")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Scope(ResourceType.APPLICATION)
public class NgMigrationResource {
  @Inject DiscoveryService discoveryService;

  @GET
  @Path("/discover")
  @Timed
  @ExceptionMetered
  public RestResponse<DiscoveryResult> discoverEntities(@QueryParam("entityId") String entityId,
      @QueryParam("appId") String appId, @QueryParam("accountId") String accountId,
      @QueryParam("entityType") NGMigrationEntityType entityType) {
    return new RestResponse<>(discoveryService.discover(accountId, appId, entityId, entityType));
  }

  @POST
  @Path("/files")
  @Timed
  @ExceptionMetered
  public RestResponse<List<NGYamlFile>> getMigratedFiles(@QueryParam("entityId") String entityId,
      @QueryParam("appId") String appId, @QueryParam("accountId") String accountId,
      @QueryParam("entityType") NGMigrationEntityType entityType, MigrationInputDTO inputDTO) {
    DiscoveryResult result = discoveryService.discover(accountId, appId, entityId, entityType);
    return new RestResponse<>(discoveryService.migrateEntity(inputDTO, result));
  }
}
