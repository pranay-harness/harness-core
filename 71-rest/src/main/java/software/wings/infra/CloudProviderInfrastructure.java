package software.wings.infra;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import software.wings.api.CloudProviderType;

@JsonTypeInfo(use = Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
public interface CloudProviderInfrastructure {
  String getCloudProviderId();
  CloudProviderType getCloudProviderType();
  String getCloudProviderInfrastructureType();
}
