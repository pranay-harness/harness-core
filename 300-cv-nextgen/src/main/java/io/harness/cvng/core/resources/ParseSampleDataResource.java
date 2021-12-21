package io.harness.cvng.core.resources;

import io.harness.annotations.ExposeInternalException;
import io.harness.cvng.core.beans.TimeSeriesSampleDTO;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.services.api.ParseSampleDataService;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("parseSampleDara")
@Path("/parse-sample-data")
@Produces("application/json")
@NextGenManagerAuth
@ExposeInternalException
@ApiResponses(value =
    {
      @ApiResponse(code = 400, response = FailureDTO.class, message = "Bad Request")
      , @ApiResponse(code = 500, response = ErrorDTO.class, message = "Internal server error")
    })
public class ParseSampleDataResource {
  @Inject private ParseSampleDataService sampleDataService;

  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "parse sample data for given json response", nickname = "fetchTimeSeries")
  public ResponseDTO<List<TimeSeriesSampleDTO>> getParsedSampleData(@NotNull @QueryParam("accountId") String accountId,
      @QueryParam("orgIdentifier") @NotNull String orgIdentifier,
      @QueryParam("projectIdentifier") @NotNull String projectIdentifier,
      @QueryParam("jsonResponse") @NotNull String jsonResponse, @QueryParam("groupName") @NotNull String groupName,
      @QueryParam("metricValueJSONPath") @NotNull String metricValueJSONPath,
      @QueryParam("timestampJSONPath") @NotNull String timestampJSONPath,
      @QueryParam("timestampFormat") String timestampFormat) {
    ProjectParams projectParams = ProjectParams.builder()
                                      .accountIdentifier(accountId)
                                      .orgIdentifier(orgIdentifier)
                                      .projectIdentifier(projectIdentifier)
                                      .build();
    return ResponseDTO.newResponse(sampleDataService.parseSampleData(
        projectParams, jsonResponse, groupName, metricValueJSONPath, timestampJSONPath, timestampFormat));
  }
}
