package software.wings.delegatetasks.rancher;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class RancherGenerateKubeconfigResponse {
  private String type;
  private String config;
}