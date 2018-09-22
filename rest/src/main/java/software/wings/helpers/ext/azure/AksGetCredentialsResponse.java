package software.wings.helpers.ext.azure;

import lombok.Data;

@Data
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