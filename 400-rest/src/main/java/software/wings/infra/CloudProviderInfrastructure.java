package software.wings.infra;

import software.wings.api.CloudProviderType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use = Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
public interface CloudProviderInfrastructure {
  String getCloudProviderId();
  @JsonIgnore CloudProviderType getCloudProviderType();
  @JsonIgnore String getInfrastructureType();
}
