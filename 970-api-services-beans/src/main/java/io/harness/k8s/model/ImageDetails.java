package io.harness.k8s.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageDetails {
  private String name;
  private String tag;
  private String sourceName;
  private String registryUrl;
  private String username;
  private String password;
  private String domainName;
}
