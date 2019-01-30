package software.wings.resources;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import io.swagger.annotations.Api;
import software.wings.beans.RestResponse;
import software.wings.beans.alert.AlertNotificationRule;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.AlertNotificationRuleService;

import java.util.List;
import java.util.stream.Collectors;
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

@Api("alert-notification-rules")
@Path("/alert-notification-rules")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Scope(ResourceType.SETTING)
@AuthRule(permissionType = PermissionType.ACCOUNT_MANAGEMENT)
public class AlertNotificationRuleResource {
  @Inject private AlertNotificationRuleService alertNotificationRuleService;

  @POST
  public RestResponse<AlertNotificationRule> createAlertNotificationRule(
      AlertNotificationRule rule, @QueryParam("accountId") String accountId) {
    rule.setAccountId(accountId);
    return new RestResponse<>(alertNotificationRuleService.create(rule));
  }

  @PUT
  @Path("{alertNotificationRuleId}")
  public RestResponse<AlertNotificationRule> updateAlertNotificationRule(
      @PathParam("alertNotificationRuleId") String ruleId, AlertNotificationRule rule,
      @QueryParam("accountId") String accountId) {
    rule.setAccountId(accountId);
    rule.setUuid(ruleId);
    return new RestResponse<>(alertNotificationRuleService.update(rule));
  }

  @GET
  public RestResponse<List<AlertNotificationRule>> list(@QueryParam("accountId") String accountId) {
    List<AlertNotificationRule> allRules = alertNotificationRuleService.getAll(accountId);

    allRules = new ImmutableList.Builder<AlertNotificationRule>()
                   .addAll(allRules.stream().filter(rule -> !rule.isDefault()).collect(Collectors.toList()))
                   .addAll(allRules.stream().filter(AlertNotificationRule::isDefault).collect(Collectors.toList()))
                   .build();

    return new RestResponse<>(allRules);
  }

  @DELETE
  @Path("{alertNotificationRuleId}")
  public RestResponse deleteAlertNotificationRule(
      @PathParam("alertNotificationRuleId") String ruleId, @QueryParam("accountId") String accountId) {
    alertNotificationRuleService.deleteById(ruleId, accountId);
    return new RestResponse<>();
  }
}
