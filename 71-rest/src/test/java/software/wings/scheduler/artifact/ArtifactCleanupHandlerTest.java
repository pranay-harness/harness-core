package software.wings.scheduler.artifact;

import static io.harness.rule.OwnerRule.YOGESH;
import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.rule.Owner;
import io.harness.workers.background.iterator.ArtifactCleanupHandler;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import software.wings.WingsBaseTest;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStream.ArtifactStreamKeys;
import software.wings.beans.artifact.ArtifactStreamType;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@RunWith(PowerMockRunner.class)
@PrepareForTest(PersistenceIteratorFactory.class)
@PowerMockIgnore({"javax.net.*"})
public class ArtifactCleanupHandlerTest extends WingsBaseTest {
  @Mock PersistenceIteratorFactory persistenceIteratorFactory;
  @InjectMocks @Inject ArtifactCleanupHandler artifactCleanupHandler;
  @Test
  @Owner(developers = YOGESH)
  @Category(UnitTests.class)
  public void testRegisterIterators() {
    // setup mock
    when(persistenceIteratorFactory.createIterator(any(), any()))
        .thenReturn(MongoPersistenceIterator.<ArtifactStream>builder()
                        .filterExpander(query
                            -> query.field(ArtifactStreamKeys.artifactStreamType)
                                   .in(asList(ArtifactStreamType.DOCKER.name(), ArtifactStreamType.AMI.name(),
                                       ArtifactStreamType.ARTIFACTORY.name(), ArtifactStreamType.ECR.name(),
                                       ArtifactStreamType.GCR.name())))
                        .build());

    ScheduledThreadPoolExecutor executor = mock(ScheduledThreadPoolExecutor.class);
    artifactCleanupHandler.registerIterators(executor);

    verify(executor).scheduleAtFixedRate(any(), anyLong(), anyLong(), any(TimeUnit.class));
  }
}