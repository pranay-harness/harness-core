package io.harness.ccm.remote.resources;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.ccm.remote.beans.Constants.ACCOUNT_ID_DESC;

import io.harness.NGCommonEntityConstants;
import io.harness.accesscontrol.AccountIdentifier;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.commons.entities.billing.Budget;
import io.harness.ccm.commons.entities.budget.BudgetData;
import io.harness.ccm.graphql.core.budget.BudgetService;
import io.harness.ccm.utils.LogAccountIdentifier;
import io.harness.ccm.views.service.CEViewService;
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
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Api("budgets")
@Path("budgets")
@Produces(MediaType.APPLICATION_JSON)
@NextGenManagerAuth
@Slf4j
@Service
@OwnedBy(CE)
@Tag(name = "Cloud Cost Budgets", description = "This contains APIs related to Cloud Cost Budgets")
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Bad Request",
    content = { @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = FailureDTO.class)) })
@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Internal server error",
    content = { @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ErrorDTO.class)) })
public class BudgetResource {
  @Inject private BudgetService budgetService;
  @Inject private CEViewService ceViewService;

  @POST
  @Timed
  @LogAccountIdentifier
  @ExceptionMetered
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Create budget", nickname = "createBudget")
  @Operation(operationId = "createBudget",
      description = "Creates a Budget from the Budget object passed as a request body", summary = "Create a Budget",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "default",
            description = "Returns the identifier string of the new Budget created", content = {
              @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseDTO.class))
            })
      })
  public ResponseDTO<String>
  save(@Parameter(required = true, description = ACCOUNT_ID_DESC) @QueryParam(
           NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
      @RequestBody(required = true, description = "The Budget definition") Budget budget) {
    budget.setAccountId(accountId);
    return ResponseDTO.newResponse(budgetService.create(budget));
  }

  @POST
  @Path("{id}")
  @Timed
  @LogAccountIdentifier
  @ExceptionMetered
  @ApiOperation(value = "Clone budget", nickname = "cloneBudget")
  @Operation(operationId = "cloneBudget", description = "Clone an existing Budget using an existing Budget identifier",
      summary = "Clone an existing Budget",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "default",
            description = "Returns the identifier string of the new Budget created using clone operation", content = {
              @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseDTO.class))
            })
      })
  public ResponseDTO<String>
  clone(@Parameter(required = true, description = ACCOUNT_ID_DESC) @QueryParam(
            NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
      @PathParam("id") @Parameter(required = true, description = "The identifier of the Budget") String budgetId,
      @QueryParam("cloneName") @Parameter(
          description = "The name of the new Budget created after cloning operation") String budgetName) {
    return ResponseDTO.newResponse(budgetService.clone(budgetId, budgetName, accountId));
  }

  @GET
  @Path("{id}")
  @Timed
  @LogAccountIdentifier
  @ExceptionMetered
  @ApiOperation(value = "Get budget", nickname = "getBudget")
  @Operation(operationId = "getBudget", description = "Get a Cloud Cost Budget by an identifier",
      summary = "Get a Budget",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Get a Budget by it's identifier", content = {
          @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseDTO.class))
        })
      })
  public ResponseDTO<Budget>
  get(@Parameter(required = true, description = ACCOUNT_ID_DESC) @QueryParam(
          NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
      @Parameter(required = true, description = "The identifier of an existing Budget") @PathParam(
          "id") String budgetId) {
    return ResponseDTO.newResponse(budgetService.get(budgetId, accountId));
  }

  @GET
  @Timed
  @LogAccountIdentifier
  @ExceptionMetered
  @ApiOperation(value = "List budgets for account", nickname = "listBudgetsForAccount")
  @Operation(operationId = "listBudgets", description = "List all the Cloud Cost Budgets",
      summary = "List all the Budgets",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns the list of all Budgets", content = {
          @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseDTO.class))
        })
      })
  public ResponseDTO<List<Budget>>
  list(@Parameter(required = true, description = ACCOUNT_ID_DESC) @QueryParam(
      NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId) {
    return ResponseDTO.newResponse(budgetService.list(accountId));
  }

  @GET
  @Path("perspectiveBudgets")
  @Timed
  @LogAccountIdentifier
  @ExceptionMetered
  @ApiOperation(value = "List budgets for perspective", nickname = "listBudgetsForPerspective")
  @Operation(operationId = "listBudgetsForPerspective",
      description = "List all the Cloud Cost Budgets associated with a Cloud Cost Perspective identifier",
      summary = "List all the Budgets associated with a Perspective",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns the list of Budgets", content = {
          @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseDTO.class))
        })
      })
  public ResponseDTO<List<Budget>>
  list(@Parameter(required = true, description = ACCOUNT_ID_DESC) @QueryParam(
           NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
      @Parameter(required = true, description = "The identifier of an existing Perspective") @QueryParam(
          "perspectiveId") String perspectiveId) {
    return ResponseDTO.newResponse(budgetService.list(accountId, perspectiveId));
  }

  @PUT
  @Path("{id}")
  @Timed
  @LogAccountIdentifier
  @ExceptionMetered
  @ApiOperation(value = "Update budget", nickname = "updateBudget")
  @Operation(operationId = "updateBudget",
      description = "Update an existing Budget using the identifier passed as a path param",
      summary = "Update an existing Budget",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "default",
            description = "Returns a generic string message when the operation is successful", content = {
              @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseDTO.class))
            })
      })
  public ResponseDTO<String>
  update(@Parameter(required = true, description = ACCOUNT_ID_DESC) @QueryParam(
             NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
      @Valid @NotNull @Parameter(required = true, description = "The identifier of an existing Budget") @PathParam(
          "id") String budgetId,
      @RequestBody(required = true, description = "The Budget object as a request body") @NotNull Budget budget) {
    budgetService.update(budgetId, budget);
    return ResponseDTO.newResponse("Successfully updated the budget");
  }

  @DELETE
  @Path("{id}")
  @Timed
  @LogAccountIdentifier
  @ExceptionMetered
  @ApiOperation(value = "Delete budget", nickname = "deleteBudget")
  @Operation(operationId = "deleteBudget", description = "Delete an existing Cloud Cost Budget by identifier",
      summary = "Delete an existing Budget",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "default",
            description = "Returns a text message whether the operation was successful", content = {
              @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseDTO.class))
            })
      })
  public ResponseDTO<String>
  delete(@Parameter(required = true, description = ACCOUNT_ID_DESC) @QueryParam(
             NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
      @NotNull @Valid @Parameter(required = true, description = "The identifier of the Budget") @PathParam(
          "id") String budgetId) {
    budgetService.delete(budgetId, accountId);
    return ResponseDTO.newResponse("Successfully deleted the budget");
  }

  @GET
  @Path("lastMonthCost")
  @Timed
  @LogAccountIdentifier
  @ExceptionMetered
  @ApiOperation(value = "Deprecated use /perspective/lastMonthCost instead, Get last month cost for perspective.",
      nickname = "getLastMonthCost")
  @Deprecated
  public ResponseDTO<Double>
  getLastMonthCost(@Parameter(required = true, description = ACCOUNT_ID_DESC) @QueryParam(
                       NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
      @QueryParam("perspectiveId") @Parameter(
          required = true, description = "The identifier of the perspective") String perspectiveId) {
    return ResponseDTO.newResponse(ceViewService.getLastMonthCostForPerspective(accountId, perspectiveId));
  }

  @GET
  @Path("forecastCost")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Deprecated use /perspective/forecastCost instead, Get forecast cost for perspective.",
      nickname = "getForecastCost")
  @Deprecated
  public ResponseDTO<Double>
  getForecastCost(@Parameter(required = true, description = ACCOUNT_ID_DESC) @QueryParam(
                      NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @QueryParam("perspectiveId") String perspectiveId) {
    return ResponseDTO.newResponse(ceViewService.getForecastCostForPerspective(accountId, perspectiveId));
  }

  @GET
  @Path("{id}/costDetails")
  @Timed
  @LogAccountIdentifier
  @ExceptionMetered
  @ApiOperation(value = "Get cost details for budget", nickname = "getCostDetails")
  @Operation(operationId = "getCostDetails",
      description = "Get the cost details associated with a Cloud Cost Budget identifier",
      summary = "Get the cost details associated with a Budget",
      responses =
      {
        @io.swagger.v3.oas.annotations.responses.
        ApiResponse(responseCode = "default", description = "Returns the cost data of a Budget", content = {
          @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = ResponseDTO.class))
        })
      })
  public ResponseDTO<BudgetData>
  getCostDetails(@Parameter(required = true, description = ACCOUNT_ID_DESC) @QueryParam(
                     NGCommonEntityConstants.ACCOUNT_KEY) @AccountIdentifier @NotNull @Valid String accountId,
      @Parameter(required = true, description = "The identifier of the Budget") @PathParam("id") String budgetId) {
    return ResponseDTO.newResponse(budgetService.getBudgetTimeSeriesStats(budgetService.get(budgetId, accountId)));
  }
}
