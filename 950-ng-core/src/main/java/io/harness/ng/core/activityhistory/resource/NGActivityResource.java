package io.harness.ng.core.activityhistory.resource;

import io.harness.NGCommonEntityConstants;
import io.harness.NGResourceFilterConstants;
import io.harness.ng.core.activityhistory.NGActivityStatus;
import io.harness.ng.core.activityhistory.dto.ConnectivityCheckSummaryDTO;
import io.harness.ng.core.activityhistory.dto.NGActivityDTO;
import io.harness.ng.core.activityhistory.dto.NGActivitySummaryDTO;
import io.harness.ng.core.activityhistory.dto.TimeGroupType;
import io.harness.ng.core.activityhistory.service.EntityActivitySummaryService;
import io.harness.ng.core.activityhistory.service.NGActivityService;
import io.harness.ng.core.dto.ResponseDTO;

import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.domain.Page;

@Api("/activityHistory")
@Path("activityHistory")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html"})
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class NGActivityResource {
  public static final String TIME_GROUP_TYPE = "timeGroupType";
  public static final String INCLUDE_CONNECTIVITY_SUMMARY = "includeConnectivitySummary";
  public static final String TIMEZONE = "timezone";
  NGActivityService activityHistoryService;
  EntityActivitySummaryService entityActivitySummaryService;

  @GET
  @ApiOperation(value = "Get Activities where this resource was used", nickname = "listActivities")
  public ResponseDTO<Page<NGActivityDTO>> list(
      @QueryParam(NGResourceFilterConstants.PAGE_KEY) @DefaultValue("0") int page,
      @QueryParam(NGResourceFilterConstants.SIZE_KEY) @DefaultValue("100") int size,
      @NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotEmpty @QueryParam(NGCommonEntityConstants.IDENTIFIER_KEY) String referredEntityIdentifier,
      @NotNull @QueryParam(NGResourceFilterConstants.START) long startTime,
      @NotNull @QueryParam(NGResourceFilterConstants.END) long endTime,
      @QueryParam(NGCommonEntityConstants.STATUS) NGActivityStatus status) {
    return ResponseDTO.newResponse(activityHistoryService.list(page, size, accountIdentifier, orgIdentifier,
        projectIdentifier, referredEntityIdentifier, startTime, endTime, status));
  }

  @GET
  @Path("/connectivityCheckSummary")
  @ApiOperation(value = "Get ConnectivityCheck Summary", nickname = "getConnectivitySummary")
  public ResponseDTO<ConnectivityCheckSummaryDTO> getConnectivitySummary(
      @NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountIdentifier,
      @QueryParam(NGCommonEntityConstants.ORG_KEY) String orgIdentifier,
      @QueryParam(NGCommonEntityConstants.PROJECT_KEY) String projectIdentifier,
      @NotEmpty @QueryParam(NGCommonEntityConstants.IDENTIFIER_KEY) String referredEntityIdentifier,
      @NotNull @QueryParam(NGResourceFilterConstants.START) long startTime,
      @NotNull @QueryParam(NGResourceFilterConstants.END) long endTime) {
    return ResponseDTO.newResponse(activityHistoryService.getConnectivityCheckSummary(
        accountIdentifier, orgIdentifier, projectIdentifier, referredEntityIdentifier, startTime, endTime));
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
      @NotEmpty @QueryParam(NGCommonEntityConstants.IDENTIFIER_KEY) String referredEntityIdentifier,
      @NotNull @QueryParam(NGResourceFilterConstants.START) long startTime,
      @NotNull @QueryParam(NGResourceFilterConstants.END) long endTime,
      @NotNull @QueryParam(TIME_GROUP_TYPE) TimeGroupType timeGroupType) {
    return ResponseDTO.newResponse(entityActivitySummaryService.listActivitySummary(accountIdentifier, orgIdentifier,
        projectIdentifier, referredEntityIdentifier, timeGroupType, startTime, endTime));
  }
}
