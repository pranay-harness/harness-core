package software.wings.graphql.datafetcher.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.exception.WingsException.USER;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HPersistence;
import software.wings.app.MainConfiguration;
import software.wings.beans.trigger.Trigger;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.query.QLTriggerQueryParameters;
import software.wings.graphql.schema.type.trigger.QLTrigger;
import software.wings.graphql.schema.type.trigger.QLTrigger.QLTriggerBuilder;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppService;

@OwnedBy(CDC)
public class TriggerDataFetcher extends AbstractObjectDataFetcher<QLTrigger, QLTriggerQueryParameters> {
  @Inject HPersistence persistence;
  @Inject MainConfiguration mainConfiguration;
  @Inject AppService appService;

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

    QLTriggerBuilder qlTriggerBuilder = QLTrigger.builder();
    TriggerController.populateTrigger(trigger, qlTriggerBuilder, mainConfiguration, accountId);
    return qlTriggerBuilder.build();
  }
}
