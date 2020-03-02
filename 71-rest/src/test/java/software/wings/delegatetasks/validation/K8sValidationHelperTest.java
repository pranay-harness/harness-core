package software.wings.delegatetasks.validation;

import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.filesystem.FileIo;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;
import software.wings.helpers.ext.k8s.request.K8sApplyTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.helpers.ext.k8s.request.K8sRollingDeployTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sScaleTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.kustomize.KustomizeConfig;
import software.wings.helpers.ext.kustomize.KustomizeConstants;

import java.io.IOException;

public class K8sValidationHelperTest extends WingsBaseTest {
  @Inject private K8sValidationHelper k8sValidationHelper;

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void testDoesKustomizePluginDirExist() throws IOException {
    final String userDir = System.getProperty("user.dir");
    FileIo.createDirectoryIfDoesNotExist(userDir + "/" + KustomizeConstants.KUSTOMIZE_PLUGIN_DIR_SUFFIX);

    assertThat(k8sValidationHelper.doesKustomizePluginDirExist(kustomizeConfigWithPluginPath(null))).isTrue();
    assertThat(k8sValidationHelper.doesKustomizePluginDirExist(kustomizeConfigWithPluginPath(EMPTY))).isTrue();
    assertThat(k8sValidationHelper.doesKustomizePluginDirExist(kustomizeConfigWithPluginPath(userDir))).isTrue();
    assertThat(k8sValidationHelper.doesKustomizePluginDirExist(kustomizeConfigWithPluginPath("$PWD"))).isTrue();
    assertThat(k8sValidationHelper.doesKustomizePluginDirExist(kustomizeConfigWithPluginPath("."))).isTrue();

    FileIo.deleteDirectoryAndItsContentIfExists(userDir + "/" + KustomizeConstants.KUSTOMIZE_PLUGIN_DIR_SUFFIX);

    assertThat(k8sValidationHelper.doesKustomizePluginDirExist(kustomizeConfigWithPluginPath(userDir))).isFalse();
    assertThat(k8sValidationHelper.doesKustomizePluginDirExist(kustomizeConfigWithPluginPath("$PWD"))).isFalse();
    assertThat(k8sValidationHelper.doesKustomizePluginDirExist(kustomizeConfigWithPluginPath("."))).isFalse();
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void getKustomizeCriteria() {
    assertThat(k8sValidationHelper.getKustomizeCriteria(kustomizeConfigWithPluginPath(null))).isNull();
    assertThat(k8sValidationHelper.getKustomizeCriteria(kustomizeConfigWithPluginPath(""))).isNull();
    assertThat(k8sValidationHelper.getKustomizeCriteria(kustomizeConfigWithPluginPath("plugin-path")))
        .isEqualTo("KustomizePluginDir:plugin-path");
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void testKustomizeValidationNeeded() {
    assertThat(k8sValidationHelper.kustomizeValidationNeeded(k8sScaleTaskParams())).isFalse();
    assertThat(k8sValidationHelper.kustomizeValidationNeeded(k8sTaskParamsWithNoKustomizeConfig())).isFalse();
    assertThat(k8sValidationHelper.kustomizeValidationNeeded(k8sTaskParamsWithKustomizePluginPath(null))).isTrue();
    assertThat(k8sValidationHelper.kustomizeValidationNeeded(k8sTaskParamsWithKustomizePluginPath(EMPTY))).isTrue();
    assertThat(k8sValidationHelper.kustomizeValidationNeeded(k8sTaskParamsWithKustomizePluginPath("foo"))).isTrue();
  }

  private K8sTaskParameters k8sScaleTaskParams() {
    return K8sScaleTaskParameters.builder().build();
  }

  private K8sTaskParameters k8sTaskParamsWithNoKustomizeConfig() {
    return K8sRollingDeployTaskParameters.builder()
        .k8sDelegateManifestConfig(K8sDelegateManifestConfig.builder().build())
        .build();
  }

  private K8sTaskParameters k8sTaskParamsWithKustomizePluginPath(String kustomizePluginPath) {
    return K8sApplyTaskParameters.builder()
        .k8sDelegateManifestConfig(
            K8sDelegateManifestConfig.builder()
                .kustomizeConfig(KustomizeConfig.builder().pluginRootDir(kustomizePluginPath).build())
                .build())
        .build();
  }

  private KustomizeConfig kustomizeConfigWithPluginPath(String kustomizePluginPath) {
    return KustomizeConfig.builder().pluginRootDir(kustomizePluginPath).build();
  }
}