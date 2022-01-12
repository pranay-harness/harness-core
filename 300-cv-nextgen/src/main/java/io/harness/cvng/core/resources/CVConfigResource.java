/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.resources;

import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.core.beans.DatasourceTypeDTO;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("cv-config")
@Path("cv-config")
@Produces("application/json")
@ExposeInternalException
@NextGenManagerAuth
public class CVConfigResource {
  @Inject private CVConfigService cvConfigService;
  @GET
  @Path("/product-names")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "gets a list of supported products", nickname = "getProductNames")
  public RestResponse<List<String>> getProductNames(@QueryParam("accountId") @Valid final String accountId,
      @QueryParam("connectorIdentifier") String connectorIdentifier) {
    return new RestResponse<>(cvConfigService.getProductNames(accountId, connectorIdentifier));
  }

  @GET
  @Path("/datasource-types")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "gets a list of datasource types for this filter", nickname = "getDataSourcetypes")
  public RestResponse<Set<DatasourceTypeDTO>> getDataSourcetypes(@NotNull @QueryParam("accountId") String accountId,
      @NotNull @QueryParam("projectIdentifier") String projectIdentifier,
      @NotNull @QueryParam("orgIdentifier") String orgIdentifier,
      @QueryParam("environmentIdentifier") String environmentIdentifier,
      @QueryParam("serviceIdentifier") String serviceIdentifier,
      @QueryParam("monitoringCategory") CVMonitoringCategory monitoringCategory) {
    return new RestResponse<>(cvConfigService.getDataSourcetypes(
        accountId, projectIdentifier, orgIdentifier, environmentIdentifier, serviceIdentifier, monitoringCategory));
  }
}
