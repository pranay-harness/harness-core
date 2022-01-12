/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.resources;

import io.harness.cf.client.api.CfClient;
import io.harness.cf.client.dto.Target;
import io.harness.rest.RestResponse;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Provider;
import io.swagger.annotations.Api;
import javax.annotation.Nullable;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Api("cf-feature")
@Path("/cf-feature")
@Produces(MediaType.APPLICATION_JSON)
public class SampleCfClientResource {
  @Inject @Nullable private Provider<CfClient> cfClient;

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<Boolean> get(
      @QueryParam("accountId") String accountId, @QueryParam("feature") String featureIdentifier) {
    Target target = Target.builder()
                        .attributes(new ImmutableMap.Builder<String, Object>().put("accountId", accountId).build())
                        .build();
    return new RestResponse<>(cfClient.get().boolVariation(featureIdentifier, target, false));
  }
}
