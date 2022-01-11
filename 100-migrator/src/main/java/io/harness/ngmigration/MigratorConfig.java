package io.harness.ngmigration;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static com.google.common.collect.ImmutableMap.of;

import io.harness.annotations.dev.OwnedBy;

import software.wings.app.MainConfiguration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Singleton;
import io.dropwizard.Configuration;
import io.dropwizard.bundles.assets.AssetsBundleConfiguration;
import io.dropwizard.bundles.assets.AssetsConfiguration;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@Singleton
@OwnedBy(CDC)
public class MigratorConfig extends Configuration implements AssetsBundleConfiguration {
  @JsonProperty
  private AssetsConfiguration assetsConfiguration =
      AssetsConfiguration.builder()
          .mimeTypes(of("js", "application/json; charset=UTF-8", "zip", "application/zip"))
          .build();

  private MainConfiguration cg;

  @Override
  public AssetsConfiguration getAssetsConfiguration() {
    return assetsConfiguration;
  }
}
