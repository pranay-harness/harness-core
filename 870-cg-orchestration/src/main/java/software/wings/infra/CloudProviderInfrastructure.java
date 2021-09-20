package software.wings.infra;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import software.wings.api.CloudProviderType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use = Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@OwnedBy(HarnessTeam.CDP)
public interface CloudProviderInfrastructure {
  String getCloudProviderId();
  @JsonIgnore CloudProviderType getCloudProviderType();
  @JsonIgnore String getInfrastructureType();
}
