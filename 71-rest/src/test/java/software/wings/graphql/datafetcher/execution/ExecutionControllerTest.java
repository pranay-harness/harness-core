package software.wings.graphql.datafetcher.execution;

import static io.harness.rule.OwnerRule.AADITI;

import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_SOURCE_NAME;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.BUILD_NO;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.beans.Service;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.NexusArtifactStream;
import software.wings.graphql.datafetcher.AbstractDataFetcherTestBase;
import software.wings.graphql.schema.mutation.execution.input.QLArtifactInputType;
import software.wings.graphql.schema.mutation.execution.input.QLArtifactValueInput;
import software.wings.graphql.schema.mutation.execution.input.QLParameterValueInput;
import software.wings.graphql.schema.mutation.execution.input.QLParameterizedArtifactSourceInput;
import software.wings.graphql.schema.mutation.execution.input.QLServiceInput;
import software.wings.service.ArtifactStreamHelper;
import software.wings.service.impl.artifact.ArtifactCollectionServiceAsyncImpl;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.utils.RepositoryFormat;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class ExecutionControllerTest extends AbstractDataFetcherTestBase {
  @Mock ServiceResourceService serviceResourceService;
  @Mock ArtifactStreamService artifactStreamService;
  @Mock ArtifactStreamHelper artifactStreamHelper;
  @Mock ArtifactService artifactService;
  @Mock ArtifactCollectionServiceAsyncImpl artifactCollectionServiceAsync;
  @Inject @InjectMocks private ExecutionController executionController;
  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void getArtifactsForParameterizedSource() {
    NexusArtifactStream nexusArtifactStream = NexusArtifactStream.builder()
                                                  .accountId(ACCOUNT_ID)
                                                  .appId(APP_ID)
                                                  .settingId(SETTING_ID)
                                                  .jobname("${repo}")
                                                  .packageName("${package}")
                                                  .autoPopulate(false)
                                                  .serviceId(SERVICE_ID)
                                                  .name("testNexus")
                                                  .uuid(ARTIFACT_STREAM_ID)
                                                  .sourceName(ARTIFACT_SOURCE_NAME)
                                                  .repositoryFormat(RepositoryFormat.npm.name())
                                                  .build();
    nexusArtifactStream.setArtifactStreamParameterized(true);
    List<QLServiceInput> serviceInputs = asList(
        QLServiceInput.builder()
            .name(SERVICE_NAME)
            .artifactValueInput(
                QLArtifactValueInput.builder()
                    .parameterizedArtifactSource(
                        QLParameterizedArtifactSourceInput.builder()
                            .buildNumber(BUILD_NO)
                            .artifactSourceName(ARTIFACT_SOURCE_NAME)
                            .parameterValueInputs(
                                asList(QLParameterValueInput.builder().name("repo").value("npm-internal").build(),
                                    QLParameterValueInput.builder().name("package").value("npm-app1").build()))
                            .build())
                    .valueType(QLArtifactInputType.PARAMETERIZED_ARTIFACT_SOURCE)
                    .build())
            .build());
    List<Artifact> artifacts = new ArrayList<>();
    when(serviceResourceService.get(APP_ID, SERVICE_ID))
        .thenReturn(Service.builder().uuid(SERVICE_ID).accountId(ACCOUNT_ID).appId(APP_ID).name(SERVICE_NAME).build());
    when(artifactStreamService.getArtifactStreamByName(APP_ID, SERVICE_ID, ARTIFACT_SOURCE_NAME))
        .thenReturn(nexusArtifactStream);
    when(artifactService.getArtifactByBuildNumberAndSourceName(any(), anyString(), anyBoolean(), anyString()))
        .thenReturn(null);
    when(artifactCollectionServiceAsync.collectNewArtifacts(anyString(), any(), anyString(), any()))
        .thenReturn(Artifact.Builder.anArtifact().withUuid(ARTIFACT_ID).build());
    executionController.getArtifactsFromServiceInputs(
        serviceInputs, APP_ID, asList(SERVICE_ID), artifacts, new ArrayList<>());
    assertThat(artifacts.size()).isEqualTo(1);
    assertThat(artifacts.get(0)).extracting(Artifact::getUuid).isEqualTo(ARTIFACT_ID);
  }
}
