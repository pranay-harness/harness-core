package io.harness.ccm.remote.resources;

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
import com.google.cloud.bigquery.BigQuery;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
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
import org.springframework.web.bind.annotation.RequestBody;

@Api("perspective")
@Path("perspective")
@Produces(MediaType.APPLICATION_JSON)
@NextGenManagerAuth
@Slf4j
@Service
@OwnedBy(CE)
@Tag(name = "perspective", description = "This contains APIs related to Cloud Cost Perspectives")
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request",
    content = { @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FailureDTO.class)) })
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
    content = { @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDTO.class)) })
public class PerspectiveResource {
  private final CEViewService ceViewService;
  private final CEReportScheduleService ceReportScheduleService;
  private final ViewCustomFieldService viewCustomFieldService;
  private final BigQueryService bigQueryService;
  private final BigQueryHelper bigQueryHelper;

  @Inject
  public PerspectiveResource(CEViewService ceViewService, CEReportScheduleService ceReportScheduleService,
      ViewCustomFieldService viewCustomFieldService, BigQueryService bigQueryService, BigQueryHelper bigQueryHelper) {
    this.ceViewService = ceViewService;
    this.ceReportScheduleService = ceReportScheduleService;
    this.viewCustomFieldService = viewCustomFieldService;
    this.bigQueryService = bigQueryService;
    this.bigQueryHelper = bigQueryHelper;
  }

  @POST
  @Timed
  @ExceptionMetered
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Create perspective", nickname = "createPerspective")
  //  @FeatureRestrictionCheck(FeatureRestrictionName.PERSPECTIVES)
  @LogAccountIdentifier
  @Operation(operationId = "createPerspective", description = "create a Perspective",
      summary =
          "create a Perspective, accepts a url param 'clone' which decides whether the perspective being created should be a clone of existing perspective, and a Request Body with the PerspectiveDefinition",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "default",
            description = "returns a created CEView object with all the rules and filters", content = {
              @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseDTO.class))
            })
      })
  public ResponseDTO<CEView>
  create(@QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
      @QueryParam("clone") @Parameter(required = true,
          description =
              "whether the Perspective being created should be a clone of existing Perspective, if true we will ignore the uuid field in the request body and create a completely new Perspective")
      boolean clone,
      @Parameter(required = true,
          description = "request body containing CEView object to create") @Valid @RequestBody CEView ceView) {
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
    BigQuery bigQuery = bigQueryService.get();
    String cloudProviderTableName = bigQueryHelper.getCloudProviderTableName(ceView.getAccountId(), UNIFIED_TABLE);
    return ceViewService.updateTotalCost(ceView, bigQuery, cloudProviderTableName);
  }

  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Get perspective", nickname = "getPerspective")
  @LogAccountIdentifier
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(operationId = "getPerspective", description = "get a Perspective by identifier",
      summary = "get complete CEView object by Perspective identifier passed as a url param",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "default",
            description =
                "returns a CEView object with all the rules and filters, returns null if no Perspective exists for that particular identifier",
            content = {
              @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseDTO.class))
            })
      })
  public ResponseDTO<CEView>
  get(@QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
      @QueryParam("perspectiveId") @Parameter(required = true,
          description = "the identifier of the Perspective to fetch") @NotBlank @Valid String perspectiveId) {
    return ResponseDTO.newResponse(ceViewService.get(perspectiveId));
  }

  @PUT
  @Timed
  @ExceptionMetered
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Update perspective", nickname = "updatePerspective")
  @LogAccountIdentifier
  @Operation(operationId = "updatePerspective", description = "update an existing Perspective",
      summary =
          "update an existing Perspective, it accepts a CEView and upserts it using the uuid mentioned in the definition",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "default", description = "upserted CEView object with all the rules and filters", content = {
              @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseDTO.class))
            })
      })
  public ResponseDTO<CEView>
  update(@QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
      @Parameter(required = true,
          description = "request body containing CEView object to upsert") @Valid @RequestBody CEView ceView) {
    ceView.setAccountId(accountId);
    log.info(ceView.toString());

    return ResponseDTO.newResponse(updateTotalCost(ceViewService.update(ceView)));
  }

  @DELETE
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Delete perspective", nickname = "deletePerspective")
  @LogAccountIdentifier
  @Operation(operationId = "deletePerspective", description = "delete a Perspective by identifier",
      summary =
          "deletes a perspective by identifier, it accepts a mandatory CEView's identifier as url param and returns a test response on successful deletion",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "default",
            description = "a string text message whether the delete was successful or not", content = {
              @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseDTO.class))
            })
      })
  public ResponseDTO<String>
  delete(@QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
      @QueryParam("perspectiveId") @Parameter(required = true,
          description = "the identifier of the CEView object to delete") @NotNull @Valid String perspectiveId) {
    ceViewService.delete(perspectiveId, accountId);

    ceReportScheduleService.deleteAllByView(perspectiveId, accountId);

    viewCustomFieldService.deleteByViewId(perspectiveId, accountId);

    return ResponseDTO.newResponse("Successfully deleted the view");
  }
}
