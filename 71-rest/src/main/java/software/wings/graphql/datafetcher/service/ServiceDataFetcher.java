package software.wings.graphql.datafetcher.service;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Service;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.schema.query.QLServiceQueryParameters;
import software.wings.graphql.schema.type.QLService;
import software.wings.graphql.schema.type.QLService.QLServiceBuilder;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

@Slf4j
public class ServiceDataFetcher extends AbstractDataFetcher<QLService, QLServiceQueryParameters> {
  @Inject HPersistence persistence;

  @Override
  @AuthRule(permissionType = PermissionType.SERVICE, action = Action.READ)
  protected QLService fetch(QLServiceQueryParameters qlQuery, String accountId) {
    Service service = persistence.get(Service.class, qlQuery.getServiceId());
    if (service == null) {
      throw new InvalidRequestException("Service does not exist", WingsException.USER);
    }

    if (!service.getAccountId().equals(accountId)) {
      throw new InvalidRequestException("Service does not exist", WingsException.USER);
    }

    final QLServiceBuilder builder = QLService.builder();
    ServiceController.populateService(service, builder);
    return builder.build();
  }
}
