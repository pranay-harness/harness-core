package io.harness.beans.yaml;

import static io.harness.rule.OwnerRule.ALEKSANDAR;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CiBeansTestBase;
import io.harness.beans.stages.IntegrationStage;
import io.harness.beans.steps.stepinfo.GitCloneStepInfo;
import io.harness.beans.steps.stepinfo.PluginStepInfo;
import io.harness.beans.steps.stepinfo.PublishStepInfo;
import io.harness.beans.steps.stepinfo.RunStepInfo;
import io.harness.beans.steps.stepinfo.TestIntelligenceStepInfo;
import io.harness.beans.steps.stepinfo.publish.artifact.DockerFileArtifact;
import io.harness.beans.steps.stepinfo.publish.artifact.connectors.GcrConnector;
import io.harness.beans.yaml.extended.CustomSecretVariable;
import io.harness.beans.yaml.extended.CustomTextVariable;
import io.harness.beans.yaml.extended.CustomVariable;
import io.harness.beans.yaml.extended.connector.GitConnectorYaml;
import io.harness.beans.yaml.extended.container.Container;
import io.harness.beans.yaml.extended.infrastrucutre.Infrastructure;
import io.harness.beans.yaml.extended.infrastrucutre.K8sDirectInfraYaml;
import io.harness.beans.yaml.extended.reports.JunitTestReport;
import io.harness.beans.yaml.extended.reports.UnitTestReport;
import io.harness.category.element.UnitTests;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.ngpipeline.pipeline.beans.yaml.NgPipeline;
import io.harness.pms.yaml.ParameterField;
import io.harness.rule.Owner;
import io.harness.yaml.core.ExecutionElement;
import io.harness.yaml.core.ParallelStepElement;
import io.harness.yaml.core.StageElement;
import io.harness.yaml.core.StepElement;
import io.harness.yaml.utils.YamlPipelineUtils;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CIPipelineYamlTest extends CiBeansTestBase {
  @Test
  @Owner(developers = ALEKSANDAR)
  @Category(UnitTests.class)
  public void testCiPipelineConversion() throws IOException {
    ClassLoader classLoader = this.getClass().getClassLoader();
    List<String> paths = Arrays.asList("rspec.xml", "reports.xml");
    JunitTestReport junitTestReport =
        JunitTestReport.builder().spec(JunitTestReport.Spec.builder().paths(paths).build()).build();
    List<UnitTestReport> unitTestReportList = Arrays.asList(junitTestReport);
    final URL testFile = classLoader.getResource("ci.yml");
    NgPipeline ngPipelineActual = YamlPipelineUtils.read(testFile, NgPipeline.class);
    NgPipeline ngPipelineExpected =
        NgPipeline.builder()
            .identifier("cipipeline")
            .name("Integration Pipeline")
            .description(ParameterField.createValueField("sample pipeline used for testing"))
            .stages(singletonList(
                StageElement.builder()
                    .identifier("masterBuildUpload")
                    .type("ci")
                    .stageType(
                        IntegrationStage.builder()
                            .identifier("masterBuildUpload")
                            .infrastructure(K8sDirectInfraYaml.builder()
                                                .type(Infrastructure.Type.KUBERNETES_DIRECT)
                                                .spec(K8sDirectInfraYaml.Spec.builder()
                                                          .connectorRef("MyKubeCluster1")
                                                          .namespace("ShoppingCart")
                                                          .build())
                                                .build())
                            .gitConnector(GitConnectorYaml.builder()
                                              .identifier("gitRepo")
                                              .type("git")
                                              .spec(GitConnectorYaml.Spec.builder()
                                                        .authScheme(GitConnectorYaml.Spec.AuthScheme.builder()
                                                                        .type("ssh")
                                                                        .sshKey("gitSsh")
                                                                        .build())
                                                        .repo("master")
                                                        .build())
                                              .build())
                            .container(Container.builder()
                                           .identifier("jenkinsSlaveImage")
                                           .connector("npquotecenter")
                                           .imagePath("us.gcr.io/platform-205701/jenkins-slave-portal-oracle-8u191:12")
                                           .build())
                            .customVariables(asList(
                                CustomTextVariable.builder().name("internalPath").value("{input}").build(),
                                CustomSecretVariable.builder()
                                    .name("runTimeVal")
                                    .type(CustomVariable.Type.SECRET)
                                    .value(SecretRefData.builder().scope(Scope.ACCOUNT).identifier("secret").build())
                                    .build()))
                            .execution(
                                ExecutionElement.builder()
                                    .steps(asList(StepElement.builder()
                                                      .identifier("gitClone")
                                                      .type("gitClone")
                                                      .stepSpecType(GitCloneStepInfo.builder()
                                                                        .identifier("gitClone")
                                                                        .gitConnector("gitGlobal")
                                                                        .path("portal")
                                                                        .branch("master")
                                                                        .build())
                                                      .build(),
                                        ParallelStepElement.builder()
                                            .sections(asList(StepElement.builder()
                                                                 .identifier("runLint")
                                                                 .type("run")
                                                                 .stepSpecType(RunStepInfo.builder()
                                                                                   .identifier("runLint")
                                                                                   .retry(2)
                                                                                   .timeout(30)
                                                                                   .command("./run lint")
                                                                                   .build())
                                                                 .build(),
                                                StepElement.builder()
                                                    .identifier("runAllUnitTests")
                                                    .type("run")
                                                    .stepSpecType(
                                                        RunStepInfo.builder()
                                                            .identifier("runAllUnitTests")
                                                            .retry(2)
                                                            .timeout(30)
                                                            .command(
                                                                "mvn -U clean package -Dbuild.number=${BUILD_NUMBER} -DgitBranch=master -DforkMode=perthread -DthreadCount=3 -DargLine=\"-Xmx2048m\"")
                                                            .reports(unitTestReportList)
                                                            .build())
                                                    .build(),
                                                StepElement.builder()
                                                    .identifier("runUnitTestsIntelligently")
                                                    .type("testIntelligence")
                                                    .stepSpecType(TestIntelligenceStepInfo.builder()
                                                                      .identifier("runUnitTestsIntelligently")
                                                                      .buildTool("maven")
                                                                      .goals("echo \"Running test\"")
                                                                      .language("java")
                                                                      .retry(2)
                                                                      .timeout(30)
                                                                      .build())
                                                    .build()

                                                    ))
                                            .build(),
                                        StepElement.builder()
                                            .identifier("generateReport")
                                            .type("run")
                                            .stepSpecType(RunStepInfo.builder()
                                                              .identifier("generateReport")
                                                              .retry(2)
                                                              .timeout(30)
                                                              .command("./ci/generate_report.sh")
                                                              .build())
                                            .build(),
                                        StepElement.builder()
                                            .identifier("buildMaster")
                                            .type("run")
                                            .stepSpecType(RunStepInfo.builder()
                                                              .identifier("buildMaster")
                                                              .retry(2)
                                                              .timeout(75)
                                                              .command("mvn clean install")
                                                              .build())
                                            .build(),
                                        StepElement.builder()
                                            .identifier("uploadArtifact")
                                            .type("publishArtifacts")
                                            .stepSpecType(
                                                PublishStepInfo.builder()
                                                    .identifier("uploadArtifact")
                                                    .publishArtifacts(singletonList(
                                                        DockerFileArtifact.builder()
                                                            .dockerFile("Dockerfile")
                                                            .context("workspace")
                                                            .image("ui")
                                                            .tag("1.0.0")
                                                            .buildArguments(
                                                                asList(DockerFileArtifact.BuildArgument.builder()
                                                                           .key("key1")
                                                                           .value("value1")
                                                                           .build(),
                                                                    DockerFileArtifact.BuildArgument.builder()
                                                                        .key("key2")
                                                                        .value("value2")
                                                                        .build()))
                                                            .connector(GcrConnector.builder()
                                                                           .connectorRef("myDockerRepoConnector")
                                                                           .location("eu.gcr.io/harness/ui:latest")
                                                                           .build())
                                                            .build()))
                                                    .build())
                                            .build(),
                                        StepElement.builder()
                                            .identifier("minio")
                                            .type("plugin")
                                            .stepSpecType(PluginStepInfo.builder()
                                                              .identifier("minio")
                                                              .image("plugins/s3")
                                                              .settings(ImmutableMap.<String, String>builder()
                                                                            .put("secret_key", "foo")
                                                                            .put("access_key", "bar")
                                                                            .build())
                                                              .build())
                                            .build()))
                                    .build())
                            .build())
                    .build()))
            .build();
    assertThat(ngPipelineActual).usingRecursiveComparison().isEqualTo(ngPipelineExpected);
  }
}
