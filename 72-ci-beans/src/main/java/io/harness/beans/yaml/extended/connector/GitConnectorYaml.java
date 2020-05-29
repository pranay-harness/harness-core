package io.harness.beans.yaml.extended.connector;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.yaml.core.intfc.Connector;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName("git")
public class GitConnectorYaml implements Connector {
  @NotNull private String type;
  @NotNull private String identifier;
  @NotNull private Spec spec;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Spec {
    @NotNull private AuthScheme authScheme;
    @NotNull private String repo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AuthScheme {
      @NotNull private String type;
      @NotNull private String sshKey;
    }
  }
}
