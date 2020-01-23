package software.wings.graphql.datafetcher.application;

import com.google.inject.Inject;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Application;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.query.QLApplicationGitSyncConfigQueryParameters;
import software.wings.graphql.schema.type.QLGitSyncConfig;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppService;
import software.wings.yaml.gitSync.YamlGitConfig;

@Slf4j
public class ApplicationGitSyncConfigDataFetcher
    extends AbstractObjectDataFetcher<QLGitSyncConfig, QLApplicationGitSyncConfigQueryParameters> {
  @Inject AppService appService;

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public QLGitSyncConfig fetch(QLApplicationGitSyncConfigQueryParameters qlQuery, String accountId) {
    final Application applicationWithGitConfig = getApplicationWithGitConfig(qlQuery.getApplicationId());
    final YamlGitConfig yamlGitConfig = applicationWithGitConfig.getYamlGitConfig();
    if (yamlGitConfig == null) {
      return null;
    }
    return YamlGitConfigController.populateQLGitConfig(yamlGitConfig, QLGitSyncConfig.builder()).build();
  }

  private Application getApplicationWithGitConfig(String applicationId) {
    return appService.get(applicationId);
  }
}
