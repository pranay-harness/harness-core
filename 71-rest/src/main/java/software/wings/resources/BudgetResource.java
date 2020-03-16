package software.wings.resources;

import com.google.inject.Inject;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import io.harness.ccm.budget.BudgetService;
import io.harness.ccm.budget.entities.Budget;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import org.hibernate.validator.constraints.NotEmpty;

import java.util.List;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("budgets")
@Path("/budgets")
@Produces("application/json")
public class BudgetResource {
  private BudgetService budgetService;

  @Inject
  public BudgetResource(BudgetService budgetService) {
    this.budgetService = budgetService;
  }

  @POST
  @Timed
  @ExceptionMetered
  public RestResponse<String> save(@QueryParam("accountId") String accountId, Budget budget) {
    budget.setAccountId(accountId);
    return new RestResponse<>(budgetService.create(budget));
  }

  @POST
  @Path("{id}")
  @Timed
  @ExceptionMetered
  public RestResponse<String> clone(
      @QueryParam("accountId") String accountId, @PathParam("id") String budgetId, String budgetName) {
    return new RestResponse<>(budgetService.clone(budgetId, budgetName));
  }

  @GET
  @Path("{id}")
  @Timed
  @ExceptionMetered
  public RestResponse<Budget> get(@QueryParam("accountId") String accountId, @PathParam("id") String budgetId) {
    return new RestResponse<>(budgetService.get(budgetId));
  }

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<List<Budget>> list(@NotEmpty @QueryParam("accountId") String accountId,
      @QueryParam("count") Integer count, @QueryParam("startIndex") Integer startIndex) {
    return new RestResponse<>(budgetService.list(accountId, count, startIndex));
  }

  @PUT
  @Path("{id}")
  @Timed
  @ExceptionMetered
  public RestResponse update(@PathParam("id") String budgetId, Budget budget) {
    budgetService.update(budgetId, budget);
    return new RestResponse();
  }

  @DELETE
  @Path("{id}")
  @Timed
  @ExceptionMetered
  public RestResponse delete(@NotEmpty @QueryParam("accountId") String accountId, @PathParam("id") String budgetId) {
    budgetService.delete(budgetId);
    return new RestResponse();
  }
}
