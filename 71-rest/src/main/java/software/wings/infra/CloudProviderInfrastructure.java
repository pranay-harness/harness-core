package software.wings.infra;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use = Id.NAME, property = "type")
public interface CloudProviderInfrastructure {
  String getCloudProviderId();
}
