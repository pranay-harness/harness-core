package software.wings.graphql.datafetcher.service;

import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Service;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.schema.type.QLService;
import software.wings.graphql.schema.type.QLService.QLServiceBuilder;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.service.impl.security.auth.AuthHandler;

@Slf4j
public class ServiceDataFetcher extends AbstractDataFetcher<QLService> {
  public static final String SERVICE_ID_ARG = "serviceId";
  @Inject HPersistence persistence;

  private static final PermissionAttribute permissionAttribute =
      new PermissionAttribute(PermissionType.PIPELINE, Action.READ);

  @Inject
  public ServiceDataFetcher(AuthHandler authHandler) {
    super(authHandler);
  }

  @Override
  protected QLService fetch(DataFetchingEnvironment dataFetchingEnvironment) {
    String serviceId = (String) getArgumentValue(dataFetchingEnvironment, SERVICE_ID_ARG);
    Service service = persistence.get(Service.class, serviceId);
    if (service == null) {
      throw new InvalidRequestException("Service does not exist", WingsException.USER);
    }

    final QLServiceBuilder builder = QLService.builder();
    ServiceController.populateService(service, builder);
    return builder.build();
  }
}
