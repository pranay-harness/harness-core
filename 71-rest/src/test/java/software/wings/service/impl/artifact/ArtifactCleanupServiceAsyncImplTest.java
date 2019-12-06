package software.wings.service.impl.artifact;

import static io.harness.rule.OwnerRule.HARSH;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.artifact.GcrArtifactStream;
import software.wings.service.intfc.SettingsService;

public class ArtifactCleanupServiceAsyncImplTest extends WingsBaseTest {
  @Mock private SettingsService settingsService;

  @Inject @InjectMocks private ArtifactCleanupServiceAsyncImpl artifactCleanupServiceAsync;

  private static final String ARTIFACT_STREAM_ID_5 = "ARTIFACT_STREAM_ID_5";

  GcrArtifactStream gcrArtifactStream = GcrArtifactStream.builder()
                                            .uuid(ARTIFACT_STREAM_ID_5)
                                            .appId(APP_ID)
                                            .sourceName("ARTIFACT_SOURCE")
                                            .serviceId(SERVICE_ID)
                                            .build();

  @Test
  @Owner(developers = HARSH)
  @Category(UnitTests.class)
  public void testRegisterIterators() {
    artifactCleanupServiceAsync.cleanupArtifactsAsync(gcrArtifactStream);
    verify(settingsService, times(1)).get(any());
  }
}