package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.rule.OwnerRule.AADITI;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.beans.Application;
import software.wings.beans.FeatureName;
import software.wings.beans.Service;
import software.wings.beans.artifact.NexusArtifactStream;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.sm.StateType;
import software.wings.utils.RepositoryFormat;
import software.wings.yaml.handler.BaseYamlHandlerTest;
import software.wings.yaml.workflow.StepYaml;

import java.util.HashMap;
import java.util.Map;

public class StepYamlHandlerTest extends BaseYamlHandlerTest {
  @InjectMocks @Inject private StepYamlHandler stepYamlHandler;
  @Mock private FeatureFlagService featureFlagService;
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private AppService appService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private YamlHelper yamlHelper;
  private NexusArtifactStream nexusArtifactStream;

  @Before
  public void setUp() {
    nexusArtifactStream = getNexusArtifactStream();
    Application application = Application.Builder.anApplication().name("a1").uuid(APP_ID).accountId(ACCOUNT_ID).build();
    when(appService.getAppByName(ACCOUNT_ID, "a1")).thenReturn(application);
    when(appService.get(APP_ID)).thenReturn(application);
    when(yamlHelper.getAppId(ACCOUNT_ID, "Setup/Applications/a1/Workflows/build.yaml")).thenReturn(APP_ID);
    when(featureFlagService.isEnabled(FeatureName.NAS_SUPPORT, ACCOUNT_ID)).thenReturn(true);
    when(serviceResourceService.getServiceByName(APP_ID, "s1")).thenReturn(Service.builder().uuid(SERVICE_ID).build());
    when(artifactStreamService.getArtifactStreamByName(APP_ID, SERVICE_ID, "test")).thenReturn(nexusArtifactStream);
    when(artifactStreamService.get(ARTIFACT_STREAM_ID)).thenReturn(nexusArtifactStream);
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void testToBeanWithoutBuildNo() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("regex", false);
    properties.put("artifactStreamName", "test");
    properties.put("serviceName", "s1");
    Map<String, Object> runtimeValues = new HashMap<>();
    runtimeValues.put("repo", "npm-internal");
    runtimeValues.put("package", "npm-app1");
    properties.put("runtimeValues", runtimeValues);
    ChangeContext<StepYaml> changeContext =
        ChangeContext.Builder.aChangeContext()
            .withYamlType(YamlType.WORKFLOW)
            .withYaml(StepYaml.builder()
                          .name("Artifact Collection")
                          .properties(properties)
                          .type(StateType.ARTIFACT_COLLECTION.name())
                          .build())
            .withChange(GitFileChange.Builder.aGitFileChange()
                            .withFilePath("Setup/Applications/a1/Workflows/build.yaml")
                            .withAccountId(ACCOUNT_ID)
                            .build())
            .build();

    try {
      stepYamlHandler.upsertFromYaml(changeContext, null);
    } catch (Exception e) {
      assertThat(e).isInstanceOf(InvalidRequestException.class);
      assertThat(e.getMessage()).contains("buildNo not provided");
    }
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void testToBeanWithRegex() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("regex", true);
    properties.put("artifactStreamName", "test");
    properties.put("serviceName", "s1");
    properties.put("buildNo", "1.0.0");
    Map<String, Object> runtimeValues = new HashMap<>();
    runtimeValues.put("repo", "npm-internal");
    runtimeValues.put("package", "npm-app1");
    properties.put("runtimeValues", runtimeValues);
    ChangeContext<StepYaml> changeContext =
        ChangeContext.Builder.aChangeContext()
            .withYamlType(YamlType.WORKFLOW)
            .withYaml(StepYaml.builder()
                          .name("Artifact Collection")
                          .properties(properties)
                          .type(StateType.ARTIFACT_COLLECTION.name())
                          .build())
            .withChange(GitFileChange.Builder.aGitFileChange()
                            .withFilePath("Setup/Applications/a1/Workflows/build.yaml")
                            .withAccountId(ACCOUNT_ID)
                            .build())
            .build();

    try {
      stepYamlHandler.upsertFromYaml(changeContext, null);
    } catch (Exception e) {
      assertThat(e).isInstanceOf(InvalidRequestException.class);
      assertThat(e.getMessage()).contains("Regex cannot be set for parameterized artifact source");
    }
  }

  @Test
  @Owner(developers = AADITI)
  @Category(UnitTests.class)
  public void testToBeanWithoutRuntimeValues() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("buildNo", "1.0.0");
    properties.put("artifactStreamName", "test");
    properties.put("serviceName", "s1");
    Map<String, Object> runtimeValues = new HashMap<>();
    runtimeValues.put("repo", "npm-internal");
    properties.put("runtimeValues", runtimeValues);
    ChangeContext<StepYaml> changeContext =
        ChangeContext.Builder.aChangeContext()
            .withYamlType(YamlType.WORKFLOW)
            .withYaml(StepYaml.builder()
                          .name("Artifact Collection")
                          .properties(properties)
                          .type(StateType.ARTIFACT_COLLECTION.name())
                          .build())
            .withChange(GitFileChange.Builder.aGitFileChange()
                            .withFilePath("Setup/Applications/a1/Workflows/build.yaml")
                            .withAccountId(ACCOUNT_ID)
                            .build())
            .build();

    when(artifactStreamService.getArtifactStreamParameters(ARTIFACT_STREAM_ID)).thenReturn(asList("repo", "package"));
    try {
      stepYamlHandler.upsertFromYaml(changeContext, null);
    } catch (Exception e) {
      assertThat(e).isInstanceOf(InvalidRequestException.class);
      assertThat(e.getMessage()).contains("runtime values not provided");
    }
  }

  private NexusArtifactStream getNexusArtifactStream() {
    NexusArtifactStream nexusArtifactStream = NexusArtifactStream.builder()
                                                  .accountId(ACCOUNT_ID)
                                                  .appId(APP_ID)
                                                  .settingId(SETTING_ID)
                                                  .jobname("${repo}")
                                                  .packageName("${package}")
                                                  .autoPopulate(false)
                                                  .serviceId(SERVICE_ID)
                                                  .repositoryFormat(RepositoryFormat.npm.name())
                                                  .name("test")
                                                  .uuid(ARTIFACT_STREAM_ID)
                                                  .build();
    nexusArtifactStream.setArtifactStreamParameterized(true);
    return nexusArtifactStream;
  }
}
