package migrations.all;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.beans.FeatureName.INFRA_MAPPING_REFACTOR;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;

import migrations.Migration;
import software.wings.beans.FeatureFlag;
import software.wings.beans.FeatureFlag.FeatureFlagKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.yaml.YamlGitService;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TriggerFullAppGitSync implements Migration {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private YamlGitService yamlGitService;

  @Override
  public void migrate() {
    final FeatureFlag featureFlag = wingsPersistence.createQuery(FeatureFlag.class)
                                        .filter(FeatureFlagKeys.name, INFRA_MAPPING_REFACTOR.name())
                                        .get();
    Set<String> accounts = featureFlag.getAccountIds();
    if (isNotEmpty(accounts)) {
      ExecutorService executorService = Executors.newFixedThreadPool(
          10, new ThreadFactoryBuilder().setNameFormat("migrate-TriggerFullAppGitSync").build());
      for (String accountId : accounts) {
        executorService.submit(() -> yamlGitService.fullSyncForEntireAccount(accountId));
      }
    }
  }
}
