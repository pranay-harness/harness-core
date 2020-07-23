package io.harness.perpetualtask.artifact;

import static io.harness.exception.WingsException.USER;
import static io.harness.logging.CommandExecutionStatus.FAILURE;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.SRINIVAS;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.delegatetasks.buildsource.BuildSourceParameters.BuildSourceRequestType;

import com.google.inject.Injector;
import com.google.inject.Key;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.delegatetasks.buildsource.BuildSourceExecutionResponse;
import software.wings.delegatetasks.buildsource.BuildSourceParameters;
import software.wings.exception.InvalidArtifactServerException;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.impl.ServiceClassLocator;
import software.wings.service.intfc.CustomBuildService;
import software.wings.service.intfc.DockerBuildService;
import software.wings.service.intfc.JenkinsBuildService;

import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class ArtifactRepositoryServiceImplTest extends CategoryTest {
  private ArtifactRepositoryServiceImpl artifactRepositoryService;

  private ServiceClassLocator serviceClassLocator = new ServiceClassLocator();
  @Mock private Injector injector;
  @Mock private CustomBuildService customBuildService;
  @Mock private JenkinsBuildService jenkinsBuildService;
  @Mock private DockerBuildService dockerBuildService;

  private static final String APP_ID = "APP_ID";
  private static final String ACCOUNT_ID = "ACCOUNT_ID";

  @Before
  public void setUp() throws Exception {
    artifactRepositoryService = new ArtifactRepositoryServiceImpl(serviceClassLocator, injector);
    when(injector.getInstance(Key.get(CustomBuildService.class))).thenReturn(customBuildService);
    when(injector.getInstance(Key.get(JenkinsBuildService.class))).thenReturn(jenkinsBuildService);
    when(injector.getInstance(Key.get(DockerBuildService.class))).thenReturn(dockerBuildService);
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldPublishCollectedArtifacts() {
    final String docker = "DOCKER";
    final ArtifactStreamAttributes artifactStreamAttributes =
        ArtifactStreamAttributes.builder().artifactStreamType(docker).build();

    BuildSourceParameters buildSourceParameters = BuildSourceParameters.builder()
                                                      .accountId(ACCOUNT_ID)
                                                      .appId(APP_ID)
                                                      .artifactStreamAttributes(artifactStreamAttributes)
                                                      .artifactStreamType(docker)
                                                      .buildSourceRequestType(BuildSourceRequestType.GET_BUILDS)
                                                      .limit(-1)
                                                      .isCollection(true)
                                                      .build();

    when(dockerBuildService.getBuilds(APP_ID, artifactStreamAttributes, null, null))
        .thenReturn(asList(BuildDetails.Builder.aBuildDetails().withNumber("alpine").build()));

    final BuildSourceExecutionResponse buildSourceExecutionResponse =
        artifactRepositoryService.publishCollectedArtifacts(buildSourceParameters);
    assertThat(buildSourceExecutionResponse.getCommandExecutionStatus()).isEqualTo(SUCCESS);
    assertThat(buildSourceExecutionResponse.getBuildSourceResponse()).isNotNull();
    assertThat(buildSourceExecutionResponse.getBuildSourceResponse().getBuildDetails())
        .isNotEmpty()
        .extracting(buildDetails -> buildDetails.getNumber())
        .contains("alpine");
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldFailPublishCollectedArtifactsOnException() {
    final String docker = "DOCKER";
    final ArtifactStreamAttributes artifactStreamAttributes =
        ArtifactStreamAttributes.builder().artifactStreamType(docker).build();

    BuildSourceParameters buildSourceParameters = BuildSourceParameters.builder()
                                                      .accountId(ACCOUNT_ID)
                                                      .appId(APP_ID)
                                                      .artifactStreamType(docker)
                                                      .buildSourceRequestType(BuildSourceRequestType.GET_BUILDS)
                                                      .artifactStreamAttributes(artifactStreamAttributes)
                                                      .limit(-1)
                                                      .isCollection(true)
                                                      .build();

    when(dockerBuildService.getBuilds(APP_ID, artifactStreamAttributes, null, null))
        .thenThrow(new InvalidArtifactServerException("Invalid Docker Registry credentials", USER));

    final BuildSourceExecutionResponse buildSourceExecutionResponse =
        artifactRepositoryService.publishCollectedArtifacts(buildSourceParameters);

    assertThat(buildSourceExecutionResponse.getCommandExecutionStatus()).isEqualTo(FAILURE);
    assertThat(buildSourceExecutionResponse.getErrorMessage()).isNotEmpty();
    assertThat(buildSourceExecutionResponse.getBuildSourceResponse()).isNull();
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldCollectCustomBuilds() {
    final ArtifactStreamAttributes artifactStreamAttributes =
        ArtifactStreamAttributes.builder().artifactStreamType("CUSTOM").build();
    BuildSourceParameters buildSourceParameters = BuildSourceParameters.builder()
                                                      .accountId(ACCOUNT_ID)
                                                      .appId(APP_ID)
                                                      .artifactStreamAttributes(artifactStreamAttributes)
                                                      .artifactStreamType("CUSTOM")
                                                      .buildSourceRequestType(BuildSourceRequestType.GET_BUILDS)
                                                      .limit(-1)
                                                      .isCollection(true)
                                                      .build();

    when(customBuildService.getBuilds(artifactStreamAttributes))
        .thenReturn(asList(BuildDetails.Builder.aBuildDetails().withNumber("1.0").build()));
    final List<BuildDetails> buildDetails = artifactRepositoryService.collectBuilds(buildSourceParameters);
    assertThat(buildDetails).isNotEmpty();
    assertThat(buildDetails).extracting(buildDetails1 -> buildDetails1.getNumber()).contains("1.0");
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldCollectDockerBuilds() {
    final ArtifactStreamAttributes artifactStreamAttributes =
        ArtifactStreamAttributes.builder().artifactStreamType("DOCKER").build();
    BuildSourceParameters buildSourceParameters = BuildSourceParameters.builder()
                                                      .accountId(ACCOUNT_ID)
                                                      .appId(APP_ID)
                                                      .artifactStreamAttributes(artifactStreamAttributes)
                                                      .artifactStreamType("DOCKER")
                                                      .buildSourceRequestType(BuildSourceRequestType.GET_BUILDS)
                                                      .limit(-1)
                                                      .isCollection(true)
                                                      .build();

    when(dockerBuildService.getBuilds(APP_ID, artifactStreamAttributes, null, null))
        .thenReturn(asList(BuildDetails.Builder.aBuildDetails().withNumber("alpine").build()));
    final List<BuildDetails> buildDetails = artifactRepositoryService.collectBuilds(buildSourceParameters);
    assertThat(buildDetails).isNotEmpty();
    assertThat(buildDetails).extracting(buildDetails1 -> buildDetails1.getNumber()).contains("alpine");
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldCollectJenkinsBuild() {
    final ArtifactStreamAttributes artifactStreamAttributes =
        ArtifactStreamAttributes.builder().artifactStreamType("JENKINS").build();
    BuildSourceParameters buildSourceParameters =
        BuildSourceParameters.builder()
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .artifactStreamAttributes(artifactStreamAttributes)
            .artifactStreamType("JENKINS")
            .buildSourceRequestType(BuildSourceRequestType.GET_LAST_SUCCESSFUL_BUILD)
            .limit(-1)
            .isCollection(true)
            .build();

    when(jenkinsBuildService.getLastSuccessfulBuild(APP_ID, artifactStreamAttributes, null, null))
        .thenReturn(BuildDetails.Builder.aBuildDetails().withNumber("1.0").build());
    final List<BuildDetails> buildDetails = artifactRepositoryService.collectBuilds(buildSourceParameters);
    assertThat(buildDetails).isNotEmpty();
    assertThat(buildDetails).extracting(buildDetails1 -> buildDetails1.getNumber()).contains("1.0");
  }
}