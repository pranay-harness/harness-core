package software.wings.graphql.datafetcher.secretManager;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.Getter;

@TargetModule(Module._380_CG_GRAPHQL)
public enum SecretManagerDataFetchers {
  HASHICORP_VAULT_DATA_FETCHER;

  @Getter private final String name = this.name();
}
