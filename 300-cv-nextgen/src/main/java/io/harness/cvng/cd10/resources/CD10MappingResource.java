package io.harness.cvng.cd10.resources;

import static io.harness.cvng.analysis.CVAnalysisConstants.CD_10_MAPPING_RESOURCE;

import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.cd10.beans.CD10MappingsDTO;
import io.harness.cvng.cd10.services.api.CD10MappingService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;

@Api(CD_10_MAPPING_RESOURCE)
@Path(CD_10_MAPPING_RESOURCE)
@Produces("application/json")
@Slf4j
@ExposeInternalException
@NextGenManagerAuth
public class CD10MappingResource {
  @Inject private CD10MappingService cd10MappingService;
  @POST
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "create cd 1.0 mapping (Batch API)", nickname = "createCD10Mappings")
  public void create(@QueryParam("accountId") @NotNull String accountId, CD10MappingsDTO cd10MappingsDTO) {
    cd10MappingService.create(accountId, cd10MappingsDTO);
  }

  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "list cd 1.0 env mapping ", nickname = "getCD10Mappings")
  public RestResponse<CD10MappingsDTO> get(@QueryParam("accountId") @NotNull String accountId,
      @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier) {
    return new RestResponse<>(cd10MappingService.list(accountId, orgIdentifier, projectIdentifier));
  }
}
