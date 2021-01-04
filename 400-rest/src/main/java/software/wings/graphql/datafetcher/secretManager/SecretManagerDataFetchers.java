package software.wings.graphql.datafetcher.secretManager;

import lombok.Getter;

public enum SecretManagerDataFetchers {
  HASHICORP_VAULT_DATA_FETCHER;

  @Getter private final String name = this.name();
}
