package software.wings.helpers.ext.helm;

import static io.harness.rule.OwnerRule.YOGESH;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.delegate.command.CommandExecutionResult;
import io.harness.rule.Owner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.WingsBaseTest;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.helpers.ext.helm.HelmClientImpl.HelmCliResponse;
import software.wings.helpers.ext.helm.HelmConstants.HelmVersion;
import software.wings.helpers.ext.helm.request.HelmCommandRequest;
import software.wings.helpers.ext.helm.request.HelmInstallCommandRequest;
import software.wings.helpers.ext.helm.request.HelmRollbackCommandRequest;
import software.wings.service.intfc.k8s.delegate.K8sGlobalConfigService;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

public class HelmClientImplTest extends WingsBaseTest {
  @Mock private K8sGlobalConfigService k8sGlobalConfigService;
  @InjectMocks private HelmClientImpl helmClient = Mockito.spy(HelmClientImpl.class);
  private ArgumentCaptor<String> stringCaptor = ArgumentCaptor.forClass(String.class);
  private HelmInstallCommandRequest helmInstallCommandRequest = HelmInstallCommandRequest.builder().build();
  private HelmRollbackCommandRequest helmRollbackCommandRequest = HelmRollbackCommandRequest.builder().build();
  private ExecutionLogCallback executionLogCallback = Mockito.mock(ExecutionLogCallback.class);

  @Before
  public void setup() throws InterruptedException, TimeoutException, IOException {
    doReturn(
        HelmCliResponse.builder().commandExecutionStatus(CommandExecutionResult.CommandExecutionStatus.SUCCESS).build())
        .when(helmClient)
        .executeHelmCLICommand(anyString(), anyLong());
    buildHelmInstallCommandRequest();
    buildHelmRollbackCommandRequest();
    when(k8sGlobalConfigService.getHelmPath(any(HelmVersion.class))).thenReturn("/client-tools/v3.1/helm");
  }

  @After
  public void after() {
    verify(k8sGlobalConfigService, never()).getHelmPath(HelmVersion.V2);
  }

  private void buildHelmRollbackCommandRequest() {
    helmRollbackCommandRequest.setNewReleaseVersion(3);
    helmRollbackCommandRequest.setRollbackVersion(1);
    helmRollbackCommandRequest.setPrevReleaseVersion(2);
    helmRollbackCommandRequest.setTimeoutInMillis(10000);
    helmRollbackCommandRequest.setKubeConfigLocation("~/.kube/dummy-config");
    helmRollbackCommandRequest.setExecutionLogCallback(executionLogCallback);
    helmRollbackCommandRequest.setReleaseName("best-release-ever");
    helmRollbackCommandRequest.setChartSpecification(
        HelmChartSpecification.builder().chartUrl("https://abc.com").chartName("redis").chartVersion("1.1.1").build());
    helmRollbackCommandRequest.setCommandFlags(null);
  }

  private void buildHelmInstallCommandRequest() {
    helmInstallCommandRequest.setExecutionLogCallback(executionLogCallback);
    helmInstallCommandRequest.setReleaseName("crazy-helm");
    helmInstallCommandRequest.setChartSpecification(HelmChartSpecification.builder()
                                                        .chartVersion("0.0.1")
                                                        .chartName("harness")
                                                        .chartUrl("https://oci-registry")
                                                        .build());
    helmInstallCommandRequest.setNamespace("helm-namespace");
    helmInstallCommandRequest.setKubeConfigLocation("~/.kube/dummy-config");
    helmInstallCommandRequest.setVariableOverridesYamlFiles(asList("value-0.yaml", "value-1.yaml"));
    helmInstallCommandRequest.setRepoName("stable");
    helmInstallCommandRequest.setCommandFlags(null);
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void upgrade() throws Exception {
    ConsumerWrapper<HelmCommandRequest> command = r -> helmClient.upgrade((HelmInstallCommandRequest) r);
    assertThat(getCommandWithNoKubeConfig(HelmVersion.V2, command, helmInstallCommandRequest))
        .isEqualTo(
            "helm upgrade  crazy-helm harness --repo https://oci-registry --version 0.0.1  -f ./repository/helm/overrides/3d6bbbe972d7519aa70587fc065139e1.yaml -f ./repository/helm/overrides/e8073c3baf625e6ea83327282c26f1a6.yaml");
    assertThat(getCommandWithNoValueOverride(HelmVersion.V2, command, helmInstallCommandRequest))
        .isEqualTo(
            "KUBECONFIG=~/.kube/dummy-config helm upgrade  crazy-helm harness --repo https://oci-registry --version 0.0.1");
    assertThat(getCommandWithCommandFlags(HelmVersion.V2, command, helmInstallCommandRequest))
        .isEqualTo(
            "KUBECONFIG=~/.kube/dummy-config helm upgrade --debug --tls crazy-helm harness --repo https://oci-registry --version 0.0.1  -f ./repository/helm/overrides/3d6bbbe972d7519aa70587fc065139e1.yaml -f ./repository/helm/overrides/e8073c3baf625e6ea83327282c26f1a6.yaml");
    assertThat(getCommandWithNoKubeConfig(HelmVersion.V3, command, helmInstallCommandRequest))
        .isEqualTo(
            "/client-tools/v3.1/helm upgrade  crazy-helm harness --repo https://oci-registry --version 0.0.1  -f ./repository/helm/overrides/3d6bbbe972d7519aa70587fc065139e1.yaml -f ./repository/helm/overrides/e8073c3baf625e6ea83327282c26f1a6.yaml");
    assertThat(getCommandWithNoValueOverride(HelmVersion.V3, command, helmInstallCommandRequest))
        .isEqualTo(
            "KUBECONFIG=~/.kube/dummy-config /client-tools/v3.1/helm upgrade  crazy-helm harness --repo https://oci-registry --version 0.0.1");
    assertThat(getCommandWithCommandFlags(HelmVersion.V3, command, helmInstallCommandRequest))
        .isEqualTo(
            "KUBECONFIG=~/.kube/dummy-config /client-tools/v3.1/helm upgrade --debug --tls crazy-helm harness --repo https://oci-registry --version 0.0.1  -f ./repository/helm/overrides/3d6bbbe972d7519aa70587fc065139e1.yaml -f ./repository/helm/overrides/e8073c3baf625e6ea83327282c26f1a6.yaml");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void install() throws Exception {
    ConsumerWrapper<HelmCommandRequest> command = r -> helmClient.install((HelmInstallCommandRequest) r);
    assertThat(getCommandWithNoKubeConfig(HelmVersion.V2, command, helmInstallCommandRequest))
        .isEqualTo(
            "helm install  harness --repo https://oci-registry --version 0.0.1  -f ./repository/helm/overrides/3d6bbbe972d7519aa70587fc065139e1.yaml -f ./repository/helm/overrides/e8073c3baf625e6ea83327282c26f1a6.yaml --name crazy-helm --namespace helm-namespace");
    assertThat(getCommandWithNoValueOverride(HelmVersion.V2, command, helmInstallCommandRequest))
        .isEqualTo(
            "KUBECONFIG=~/.kube/dummy-config helm install  harness --repo https://oci-registry --version 0.0.1  --name crazy-helm --namespace helm-namespace");
    assertThat(getCommandWithCommandFlags(HelmVersion.V2, command, helmInstallCommandRequest))
        .isEqualTo(
            "KUBECONFIG=~/.kube/dummy-config helm install --debug --tls harness --repo https://oci-registry --version 0.0.1  -f ./repository/helm/overrides/3d6bbbe972d7519aa70587fc065139e1.yaml -f ./repository/helm/overrides/e8073c3baf625e6ea83327282c26f1a6.yaml --name crazy-helm --namespace helm-namespace");
    assertThat(getCommandWithNoKubeConfig(HelmVersion.V3, command, helmInstallCommandRequest))
        .isEqualTo(
            "/client-tools/v3.1/helm install  crazy-helm harness --repo https://oci-registry --version 0.0.1    -f ./repository/helm/overrides/3d6bbbe972d7519aa70587fc065139e1.yaml -f ./repository/helm/overrides/e8073c3baf625e6ea83327282c26f1a6.yaml --namespace helm-namespace");
    assertThat(getCommandWithNoValueOverride(HelmVersion.V3, command, helmInstallCommandRequest))
        .isEqualTo(
            "KUBECONFIG=~/.kube/dummy-config /client-tools/v3.1/helm install  crazy-helm harness --repo https://oci-registry --version 0.0.1    --namespace helm-namespace");
    assertThat(getCommandWithCommandFlags(HelmVersion.V3, command, helmInstallCommandRequest))
        .isEqualTo(
            "KUBECONFIG=~/.kube/dummy-config /client-tools/v3.1/helm install  crazy-helm harness --repo https://oci-registry --version 0.0.1  --debug --tls  -f ./repository/helm/overrides/3d6bbbe972d7519aa70587fc065139e1.yaml -f ./repository/helm/overrides/e8073c3baf625e6ea83327282c26f1a6.yaml --namespace helm-namespace");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void rollback() throws Exception {
    ConsumerWrapper<HelmCommandRequest> command = r -> helmClient.rollback((HelmRollbackCommandRequest) r);
    assertThat(getCommandWithNoKubeConfig(HelmVersion.V2, command, helmRollbackCommandRequest))
        .isEqualTo("helm rollback  best-release-ever 2");
    assertThat(getCommandWithCommandFlags(HelmVersion.V2, command, helmRollbackCommandRequest))
        .isEqualTo("KUBECONFIG=~/.kube/dummy-config helm rollback --debug --tls best-release-ever 2");
    assertThat(getCommandWithNoKubeConfig(HelmVersion.V3, command, helmRollbackCommandRequest))
        .isEqualTo("/client-tools/v3.1/helm rollback  best-release-ever 2");
    assertThat(getCommandWithCommandFlags(HelmVersion.V3, command, helmRollbackCommandRequest))
        .isEqualTo(
            "KUBECONFIG=~/.kube/dummy-config /client-tools/v3.1/helm rollback  best-release-ever 2 --debug --tls");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void releaseHistory() throws Exception {
    ConsumerWrapper<HelmCommandRequest> command = helmClient::releaseHistory;
    assertThat(getCommandWithNoKubeConfig(HelmVersion.V2, command, helmInstallCommandRequest))
        .isEqualTo("helm hist  crazy-helm --max 5");
    assertThat(getCommandWithNoValueOverride(HelmVersion.V2, command, helmInstallCommandRequest))
        .isEqualTo("KUBECONFIG=~/.kube/dummy-config helm hist  crazy-helm --max 5");
    assertThat(getCommandWithCommandFlags(HelmVersion.V2, command, helmInstallCommandRequest))
        .isEqualTo("KUBECONFIG=~/.kube/dummy-config helm hist --debug --tls crazy-helm --max 5");
    assertThat(getCommandWithNoKubeConfig(HelmVersion.V3, command, helmInstallCommandRequest))
        .isEqualTo("/client-tools/v3.1/helm hist crazy-helm   --max 5");
    assertThat(getCommandWithNoValueOverride(HelmVersion.V3, command, helmInstallCommandRequest))
        .isEqualTo("KUBECONFIG=~/.kube/dummy-config /client-tools/v3.1/helm hist crazy-helm   --max 5");
    assertThat(getCommandWithCommandFlags(HelmVersion.V3, command, helmInstallCommandRequest))
        .isEqualTo("KUBECONFIG=~/.kube/dummy-config /client-tools/v3.1/helm hist crazy-helm --debug --tls  --max 5");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void listReleases() throws Exception {
    ConsumerWrapper<HelmCommandRequest> command = r -> helmClient.listReleases((HelmInstallCommandRequest) r);
    assertThat(getCommandWithNoKubeConfig(HelmVersion.V2, command, helmInstallCommandRequest))
        .isEqualTo("helm list  ^crazy-helm$");
    assertThat(getCommandWithNoValueOverride(HelmVersion.V2, command, helmInstallCommandRequest))
        .isEqualTo("KUBECONFIG=~/.kube/dummy-config helm list  ^crazy-helm$");
    assertThat(getCommandWithCommandFlags(HelmVersion.V2, command, helmInstallCommandRequest))
        .isEqualTo("KUBECONFIG=~/.kube/dummy-config helm list --debug --tls ^crazy-helm$");
    assertThat(getCommandWithNoKubeConfig(HelmVersion.V3, command, helmInstallCommandRequest))
        .isEqualTo("/client-tools/v3.1/helm list  --filter ^crazy-helm$");
    assertThat(getCommandWithNoValueOverride(HelmVersion.V3, command, helmInstallCommandRequest))
        .isEqualTo("KUBECONFIG=~/.kube/dummy-config /client-tools/v3.1/helm list  --filter ^crazy-helm$");
    assertThat(getCommandWithCommandFlags(HelmVersion.V3, command, helmInstallCommandRequest))
        .isEqualTo("KUBECONFIG=~/.kube/dummy-config /client-tools/v3.1/helm list --debug --tls --filter ^crazy-helm$");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void getClientAndServerVersion() throws Exception {
    ConsumerWrapper<HelmCommandRequest> command = helmClient::getClientAndServerVersion;
    assertThat(getCommandWithNoKubeConfig(HelmVersion.V2, command, helmInstallCommandRequest))
        .isEqualTo("helm version --short");
    assertThat(getCommandWithNoValueOverride(HelmVersion.V2, command, helmInstallCommandRequest))
        .isEqualTo("KUBECONFIG=~/.kube/dummy-config helm version --short");
    assertThat(getCommandWithCommandFlags(HelmVersion.V2, command, helmInstallCommandRequest))
        .isEqualTo("KUBECONFIG=~/.kube/dummy-config helm version --short --debug --tls");
    assertThat(getCommandWithNoKubeConfig(HelmVersion.V3, command, helmInstallCommandRequest))
        .isEqualTo("helm version --short");
    assertThat(getCommandWithNoValueOverride(HelmVersion.V3, command, helmInstallCommandRequest))
        .isEqualTo("KUBECONFIG=~/.kube/dummy-config helm version --short");
    assertThat(getCommandWithCommandFlags(HelmVersion.V3, command, helmInstallCommandRequest))
        .isEqualTo("KUBECONFIG=~/.kube/dummy-config helm version --short --debug --tls");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void addPublicRepo() throws Exception {
    ConsumerWrapper<HelmCommandRequest> command = helmClient::addPublicRepo;
    assertThat(getCommandWithNoKubeConfig(HelmVersion.V2, command, helmInstallCommandRequest))
        .isEqualTo("helm repo add stable https://oci-registry");
    assertThat(getCommandWithNoValueOverride(HelmVersion.V2, command, helmInstallCommandRequest))
        .isEqualTo("KUBECONFIG=~/.kube/dummy-config helm repo add stable https://oci-registry");
    assertThat(getCommandWithCommandFlags(HelmVersion.V2, command, helmInstallCommandRequest))
        .isEqualTo("KUBECONFIG=~/.kube/dummy-config helm repo add stable https://oci-registry");
    assertThat(getCommandWithNoKubeConfig(HelmVersion.V3, command, helmInstallCommandRequest))
        .isEqualTo("/client-tools/v3.1/helm repo add stable https://oci-registry");
    assertThat(getCommandWithNoValueOverride(HelmVersion.V3, command, helmInstallCommandRequest))
        .isEqualTo("/client-tools/v3.1/helm repo add stable https://oci-registry");
    assertThat(getCommandWithCommandFlags(HelmVersion.V3, command, helmInstallCommandRequest))
        .isEqualTo("/client-tools/v3.1/helm repo add stable https://oci-registry");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void repoUpdate() throws Exception {
    ConsumerWrapper<HelmCommandRequest> command = helmClient::repoUpdate;
    assertThat(getCommandWithNoKubeConfig(HelmVersion.V2, command, helmInstallCommandRequest))
        .isEqualTo("helm repo update");
    assertThat(getCommandWithNoValueOverride(HelmVersion.V2, command, helmInstallCommandRequest))
        .isEqualTo("KUBECONFIG=~/.kube/dummy-config helm repo update");
    assertThat(getCommandWithCommandFlags(HelmVersion.V2, command, helmInstallCommandRequest))
        .isEqualTo("KUBECONFIG=~/.kube/dummy-config helm repo update");
    assertThat(getCommandWithNoKubeConfig(HelmVersion.V3, command, helmInstallCommandRequest))
        .isEqualTo("/client-tools/v3.1/helm repo update");
    assertThat(getCommandWithNoValueOverride(HelmVersion.V3, command, helmInstallCommandRequest))
        .isEqualTo("/client-tools/v3.1/helm repo update");
    assertThat(getCommandWithCommandFlags(HelmVersion.V3, command, helmInstallCommandRequest))
        .isEqualTo("/client-tools/v3.1/helm repo update");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void getHelmRepoList() throws Exception {
    ConsumerWrapper<HelmCommandRequest> command = helmClient::getHelmRepoList;
    assertThat(getCommandWithNoKubeConfig(HelmVersion.V2, command, helmInstallCommandRequest))
        .isEqualTo("helm repo list");
    assertThat(getCommandWithNoValueOverride(HelmVersion.V2, command, helmInstallCommandRequest))
        .isEqualTo("KUBECONFIG=~/.kube/dummy-config helm repo list");
    assertThat(getCommandWithCommandFlags(HelmVersion.V2, command, helmInstallCommandRequest))
        .isEqualTo("KUBECONFIG=~/.kube/dummy-config helm repo list");
    assertThat(getCommandWithNoKubeConfig(HelmVersion.V3, command, helmInstallCommandRequest))
        .isEqualTo("/client-tools/v3.1/helm repo list");
    assertThat(getCommandWithNoValueOverride(HelmVersion.V3, command, helmInstallCommandRequest))
        .isEqualTo("/client-tools/v3.1/helm repo list");
    assertThat(getCommandWithCommandFlags(HelmVersion.V3, command, helmInstallCommandRequest))
        .isEqualTo("/client-tools/v3.1/helm repo list");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void deleteHelmRelease() throws Exception {
    ConsumerWrapper<HelmCommandRequest> command = helmClient::deleteHelmRelease;
    assertThat(getCommandWithNoKubeConfig(HelmVersion.V2, command, helmInstallCommandRequest))
        .isEqualTo("helm delete  --purge crazy-helm");
    assertThat(getCommandWithNoValueOverride(HelmVersion.V2, command, helmInstallCommandRequest))
        .isEqualTo("KUBECONFIG=~/.kube/dummy-config helm delete  --purge crazy-helm");
    assertThat(getCommandWithCommandFlags(HelmVersion.V2, command, helmInstallCommandRequest))
        .isEqualTo("KUBECONFIG=~/.kube/dummy-config helm delete --debug --tls --purge crazy-helm");
    assertThat(getCommandWithNoKubeConfig(HelmVersion.V3, command, helmInstallCommandRequest))
        .isEqualTo("/client-tools/v3.1/helm uninstall crazy-helm");
    assertThat(getCommandWithNoValueOverride(HelmVersion.V3, command, helmInstallCommandRequest))
        .isEqualTo("KUBECONFIG=~/.kube/dummy-config /client-tools/v3.1/helm uninstall crazy-helm");
    assertThat(getCommandWithCommandFlags(HelmVersion.V3, command, helmInstallCommandRequest))
        .isEqualTo("KUBECONFIG=~/.kube/dummy-config /client-tools/v3.1/helm uninstall crazy-helm --debug --tls");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void searchChart() throws Exception {
    ConsumerWrapper<HelmCommandRequest> command =
        r -> helmClient.searchChart((HelmInstallCommandRequest) r, "harness-helm");
    assertThat(getCommandWithNoKubeConfig(HelmVersion.V2, command, helmInstallCommandRequest))
        .isEqualTo("helm search harness-helm");
    assertThat(getCommandWithNoValueOverride(HelmVersion.V2, command, helmInstallCommandRequest))
        .isEqualTo("KUBECONFIG=~/.kube/dummy-config helm search harness-helm");
    assertThat(getCommandWithCommandFlags(HelmVersion.V2, command, helmInstallCommandRequest))
        .isEqualTo("KUBECONFIG=~/.kube/dummy-config helm search harness-helm");
    assertThat(getCommandWithNoKubeConfig(HelmVersion.V3, command, helmInstallCommandRequest))
        .isEqualTo("/client-tools/v3.1/helm search repo harness-helm");
    assertThat(getCommandWithNoValueOverride(HelmVersion.V3, command, helmInstallCommandRequest))
        .isEqualTo("/client-tools/v3.1/helm search repo harness-helm");
    assertThat(getCommandWithCommandFlags(HelmVersion.V3, command, helmInstallCommandRequest))
        .isEqualTo("/client-tools/v3.1/helm search repo harness-helm");
  }

  private String getCommandWithCommandFlags(HelmVersion helmVersion, ConsumerWrapper<HelmCommandRequest> consumer,
      HelmCommandRequest request) throws Exception {
    request.setHelmVersion(helmVersion);
    request.setCommandFlags("--debug --tls");
    return getHelmCommandPassedToExecutor(consumer, request);
  }

  private void testWithoutKubeConfig(ConsumerWrapper<HelmCommandRequest> consumer) throws Exception {
    String command_v2 = getCommandWithNoKubeConfig(HelmVersion.V2, consumer, helmInstallCommandRequest);
    String command_v3 = getCommandWithNoKubeConfig(HelmVersion.V3, consumer, helmInstallCommandRequest);
    assertThat(command_v2)
        .isEqualTo(
            "helm install  harness --repo https://oci-registry --version 0.0.1  --name crazy-helm --namespace helm-namespace");
    assertThat(command_v3)
        .isEqualTo(
            "/client-tools/v3.1/helm install  crazy-helm harness --repo https://oci-registry --version 0.0.1    --namespace helm-namespace");
  }

  private String getCommandWithNoKubeConfig(HelmVersion helmVersion, ConsumerWrapper<HelmCommandRequest> consumer,
      HelmCommandRequest request) throws Exception {
    request.setHelmVersion(helmVersion);
    request.setKubeConfigLocation(null);
    return getHelmCommandPassedToExecutor(consumer, request);
  }

  private void testWithoutValueOverride(ConsumerWrapper<HelmCommandRequest> consumer) throws Exception {
    String command_v2 = getCommandWithNoValueOverride(HelmVersion.V2, consumer, helmInstallCommandRequest);
    String command_v3 = getCommandWithNoValueOverride(HelmVersion.V3, consumer, helmInstallCommandRequest);
    assertThat(command_v2)
        .isEqualTo(
            "KUBECONFIG=~/.kube/dummy-config helm install  harness --repo https://oci-registry --version 0.0.1  --name crazy-helm --namespace helm-namespace");
    assertThat(command_v3)
        .isEqualTo(
            "KUBECONFIG=~/.kube/dummy-config /client-tools/v3.1/helm install  crazy-helm harness --repo https://oci-registry --version 0.0.1    --namespace helm-namespace");
  }

  private String getCommandWithNoValueOverride(HelmVersion helmVersion, ConsumerWrapper<HelmCommandRequest> consumer,
      HelmInstallCommandRequest request) throws Exception {
    request.setHelmVersion(helmVersion);
    request.setVariableOverridesYamlFiles(null);
    return getHelmCommandPassedToExecutor(consumer, request).trim();
  }

  ;

  private String getHelmCommandPassedToExecutor(
      ConsumerWrapper<HelmCommandRequest> consumer, HelmCommandRequest request) throws Exception {
    consumer.accept(request);
    verify(helmClient, Mockito.atLeastOnce()).executeHelmCLICommand(stringCaptor.capture(), anyLong());
    buildHelmInstallCommandRequest();
    buildHelmRollbackCommandRequest();
    return stringCaptor.getValue();
  }

  private void testWithoutValueOverrideV3()
      throws InterruptedException, TimeoutException, IOException, ExecutionException {
    helmInstallCommandRequest.setHelmVersion(HelmVersion.V3);
    helmInstallCommandRequest.setVariableOverridesYamlFiles(null);
    helmClient.install(helmInstallCommandRequest);
    verify(helmClient, Mockito.times(1)).executeHelmCLICommand(stringCaptor.capture());
    String command = stringCaptor.getValue();
    assertThat(command).doesNotContain("$");
  }

  private void testInstallV2() throws InterruptedException, ExecutionException, TimeoutException, IOException {
    helmInstallCommandRequest.setHelmVersion(HelmVersion.V2);
    helmClient.install(helmInstallCommandRequest);
    verify(helmClient, Mockito.times(1)).executeHelmCLICommand(stringCaptor.capture());
    String command = stringCaptor.getValue();
    assertThat(command).doesNotContain("$");
  }

  private void testInstallV3() {
    helmInstallCommandRequest.setHelmVersion(HelmVersion.V3);
  }

  @FunctionalInterface
  public interface ConsumerWrapper<T> {
    void accept(T t) throws Exception;
  }
}