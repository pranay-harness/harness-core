package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.PIPELINE;
import static software.wings.beans.EntityType.PROVISIONER;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.EntityType.TRIGGER;
import static software.wings.beans.EntityType.WORKFLOW;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotBlank;
import software.wings.beans.EntityType;
import software.wings.beans.HarnessTag;
import software.wings.beans.HarnessTag.HarnessTagKeys;
import software.wings.beans.HarnessTagLink;
import software.wings.beans.HarnessTagLink.HarnessTagLinkKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.HarnessTagService;

import java.util.HashSet;
import java.util.Set;
import javax.validation.constraints.NotNull;
import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
@Singleton
@Slf4j
public class HarnessTagServiceImpl implements HarnessTagService {
  @Inject private WingsPersistence wingsPersistence;

  private static final Set<EntityType> supportedEntityTypes =
      ImmutableSet.of(SERVICE, ENVIRONMENT, WORKFLOW, PROVISIONER, PIPELINE, TRIGGER);

  @Override
  public HarnessTag create(HarnessTag tag) {
    sanitizeHarnessTag(tag);

    HarnessTag existingTag = get(tag.getAccountId(), tag.getKey());

    if (existingTag != null) {
      throw new InvalidRequestException("Tag with given key already exists");
    }
    wingsPersistence.save(tag);
    return get(tag.getAccountId(), tag.getKey());
  }

  @Override
  public HarnessTag update(HarnessTag tag) {
    sanitizeHarnessTag(tag);

    HarnessTag existingTag = get(tag.getAccountId(), tag.getKey());

    if (existingTag == null) {
      throw new InvalidRequestException("Tag with given key does not exist");
    }
    wingsPersistence.updateField(
        HarnessTag.class, existingTag.getUuid(), HarnessTagKeys.allowedValues, tag.getAllowedValues());
    return get(tag.getAccountId(), tag.getKey());
  }

  @Override
  public HarnessTag get(@NotBlank String accountId, @NotBlank String key) {
    return wingsPersistence.createQuery(HarnessTag.class)
        .filter(HarnessTagKeys.accountId, accountId)
        .filter(HarnessTagKeys.key, key.trim())
        .get();
  }

  @Override
  public PageResponse<HarnessTag> list(PageRequest<HarnessTag> request) {
    return wingsPersistence.query(HarnessTag.class, request);
  }

  @Override
  public void delete(@NotBlank String accountId, @NotBlank String key) {
    wingsPersistence.delete(wingsPersistence.createQuery(HarnessTag.class)
                                .filter(HarnessTagKeys.accountId, accountId)
                                .filter(HarnessTagKeys.key, key.trim()));
  }

  @Override
  public void delete(@NotNull HarnessTag tag) {
    this.delete(tag.getAccountId(), tag.getKey());
  }

  @Override
  public void attachTag(HarnessTagLink tagLink) {
    validateAndSanitizeTagLink(tagLink);
    validateAndCreateTagIfNeeded(tagLink.getAccountId(), tagLink.getKey(), tagLink.getValue());

    HarnessTagLink existingTagLink = wingsPersistence.createQuery(HarnessTagLink.class)
                                         .filter(HarnessTagLinkKeys.accountId, tagLink.getAccountId())
                                         .filter(HarnessTagLinkKeys.entityId, tagLink.getEntityId())
                                         .filter(HarnessTagLinkKeys.key, tagLink.getKey())
                                         .get();

    if (existingTagLink != null) {
      wingsPersistence.updateField(
          HarnessTagLink.class, existingTagLink.getUuid(), HarnessTagLinkKeys.value, tagLink.getValue());
    } else {
      wingsPersistence.save(tagLink);
    }
  }

  @Override
  public void detachTag(@NotBlank String accountId, @NotBlank String entityId, @NotBlank String key) {
    wingsPersistence.delete(wingsPersistence.createQuery(HarnessTagLink.class)
                                .filter(HarnessTagLinkKeys.accountId, accountId)
                                .filter(HarnessTagLinkKeys.entityId, entityId)
                                .filter(HarnessTagLinkKeys.key, key.trim()));
  }

  @Override
  public PageResponse<HarnessTagLink> listResourcesWithTag(PageRequest<HarnessTagLink> request) {
    return wingsPersistence.query(HarnessTagLink.class, request);
  }

  private void sanitizeHarnessTag(HarnessTag tag) {
    tag.setKey(tag.getKey().trim());
    if (isNotEmpty(tag.getAllowedValues())) {
      Set<String> sanitizedAllowedValues = new HashSet<>();
      for (String value : tag.getAllowedValues()) {
        sanitizedAllowedValues.add(value.trim());
      }
      tag.setAllowedValues(sanitizedAllowedValues);
    }
  }

  private void validateAndSanitizeTagLink(HarnessTagLink tagLink) {
    if (!supportedEntityTypes.contains(tagLink.getEntityType())) {
      throw new InvalidRequestException("Unsupported entityType specified. " + tagLink.getEntityType());
    }

    if (isBlank(tagLink.getKey())) {
      throw new InvalidRequestException("Tag key cannot be blank");
    }

    if (tagLink.getValue() == null) {
      throw new InvalidRequestException("Tag value cannot be null");
    }

    tagLink.setKey(tagLink.getKey().trim());
    tagLink.setValue(tagLink.getValue().trim());
  }

  private void validateAndCreateTagIfNeeded(String accountId, String key, String value) {
    HarnessTag existingTag = get(accountId, key);
    if (existingTag == null) {
      create(HarnessTag.builder().accountId(accountId).key(key).build());
      return;
    }

    if (isNotEmpty(existingTag.getAllowedValues()) && !existingTag.getAllowedValues().contains(value)) {
      throw new InvalidRequestException(
          String.format("'%s' is not in allowedValues:%s for Tag:%s", value, existingTag.getAllowedValues(), key));
    }
  }
}
