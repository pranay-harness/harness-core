package software.wings.service.impl;

import static io.harness.beans.SearchFilter.Operator.EQ;
import static software.wings.audit.ResourceType.APPLICATION;
import static software.wings.audit.ResourceType.ARTIFACT_SERVER;
import static software.wings.audit.ResourceType.CLOUD_PROVIDER;
import static software.wings.audit.ResourceType.COLLABORATION_PROVIDER;
import static software.wings.audit.ResourceType.CONNECTION_ATTRIBUTES;
import static software.wings.audit.ResourceType.ENCRYPTED_RECORDS;
import static software.wings.audit.ResourceType.ENVIRONMENT;
import static software.wings.audit.ResourceType.LOAD_BALANCER;
import static software.wings.audit.ResourceType.PIPELINE;
import static software.wings.audit.ResourceType.PROVISIONER;
import static software.wings.audit.ResourceType.SERVICE;
import static software.wings.audit.ResourceType.SETTING;
import static software.wings.audit.ResourceType.SOURCE_REPO_PROVIDER;
import static software.wings.audit.ResourceType.TEMPLATE;
import static software.wings.audit.ResourceType.TEMPLATE_FOLDER;
import static software.wings.audit.ResourceType.TRIGGER;
import static software.wings.audit.ResourceType.USER_GROUP;
import static software.wings.audit.ResourceType.VERIFICATION_PROVIDER;
import static software.wings.audit.ResourceType.WORKFLOW;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotBlank;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import software.wings.audit.EntityAuditRecord;
import software.wings.beans.Application;
import software.wings.beans.NameValuePair;
import software.wings.beans.ResourceLookup;
import software.wings.beans.ResourceLookup.ResourceLookupKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.ResourceLookupService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.yaml.YamlResourceService;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Audit Service Implementation class.
 *
 * @author Rishi
 */
@Singleton
@Slf4j
public class ResourceLookupServiceImpl implements ResourceLookupService {
  @Inject private FileService fileService;
  @Inject private TimeLimiter timeLimiter;
  @Inject private EntityHelper entityHelper;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private EntityNameCache entityNameCache;
  @Inject private YamlResourceService yamlResourceService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private EnvironmentService environmentService;
  @Inject private HarnessTagFilterHelper harnessTagFilterHelper;

  private static List<String> applicationLevelResource =
      Arrays.asList(APPLICATION.name(), SERVICE.name(), ENVIRONMENT.name(), WORKFLOW.name(), PIPELINE.name(),
          PROVISIONER.name(), TRIGGER.name(), TEMPLATE.name(), TEMPLATE_FOLDER.name());

  private static List<String> accountLevelResource = Arrays.asList(CLOUD_PROVIDER.name(), ARTIFACT_SERVER.name(),
      SOURCE_REPO_PROVIDER.name(), COLLABORATION_PROVIDER.name(), LOAD_BALANCER.name(), VERIFICATION_PROVIDER.name(),
      SETTING.name(), ENCRYPTED_RECORDS.name(), TEMPLATE.name(), TEMPLATE_FOLDER.name(), CONNECTION_ATTRIBUTES.name(),
      USER_GROUP.name());

  private static Set<String> resourceTypeSet = new HashSet<>();

  static {
    resourceTypeSet.addAll(applicationLevelResource);
    resourceTypeSet.addAll(accountLevelResource);
  }

  private WingsPersistence wingsPersistence;

  @Inject
  public ResourceLookupServiceImpl(WingsPersistence wingsPersistence) {
    this.wingsPersistence = wingsPersistence;
  }

  @Override
  public PageResponse<ResourceLookup> list(PageRequest<ResourceLookup> req) {
    return wingsPersistence.query(ResourceLookup.class, req);
  }

  @Override
  public ResourceLookup create(ResourceLookup header) {
    String id = wingsPersistence.save(header);
    header.setUuid(id);
    return header;
  }

  @Override
  public ResourceLookup getWithResourceId(String accountId, String resourceId) {
    return wingsPersistence.createQuery(ResourceLookup.class)
        .filter(ResourceLookupKeys.resourceId, resourceId)
        .filter(ResourceLookupKeys.accountId, accountId)
        .get();
  }

  @Override
  public void updateResourceName(ResourceLookup resourceLookup) {
    if (resourceLookup == null) {
      return;
    }
    ResourceLookup lookup = wingsPersistence.createQuery(ResourceLookup.class)
                                .filter(ResourceLookupKeys.resourceId, resourceLookup.getResourceId())
                                .project(ResourceLookupKeys.uuid, true)
                                .project(ResourceLookupKeys.resourceName, true)
                                .get();

    if (lookup != null) {
      if (!lookup.getResourceName().equals(resourceLookup.getResourceName())) {
        UpdateOperations<ResourceLookup> updateOperations =
            wingsPersistence.createUpdateOperations(ResourceLookup.class)
                .set(ResourceLookupKeys.resourceName, resourceLookup.getResourceName());

        wingsPersistence.update(lookup, updateOperations);
      }
    } else {
      create(resourceLookup);
    }
  }

  @Override
  public void delete(ResourceLookup resourceLookup) {
    ResourceLookup lookup = wingsPersistence.createQuery(ResourceLookup.class)
                                .filter(ResourceLookupKeys.accountId, resourceLookup.getAccountId())
                                .filter(ResourceLookupKeys.resourceType, resourceLookup.getResourceType())
                                .filter(ResourceLookupKeys.appId, resourceLookup.getAppId())
                                .filter(resourceLookup.getResourceName(), resourceLookup.getResourceName())
                                .project(ResourceLookupKeys.uuid, true)
                                .get();

    if (lookup != null) {
      wingsPersistence.delete(ResourceLookup.class, lookup.getUuid());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<String> listApplicationLevelResourceTypes() {
    return applicationLevelResource;
  }

  @Override
  public List<String> listAccountLevelResourceTypes() {
    return accountLevelResource;
  }

  public void deleteResourceLookupRecordIfNeeded(EntityAuditRecord record, String accountId) {
    try {
      if (isResourceLookupEntity(record)) {
        delete(ResourceLookup.builder()
                   .accountId(accountId)
                   .appId(record.getAppId())
                   .resourceName(record.getEntityName())
                   .resourceType(record.getAffectedResourceType())
                   .resourceId(record.getAffectedResourceId())
                   .build());
      }
    } catch (Exception e) {
      logger.warn("Failing to create ResourceLookupEntry for Record: " + record);
    }
  }

  public void saveResourceLookupRecordIfNeeded(EntityAuditRecord record, String accountId) {
    try {
      if (isResourceLookupEntity(record)) {
        create(ResourceLookup.builder()
                   .accountId(accountId)
                   .appId(record.getAppId())
                   .resourceName(record.getEntityName())
                   .resourceType(record.getAffectedResourceType())
                   .resourceId(record.getAffectedResourceId())
                   .build());
      }
    } catch (Exception e) {
      logger.warn("Failing to create ResourceLookupEntry for Record: " + record);
    }
  }

  public <T> void updateResourceLookupRecordIfNeeded(
      EntityAuditRecord record, String accountId, T newEntity, T oldEntity) {
    try {
      if (isResourceLookupEntity(record)) {
        ResourceLookup lookup = wingsPersistence.createQuery(ResourceLookup.class)
                                    .filter(ResourceLookupKeys.accountId, accountId)
                                    .filter(ResourceLookupKeys.resourceType, record.getAffectedResourceType())
                                    .filter(ResourceLookupKeys.appId, record.getAppId())
                                    .filter(ResourceLookupKeys.resourceId, record.getAffectedResourceId())
                                    .disableValidation()
                                    .get();
        if (lookup == null) {
          saveResourceLookupRecordIfNeeded(record, accountId);
        } else {
          if (!record.getEntityName().equals(lookup.getResourceName())) {
            lookup.setResourceName(record.getEntityName());
            updateResourceName(lookup);
          }
        }
      }
    } catch (Exception e) {
      logger.warn("Failing to create ResourceLookupEntry for Record: " + record);
    }
  }

  private boolean isResourceLookupEntity(EntityAuditRecord record) {
    if (Application.GLOBAL_APP_ID.equals(record.getAppId())) {
      return true;
    }

    if (resourceTypeSet.contains(record.getEntityType())) {
      return true;
    }

    return false;
  }

  @Override
  public void updateResourceLookupRecordWithTags(
      @NotBlank String accountId, @NotBlank String entityId, @NotBlank String tagKey, String tagValue, boolean addTag) {
    Query<ResourceLookup> query = wingsPersistence.createQuery(ResourceLookup.class)
                                      .filter(ResourceLookupKeys.accountId, accountId)
                                      .filter(ResourceLookupKeys.resourceId, entityId)
                                      .disableValidation();

    if (addTag) {
      UpdateResults update = wingsPersistence.update(query.filter("tags.name", tagKey),
          wingsPersistence.createUpdateOperations(ResourceLookup.class).set("tags.$.value", tagValue));

      // if the tag doesn't exist, then add a new one
      if (update.getUpdatedCount() == 0) {
        wingsPersistence.update(wingsPersistence.createQuery(ResourceLookup.class)
                                    .filter(ResourceLookupKeys.accountId, accountId)
                                    .filter(ResourceLookupKeys.resourceId, entityId)
                                    .disableValidation(),
            wingsPersistence.createUpdateOperations(ResourceLookup.class)
                .addToSet(ResourceLookupKeys.tags, NameValuePair.builder().name(tagKey).value(tagValue).build()));
      }
    } else {
      wingsPersistence.update(query,
          wingsPersistence.createUpdateOperations(ResourceLookup.class)
              .removeAll(ResourceLookupKeys.tags, NameValuePair.builder().name(tagKey).build()));
    }
  }

  @Override
  public PageResponse<ResourceLookup> listResourceLookupRecordsWithTags(
      String accountId, String filter, String limit, String offset) {
    PageRequest<ResourceLookup> pageRequest = new PageRequest<>();

    pageRequest.addFilter(ResourceLookupKeys.accountId, EQ, accountId);
    pageRequest.setLimit(limit);
    pageRequest.setOffset(offset);

    harnessTagFilterHelper.addHarnessTagFiltersToPageRequest(pageRequest, filter);
    return wingsPersistence.query(ResourceLookup.class, pageRequest);
  }
}