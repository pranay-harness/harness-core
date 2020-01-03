package software.wings.service.impl.artifact;

import static io.harness.rule.OwnerRule.SRINIVAS;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.expression.ExpressionEvaluator;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.CustomArtifactStream;
import software.wings.beans.artifact.CustomArtifactStream.Action;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.CustomBuildService;
import software.wings.service.intfc.artifact.CustomBuildSourceService;

import java.util.List;

public class CustomBuildSourceServiceTest extends WingsBaseTest {
  @Mock private ArtifactStreamService artifactStreamService;
  @Mock private AppService appService;
  @Inject @InjectMocks private CustomBuildSourceService customBuildSourceService;
  @Inject @InjectMocks private ArtifactCollectionUtils artifactCollectionUtils;
  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Mock private ExpressionEvaluator evaluator;
  @Mock private CustomBuildService customBuildService;

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldGetBuilds() {
    ArtifactStream customArtifactStream =
        CustomArtifactStream.builder()
            .accountId(ACCOUNT_ID)
            .appId(APP_ID)
            .serviceId(SERVICE_ID)
            .name("Custom Artifact Stream" + System.currentTimeMillis())
            .scripts(asList(CustomArtifactStream.Script.builder()
                                .action(Action.FETCH_VERSIONS)
                                .scriptString("echo Hello World!! and echo ${secrets.getValue(My Secret)}")
                                .timeout("60")
                                .build()))
            .build();

    when(artifactStreamService.get(ARTIFACT_STREAM_ID)).thenReturn(customArtifactStream);
    when(appService.get(APP_ID))
        .thenReturn(Application.Builder.anApplication()
                        .name("Custom Repository App")
                        .uuid(APP_ID)
                        .accountId(ACCOUNT_ID)
                        .build());

    when(delegateProxyFactory.get(any(), any())).thenReturn(customBuildService);
    when(customBuildService.getBuilds(any(ArtifactStreamAttributes.class)))
        .thenReturn(asList(BuildDetails.Builder.aBuildDetails().withNumber("1").build()));
    final List<BuildDetails> builds = customBuildSourceService.getBuilds(ARTIFACT_STREAM_ID);
    assertThat(builds).isNotEmpty();
    verify(evaluator, times(2)).substitute(any(), any());
  }
}
