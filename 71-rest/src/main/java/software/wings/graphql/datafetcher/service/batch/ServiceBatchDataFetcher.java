package software.wings.graphql.datafetcher.service.batch;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;

import software.wings.graphql.datafetcher.AbstractBatchDataFetcher;
import software.wings.graphql.schema.query.QLServiceQueryParameters;
import software.wings.graphql.schema.type.QLService;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

import java.util.concurrent.CompletionStage;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.dataloader.DataLoader;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ServiceBatchDataFetcher extends AbstractBatchDataFetcher<QLService, QLServiceQueryParameters, String> {
  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public CompletionStage<QLService> load(
      QLServiceQueryParameters qlQuery, @NotNull DataLoader<String, QLService> dataLoader) {
    final String serviceId;
    if (StringUtils.isNotBlank(qlQuery.getServiceId())) {
      serviceId = qlQuery.getServiceId();
    } else {
      throw new InvalidRequestException("ServiceId not present in query", WingsException.USER);
    }
    return dataLoader.load(serviceId);
  }
}
