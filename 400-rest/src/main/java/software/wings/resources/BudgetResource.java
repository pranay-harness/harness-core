/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import static software.wings.security.PermissionAttribute.ResourceType.USER;

import io.harness.ccm.budget.BudgetService;
import io.harness.ccm.commons.entities.billing.Budget;
import io.harness.rest.RestResponse;

import software.wings.security.annotations.Scope;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.util.List;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import org.hibernate.validator.constraints.NotEmpty;

@Api("budgets")
@Path("/budgets")
@Produces("application/json")
@Scope(USER)
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
  public RestResponse<String> clone(@QueryParam("accountId") String accountId, @PathParam("id") String budgetId,
      @QueryParam("cloneName") String budgetName) {
    return new RestResponse<>(budgetService.clone(budgetId, budgetName, accountId));
  }

  @GET
  @Path("{id}")
  @Timed
  @ExceptionMetered
  public RestResponse<Budget> get(@QueryParam("accountId") String accountId, @PathParam("id") String budgetId) {
    return new RestResponse<>(budgetService.get(budgetId, accountId));
  }

  @GET
  @Timed
  @ExceptionMetered
  public RestResponse<List<Budget>> list(@NotEmpty @QueryParam("accountId") String accountId,
      @QueryParam("count") Integer count, @QueryParam("startIndex") Integer startIndex) {
    return new RestResponse<>(budgetService.list(accountId, count, startIndex));
  }

  @GET
  @Path("perspectiveBudgets")
  @Timed
  @ExceptionMetered
  public RestResponse<List<Budget>> list(
      @NotEmpty @QueryParam("accountId") String accountId, @QueryParam("viewId") String viewId) {
    return new RestResponse<>(budgetService.list(accountId, viewId));
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
    budgetService.delete(budgetId, accountId);
    return new RestResponse();
  }
}
