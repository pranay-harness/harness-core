package io.harness.ngmigration.api;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngmigration.DiscoveryService;
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
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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
  @Path("{pipelineId}/discover")
  @Timed
  @ExceptionMetered
  public RestResponse<DiscoveryResult> discoverEntities(@PathParam("pipelineId") String pipelineId,
      @QueryParam("appId") String appId, @QueryParam("accountId") String accountId) {
    return new RestResponse<>(discoveryService.discover(accountId, appId, pipelineId, NGMigrationEntityType.PIPELINE));
  }

  @GET
  @Path("{pipelineId}/files")
  @Timed
  @ExceptionMetered
  public RestResponse<List<NGYamlFile>> getMigratedFiles(@PathParam("pipelineId") String pipelineId,
      @QueryParam("appId") String appId, @QueryParam("accountId") String accountId) {
    DiscoveryResult result = discoveryService.discover(accountId, appId, pipelineId, NGMigrationEntityType.PIPELINE);
    return new RestResponse<>(discoveryService.migratePipeline(result.getEntities(), result.getLinks(), pipelineId));
  }
}
