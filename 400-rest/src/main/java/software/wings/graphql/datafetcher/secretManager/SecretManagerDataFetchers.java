package software.wings.graphql.datafetcher.secretManager;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Getter;

@OwnedBy(PL)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public enum SecretManagerDataFetchers {
  HASHICORP_VAULT_DATA_FETCHER,
  CUSTOM_SECRET_MANAGER_DATA_FETCHER;

  @Getter private final String name = this.name();
}
