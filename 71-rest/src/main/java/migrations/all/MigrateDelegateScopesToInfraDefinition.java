package migrations.all;

import com.google.inject.Inject;

import io.harness.data.structure.EmptyPredicate;
import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.DelegateScope;
import software.wings.beans.DelegateScope.DelegateScopeKeys;
import software.wings.beans.InfrastructureMapping;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.DelegateScopeService;
import software.wings.service.intfc.InfrastructureMappingService;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class MigrateDelegateScopesToInfraDefinition implements Migration {
  @Inject private DelegateScopeService delegateScopeService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private WingsPersistence wingsPersistence;

  private static final String accountId = "zEaak-FLS425IEO7OLzMUg";

  @Override
  public void migrate() {
    logger.info("Running infra migration for Delegate Scopes.Retrieving applications for accountId: " + accountId);
    try (
        HIterator<DelegateScope> scopes = new HIterator<>(
            wingsPersistence.createQuery(DelegateScope.class).filter(DelegateScopeKeys.accountId, accountId).fetch())) {
      logger.info("[Delegate Scoping Migration]: Updating Delegate Scopes.");
      while (scopes.hasNext()) {
        DelegateScope scope = scopes.next();
        try {
          logger.info(
              "[Delegate Scoping Migration]: Starting to migrate" + scope.getName() + " with id: " + scope.getUuid());
          migrate(scope);
        } catch (Exception e) {
          logger.error("[Delegate Scoping Migration]: Migration Failed for scope " + scope.getName()
              + " with id: " + scope.getUuid());
        }
      }
    }
  }

  private void migrate(DelegateScope scope) {
    List<String> infraMappingIds = scope.getServiceInfrastructures();
    if (EmptyPredicate.isNotEmpty(infraMappingIds)) {
      // Using wings persistence here as no appId present
      List<InfrastructureMapping> infrastructureMappings = wingsPersistence.createQuery(InfrastructureMapping.class)
                                                               .filter(InfrastructureMapping.ACCOUNT_ID_KEY, accountId)
                                                               .field(InfrastructureMapping.ID_KEY)
                                                               .in(infraMappingIds)
                                                               .asList();

      List<String> serviceIds =
          infrastructureMappings.stream().map(InfrastructureMapping::getServiceId).collect(Collectors.toList());
      List<String> infraDefinitionIds = infrastructureMappings.stream()
                                            .map(InfrastructureMapping::getInfrastructureDefinitionId)
                                            .collect(Collectors.toList());

      logger.info("[Delegate Scoping Migration]: Setting " + serviceIds.size() + " on scope " + scope.getName()
          + " with id: " + scope.getUuid());
      scope.setServices(serviceIds);

      logger.info("[Delegate Scoping Migration]: Setting " + infraDefinitionIds.size() + " on scope " + scope.getName()
          + " with id: " + scope.getUuid());
      scope.setInfrastructureDefinitions(infraDefinitionIds);

      logger.info("[Delegate Scoping Migration]: Updating scope " + scope.getName() + " with id: " + scope.getUuid());
      delegateScopeService.update(scope);
      logger.info("[Delegate Scoping Migration]: Updated scope " + scope.getName() + " with id: " + scope.getUuid());
    } else {
      logger.info("[Delegate Scoping Migration]: No Service Infras Found for scope " + scope.getName()
          + " with id: " + scope.getUuid());
    }
  }
}
