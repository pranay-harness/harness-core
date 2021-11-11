package io.harness.ccm.remote.resources;

import static io.harness.NGCommonEntityConstants.ACCOUNT_PARAM_MESSAGE;
import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.ccm.commons.utils.BigQueryHelper.UNIFIED_TABLE;

import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.bigQuery.BigQueryService;
import io.harness.ccm.commons.utils.BigQueryHelper;
import io.harness.ccm.utils.LogAccountIdentifier;
import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.service.CEReportScheduleService;
import io.harness.ccm.views.service.CEViewService;
import io.harness.ccm.views.service.ViewCustomFieldService;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotBlank;
import org.springframework.stereotype.Service;

@Api("perspective")
@Path("perspective")
@Produces(MediaType.APPLICATION_JSON)
@NextGenManagerAuth
@Slf4j
@Service
@OwnedBy(CE)
@Tag(name = "Cloud Cost Perspectives", description = "This contains APIs related to Cloud Cost Perspectives")
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request",
    content = { @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FailureDTO.class)) })
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
    content = { @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDTO.class)) })
public class PerspectiveResource {
  private final CEViewService ceViewService;
  private final CEReportScheduleService ceReportScheduleService;
  private final ViewCustomFieldService viewCustomFieldService;
  private final BigQueryHelper bigQueryHelper;

  @Inject
  public PerspectiveResource(CEViewService ceViewService, CEReportScheduleService ceReportScheduleService,
      ViewCustomFieldService viewCustomFieldService, BigQueryHelper bigQueryHelper) {
    this.ceViewService = ceViewService;
    this.ceReportScheduleService = ceReportScheduleService;
    this.viewCustomFieldService = viewCustomFieldService;
    this.bigQueryHelper = bigQueryHelper;
  }

  @GET
  @Path("lastMonthCost")
  @Timed
  @LogAccountIdentifier
  @ExceptionMetered
  @ApiOperation(value = "Get last month cost for perspective", nickname = "getLastMonthCost")
  @Operation(operationId = "getLastMonthCostV2", description = "Get last month cost for a Perspective",
      summary = "Get the last month cost for a Perspective",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(description = "Returns a number having the cost of last month",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) })
      })
  public ResponseDTO<Double>
  getLastMonthCost(@Parameter(required = true, description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
                       NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
      @NotNull @Valid @QueryParam("perspectiveId") @Parameter(required = true,
          description = "The Perspective identifier for which we want the last month cost") String perspectiveId) {
    return ResponseDTO.newResponse(ceViewService.getLastMonthCostForPerspective(accountId, perspectiveId));
  }

  @GET
  @Path("forecastCost")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Get forecast cost for perspective", nickname = "getForecastCost")
  @Operation(operationId = "getForecastCostV2",
      description = "Get the forecasted cost of a Perspective for next 30 days",
      summary = "Get the forecasted cost of a Perspective",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(description = "Returns a number having the forecast cost of a Perspective for next 30 days",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) })
      })
  public ResponseDTO<Double>
  getForecastCost(@Parameter(required = true, description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
                      NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
      @Valid @NotNull @Parameter(
          required = true, description = "The Perspective identifier for which we want the forecast cost")
      @QueryParam("perspectiveId") String perspectiveId) {
    return ResponseDTO.newResponse(ceViewService.getForecastCostForPerspective(accountId, perspectiveId));
  }

  @POST
  @Timed
  @ExceptionMetered
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Create perspective", nickname = "createPerspective")
  //  @FeatureRestrictionCheck(FeatureRestrictionName.PERSPECTIVES)
  @LogAccountIdentifier
  @Operation(operationId = "createPerspective",
      description =
          "Create a Perspective, accepts a url param 'clone' which decides whether the Perspective being created should be a clone of existing Perspective, and a Request Body with the PerspectiveDefinition",
      summary = "Create a Perspective",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(description = "Returns a created CEView object with all the rules and filters",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) })
      })
  public ResponseDTO<CEView>
  create(@Parameter(required = true, description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
             NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
      @QueryParam("clone") @Parameter(required = true,
          description =
              "Whether the Perspective being created should be a clone of existing Perspective, if true we will ignore the uuid field in the request body and create a completely new Perspective")
      boolean clone,
      @RequestBody(required = true,
          description = "Request body containing Perspective's CEView object to create") @Valid CEView ceView) {
    ceView.setAccountId(accountId);
    if (clone) {
      // reset these fields which gets set downstream appropriately
      ceView.setCreatedBy(null);
      ceView.setCreatedAt(0);
      ceView.setUuid(null);
    }
    return ResponseDTO.newResponse(updateTotalCost(ceViewService.save(ceView)));
  }

  private CEView updateTotalCost(CEView ceView) {
    String cloudProviderTableName = bigQueryHelper.getCloudProviderTableName(ceView.getAccountId(), UNIFIED_TABLE);
    return ceViewService.updateTotalCost(ceView, cloudProviderTableName);
  }

  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Get perspective", nickname = "getPerspective")
  @LogAccountIdentifier
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(operationId = "getPerspective",
      description = "Get complete CEView object by Perspective identifier passed as a url param",
      summary = "Get a Perspective by identifier",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            description =
                "Returns a CEView object with all the rules and filters, returns null if no Perspective exists for that particular identifier",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) })
      })
  public ResponseDTO<CEView>
  get(@Parameter(required = true, description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
      @QueryParam("perspectiveId") @Parameter(required = true,
          description = "The identifier of the Perspective to fetch") @NotBlank @Valid String perspectiveId) {
    return ResponseDTO.newResponse(ceViewService.get(perspectiveId));
  }

  @PUT
  @Timed
  @ExceptionMetered
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Update perspective", nickname = "updatePerspective")
  @LogAccountIdentifier
  @Operation(operationId = "updatePerspective",
      description =
          "Update an existing Perspective, it accepts a CEView and upserts it using the uuid mentioned in the definition",
      summary = "Update an existing Perspective",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(description = "Upserted CEView object with all the rules and filters",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) })
      })
  public ResponseDTO<CEView>
  update(@Parameter(required = true, description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
             NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
      @Valid @RequestBody(required = true,
          description = "Request body containing Perspective's CEView object to update") CEView ceView) {
    ceView.setAccountId(accountId);
    log.info(ceView.toString());

    return ResponseDTO.newResponse(updateTotalCost(ceViewService.update(ceView)));
  }

  @DELETE
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Delete perspective", nickname = "deletePerspective")
  @LogAccountIdentifier
  @Operation(operationId = "deletePerspective",
      description =
          "Deletes a perspective by identifier, it accepts a mandatory CEView's identifier as url param and returns a test response on successful deletion",
      summary = "Delete a Perspective by identifier",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(description = "A string text message whether the delete was successful or not",
            content = { @Content(mediaType = MediaType.APPLICATION_JSON) })
      })
  public ResponseDTO<String>
  delete(@Parameter(required = true, description = ACCOUNT_PARAM_MESSAGE) @QueryParam(
             NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
      @QueryParam("perspectiveId") @Parameter(required = true,
          description = "The identifier of the CEView object to delete") @NotNull @Valid String perspectiveId) {
    ceViewService.delete(perspectiveId, accountId);

    ceReportScheduleService.deleteAllByView(perspectiveId, accountId);

    viewCustomFieldService.deleteByViewId(perspectiveId, accountId);

    return ResponseDTO.newResponse("Successfully deleted the view");
  }
}
