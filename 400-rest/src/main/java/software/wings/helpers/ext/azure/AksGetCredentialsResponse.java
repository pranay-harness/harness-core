package software.wings.helpers.ext.azure;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.Data;

@Data
@TargetModule(HarnessModule._970_API_SERVICES_BEANS)
public class AksGetCredentialsResponse {
  private String id;
  private String location;
  private String name;
  private String type;
  private AksGetCredentialProperties properties;

  @Data
  public class AksGetCredentialProperties {
    private String kubeConfig;
  }
}
