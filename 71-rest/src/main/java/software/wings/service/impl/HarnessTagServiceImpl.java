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

import com.mongodb.BasicDBObject;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.ReadPref;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotBlank;
import software.wings.beans.EntityType;
import software.wings.beans.HarnessTag;
import software.wings.beans.HarnessTag.HarnessTagKeys;
import software.wings.beans.HarnessTagLink;
import software.wings.beans.HarnessTagLink.HarnessTagLinkKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.HarnessTagService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

  private static int MAX_TAG_KEY_LENGTH = 128;
  private static int MAX_TAG_VALUE_LENGTH = 256;
  private static long MAX_TAGS_PER_ACCOUNT = 500L;
  private static long MAX_TAGS_PER_RESOURCE = 50L;

  @Override
  public HarnessTag create(HarnessTag tag) {
    sanitizeAndValidateHarnessTag(tag);

    HarnessTag existingTag = get(tag.getAccountId(), tag.getKey());

    if (existingTag != null) {
      throw new InvalidRequestException("Tag with given key already exists");
    }

    if (getTagCount(tag.getAccountId()) >= MAX_TAGS_PER_ACCOUNT) {
      throw new InvalidRequestException("Cannot add more tags. Maximum tags supported are " + MAX_TAGS_PER_ACCOUNT);
    }

    wingsPersistence.save(tag);
    return get(tag.getAccountId(), tag.getKey());
  }

  @Override
  public HarnessTag update(HarnessTag tag) {
    sanitizeAndValidateHarnessTag(tag);

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
  public HarnessTag getTagWithInUseValues(@NotBlank String accountId, @NotBlank String key) {
    HarnessTag result = wingsPersistence.createQuery(HarnessTag.class)
                            .filter(HarnessTagKeys.accountId, accountId)
                            .filter(HarnessTagKeys.key, key.trim())
                            .get();

    if (result == null) {
      return null;
    }

    result.setInUseValues(getInUseValues(accountId, key));

    return result;
  }

  @Override
  public PageResponse<HarnessTag> list(PageRequest<HarnessTag> request) {
    return wingsPersistence.query(HarnessTag.class, request);
  }

  @Override
  public void delete(@NotBlank String accountId, @NotBlank String key) {
    if (isTagInUse(accountId, key)) {
      throw new InvalidRequestException("Tag is in use. Cannot delete");
    }
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
      if (getTagLinkCount(tagLink.getAccountId(), tagLink.getEntityId()) >= MAX_TAGS_PER_RESOURCE) {
        throw new InvalidRequestException(
            "Cannot attach more tags on resource. Maximum tags supported are " + MAX_TAGS_PER_RESOURCE);
      }
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

  @Override
  public void pruneTagLinks(String accountId, String entityId) {
    wingsPersistence.delete(wingsPersistence.createQuery(HarnessTagLink.class)
                                .filter(HarnessTagLinkKeys.accountId, accountId)
                                .filter(HarnessTagLinkKeys.entityId, entityId));
  }

  private void sanitizeAndValidateHarnessTag(HarnessTag tag) {
    tag.setKey(tag.getKey().trim());

    validateTagKey(tag.getKey());

    if (isNotEmpty(tag.getAllowedValues())) {
      Set<String> sanitizedAllowedValues = new HashSet<>();
      for (String value : tag.getAllowedValues()) {
        validateTagValue(value);
        sanitizedAllowedValues.add(value.trim());
      }
      tag.setAllowedValues(sanitizedAllowedValues);
    }
  }

  private void validateTagKey(String key) {
    if (isBlank(key)) {
      throw new InvalidRequestException("Tag key cannot be blank");
    }

    if (key.length() > MAX_TAG_KEY_LENGTH) {
      throw new InvalidRequestException("Max allowed size for tag key is " + MAX_TAG_KEY_LENGTH);
    }
  }

  private void validateTagValue(String value) {
    if (value == null) {
      throw new InvalidRequestException("Tag value cannot be null");
    }

    if (value.length() > MAX_TAG_VALUE_LENGTH) {
      throw new InvalidRequestException("Max allowed size for tag value is " + MAX_TAG_VALUE_LENGTH);
    }
  }

  private void validateAndSanitizeTagLink(HarnessTagLink tagLink) {
    if (!supportedEntityTypes.contains(tagLink.getEntityType())) {
      throw new InvalidRequestException("Unsupported entityType specified. " + tagLink.getEntityType());
    }

    if (tagLink.getValue() == null) {
      throw new InvalidRequestException("Tag value cannot be null");
    }

    String trimmedKey = tagLink.getKey().trim();
    String trimmedValue = tagLink.getValue().trim();

    validateTagKey(trimmedKey);
    validateTagValue(trimmedValue);

    tagLink.setKey(trimmedKey);
    tagLink.setValue(trimmedValue);
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

  private long getTagCount(String accountId) {
    return wingsPersistence.createQuery(HarnessTag.class).filter(HarnessTagKeys.accountId, accountId).count();
  }

  private long getTagLinkCount(String accountId, String entityId) {
    return wingsPersistence.createQuery(HarnessTagLink.class)
        .filter(HarnessTagLinkKeys.accountId, accountId)
        .filter(HarnessTagLinkKeys.entityId, entityId)
        .count();
  }

  private boolean isTagInUse(String accountId, String key) {
    BasicDBObject andQuery = new BasicDBObject();
    List<BasicDBObject> conditions = new ArrayList<>();
    conditions.add(new BasicDBObject(HarnessTagLinkKeys.accountId, accountId));
    conditions.add(new BasicDBObject(HarnessTagLinkKeys.key, key));
    andQuery.put("$and", conditions);

    return wingsPersistence.getCollection(HarnessTagLink.class, ReadPref.NORMAL).findOne(andQuery) != null;
  }

  private Set<String> getInUseValues(String accountId, String key) {
    BasicDBObject andQuery = new BasicDBObject();
    List<BasicDBObject> conditions = new ArrayList<>();
    conditions.add(new BasicDBObject(HarnessTagLinkKeys.accountId, accountId));
    conditions.add(new BasicDBObject(HarnessTagLinkKeys.key, key));
    andQuery.put("$and", conditions);

    return new HashSet<>(wingsPersistence.getCollection(HarnessTagLink.class, ReadPref.NORMAL)
                             .distinct(HarnessTagLinkKeys.value, andQuery));
  }
}
