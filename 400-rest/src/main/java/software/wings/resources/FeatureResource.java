/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.resources;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static software.wings.security.PermissionAttribute.PermissionType.LOGGED_IN;

import io.harness.rest.RestResponse;

import software.wings.features.api.FeatureRestrictions;
import software.wings.features.api.FeatureService;
import software.wings.features.api.FeaturesUsageComplianceReport;
import software.wings.security.annotations.AuthRule;

import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

@Api("features")
@Path("/features")
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class FeatureResource {
  @Inject private FeatureService featureService;

  @GET
  @Path("/usage-compliance-report")
  @AuthRule(permissionType = LOGGED_IN)
  @Timed
  public RestResponse<FeaturesUsageComplianceReport> getFeaturesUsageComplianceReport(
      @QueryParam("accountId") @NotEmpty String accountId, @QueryParam("targetAccountType") String targetAccountType) {
    FeaturesUsageComplianceReport report = isEmpty(targetAccountType)
        ? featureService.getFeaturesUsageComplianceReport(accountId)
        : featureService.getFeaturesUsageComplianceReport(accountId, targetAccountType);

    return new RestResponse<>(report);
  }

  @GET
  @Path("/restrictions")
  @AuthRule(permissionType = LOGGED_IN)
  public RestResponse<FeatureRestrictions> getFeatureRestrictions(@QueryParam("accountId") String accountId) {
    return new RestResponse<>(featureService.getFeatureRestrictions());
  }
}
