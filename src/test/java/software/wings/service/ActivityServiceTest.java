package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Activity.Builder.anActivity;

import com.google.inject.Inject;

import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.Activity;
import software.wings.beans.Activity.Status;
import software.wings.dl.PageRequest;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ActivityService;

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
public class ActivityServiceTest extends WingsBaseTest {
  private static final Activity activity = anActivity()
                                               .withAppId("APP_ID")
                                               .withArtifactName("ARTIFACT")
                                               .withCommandName("COMMAND")
                                               .withCommandType("EXEC")
                                               .withHostName("host1")
                                               .withReleaseName("REL1")
                                               .withServiceName("SERVICE")
                                               .withServiceId("SERVICE_ID")
                                               .withServiceTemplateName("SERVICE_TEMPLATE")
                                               .withServiceTemplateId("SERVICE_TEMPLATE_ID")
                                               .withStatus(Status.RUNNING)
                                               .build();

  @Inject private ActivityService activityService;

  @Inject private WingsPersistence wingsPersistence;

  @Test
  public void shouldListActivities() {
    wingsPersistence.save(activity);
    assertThat(activityService.list(new PageRequest<>())).hasSize(1).containsExactly(activity);
  }

  @Test
  public void shouldGetActivity() {
    wingsPersistence.save(activity);
    assertThat(activityService.get(activity.getUuid(), activity.getAppId())).isEqualTo(activity);
  }

  @Test
  public void shouldSaveActivity() {
    activityService.save(activity);
    assertThat(wingsPersistence.get(Activity.class, activity.getAppId(), activity.getUuid())).isEqualTo(activity);
  }
}
