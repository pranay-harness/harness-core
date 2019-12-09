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
import io.harness.rule.OwnerRule.Owner;
import io.harness.waiter.WaitNotifyEngine;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.artifact.CustomArtifactStream;
import software.wings.beans.artifact.CustomArtifactStream.Action;
import software.wings.service.intfc.DelegateService;

public class ArtifactCollectionServiceAsyncImplTest extends WingsBaseTest {
  @Mock private DelegateService delegateService;

  @Mock private WaitNotifyEngine mockWaitNotifyEngine;

  @Inject @InjectMocks private ArtifactCollectionServiceAsyncImpl artifactCollectionServiceAsync;

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
  public void verifyCustomCollection() {
    when(delegateService.queueTask(any())).thenReturn("ID");
    artifactCollectionServiceAsync.collectNewArtifactsAsync(customArtifactStream, "permitId");

    verify(mockWaitNotifyEngine, times(1)).waitForAllOn(any(), any(), any());
  }
}
