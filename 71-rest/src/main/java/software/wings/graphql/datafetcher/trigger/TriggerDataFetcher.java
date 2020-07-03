package software.wings.graphql.datafetcher.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.exception.WingsException.USER;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HPersistence;
import software.wings.beans.trigger.Trigger;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.query.QLTriggerQueryParameters;
import software.wings.graphql.schema.type.trigger.QLTrigger;
import software.wings.graphql.schema.type.trigger.QLTrigger.QLTriggerBuilder;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.impl.trigger.TriggerAuthHandler;
import software.wings.service.intfc.AppService;

import java.util.Collections;

@OwnedBy(CDC)
public class TriggerDataFetcher extends AbstractObjectDataFetcher<QLTrigger, QLTriggerQueryParameters> {
  @Inject HPersistence persistence;
  @Inject AppService appService;
  @Inject TriggerAuthHandler triggerAuthHandler;
  @Inject TriggerController triggerController;

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  protected QLTrigger fetch(QLTriggerQueryParameters parameters, String accountId) {
    Trigger trigger = persistence.get(Trigger.class, parameters.getTriggerId());
    if (trigger == null) {
      return null;
    }

    if (!accountId.equals(appService.getAccountIdByAppId(trigger.getAppId()))) {
      throw new InvalidRequestException("Trigger doesn't exist", USER);
    }
    triggerAuthHandler.authorizeAppAccess(Collections.singletonList(trigger.getAppId()), accountId);

    QLTriggerBuilder qlTriggerBuilder = QLTrigger.builder();
    triggerController.populateTrigger(trigger, qlTriggerBuilder, accountId);
    return qlTriggerBuilder.build();
  }
}
