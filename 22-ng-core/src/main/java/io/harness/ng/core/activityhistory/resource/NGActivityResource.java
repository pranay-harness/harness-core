package io.harness.ng.core.activityhistory.resource;

import com.google.inject.Inject;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.ng.core.activityhistory.dto.NGActivityDTO;
import io.harness.ng.core.activityhistory.dto.NGActivityListDTO;
import io.harness.ng.core.activityhistory.dto.NGActivitySummaryDTO;
import io.harness.ng.core.activityhistory.service.NGActivityService;
import io.harness.ng.core.dto.ResponseDTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.domain.Page;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("/activityHistory")
@Path("activityHistory")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html"})
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class NGActivityResource {
  NGActivityService activityHistoryService;

  @GET
  @ApiOperation(value = "Get Activities where this resource was used", nickname = "listActivities")
  public ResponseDTO<NGActivityListDTO> list(
      @QueryParam(NGResourceFilterConstants.PAGE_KEY) @DefaultValue("0") int page,
      @QueryParam(NGResourceFilterConstants.SIZE_KEY) @DefaultValue("100") int size,
      @NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.IDENTIFIER_KEY) String referredEntityIdentifier,
      @QueryParam(NGResourceFilterConstants.START) long start, @QueryParam(NGResourceFilterConstants.END) long end) {
    return ResponseDTO.newResponse(activityHistoryService.list(
        page, size, accountIdentifier, orgIdentifier, projectIdentifier, referredEntityIdentifier, start, end));
  }

  @POST
  @ApiOperation(value = "Saves the activity", nickname = "postActivity")
  public ResponseDTO<NGActivityDTO> save(NGActivityDTO activityHistory) {
    return ResponseDTO.newResponse(activityHistoryService.save(activityHistory));
  }

  @GET
  @Path("/summary")
  @ApiOperation(value = "Get Activities Summary", nickname = "getActivitiesSummary")
  public ResponseDTO<Page<NGActivitySummaryDTO>> getActivitiesSummary(
      @NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @QueryParam(NGCommonEntityConstants.IDENTIFIER_KEY) String referredEntityIdentifier,
      @QueryParam(NGResourceFilterConstants.START) long start, @QueryParam(NGResourceFilterConstants.END) long end) {
    return ResponseDTO.newResponse(activityHistoryService.listActivitySummary(
        accountIdentifier, orgIdentifier, projectIdentifier, referredEntityIdentifier, start, end));
  }
}
