package io.harness.ccm.remote.resources;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.NGCommonEntityConstants;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.commons.entities.billing.Budget;
import io.harness.ccm.commons.entities.budget.BudgetData;
import io.harness.ccm.graphql.core.budget.BudgetService;
import io.harness.rest.RestResponse;
import io.harness.security.annotations.NextGenManagerAuth;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import java.util.List;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.stereotype.Service;

@Api("budgets")
@Path("/budgets")
@Produces("application/json")
@NextGenManagerAuth
@Slf4j
@Service
@OwnedBy(CE)
public class BudgetResource {
  @Inject private BudgetService budgetService;

  @POST
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Create budget", nickname = "createBudget")
  public RestResponse<String> save(@QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId, Budget budget) {
    budget.setAccountId(accountId);
    return new RestResponse<>(budgetService.create(budget));
  }

  @POST
  @Path("{id}")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Clone budget", nickname = "cloneBudget")
  public RestResponse<String> clone(@QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @PathParam("id") String budgetId, @QueryParam("cloneName") String budgetName) {
    return new RestResponse<>(budgetService.clone(budgetId, budgetName, accountId));
  }

  @GET
  @Path("{id}")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Get budget", nickname = "getBudget")
  public RestResponse<Budget> get(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId, @PathParam("id") String budgetId) {
    return new RestResponse<>(budgetService.get(budgetId, accountId));
  }

  @GET
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "List budgets for account", nickname = "listBudgetsForAccount")
  public RestResponse<List<Budget>> list(@NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId) {
    return new RestResponse<>(budgetService.list(accountId));
  }

  @GET
  @Path("perspectiveBudgets")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "List budgets for perspective", nickname = "listBudgetsForPerspective")
  public RestResponse<List<Budget>> list(@NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @QueryParam("perspectiveId") String perspectiveId) {
    return new RestResponse<>(budgetService.list(accountId, perspectiveId));
  }

  @PUT
  @Path("{id}")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Update budget", nickname = "updateBudget")
  public RestResponse<String> update(@QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @PathParam("id") String budgetId, Budget budget) {
    budgetService.update(budgetId, budget);
    return new RestResponse<>("Successfully updated the budget");
  }

  @DELETE
  @Path("{id}")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Delete budget", nickname = "deleteBudget")
  public RestResponse<String> delete(
      @NotEmpty @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId, @PathParam("id") String budgetId) {
    budgetService.delete(budgetId, accountId);
    return new RestResponse<>("Successfully deleted the budget");
  }

  @GET
  @Path("lastMonthCost")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Get last month cost for perspective", nickname = "getLastMonthCost")
  public RestResponse<Double> getLastMonthCost(@QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @QueryParam("perspectiveId") String perspectiveId) {
    return new RestResponse<>(budgetService.getLastMonthCostForPerspective(accountId, perspectiveId));
  }

  @GET
  @Path("forecastCost")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Get forecast cost for perspective", nickname = "getForecastCost")
  public RestResponse<Double> getForecastCost(@QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId,
      @QueryParam("perspectiveId") String perspectiveId) {
    return new RestResponse<>(budgetService.getForecastCostForPerspective(accountId, perspectiveId));
  }

  @GET
  @Path("{id}/costDetails")
  @Timed
  @ExceptionMetered
  @ApiOperation(value = "Get cost details for budget", nickname = "getCostDetails")
  public RestResponse<BudgetData> getCostDetails(
      @QueryParam(NGCommonEntityConstants.ACCOUNT_KEY) String accountId, @PathParam("id") String budgetId) {
    return new RestResponse<>(budgetService.getBudgetTimeSeriesStats(budgetService.get(budgetId, accountId)));
  }
}
