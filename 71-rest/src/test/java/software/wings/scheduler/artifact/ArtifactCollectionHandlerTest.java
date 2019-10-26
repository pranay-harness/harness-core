package software.wings.scheduler.artifact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.logging.ExceptionLogger;
import io.harness.workers.background.critical.iterator.ArtifactCollectionHandler;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import software.wings.WingsBaseTest;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.service.intfc.PermitService;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ArtifactCollectionHandler.class, ExceptionLogger.class, Logger.class})
public class ArtifactCollectionHandlerTest extends WingsBaseTest {
  private static final String ARTIFACT_STREAM_ID = "ARTIFACT_STREAM_ID";

  @Mock private PermitService permitService;
  @InjectMocks @Inject private ArtifactCollectionHandler artifactCollectionHandler;

  @Test
  @Category(UnitTests.class)
  public void testHandleForNPEInAccountId() {
    PowerMockito.mockStatic(ExceptionLogger.class);
    ArtifactStream ARTIFACT_STREAM = DockerArtifactStream.builder()
                                         .uuid(ARTIFACT_STREAM_ID)
                                         .appId(APP_ID)
                                         .sourceName(ARTIFACT_STREAM_NAME)
                                         .settingId(SETTING_ID)
                                         .serviceId(SERVICE_ID)
                                         .imageName("image_name")
                                         .build();

    when(permitService.acquirePermit(any())).thenThrow(new WingsException("Exception"));
    artifactCollectionHandler.handle(ARTIFACT_STREAM);

    ArgumentCaptor<WingsException> argumentCaptor = ArgumentCaptor.forClass(WingsException.class);
    PowerMockito.verifyStatic();
    ExceptionLogger.logProcessedMessages(argumentCaptor.capture(), any(), any());
    WingsException wingsException = argumentCaptor.getValue();

    assertThat(wingsException).isNotNull();
    assertThat(wingsException.calcRecursiveContextObjects().values().size()).isEqualTo(1);
    assertThat(wingsException.calcRecursiveContextObjects().values()).contains(ARTIFACT_STREAM_ID);
  }

  @Test
  @Category(UnitTests.class)
  public void testHandleWithValidAccountId() {
    PowerMockito.mockStatic(ExceptionLogger.class);

    ArtifactStream ARTIFACT_STREAM = DockerArtifactStream.builder()
                                         .uuid(ARTIFACT_STREAM_ID)
                                         .sourceName(ARTIFACT_STREAM_NAME)
                                         .appId(APP_ID)
                                         .settingId(SETTING_ID)
                                         .serviceId(SERVICE_ID)
                                         .imageName("image_name")
                                         .accountId(ACCOUNT_ID)
                                         .build();

    when(permitService.acquirePermit(any())).thenThrow(new WingsException("Exception"));
    artifactCollectionHandler.handle(ARTIFACT_STREAM);

    ArgumentCaptor<WingsException> argumentCaptor = ArgumentCaptor.forClass(WingsException.class);
    PowerMockito.verifyStatic();
    ExceptionLogger.logProcessedMessages(argumentCaptor.capture(), any(), any());
    WingsException wingsException = argumentCaptor.getValue();

    assertThat(wingsException).isNotNull();
    assertThat(wingsException.calcRecursiveContextObjects().values().size()).isEqualTo(2);
    assertThat(wingsException.calcRecursiveContextObjects().values()).contains(ACCOUNT_ID, ARTIFACT_STREAM_ID);
  }
}
