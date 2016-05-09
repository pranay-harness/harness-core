package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Artifact.ArtifactBuilder.anArtifact;
import static software.wings.beans.Release.ReleaseBuilder.aRelease;
import static software.wings.beans.User.Builder.anUser;

import org.junit.Before;
import org.junit.Test;
import software.wings.WingsBaseUnitTest;
import software.wings.beans.Artifact.ArtifactBuilder;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ArtifactService;

import javax.inject.Inject;
import javax.validation.ConstraintViolationException;

/**
 * Created by peeyushaggarwal on 4/4/16.
 */
public class ArtifactServiceTest extends WingsBaseUnitTest {
  @Inject private ArtifactService artifactService;

  private ArtifactBuilder builder = anArtifact()
                                        .withApplication(anApplication().withUuid("APP_ID").build())
                                        .withAppId("APP_ID")
                                        .withRelease(aRelease().withUuid("RELEASE_ID").build())
                                        .withArtifactSourceName("ARTIFACT_SOURCE")
                                        .withCompName("COMP_NAME")
                                        .withRevision("1.0")
                                        .withDisplayName("DISPLAY_NAME")
                                        .withCreatedAt(System.currentTimeMillis())
                                        .withCreatedBy(anUser().withUuid("USER_ID").build());

  /**
   * test setup.
   */
  @Before
  public void setUp() {
    wingsRule.getDatastore().save(anApplication().withUuid("APP_ID").build());
    wingsRule.getDatastore().save(
        aRelease().withUuid("RELEASE_ID").withApplication(anApplication().withUuid("APP_ID").build()).build());
  }

  @Test
  public void shouldCreateArtifactWhenValid() {
    assertThat(artifactService.create(builder.build())).isNotNull();
  }

  @Test
  public void shouldThrowExceptionWhenAppIdDoesNotMatchForArtifacToBeCreated() {
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(
            () -> artifactService.create(builder.withApplication(anApplication().withUuid("APP_ID1").build()).build()));
  }

  @Test
  public void shouldThrowExceptionWhenReleaseIdDoesNotMatchForArtifacToBeCreated() {
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(
            () -> artifactService.create(builder.withRelease(aRelease().withUuid("RELEASE_ID1").build()).build()));
  }

  @Test
  public void shouldThrowExceptionWhenArtifactToBeCreatedIsInvalid() {
    assertThatExceptionOfType(ConstraintViolationException.class)
        .isThrownBy(() -> artifactService.create(builder.withArtifactSourceName(null).build()));
  }
}
