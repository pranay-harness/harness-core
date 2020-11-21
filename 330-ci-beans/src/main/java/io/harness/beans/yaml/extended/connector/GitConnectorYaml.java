package io.harness.beans.yaml.extended.connector;

import io.harness.data.validator.EntityIdentifier;
import io.harness.yaml.core.intfc.Connector;

import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName("git")
public class GitConnectorYaml implements Connector {
  @NotNull private String type;
  @NotNull @EntityIdentifier private String identifier;
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
