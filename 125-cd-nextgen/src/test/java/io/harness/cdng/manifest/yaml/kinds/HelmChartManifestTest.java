package io.harness.cdng.manifest.yaml.kinds;

import static io.harness.steps.shellScript.manifest.yaml.HelmCommandFlagType.Fetch;
import static io.harness.steps.shellScript.manifest.yaml.HelmCommandFlagType.Template;
import static io.harness.steps.shellScript.manifest.yaml.HelmCommandFlagType.Version;
import static io.harness.rule.OwnerRule.ABOSII;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.steps.shellScript.manifest.yaml.HelmManifestCommandFlag;
import io.harness.cdng.manifest.yaml.StoreConfigWrapper;
import io.harness.k8s.model.HelmVersion;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;

import java.util.Arrays;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;

public class HelmChartManifestTest extends CategoryTest {
  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testApplyOverrides() {
    StoreConfigWrapper originalStoreConfig = Mockito.mock(StoreConfigWrapper.class);
    StoreConfigWrapper overrideStoreConfig = Mockito.mock(StoreConfigWrapper.class);
    HelmChartManifest original = HelmChartManifest.builder()
                                     .helmVersion(HelmVersion.V3)
                                     .skipResourceVersioning(ParameterField.createValueField(true))
                                     .storeConfigWrapper(originalStoreConfig)
                                     .commandFlags(Arrays.asList(HelmManifestCommandFlag.builder()
                                                                     .commandType(Version)
                                                                     .flag(ParameterField.createValueField("--version"))
                                                                     .build(),
                                         HelmManifestCommandFlag.builder()
                                             .commandType(Fetch)
                                             .flag(ParameterField.createValueField("--debug"))
                                             .build()))
                                     .build();

    HelmChartManifest override =
        HelmChartManifest.builder()
            .helmVersion(HelmVersion.V2)
            .skipResourceVersioning(ParameterField.createValueField(false))
            .storeConfigWrapper(overrideStoreConfig)
            .commandFlags(Arrays.asList(HelmManifestCommandFlag.builder()
                                            .commandType(Template)
                                            .flag(ParameterField.createValueField("--template"))
                                            .build(),
                HelmManifestCommandFlag.builder()
                    .commandType(Fetch)
                    .flag(ParameterField.createValueField("--updated-debug"))
                    .build()))
            .build();

    doReturn(overrideStoreConfig).when(originalStoreConfig).applyOverrides(overrideStoreConfig);

    HelmChartManifest result = (HelmChartManifest) original.applyOverrides(override);

    assertThat(original.getHelmVersion()).isEqualTo(HelmVersion.V3);
    assertThat(original.getSkipResourceVersioning().getValue()).isTrue();
    assertThat(original.getStoreConfigWrapper()).isEqualTo(originalStoreConfig);
    assertThat(original.getCommandFlags().stream().map(HelmManifestCommandFlag::getCommandType))
        .containsExactlyInAnyOrder(Version, Fetch);
    assertThat(original.getCommandFlags().stream().map(HelmManifestCommandFlag::getFlag).map(ParameterField::getValue))
        .containsExactlyInAnyOrder("--version", "--debug");

    assertThat(override.getHelmVersion()).isEqualTo(HelmVersion.V2);
    assertThat(override.getSkipResourceVersioning().getValue()).isFalse();
    assertThat(override.getStoreConfigWrapper()).isEqualTo(overrideStoreConfig);
    assertThat(override.getCommandFlags().stream().map(HelmManifestCommandFlag::getCommandType))
        .containsExactlyInAnyOrder(Template, Fetch);
    assertThat(override.getCommandFlags().stream().map(HelmManifestCommandFlag::getFlag).map(ParameterField::getValue))
        .containsExactlyInAnyOrder("--template", "--updated-debug");

    assertThat(result.getHelmVersion()).isEqualTo(HelmVersion.V2);
    assertThat(result.getSkipResourceVersioning().getValue()).isFalse();
    assertThat(result.getStoreConfigWrapper()).isEqualTo(overrideStoreConfig);
    assertThat(result.getCommandFlags().stream().map(HelmManifestCommandFlag::getCommandType))
        .containsExactlyInAnyOrder(Template, Fetch);
    assertThat(result.getCommandFlags().stream().map(HelmManifestCommandFlag::getFlag).map(ParameterField::getValue))
        .containsExactlyInAnyOrder("--template", "--updated-debug");
  }

  @Test
  @Owner(developers = ABOSII)
  @Category(UnitTests.class)
  public void testApplyOverridesWithNulls() {
    StoreConfigWrapper storeConfig = Mockito.mock(StoreConfigWrapper.class);
    HelmChartManifest original =
        HelmChartManifest.builder()
            .skipResourceVersioning(ParameterField.createValueField(true))
            .storeConfigWrapper(storeConfig)
            .helmVersion(HelmVersion.V3)
            .commandFlags(Arrays.asList(HelmManifestCommandFlag.builder()
                                            .commandType(Template)
                                            .flag(ParameterField.createValueField("--template"))
                                            .build(),
                HelmManifestCommandFlag.builder()
                    .commandType(Fetch)
                    .flag(ParameterField.createValueField("--debug"))
                    .build()))
            .build();

    HelmChartManifest override = HelmChartManifest.builder().build();

    HelmChartManifest result = (HelmChartManifest) original.applyOverrides(override);

    assertThat(original.getHelmVersion()).isEqualTo(HelmVersion.V3);
    assertThat(original.getSkipResourceVersioning().getValue()).isTrue();
    assertThat(original.getCommandFlags().stream().map(HelmManifestCommandFlag::getCommandType))
        .containsExactlyInAnyOrder(Template, Fetch);
    assertThat(original.getCommandFlags().stream().map(HelmManifestCommandFlag::getFlag).map(ParameterField::getValue))
        .containsExactlyInAnyOrder("--template", "--debug");

    assertThat(result).isEqualTo(original);
  }
}