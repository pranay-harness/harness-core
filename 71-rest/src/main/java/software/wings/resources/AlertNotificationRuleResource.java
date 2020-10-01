package software.wings.resources;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_ALERT_NOTIFICATION_RULES;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.rest.RestResponse;
import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.RequestBody;
import software.wings.beans.alert.AlertNotificationRule;
import software.wings.beans.alert.NotificationRulesStatus;
import software.wings.security.PermissionAttribute.ResourceType;
import software.wings.security.annotations.AuthRule;
import software.wings.security.annotations.Scope;
import software.wings.service.intfc.AlertNotificationRuleService;
import software.wings.service.intfc.alert.NotificationRulesStatusService;

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

@OwnedBy(PL)
@Api("alert-notification-rules")
@Path("/alert-notification-rules")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Scope(ResourceType.SETTING)
@AuthRule(permissionType = MANAGE_ALERT_NOTIFICATION_RULES)
public class AlertNotificationRuleResource {
  @Inject private AlertNotificationRuleService alertNotificationRuleService;
  @Inject private NotificationRulesStatusService rulesStatusService;

  @GET
  @Path("/status")
  public RestResponse<NotificationRulesStatus> getStatus(@QueryParam("accountId") String accountId) {
    return new RestResponse<>(rulesStatusService.get(accountId));
  }

  @PUT
  @Path("/status")
  public RestResponse<NotificationRulesStatus> updateStatus(
      @QueryParam("accountId") String accountId, @RequestBody NotificationRulesStatus status) {
    return new RestResponse<>(rulesStatusService.update(accountId, status.isEnabled()));
  }

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
