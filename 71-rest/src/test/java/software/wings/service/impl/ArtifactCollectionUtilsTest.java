package software.wings.service.impl;

import static io.harness.rule.OwnerRule.GARVIT;
import static io.harness.rule.OwnerRule.ROHITKARELIA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.SETTING_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.CustomArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.service.impl.artifact.ArtifactCollectionUtils;
import software.wings.service.intfc.SettingsService;

public class ArtifactCollectionUtilsTest extends WingsBaseTest {
  @Mock private SettingsService settingsService;
  @Inject @InjectMocks private ArtifactCollectionUtils artifactCollectionUtils;

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldSkipArtifactStreamIterationExcessiveFailedAttempts() {
    ArtifactStream artifactStream = DockerArtifactStream.builder().build();
    artifactStream.setFailedCronAttempts(PermitServiceImpl.MAX_FAILED_ATTEMPTS + 1);

    assertThat(artifactCollectionUtils.skipArtifactStreamIteration(artifactStream, true)).isTrue();
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldSkipArtifactStreamIterationForInvalidSetting() {
    when(settingsService.getOnlyConnectivityError(SETTING_ID)).thenReturn(null);
    ArtifactStream artifactStream = DockerArtifactStream.builder().settingId(SETTING_ID).build();

    artifactCollectionUtils.skipArtifactStreamIteration(artifactStream, true);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldSkipArtifactStreamIterationForConnectivityError() {
    when(settingsService.getOnlyConnectivityError(SETTING_ID))
        .thenReturn(SettingAttribute.Builder.aSettingAttribute().withConnectivityError("err").build());
    ArtifactStream artifactStream = DockerArtifactStream.builder().settingId(SETTING_ID).build();

    assertThat(artifactCollectionUtils.skipArtifactStreamIteration(artifactStream, true)).isTrue();
    assertThat(artifactCollectionUtils.skipArtifactStreamIteration(artifactStream, false)).isTrue();
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void shouldNotSkipArtifactStreamIterationForConnectivityError() {
    when(settingsService.getOnlyConnectivityError(SETTING_ID))
        .thenReturn(SettingAttribute.Builder.aSettingAttribute().build());
    ArtifactStream artifactStream = DockerArtifactStream.builder().settingId(SETTING_ID).build();

    assertThat(artifactCollectionUtils.skipArtifactStreamIteration(artifactStream, true)).isFalse();
    assertThat(artifactCollectionUtils.skipArtifactStreamIteration(artifactStream, false)).isFalse();
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void shouldNotSkipArtifactStreamIterationForConnectivityErrorForCustomArtifactSource() {
    ArtifactStream artifactStream = CustomArtifactStream.builder().build();
    assertThat(artifactCollectionUtils.skipArtifactStreamIteration(artifactStream, true)).isFalse();
  }
}
