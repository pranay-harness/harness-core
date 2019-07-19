package software.wings.service.impl;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.govern.Switch.unhandled;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.beans.EntityType.APPLICATION;
import static software.wings.beans.EntityType.ENVIRONMENT;
import static software.wings.beans.EntityType.PIPELINE;
import static software.wings.beans.EntityType.PROVISIONER;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.EntityType.TRIGGER;
import static software.wings.beans.EntityType.WORKFLOW;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.mongodb.BasicDBObject;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.persistence.PersistentEntity;
import io.harness.validation.Update;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotBlank;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.EntityType;
import software.wings.beans.Event.Type;
import software.wings.beans.HarnessTag;
import software.wings.beans.HarnessTag.HarnessTagKeys;
import software.wings.beans.HarnessTagLink;
import software.wings.beans.HarnessTagLink.HarnessTagLinkKeys;
import software.wings.beans.ResourceLookup;
import software.wings.beans.User;
import software.wings.beans.trigger.Trigger;
import software.wings.dl.WingsPersistence;
import software.wings.security.PermissionAttribute;
import software.wings.security.PermissionAttribute.Action;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.UserThreadLocal;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.HarnessTagService;
import software.wings.service.intfc.InfrastructureProvisionerService;
import software.wings.service.intfc.PipelineService;
import software.wings.service.intfc.ResourceLookupService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.yaml.YamlPushService;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.validation.constraints.NotNull;
import javax.validation.executable.ValidateOnExecution;

@ValidateOnExecution
@Singleton
@Slf4j
public class HarnessTagServiceImpl implements HarnessTagService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private AuthHandler authHandler;
  @Inject private ResourceLookupService resourceLookupService;
  @Inject private YamlPushService yamlPushService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private EnvironmentService environmentService;
  @Inject private WorkflowService workflowService;
  @Inject private PipelineService pipelineService;
  @Inject private InfrastructureProvisionerService infrastructureProvisionerService;
  @Inject private TriggerService triggerService;
  @Inject private AuthService authService;
  @Inject private AppService appService;
  @Inject private EntityNameCache entityNameCache;

  private static final Set<EntityType> supportedEntityTypes =
      ImmutableSet.of(SERVICE, ENVIRONMENT, WORKFLOW, PROVISIONER, PIPELINE, TRIGGER, APPLICATION);

  private static final String ALLOWED_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-_ /";
  private static final Set<Character> ALLOWED_CHARS_SET = Sets.newHashSet(Lists.charactersOf(ALLOWED_CHARS));

  private static int MAX_TAG_KEY_LENGTH = 128;
  private static int MAX_TAG_VALUE_LENGTH = 256;
  private static long MAX_TAGS_PER_ACCOUNT = 500L;
  private static long MAX_TAGS_PER_RESOURCE = 50L;

  @Override
  public HarnessTag createTag(HarnessTag tag, boolean syncFromGit) {
    sanitizeAndValidateHarnessTag(tag);

    HarnessTag existingTag = get(tag.getAccountId(), tag.getKey());

    if (existingTag != null) {
      throw new InvalidRequestException("Tag with given Tag Name already exists");
    }

    if (getTagCount(tag.getAccountId()) >= MAX_TAGS_PER_ACCOUNT) {
      throw new InvalidRequestException("Cannot add more tags. Maximum tags supported are " + MAX_TAGS_PER_ACCOUNT);
    }

    wingsPersistence.save(tag);
    HarnessTag savedTag = get(tag.getAccountId(), tag.getKey());

    yamlPushService.pushYamlChangeSet(savedTag.getAccountId(), savedTag, savedTag, Type.UPDATE, syncFromGit, false);

    return savedTag;
  }

  @Override
  public HarnessTag create(HarnessTag tag) {
    return createTag(tag, false);
  }

  @Override
  public HarnessTag updateTag(HarnessTag tag, boolean syncFromGit) {
    sanitizeAndValidateHarnessTag(tag);

    HarnessTag existingTag = get(tag.getAccountId(), tag.getKey());
    if (existingTag == null) {
      throw new InvalidRequestException("Tag with given Tag Name does not exist");
    }

    validateAllowedValuesUpdate(tag, existingTag);

    wingsPersistence.updateField(
        HarnessTag.class, existingTag.getUuid(), HarnessTagKeys.allowedValues, tag.getAllowedValues());
    HarnessTag updatedTag = get(tag.getAccountId(), tag.getKey());

    yamlPushService.pushYamlChangeSet(
        updatedTag.getAccountId(), updatedTag, updatedTag, Type.UPDATE, syncFromGit, false);

    return updatedTag;
  }

  @Override
  public HarnessTag update(HarnessTag tag) {
    return updateTag(tag, false);
  }

  private void validateAllowedValuesUpdate(HarnessTag harnessTag, HarnessTag existingTag) {
    Set<String> existingAllowedValues = existingTag.getAllowedValues();
    if (isEmpty(existingAllowedValues)) {
      return;
    }

    Set<String> removedAllowedValues = existingAllowedValues;
    removedAllowedValues.removeAll(harnessTag.getAllowedValues());

    if (isEmpty(removedAllowedValues)) {
      return;
    }

    Set<String> inUseValues = getInUseValues(harnessTag.getAccountId(), harnessTag.getKey());
    inUseValues.retainAll(removedAllowedValues);

    if (isNotEmpty(inUseValues)) {
      throw new InvalidRequestException(format("Tag value %s is in use. Cannot delete", String.join(",", inUseValues)));
    }
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
  public PageResponse<HarnessTag> listTagsWithInUseValues(PageRequest<HarnessTag> request) {
    PageResponse<HarnessTag> response = list(request);

    List<HarnessTag> tags = response.getResponse();
    if (isEmpty(tags)) {
      return response;
    }

    for (HarnessTag harnessTag : tags) {
      harnessTag.setInUseValues(getInUseValues(harnessTag.getAccountId(), harnessTag.getKey()));
    }

    return response;
  }

  public List<HarnessTag> listTags(String accountId) {
    return wingsPersistence.createQuery(HarnessTag.class).filter(HarnessTagKeys.accountId, accountId).asList();
  }

  @Override
  public void deleteTag(@NotBlank String accountId, @NotBlank String key, boolean syncFromGit) {
    if (isTagInUse(accountId, key)) {
      throw new InvalidRequestException("Tag is in use. Cannot delete");
    }

    HarnessTag harnessTag = get(accountId, key);
    if (harnessTag == null) {
      return;
    }

    wingsPersistence.delete(wingsPersistence.createQuery(HarnessTag.class)
                                .filter(HarnessTagKeys.accountId, accountId)
                                .filter(HarnessTagKeys.key, key.trim()));

    yamlPushService.pushYamlChangeSet(
        harnessTag.getAccountId(), harnessTag, harnessTag, Type.UPDATE, syncFromGit, false);
  }

  @Override
  public void delete(@NotBlank String accountId, @NotBlank String key) {
    deleteTag(accountId, key, false);
  }

  @Override
  public void delete(@NotNull HarnessTag tag) {
    this.delete(tag.getAccountId(), tag.getKey());
  }

  @Override
  @ValidationGroups(Update.class)
  public void attachTag(HarnessTagLink tagLink) {
    attachTagWithoutGitPush(tagLink);
    pushTagLinkToGit(tagLink.getAccountId(), tagLink.getAppId(), tagLink.getEntityId(), tagLink.getEntityType(), false);
  }

  @Override
  public void attachTagWithoutGitPush(HarnessTagLink tagLink) {
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

    resourceLookupService.updateResourceLookupRecordWithTags(
        tagLink.getAccountId(), tagLink.getEntityId(), tagLink.getKey(), tagLink.getValue(), true);
  }

  @Override
  @ValidationGroups(Update.class)
  public void detachTag(HarnessTagLink tagLink) {
    detachTagWithoutGitPush(tagLink.getAccountId(), tagLink.getEntityId(), tagLink.getKey());
    pushTagLinkToGit(tagLink.getAccountId(), tagLink.getAppId(), tagLink.getEntityId(), tagLink.getEntityType(), false);
  }

  @Override
  public void detachTagWithoutGitPush(@NotBlank String accountId, @NotBlank String entityId, @NotBlank String key) {
    wingsPersistence.delete(wingsPersistence.createQuery(HarnessTagLink.class)
                                .filter(HarnessTagLinkKeys.accountId, accountId)
                                .filter(HarnessTagLinkKeys.entityId, entityId)
                                .filter(HarnessTagLinkKeys.key, key.trim()));

    resourceLookupService.updateResourceLookupRecordWithTags(accountId, entityId, key, null, false);
  }

  @Override
  public PageResponse<HarnessTagLink> listResourcesWithTag(String accountId, PageRequest<HarnessTagLink> request) {
    PageRequest<HarnessTagLink> pageRequest = request.copy();
    int offset = pageRequest.getStart();
    int limit = pageRequest.getPageSize();

    pageRequest.setOffset("0");
    pageRequest.setLimit(String.valueOf(Integer.MAX_VALUE));

    PageResponse<HarnessTagLink> pageResponse = wingsPersistence.query(HarnessTagLink.class, pageRequest);
    List<HarnessTagLink> filteredResourcesWithTag = applyAuthFilters(accountId, pageResponse.getResponse());

    List<HarnessTagLink> response;
    int total = filteredResourcesWithTag.size();
    if (total <= offset) {
      response = new ArrayList<>();
    } else {
      int endIdx = Math.min(offset + limit, total);
      response = filteredResourcesWithTag.subList(offset, endIdx);
    }

    if (isNotEmpty(response)) {
      for (HarnessTagLink harnessTagLink : response) {
        try {
          String entityName =
              entityNameCache.getEntityName(harnessTagLink.getEntityType(), harnessTagLink.getEntityId());
          String appName = entityNameCache.getEntityName(APPLICATION, harnessTagLink.getAppId());

          harnessTagLink.setEntityName(entityName);
          harnessTagLink.setAppName(appName);
        } catch (ExecutionException ex) {
          throw new WingsException("Failed to find entity name", ex, USER);
        }
      }
    }

    return aPageResponse()
        .withResponse(response)
        .withTotal(filteredResourcesWithTag.size())
        .withOffset(request.getOffset())
        .withLimit(request.getLimit())
        .build();
  }

  @Override
  public void pruneTagLinks(String accountId, String entityId) {
    wingsPersistence.delete(wingsPersistence.createQuery(HarnessTagLink.class)
                                .filter(HarnessTagLinkKeys.accountId, accountId)
                                .filter(HarnessTagLinkKeys.entityId, entityId));
  }

  @Override
  @ValidationGroups(Update.class)
  public void authorizeTagAttachDetach(String appId, HarnessTagLink tagLink) {
    validateResourceAccess(appId, tagLink, Action.UPDATE);
  }

  private void sanitizeAndValidateHarnessTag(HarnessTag tag) {
    tag.setKey(validateTagKey(tag.getKey()));

    if (isNotEmpty(tag.getAllowedValues())) {
      Set<String> sanitizedAllowedValues = new HashSet<>();
      for (String value : tag.getAllowedValues()) {
        sanitizedAllowedValues.add(validateTagValue(value));
      }
      tag.setAllowedValues(sanitizedAllowedValues);
    }
  }

  private String validateTagKey(String key) {
    if (isBlank(key)) {
      throw new InvalidRequestException("Tag name cannot be blank");
    }

    if (key.length() > MAX_TAG_KEY_LENGTH) {
      throw new InvalidRequestException("Max allowed size for tag name is " + MAX_TAG_KEY_LENGTH);
    }

    validateTagNameValueCharacterSet(key);
    return key.trim();
  }

  private String validateTagValue(String value) {
    if (value == null) {
      throw new InvalidRequestException("Tag value cannot be null");
    }

    if (value.length() > MAX_TAG_VALUE_LENGTH) {
      throw new InvalidRequestException("Max allowed size for tag value is " + MAX_TAG_VALUE_LENGTH);
    }

    validateTagNameValueCharacterSet(value);
    return value.trim();
  }

  private void validateTagNameValueCharacterSet(String value) {
    value = value.trim();

    if (!ALLOWED_CHARS_SET.containsAll(Lists.charactersOf(value))) {
      throw new InvalidRequestException("Tag name/value can contain only " + ALLOWED_CHARS);
    }

    if (Sets.newHashSet('_', '-', '/').contains(value.charAt(0))) {
      throw new InvalidRequestException("Tag name/value cannot begin with -_/");
    }
  }

  private void validateAndSanitizeTagLink(HarnessTagLink tagLink) {
    if (!supportedEntityTypes.contains(tagLink.getEntityType())) {
      throw new InvalidRequestException("Unsupported entityType specified. " + tagLink.getEntityType());
    }

    notNullCheck("appId", tagLink.getAppId());

    if (tagLink.getValue() == null) {
      throw new InvalidRequestException("Tag value cannot be null");
    }

    tagLink.setKey(validateTagKey(tagLink.getKey()));
    tagLink.setValue(validateTagValue(tagLink.getValue()));
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

    return wingsPersistence.getCollection(HarnessTagLink.class).findOne(andQuery) != null;
  }

  private Set<String> getInUseValues(String accountId, String key) {
    BasicDBObject andQuery = new BasicDBObject();
    List<BasicDBObject> conditions = new ArrayList<>();
    conditions.add(new BasicDBObject(HarnessTagLinkKeys.accountId, accountId));
    conditions.add(new BasicDBObject(HarnessTagLinkKeys.key, key));
    andQuery.put("$and", conditions);

    return new HashSet<>(
        wingsPersistence.getCollection(HarnessTagLink.class).distinct(HarnessTagLinkKeys.value, andQuery));
  }

  private List<HarnessTagLink> applyAuthFilters(String accountId, List<HarnessTagLink> tagLinks) {
    List<HarnessTagLink> filteredTagLinks = new ArrayList<>();

    if (tagLinks == null) {
      return filteredTagLinks;
    }

    tagLinks.forEach(tagLink -> {
      try {
        ResourceLookup resourceLookup = resourceLookupService.getWithResourceId(accountId, tagLink.getEntityId());

        if (resourceLookup != null) {
          validateResourceAccess(resourceLookup.getAppId(), tagLink, Action.READ);
          filteredTagLinks.add(tagLink);
        }
      } catch (Exception ex) {
        // Exception is thrown if the user does not have permissions on the entity
      }
    });

    return filteredTagLinks;
  }

  private void validateResourceAccess(String appId, HarnessTagLink tagLink, Action action) {
    notNullCheck("appId cannot be null", appId);

    if (EntityType.APPLICATION.equals(tagLink.getEntityType())) {
      authorizeApplication(appId, tagLink, action);
      return;
    }

    if (EntityType.TRIGGER.equals(tagLink.getEntityType())) {
      // For Read action, we check if the user has access to App or not.
      // This is consistent with what is done in trigger resource list
      if (Action.READ.equals(action)) {
        authorizeApplication(appId, tagLink, action);
      } else {
        authorizeTriggers(appId, tagLink);
      }

      return;
    }

    PermissionType permissionType = getPermissionType(tagLink.getEntityType());
    PermissionAttribute permissionAttribute = new PermissionAttribute(permissionType, action);

    authHandler.authorize(asList(permissionAttribute), asList(appId), tagLink.getEntityId());
  }

  private void authorizeTriggers(String appId, HarnessTagLink tagLink) {
    Trigger existingTrigger = triggerService.get(appId, tagLink.getEntityId());
    if (existingTrigger == null) {
      throw new WingsException("Trigger does not exist", USER);
    }
    triggerService.authorize(existingTrigger, true);
  }

  private void authorizeApplication(String appId, HarnessTagLink tagLink, Action action) {
    User user = UserThreadLocal.get();

    authService.authorizeAppAccess(tagLink.getAccountId(), appId, user, action);
  }

  private PermissionType getPermissionType(EntityType entityType) {
    PermissionType permissionType;

    switch (entityType) {
      case SERVICE:
        permissionType = PermissionType.SERVICE;
        break;

      case ENVIRONMENT:
        permissionType = PermissionType.ENV;
        break;

      case WORKFLOW:
        permissionType = PermissionType.WORKFLOW;
        break;

      case PIPELINE:
        permissionType = PermissionType.PIPELINE;
        break;

      case PROVISIONER:
        permissionType = PermissionType.PROVISIONER;
        break;

      default:
        unhandled(entityType);
        throw new InvalidRequestException(format("Unsupported entity type %s for tags", entityType), USER);
    }

    return permissionType;
  }

  @Override
  public List<HarnessTagLink> getTagLinksWithEntityId(String accountId, String entityId) {
    return wingsPersistence.createQuery(HarnessTagLink.class)
        .filter(HarnessTagLinkKeys.accountId, accountId)
        .filter(HarnessTagLinkKeys.entityId, entityId)
        .order(HarnessTagLinkKeys.key)
        .asList();
  }

  @Override
  public void pushTagLinkToGit(
      String accountId, String appId, String entityId, EntityType entityType, boolean syncFromGit) {
    PersistentEntity resource = getPersistentEntity(appId, entityId, entityType);

    yamlPushService.pushYamlChangeSet(accountId, resource, resource, Type.UPDATE, syncFromGit, false);
  }

  private PersistentEntity getPersistentEntity(String appId, String entityId, EntityType entityType) {
    switch (entityType) {
      case SERVICE:
        return serviceResourceService.get(appId, entityId, false);

      case ENVIRONMENT:
        return environmentService.get(appId, entityId, false);

      case WORKFLOW:
        return workflowService.readWorkflow(appId, entityId);

      case PIPELINE:
        return pipelineService.readPipeline(appId, entityId, false);

      case PROVISIONER:
        return infrastructureProvisionerService.get(appId, entityId);

      case TRIGGER:
        return triggerService.get(appId, entityId);

      case APPLICATION:
        return appService.get(entityId, false);

      default:
        unhandled(entityType);
    }

    return null;
  }

  private void pushTagsToGit(HarnessTag harnessTag, boolean syncFromGit) {
    yamlPushService.pushYamlChangeSet(
        harnessTag.getAccountId(), harnessTag, harnessTag, Type.UPDATE, syncFromGit, false);
  }
}
