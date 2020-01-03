package software.wings.service.impl.artifact;

import static io.harness.event.handler.impl.Constants.ACCOUNT_ID;
import static io.harness.rule.OwnerRule.HARSH;
import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.waiter.WaitNotifyEngine;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.artifact.CustomArtifactStream;
import software.wings.beans.artifact.CustomArtifactStream.Action;
import software.wings.beans.artifact.GcrArtifactStream;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.SettingsService;

public class ArtifactCleanupServiceAsyncImplTest extends WingsBaseTest {
  @Mock private SettingsService settingsService;
  @Mock private DelegateService delegateService;
  @Mock private WaitNotifyEngine mockWaitNotifyEngine;

  @Inject @InjectMocks private ArtifactCleanupServiceAsyncImpl artifactCleanupServiceAsync;

  private static final String ARTIFACT_STREAM_ID_5 = "ARTIFACT_STREAM_ID_5";

  GcrArtifactStream gcrArtifactStream = GcrArtifactStream.builder()
                                            .uuid(ARTIFACT_STREAM_ID_5)
                                            .appId(APP_ID)
                                            .sourceName("ARTIFACT_SOURCE")
                                            .serviceId(SERVICE_ID)
                                            .build();

  CustomArtifactStream customArtifactStream =
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

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void verifyGCRCleanup() {
    artifactCleanupServiceAsync.cleanupArtifactsAsync(gcrArtifactStream);
    verify(settingsService, times(1)).get(any());
  }

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void verifyCustomCleanup() {
    when(delegateService.queueTask(any())).thenReturn("ID");

    artifactCleanupServiceAsync.cleanupArtifactsAsync(customArtifactStream);

    verify(mockWaitNotifyEngine, times(1)).waitForAllOn(any(), any(), any());
  }
}