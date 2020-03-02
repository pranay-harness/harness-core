package software.wings.sm.states.k8s.kustomize;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;
import static io.harness.rule.OwnerRule.YOGESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.helpers.ext.kustomize.KustomizeConfig;
import software.wings.sm.ExecutionContext;

public class KustomizeHelperTest extends WingsBaseTest {
  @Mock private ExecutionContext executionContext;
  @Inject private KustomizeHelper kustomizeHelper;

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void renderKustomizeConfig() {
    final String pluginRootDirExp = "${serviceVariable.foo}";
    final String pluginRootDir = "./kustomize/plugins/";
    final String kustomizeDirExp = "${serviceVariable.path}";
    final String kustomizeDir = "./kustomize/";
    KustomizeConfig kustomizeConfig =
        KustomizeConfig.builder().pluginRootDir(pluginRootDirExp).kustomizeDirPath(kustomizeDirExp).build();

    when(executionContext.renderExpression(pluginRootDirExp)).thenReturn(pluginRootDir);
    when(executionContext.renderExpression(kustomizeDirExp)).thenReturn(kustomizeDir);

    kustomizeHelper.renderKustomizeConfig(executionContext, kustomizeConfig);

    assertThat(kustomizeConfig.getPluginRootDir()).isEqualTo(pluginRootDir);
    assertThat(kustomizeConfig.getKustomizeDirPath()).isEqualTo(kustomizeDir);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void renderNullKustomizeConfig() {
    KustomizeConfig kustomizeConfig = null;
    kustomizeHelper.renderKustomizeConfig(executionContext, kustomizeConfig);
    assertThat(kustomizeConfig).isNull();
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void renderKustomizeConfigShouldThrowExceptionWhenFailsForPluginPath() {
    final String pluginRootDirExp = "${serviceVariable.foo}";
    final String kustomizeDirExp = "${serviceVariable.path}";
    final String kustomizeDir = "./kustomize/";

    KustomizeConfig kustomizeConfig =
        KustomizeConfig.builder().pluginRootDir(pluginRootDirExp).kustomizeDirPath(kustomizeDirExp).build();

    when(executionContext.renderExpression(kustomizeDirExp)).thenReturn(kustomizeDir);
    when(executionContext.renderExpression(pluginRootDirExp)).thenReturn("null");

    assertThatThrownBy(() -> kustomizeHelper.renderKustomizeConfig(executionContext, kustomizeConfig))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Unable to render plugin root directory path");
  }

  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void renderKustomizeConfigShouldThrowExceptionWhenFailsForDirectoryPath() {
    final String kustomizeDirExp = "${serviceVariable.path}";

    KustomizeConfig kustomizeConfig = KustomizeConfig.builder().kustomizeDirPath(kustomizeDirExp).build();

    when(executionContext.renderExpression(kustomizeDirExp)).thenReturn(null);

    assertThatThrownBy(() -> kustomizeHelper.renderKustomizeConfig(executionContext, kustomizeConfig))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessageContaining("Unable to render kustomize directory path");
  }
}