package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.beans.Activity.Builder.anActivity;
import static software.wings.beans.Activity.Status.RUNNING;
import static software.wings.beans.Command.Builder.aCommand;
import static software.wings.beans.CommandUnitType.EXEC;
import static software.wings.beans.Environment.EnvironmentType.PROD;
import static software.wings.beans.ExecCommandUnit.Builder.anExecCommandUnit;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_NAME;
import static software.wings.utils.WingsTestConstants.COMMAND_NAME;
import static software.wings.utils.WingsTestConstants.COMMAND_UNIT_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.HOST_NAME;
import static software.wings.utils.WingsTestConstants.RELEASE_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_NAME;

import com.google.inject.Inject;

import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Activity;
import software.wings.beans.Command;
import software.wings.beans.CommandUnit;
import software.wings.dl.PageRequest;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.ServiceResourceService;

import java.util.List;

/**
 * Created by peeyushaggarwal on 5/27/16.
 */
public class ActivityServiceTest extends WingsBaseTest {
  private static final Activity activity = anActivity()
                                               .withEnvironmentId(ENV_ID)
                                               .withEnvironmentName(ENV_NAME)
                                               .withEnvironmentType(PROD)
                                               .withAppId(APP_ID)
                                               .withArtifactName(ARTIFACT_NAME)
                                               .withCommandName(COMMAND_NAME)
                                               .withCommandType(EXEC.name())
                                               .withHostName(HOST_NAME)
                                               .withReleaseName(RELEASE_NAME)
                                               .withServiceName(SERVICE_NAME)
                                               .withServiceId(SERVICE_ID)
                                               .withServiceTemplateName(TEMPLATE_NAME)
                                               .withServiceTemplateId(TEMPLATE_ID)
                                               .withStatus(RUNNING)
                                               .build();

  @Inject private WingsPersistence wingsPersistence;

  @Mock private ServiceResourceService serviceResourceService;

  @Inject @InjectMocks private ActivityService activityService;

  /**
   * Should list activities.
   */
  @Test
  public void shouldListActivities() {
    wingsPersistence.save(activity);
    assertThat(activityService.list(APP_ID, ENV_ID, new PageRequest<>())).hasSize(1).containsExactly(activity);
  }

  /**
   * Should get activity.
   */
  @Test
  public void shouldGetActivity() {
    wingsPersistence.save(activity);
    assertThat(activityService.get(activity.getUuid(), activity.getAppId())).isEqualTo(activity);
  }

  /**
   * Should save activity.
   */
  @Test
  public void shouldSaveActivity() {
    activityService.save(activity);
    assertThat(wingsPersistence.get(Activity.class, activity.getAppId(), activity.getUuid())).isEqualTo(activity);
  }

  /**
   * Should get activity command units.
   */
  @Test
  public void shouldGetActivityCommandUnits() {
    String activityId = wingsPersistence.save(activity);
    Command command = aCommand()
                          .withName(COMMAND_NAME)
                          .addCommandUnits(anExecCommandUnit()
                                               .withName(COMMAND_UNIT_NAME)
                                               .withCommandUnitType(EXEC)
                                               .withCommand("./bin/start.sh")
                                               .build())
                          .build();
    when(serviceResourceService.getCommandByName(APP_ID, SERVICE_ID, COMMAND_NAME)).thenReturn(command);
    List<CommandUnit> commandUnits = activityService.getCommandUnits(APP_ID, activityId);
    assertThat(commandUnits.size()).isEqualTo(1);
    assertThat(commandUnits.get(0).getCommandUnitType()).isEqualTo(EXEC);
    assertThat(commandUnits.get(0).getName()).isEqualTo(COMMAND_UNIT_NAME);
  }

  /**
   * Shouldget last activity for service.
   */
  @Test
  public void shouldgetLastActivityForService() {
    wingsPersistence.save(activity);
    Activity activityForService = activityService.getLastActivityForService(APP_ID, SERVICE_ID);
    assertThat(activityForService).isEqualTo(activity);
  }

  /**
   * Shouldget last production activity for service.
   */
  @Test
  public void shouldgetLastProductionActivityForService() {
    activity.setEnvironmentType(PROD);
    wingsPersistence.save(activity);
    Activity lastProductionActivityForService = activityService.getLastProductionActivityForService(APP_ID, SERVICE_ID);
    assertThat(lastProductionActivityForService).isEqualTo(activity);
  }
}
