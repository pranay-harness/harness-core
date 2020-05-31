package software.wings.service.impl.artifact;

import static io.harness.persistence.HQuery.excludeAuthority;
import static java.util.Arrays.asList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import com.google.inject.Inject;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import software.wings.beans.FeatureName;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.FeatureFlagService;

import java.util.Collections;

public class ArtifactStreamPTaskMigrationJobTest extends CategoryTest {
  @Mock private ArtifactStreamPTaskHelper artifactStreamPTaskHelper;
  @Mock private WingsPersistence mockWingsPersistence;
  @Mock private PersistentLocker persistentLocker;
  @Mock private FeatureFlagService featureFlagService;

  @Inject @InjectMocks private ArtifactStreamPTaskMigrationJob job;

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Before
  public void setup() {
    AcquiredLock<?> acquiredLock = mock(AcquiredLock.class);
    when(persistentLocker.tryToAcquireLock(anyString(), any())).thenReturn(acquiredLock);
  }

  @Test
  @Owner(developers = OwnerRule.GARVIT)
  @Category(UnitTests.class)
  public void testRunWhenGloballyEnabled() {
    Query<ArtifactStream> query = mock(Query.class);
    when(mockWingsPersistence.createQuery(ArtifactStream.class, excludeAuthority)).thenReturn(query);
    setupQuery(query);

    when(featureFlagService.isGlobalEnabled(FeatureName.ARTIFACT_PERPETUAL_TASK_MIGRATION)).thenReturn(true);
    when(featureFlagService.isGlobalEnabled(FeatureName.ARTIFACT_PERPETUAL_TASK)).thenReturn(true);

    when(query.asList(any(FindOptions.class))).thenReturn(Collections.emptyList());
    job.run();
    verify(artifactStreamPTaskHelper, never()).createPerpetualTask(any());

    when(query.asList(any(FindOptions.class))).thenReturn(asList(prepareArtifactStream(), prepareArtifactStream()));
    job.run();
    verify(artifactStreamPTaskHelper, times(2)).createPerpetualTask(any());
  }

  @Test
  @Owner(developers = OwnerRule.GARVIT)
  @Category(UnitTests.class)
  public void testRunWhenNotGloballyEnabled() {
    Query<ArtifactStream> query = mock(Query.class);
    when(mockWingsPersistence.createQuery(ArtifactStream.class)).thenReturn(query);
    setupQuery(query);

    when(featureFlagService.isGlobalEnabled(FeatureName.ARTIFACT_PERPETUAL_TASK_MIGRATION)).thenReturn(false);
    when(featureFlagService.isGlobalEnabled(FeatureName.ARTIFACT_PERPETUAL_TASK)).thenReturn(true);
    when(featureFlagService.getAccountIds(FeatureName.ARTIFACT_PERPETUAL_TASK_MIGRATION))
        .thenReturn(Collections.singleton(ACCOUNT_ID));
    when(featureFlagService.getAccountIds(FeatureName.ARTIFACT_PERPETUAL_TASK)).thenReturn(null);

    // No account has both feature flags on.
    when(query.asList(any(FindOptions.class))).thenReturn(asList(prepareArtifactStream(), prepareArtifactStream()));
    job.run();
    verify(artifactStreamPTaskHelper, never()).createPerpetualTask(any());

    // ACCOUNT_ID has both feature flags on.
    when(featureFlagService.getAccountIds(FeatureName.ARTIFACT_PERPETUAL_TASK))
        .thenReturn(Collections.singleton(ACCOUNT_ID));

    when(query.asList(any(FindOptions.class))).thenReturn(Collections.emptyList());
    job.run();
    verify(artifactStreamPTaskHelper, never()).createPerpetualTask(any());

    when(query.asList(any(FindOptions.class))).thenReturn(asList(prepareArtifactStream(), prepareArtifactStream()));
    job.run();
    verify(artifactStreamPTaskHelper, times(2)).createPerpetualTask(any());
  }

  private void setupQuery(Query<ArtifactStream> query) {
    FieldEnd fieldEnd = mock(FieldEnd.class);
    when(query.field(anyString())).thenReturn(fieldEnd);
    when(fieldEnd.in(any())).thenReturn(query);
    when(fieldEnd.doesNotExist()).thenReturn(query);
    when(query.project(anyString(), anyBoolean())).thenReturn(query);
  }

  private ArtifactStream prepareArtifactStream() {
    return DockerArtifactStream.builder()
        .accountId(ACCOUNT_ID)
        .appId(APP_ID)
        .uuid(ARTIFACT_STREAM_ID)
        .settingId(SETTING_ID)
        .imageName("wingsplugins/todolist")
        .autoPopulate(true)
        .serviceId(SERVICE_ID)
        .build();
  }
}
