package migrations.all;

import static software.wings.beans.yaml.YamlConstants.APPLICATIONS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.ENVIRONMENTS_FOLDER;
import static software.wings.beans.yaml.YamlConstants.INFRA_MAPPING_FOLDER;
import static software.wings.beans.yaml.YamlConstants.SETUP_FOLDER;

import com.google.inject.Inject;

import io.harness.data.structure.HarnessStringUtils;
import io.harness.exception.ExceptionUtils;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.GitFileChange.Builder;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.yaml.YamlChangeSetService;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class RemoveServiceInfraFolder implements Migration {
  @Inject private AppService appService;
  @Inject private EnvironmentService environmentService;
  @Inject private YamlChangeSetService yamlChangeSetService;
  private final String accountId = "kmpySmUISimoRrJL6NL73w";
  private final String DEBUG_LINE = "SERVICE_INFRA_YAML:";

  @Override
  public void migrate() {
    final List<Application> applications = appService.getAppsByAccountId(accountId);
    List<GitFileChange> gitFileChanges = new ArrayList<>();
    for (Application app : applications) {
      logger.info(HarnessStringUtils.join(StringUtils.SPACE, DEBUG_LINE, "Starting migration for app", app.getUuid()));
      try {
        List<Environment> environments = environmentService.getEnvByApp(app.getUuid());

        gitFileChanges.clear();

        environments.forEach(environment -> {
          GitFileChange gitFileChange =
              generateGitFileChangeForInfraMappingDelete(accountId, app.getName(), environment.getName());
          gitFileChanges.add(gitFileChange);
          logger.info(HarnessStringUtils.join(
              StringUtils.SPACE, DEBUG_LINE, "Adding to git file changeSet", gitFileChange.getFilePath()));
        });
        yamlChangeSetService.saveChangeSet(accountId, gitFileChanges, app);
      } catch (Exception ex) {
        logger.error(HarnessStringUtils.join(
            StringUtils.SPACE, DEBUG_LINE, ExceptionUtils.getMessage(ex), "app", app.getUuid()));
      }
      logger.info(HarnessStringUtils.join(StringUtils.SPACE, DEBUG_LINE, "Finished migration for app", app.getUuid()));
    }
  }

  private GitFileChange generateGitFileChangeForInfraMappingDelete(String accountId, String appName, String envName) {
    return Builder.aGitFileChange()
        .withAccountId(accountId)
        .withChangeType(ChangeType.DELETE)
        .withFilePath(HarnessStringUtils.join(
            "/", SETUP_FOLDER, APPLICATIONS_FOLDER, appName, ENVIRONMENTS_FOLDER, envName, INFRA_MAPPING_FOLDER))
        .build();
  }
}
