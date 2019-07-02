package software.wings.service.impl.yaml;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import io.harness.tags.TagAware;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.EntityType;
import software.wings.beans.HarnessTagLink;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.HarnessTagService;
import software.wings.service.intfc.ResourceLookupService;
import software.wings.yaml.BaseEntityYaml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@Singleton
@Slf4j
public class HarnessTagYamlHelper {
  @Inject HarnessTagService harnessTagService;
  @Inject ResourceLookupService resourceLookupService;
  @Inject AppService appService;

  public void updateYamlWithHarnessTags(PersistentEntity entity, String appId, BaseEntityYaml yaml) {
    if (!(entity instanceof TagAware)) {
      return;
    }

    String accountId = getAccountId(entity, appId);
    String entityId = ((UuidAware) entity).getUuid();
    List<HarnessTagLink> tagLinks = harnessTagService.getTagLinksWithEntityId(accountId, entityId);

    Map<String, String> harnessTags = new LinkedHashMap<>();
    tagLinks.forEach(harnessTagLink -> harnessTags.put(harnessTagLink.getKey(), harnessTagLink.getValue()));

    if (isNotEmpty(harnessTags)) {
      yaml.setHarnessTags(harnessTags);
    }
  }

  private String getAccountId(PersistentEntity entity, String appId) {
    if (!GLOBAL_APP_ID.equals(appId)) {
      return appService.getAccountIdByAppId(appId);
    }

    return null;
  }

  public void upsertTagsIfRequired(ChangeContext changeContext) {
    PersistentEntity entity = changeContext.getEntity();

    if (!(entity instanceof TagAware)) {
      return;
    }

    String accountId = changeContext.getChange().getAccountId();

    BaseEntityYaml yaml = (BaseEntityYaml) changeContext.getYaml();
    Map<String, String> tagsFromYaml = yaml.getHarnessTags();
    if (isEmpty(tagsFromYaml)) {
      tagsFromYaml = new HashMap<>();
    }

    String entityId = ((UuidAware) entity).getUuid();
    List<HarnessTagLink> tagLinks = harnessTagService.getTagLinksWithEntityId(accountId, entityId);
    String entityType = resourceLookupService.getWithResourceId(accountId, entityId).getResourceType();

    Map<String, String> tagsFromDB = new HashMap<>();
    tagLinks.forEach(harnessTagLink -> tagsFromDB.put(harnessTagLink.getKey(), harnessTagLink.getValue()));

    Map<String, String> tagsToBeAdded = new HashMap<>();
    List<String> tagsToBeRemoved = new ArrayList<>();

    for (Entry<String, String> entry : tagsFromYaml.entrySet()) {
      String tagKey = entry.getKey();
      String tagValue = entry.getValue();

      if (tagsFromDB.containsKey(tagKey)) {
        if (!tagValue.equals(tagsFromDB.get(tagKey))) {
          tagsToBeAdded.put(tagKey, tagValue);
        }
      } else {
        tagsToBeAdded.put(tagKey, tagValue);
      }
    }

    for (Entry<String, String> entry : tagsFromDB.entrySet()) {
      String tagKey = entry.getKey();

      if (!tagsFromYaml.containsKey(tagKey)) {
        tagsToBeRemoved.add(tagKey);
      }
    }

    for (Entry<String, String> entry : tagsToBeAdded.entrySet()) {
      String tagKey = entry.getKey();
      String tagValue = entry.getValue();

      HarnessTagLink harnessTagLink = HarnessTagLink.builder()
                                          .accountId(accountId)
                                          .entityId(entityId)
                                          .entityType(EntityType.valueOf(entityType))
                                          .key(tagKey)
                                          .value(tagValue)
                                          .build();
      harnessTagService.attachTagWithoutGitPush(harnessTagLink);
    }

    for (String tagKey : tagsToBeRemoved) {
      harnessTagService.detachTagWithoutGitPush(accountId, entityId, tagKey);
    }

    // We don't want to generate multiple yaml changeSets. Once all the tags are processed, then push to git
    // Don't push to git if nothing changed. Avoid generating unnecessary changesets
    if (isEmpty(tagsToBeAdded) && isEmpty(tagsToBeRemoved)) {
      return;
    }

    boolean syncFromGit = changeContext.getChange().isSyncFromGit();
    harnessTagService.pushToGit(accountId, entityId, syncFromGit);
  }
}
