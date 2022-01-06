/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static software.wings.security.PermissionAttribute.ResourceType.SETTING;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import io.harness.annotations.dev.OwnedBy;
import io.harness.rest.RestResponse;

import software.wings.beans.NameValuePair;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.AwsHelperResourceService;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

/**
 * Created by sgurubelli on 7/16/17.
 */
@Api("awshelper")
@Path("/awshelper")
@Consumes(APPLICATION_JSON)
@Produces(APPLICATION_JSON)
@Scope(SETTING)
@OwnedBy(CDP)
public class AwsHelperResource {
  @Inject private AwsHelperResourceService awsHelperResourceService;

  /**
   * List.
   *
   * @param accountId                the account id
   * @return the rest response
   */
  @GET
  @Path("/regions")
  @Timed
  @ExceptionMetered
  @Deprecated
  public RestResponse<Map<String, String>> list(@QueryParam("accountId") String accountId) {
    return new RestResponse(awsHelperResourceService.getRegions());
  }

  /**
   * List.
   *
   * @param accountId                the account id
   * @return the rest response
   */
  @GET
  @Path("/aws-regions")
  @Timed
  @ExceptionMetered
  public RestResponse<List<NameValuePair>> listAwsRegions(@QueryParam("accountId") String accountId) {
    return new RestResponse(awsHelperResourceService.getAwsRegions());
  }

  @GET
  @Path("tags")
  @Timed
  @ExceptionMetered
  public RestResponse<Set<String>> listTags(@QueryParam("appId") String appId, @QueryParam("region") String region,
      @QueryParam("computeProviderId") String computeProviderId, @QueryParam("resourceType") String resourceType) {
    return new RestResponse<>(awsHelperResourceService.listTags(appId, computeProviderId, region, resourceType));
  }

  @GET
  @Path("buckets/{settingId}")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> listBuckets(
      @QueryParam("accountId") String accountId, @PathParam("settingId") String settingId) {
    return new RestResponse(awsHelperResourceService.listBuckets(settingId));
  }

  /**
   * Get All Cloudformation Statues
   *
   * @param accountId                the account id
   * @return the rest response
   */
  @GET
  @Path("/cf-states")
  @Timed
  @ExceptionMetered
  @Deprecated
  public RestResponse<Set<String>> listCloudFormationStatues(@QueryParam("accountId") String accountId) {
    return new RestResponse<>(awsHelperResourceService.listCloudFormationStatues());
  }

  @GET
  @Path("/cloudformation/capabilities")
  @Timed
  @ExceptionMetered
  public RestResponse<List<String>> listCloudformationCapabilities() {
    return new RestResponse(awsHelperResourceService.listCloudformationCapabilities());
  }
}
