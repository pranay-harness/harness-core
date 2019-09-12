package migrations.all;

import static java.util.stream.Collectors.toList;

import com.google.inject.Inject;
import com.google.inject.name.Named;

import lombok.extern.slf4j.Slf4j;
import migrations.Migration;
import software.wings.beans.HarnessTag;
import software.wings.beans.HarnessTag.HarnessTagKeys;
import software.wings.dl.WingsPersistence;
import software.wings.features.TagsFeature;
import software.wings.features.api.PremiumFeature;
import software.wings.service.intfc.HarnessTagService;

import java.util.Collection;

@Slf4j
public class ConvertRestrictedTagsToNonRestrictedTagsForUnsupportedAccountTypes implements Migration {
  @Inject HarnessTagService tagService;
  @Inject WingsPersistence wingsPersistence;
  @Inject @Named(TagsFeature.FEATURE_NAME) private PremiumFeature tagsFeature;

  @Override
  public void migrate() {
    logger.info("Running ConvertRestrictedTagsToNonRestrictedTagsForUnsupportedAccountTypes migration");

    Collection<String> accountsForWhomTagsFeatureIsNotSupported =
        getAllAccountsUsingTags()
            .stream()
            .filter(account -> !tagsFeature.isAvailableForAccount(account))
            .collect(toList());

    logger.info(
        "Converting Restricted Tags To Non Restricted Tags for accounts {} ", accountsForWhomTagsFeatureIsNotSupported);

    tagService.convertRestrictedTagsToNonRestrictedTags(accountsForWhomTagsFeatureIsNotSupported);
  }

  @SuppressWarnings("unchecked")
  private Collection<String> getAllAccountsUsingTags() {
    return wingsPersistence.getCollection(HarnessTag.class).distinct(HarnessTagKeys.accountId);
  }
}
