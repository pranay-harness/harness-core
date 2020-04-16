package software.wings.delegatetasks.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.joor.Reflect;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.beans.GcpKubernetesCluster;
import software.wings.beans.TaskType;
import software.wings.helpers.ext.k8s.request.K8sApplyTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sBlueGreenDeployTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sCanaryDeployTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sClusterConfig;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.helpers.ext.k8s.request.K8sDeleteTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sScaleTaskParameters;
import software.wings.helpers.ext.k8s.request.K8sTaskParameters;
import software.wings.helpers.ext.kustomize.KustomizeConfig;

import java.util.List;

public class K8sCommandValidationTest extends WingsBaseTest {
  @Mock K8sValidationHelper k8sValidationHelper;

  private static final String k8s_criteria = "k8s-criteria";
  private static final String kustomize_criteria = "kustomize-criteria";

  @Before
  public void setUp() {
    Mockito.when(k8sValidationHelper.getCriteria(Matchers.any(K8sClusterConfig.class))).thenReturn(k8s_criteria);
    Mockito.when(k8sValidationHelper.getKustomizeCriteria(Matchers.any(KustomizeConfig.class)))
        .thenReturn(kustomize_criteria);
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void validateWithNonManifestParameters() {
    K8sScaleTaskParameters taskParameters = K8sScaleTaskParameters.builder().build();
    DelegateTask task = prepareK8sDelegateTask(taskParameters);
    K8sCommandValidation validationTask = prepareValidationTask(task);
    List<DelegateConnectionResult> validate = validationTask.validate();
    verify(k8sValidationHelper, times(1)).getCriteria(any(K8sClusterConfig.class));
    assertThat(validate.get(0).getCriteria()).isEqualTo(k8s_criteria);
  }

  private K8sCommandValidation prepareValidationTask(DelegateTask task) {
    K8sCommandValidation commandValidationSpy = Mockito.spy(new K8sCommandValidation("delegate-id", task, null));
    Reflect.on(commandValidationSpy).set("k8sValidationHelper", k8sValidationHelper);
    return commandValidationSpy;
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void validateWithOnlyClusterConfig() {
    K8sApplyTaskParameters taskParameters = K8sApplyTaskParameters.builder().build();
    DelegateTask task = prepareK8sDelegateTask(taskParameters);
    K8sCommandValidation validationTask = prepareValidationTask(task);
    List<DelegateConnectionResult> validate = validationTask.validate();
    verify(k8sValidationHelper, times(1)).getCriteria(any(K8sClusterConfig.class));
    assertThat(validate.get(0).getCriteria()).isEqualTo(k8s_criteria);
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void validateWithKustomizeConfigButNoPlugins() {
    K8sCanaryDeployTaskParameters taskParameters =
        K8sCanaryDeployTaskParameters.builder()
            .k8sDelegateManifestConfig(
                K8sDelegateManifestConfig.builder().kustomizeConfig(new KustomizeConfig()).build())
            .build();
    DelegateTask task = prepareK8sDelegateTask(taskParameters);

    when(k8sValidationHelper.kustomizeValidationNeeded(taskParameters)).thenReturn(true);

    K8sCommandValidation validationTask = prepareValidationTask(task);
    List<DelegateConnectionResult> validate = validationTask.validate();
    verify(k8sValidationHelper, times(1)).getCriteria(any(K8sClusterConfig.class));
    verify(k8sValidationHelper, times(1)).getKustomizeCriteria(any(KustomizeConfig.class));
    assertThat(validate.get(0).getCriteria()).contains(k8s_criteria);
    assertThat(validate.get(0).getCriteria()).contains(kustomize_criteria);
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void validateWithKustomizeConfigWithPlugins() {
    K8sCanaryDeployTaskParameters taskParameters =
        K8sCanaryDeployTaskParameters.builder()
            .k8sDelegateManifestConfig(K8sDelegateManifestConfig.builder()
                                           .kustomizeConfig(KustomizeConfig.builder().pluginRootDir("abc").build())
                                           .build())
            .build();
    DelegateTask task = prepareK8sDelegateTask(taskParameters);
    K8sCommandValidation validationTask = prepareValidationTask(task);

    when(k8sValidationHelper.kustomizeValidationNeeded(taskParameters)).thenReturn(true);

    List<DelegateConnectionResult> validate = validationTask.validate();
    verify(k8sValidationHelper, times(1)).getCriteria(any(K8sClusterConfig.class));
    verify(k8sValidationHelper, times(1)).getKustomizeCriteria(any(KustomizeConfig.class));
    assertThat(validate.get(0).getCriteria()).contains(k8s_criteria);
    assertThat(validate.get(0).getCriteria()).contains(kustomize_criteria);
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void testGetCriteria() {
    testK8sCriteria();
    testK8sWithKustomizeCriteria();
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void testValidationIfClusterNotReachable() {
    K8sTaskParameters taskParameters = K8sDeleteTaskParameters.builder().build();
    DelegateTask task = prepareK8sDelegateTask(taskParameters);
    K8sCommandValidation validationTask = prepareValidationTask(task);

    List<DelegateConnectionResult> validationResult = validationTask.validate();
    assertThat(validationResult.get(0).getCriteria()).isEqualTo(k8s_criteria);
    assertThat(validationResult.get(0).isValidated()).isFalse();
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void testValidationIfKustomizePluginDirNotFound() {
    K8sCanaryDeployTaskParameters taskParameters =
        K8sCanaryDeployTaskParameters.builder()
            .k8sDelegateManifestConfig(K8sDelegateManifestConfig.builder()
                                           .kustomizeConfig(KustomizeConfig.builder().pluginRootDir("abc").build())
                                           .build())
            .build();
    DelegateTask task = prepareK8sDelegateTask(taskParameters);
    K8sCommandValidation validationTask = prepareValidationTask(task);

    when(k8sValidationHelper.validateContainerServiceParams(any(K8sClusterConfig.class))).thenReturn(true);
    when(k8sValidationHelper.kustomizeValidationNeeded(taskParameters)).thenReturn(true);

    List<DelegateConnectionResult> validationResult = validationTask.validate();
    assertThat(validationResult.get(0).getCriteria()).contains(k8s_criteria);
    assertThat(validationResult.get(0).getCriteria()).contains(kustomize_criteria);
    assertThat(validationResult.get(0).isValidated()).isFalse();
  }

  private void testK8sCriteria() {
    K8sTaskParameters taskParameters = K8sDeleteTaskParameters.builder().build();
    DelegateTask task = prepareK8sDelegateTask(taskParameters);
    K8sCommandValidation validationTask = prepareValidationTask(task);
    assertThat(validationTask.getCriteria().get(0)).isEqualTo(k8s_criteria);
  }

  private void testK8sWithKustomizeCriteria() {
    when(k8sValidationHelper.kustomizeValidationNeeded(any(K8sTaskParameters.class))).thenReturn(true);
    K8sBlueGreenDeployTaskParameters taskParameters =
        K8sBlueGreenDeployTaskParameters.builder()
            .k8sDelegateManifestConfig(K8sDelegateManifestConfig.builder()
                                           .kustomizeConfig(KustomizeConfig.builder().pluginRootDir("abc").build())
                                           .build())
            .build();
    DelegateTask task = prepareK8sDelegateTask(taskParameters);
    K8sCommandValidation validationTask = prepareValidationTask(task);
    assertThat(validationTask.getCriteria().get(0)).contains(k8s_criteria);
    assertThat(validationTask.getCriteria().get(0)).contains(kustomize_criteria);
  }

  private DelegateTask prepareK8sDelegateTask(K8sTaskParameters taskParameters) {
    taskParameters.setK8sClusterConfig(
        K8sClusterConfig.builder()
            .gcpKubernetesCluster(GcpKubernetesCluster.builder().clusterName("default").build())
            .namespace("default")
            .build());
    return DelegateTask.builder()
        .data(TaskData.builder()
                  .async(true)
                  .taskType(TaskType.K8S_COMMAND_TASK.name())
                  .parameters(new Object[] {taskParameters})
                  .build())
        .build();
  }
}