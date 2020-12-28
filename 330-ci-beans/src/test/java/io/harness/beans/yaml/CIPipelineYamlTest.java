package io.harness.beans.yaml;

import io.harness.CiBeansTestBase;

public class CIPipelineYamlTest extends CiBeansTestBase {
  //  @Test
  //  @Owner(developers = ALEKSANDAR)
  //  @Category(UnitTests.class)
  //  public void testCiPipelineConversion() throws IOException {
  //    ClassLoader classLoader = this.getClass().getClassLoader();
  //    List<String> paths = Arrays.asList("rspec.xml", "reports.xml");
  //    JunitTestReport junitTestReport =
  //        JunitTestReport.builder().spec(JunitTestReport.Spec.builder().paths(paths).build()).build();
  //    List<UnitTestReport> unitTestReportList = Arrays.asList(junitTestReport);
  //    final URL testFile = classLoader.getResource("ci.yml");
  //    NgPipeline ngPipelineActual = YamlPipelineUtils.read(testFile, NgPipeline.class);
  //    NgPipeline ngPipelineExpected =
  //        NgPipeline.builder()
  //            .identifier("cipipeline")
  //            .name("Integration Pipeline")
  //            .description(ParameterField.createValueField("sample pipeline used for testing"))
  //            .stages(singletonList(
  //                StageElement.builder()
  //                    .identifier("masterBuildUpload")
  //                    .type("CI")
  //                    .stageType(
  //                        IntegrationStage.builder()
  //                            .identifier("masterBuildUpload")
  //                            .infrastructure(K8sDirectInfraYaml.builder()
  //                                                .type(Infrastructure.Type.KUBERNETES_DIRECT)
  //                                                .spec(K8sDirectInfraYaml.Spec.builder()
  //                                                          .connectorRef("MyKubeCluster1")
  //                                                          .namespace("ShoppingCart")
  //                                                          .build())
  //                                                .build())
  //                            .gitConnector(GitConnectorYaml.builder()
  //                                              .identifier("gitRepo")
  //                                              .type("git")
  //                                              .spec(GitConnectorYaml.Spec.builder()
  //                                                        .authScheme(GitConnectorYaml.Spec.AuthScheme.builder()
  //                                                                        .type("ssh")
  //                                                                        .sshKey("gitSsh")
  //                                                                        .build())
  //                                                        .repo("master")
  //                                                        .build())
  //                                              .build())
  //                            .container(Container.builder()
  //                                           .identifier("jenkinsSlaveImage")
  //                                           .connector("npquotecenter")
  //                                           .imagePath("us.gcr.io/platform-205701/jenkins-slave-portal-oracle-8u191:12")
  //                                           .build())
  //                            .customVariables(asList(
  //                                CustomTextVariable.builder().name("internalPath").value("{input}").build(),
  //                                CustomSecretVariable.builder()
  //                                    .name("runTimeVal")
  //                                    .type(CustomVariable.Type.SECRET)
  //                                    .value(SecretRefData.builder().scope(Scope.ACCOUNT).identifier("secret").build())
  //                                    .build()))
  //                            .execution(
  //                                ExecutionElement.builder()
  //                                    .steps(asList(StepElement.builder()
  //                                                      .identifier("gitClone")
  //                                                      .type("GitClone")
  //                                                      .stepSpecType(GitCloneStepInfo.builder()
  //                                                                        .identifier("gitClone")
  //                                                                        .gitConnector("gitGlobal")
  //                                                                        .path("portal")
  //                                                                        .branch("master")
  //                                                                        .build())
  //                                                      .build(),
  //                                        ParallelStepElement.builder()
  //                                            .sections(asList(StepElement.builder()
  //                                                                 .identifier("runLint")
  //                                                                 .type("Run")
  //                                                                 .stepSpecType(RunStepInfo.builder()
  //                                                                                   .identifier("runLint")
  //                                                                                   .retry(2)
  //                                                                                   .command("./run lint")
  //                                                                                   .build())
  //                                                                 .build(),
  //                                                StepElement.builder()
  //                                                    .identifier("runAllUnitTests")
  //                                                    .type("Run")
  //                                                    .stepSpecType(
  //                                                        RunStepInfo.builder()
  //                                                            .identifier("runAllUnitTests")
  //                                                            .retry(2)
  //                                                            .command(
  //                                                                "mvn -U clean package -Dbuild.number=${BUILD_NUMBER}
  //                                                                -DgitBranch=master -DforkMode=perthread
  //                                                                -DthreadCount=3 -DargLine=\"-Xmx2048m\"")
  //                                                            .reports(unitTestReportList)
  //                                                            .build())
  //                                                    .build(),
  //                                                StepElement.builder()
  //                                                    .identifier("runUnitTestsIntelligently")
  //                                                    .type("TestIntelligence")
  //                                                    .stepSpecType(TestIntelligenceStepInfo.builder()
  //                                                                      .identifier("runUnitTestsIntelligently")
  //                                                                      .buildTool("maven")
  //                                                                      .goals("echo \"Running test\"")
  //                                                                      .language("java")
  //                                                                      .retry(2)
  //                                                                      .build())
  //                                                    .build()
  //
  //                                                    ))
  //                                            .build(),
  //                                        StepElement.builder()
  //                                            .identifier("generateReport")
  //                                            .type("Run")
  //                                            .stepSpecType(RunStepInfo.builder()
  //                                                              .identifier("generateReport")
  //                                                              .retry(2)
  //                                                              .command("./ci/generate_report.sh")
  //                                                              .build())
  //                                            .build(),
  //                                        StepElement.builder()
  //                                            .identifier("buildMaster")
  //                                            .type("Run")
  //                                            .stepSpecType(RunStepInfo.builder()
  //                                                              .identifier("buildMaster")
  //                                                              .retry(2)
  //                                                              .command("mvn clean install")
  //                                                              .build())
  //                                            .build(),
  //                                        StepElement.builder()
  //                                            .identifier("uploadArtifact")
  //                                            .type("PublishArtifacts")
  //                                            .stepSpecType(
  //                                                PublishStepInfo.builder()
  //                                                    .identifier("uploadArtifact")
  //                                                    .publishArtifacts(singletonList(
  //                                                        DockerFileArtifact.builder()
  //                                                            .dockerFile("Dockerfile")
  //                                                            .context("workspace")
  //                                                            .image("ui")
  //                                                            .tag("1.0.0")
  //                                                            .buildArguments(
  //                                                                asList(DockerFileArtifact.BuildArgument.builder()
  //                                                                           .key("key1")
  //                                                                           .value("value1")
  //                                                                           .build(),
  //                                                                    DockerFileArtifact.BuildArgument.builder()
  //                                                                        .key("key2")
  //                                                                        .value("value2")
  //                                                                        .build()))
  //                                                            .connector(GcrConnector.builder()
  //                                                                           .connectorRef("myDockerRepoConnector")
  //                                                                           .location("eu.gcr.io/harness/ui:latest")
  //                                                                           .build())
  //                                                            .build()))
  //                                                    .build())
  //                                            .build(),
  //                                        StepElement.builder()
  //                                            .identifier("minio")
  //                                            .type("Plugin")
  //                                            .stepSpecType(PluginStepInfo.builder()
  //                                                              .identifier("minio")
  //                                                              .image("plugins/s3")
  //                                                              .settings(ImmutableMap.<String, String>builder()
  //                                                                            .put("secret_key", "foo")
  //                                                                            .put("access_key", "bar")
  //                                                                            .build())
  //                                                              .build())
  //                                            .build()))
  //                                    .build())
  //                            .build())
  //                    .build()))
  //            .build();
  //    assertThat(ngPipelineActual).usingRecursiveComparison().isEqualTo(ngPipelineExpected);
  //  }
}
