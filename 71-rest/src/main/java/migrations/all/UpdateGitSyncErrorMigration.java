package migrations.all;

import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;

import io.harness.persistence.HIterator;
import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Application;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.AppService;
import software.wings.yaml.errorhandling.GitSyncError;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class UpdateGitSyncErrorMigration implements Migration {
  @Inject WingsPersistence wingsPersistence;
  @Inject YamlHelper yamlHelper;
  @Inject AppService appService;
  private Map<String, String> appIdMap = new HashMap<>();
  @Override
  public void migrate() {
    Query<GitSyncError> query = wingsPersistence.createQuery(GitSyncError.class);

    try (HIterator<GitSyncError> records = new HIterator<>(query.fetch())) {
      while (records.hasNext()) {
        GitSyncError syncError = records.next();

        // AppId is already stamped
        if (StringUtils.isNotBlank(syncError.getAppId())) {
          continue;
        }

        String applicationId = GLOBAL_APP_ID;

        // Fetch appName from yamlPath, e.g. Setup/Applications/App1/Services/S1/index.yaml -> App1,
        // Setup/Artifact Servers/server.yaml -> null
        String appName = yamlHelper.getAppName(syncError.getYamlFilePath());

        if (StringUtils.isNotBlank(appName)) {
          String key = new StringBuilder(syncError.getAccountId()).append(":").append(appName).toString();

          if (!appIdMap.containsKey(key)) {
            Application application = appService.getAppByName(syncError.getAccountId(), appName);
            if (application != null) {
              appIdMap.put(key, application.getUuid());
            }
          }

          if (StringUtils.isNotBlank(appIdMap.get(key))) {
            applicationId = appIdMap.get(key);
          }
        }
        syncError.setAppId(applicationId);

        wingsPersistence.saveAndGet(GitSyncError.class, syncError);
        logger.info(new StringBuilder()
                        .append("Updated GitSyncError with Id:< ")
                        .append(syncError.getUuid())
                        .append(">, with appId: <")
                        .append(applicationId)
                        .append(" >")
                        .toString());
      }
    }
  }
}