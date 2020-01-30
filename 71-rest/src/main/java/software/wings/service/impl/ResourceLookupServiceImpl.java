package software.wings.service.impl;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.beans.SearchFilter.Operator.IN;
import static io.harness.beans.SortOrder.OrderType.ASC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.audit.ResourceType.API_KEY;
import static software.wings.audit.ResourceType.APPLICATION;
import static software.wings.audit.ResourceType.ARTIFACT_SERVER;
import static software.wings.audit.ResourceType.CLOUD_PROVIDER;
import static software.wings.audit.ResourceType.COLLABORATION_PROVIDER;
import static software.wings.audit.ResourceType.CONNECTION_ATTRIBUTES;
import static software.wings.audit.ResourceType.DELEGATE;
import static software.wings.audit.ResourceType.DELEGATE_PROFILE;
import static software.wings.audit.ResourceType.DELEGATE_SCOPE;
import static software.wings.audit.ResourceType.DEPLOYMENT_TRIGGER;
import static software.wings.audit.ResourceType.ENCRYPTED_RECORDS;
import static software.wings.audit.ResourceType.ENVIRONMENT;
import static software.wings.audit.ResourceType.LOAD_BALANCER;
import static software.wings.audit.ResourceType.PIPELINE;
import static software.wings.audit.ResourceType.PROVISIONER;
import static software.wings.audit.ResourceType.SECRET_MANAGER;
import static software.wings.audit.ResourceType.SERVICE;
import static software.wings.audit.ResourceType.SETTING;
import static software.wings.audit.ResourceType.SOURCE_REPO_PROVIDER;
import static software.wings.audit.ResourceType.SSO_SETTINGS;
import static software.wings.audit.ResourceType.TEMPLATE;
import static software.wings.audit.ResourceType.TEMPLATE_FOLDER;
import static software.wings.audit.ResourceType.TRIGGER;
import static software.wings.audit.ResourceType.USER;
import static software.wings.audit.ResourceType.USER_GROUP;
import static software.wings.audit.ResourceType.USER_INVITE;
import static software.wings.audit.ResourceType.VERIFICATION_PROVIDER;
import static software.wings.audit.ResourceType.WHITELISTED_IP;
import static software.wings.audit.ResourceType.WORKFLOW;
import static software.wings.service.impl.HarnessTagServiceImpl.supportedTagEntityTypes;

import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.UuidAware;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotBlank;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import software.wings.audit.EntityAuditRecord;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.NameValuePair;
import software.wings.beans.Pipeline;
import software.wings.beans.ResourceLookup;
import software.wings.beans.ResourceLookup.ResourceLookupKeys;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.entityinterface.TagAware;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.beans.trigger.Trigger;
import software.wings.dl.WingsPersistence;
import software.wings.security.PermissionAttribute.Action;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.HarnessTagService;
import software.wings.service.intfc.ResourceLookupService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.yaml.YamlResourceService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
  @Inject private ResourceLookupFilterHelper resourceLookupFilterHelper;
  @Inject private HarnessTagService harnessTagService;
  @Inject private AppService appService;

  private static List<String> applicationLevelResource =
      Arrays.asList(APPLICATION.name(), SERVICE.name(), ENVIRONMENT.name(), WORKFLOW.name(), PIPELINE.name(),
          PROVISIONER.name(), TRIGGER.name(), TEMPLATE.name(), TEMPLATE_FOLDER.name(), DEPLOYMENT_TRIGGER.name());

  private static List<String> accountLevelResource = Arrays.asList(CLOUD_PROVIDER.name(), ARTIFACT_SERVER.name(),
      SOURCE_REPO_PROVIDER.name(), COLLABORATION_PROVIDER.name(), LOAD_BALANCER.name(), VERIFICATION_PROVIDER.name(),
      SETTING.name(), ENCRYPTED_RECORDS.name(), TEMPLATE.name(), TEMPLATE_FOLDER.name(), CONNECTION_ATTRIBUTES.name(),
      USER_GROUP.name(), SECRET_MANAGER.name(), USER_GROUP.name(), SECRET_MANAGER.name(), USER.name(),
      USER_INVITE.name(), API_KEY.name(), WHITELISTED_IP.name(), DELEGATE.name(), DELEGATE_PROFILE.name(),
      DELEGATE_SCOPE.name(), SSO_SETTINGS.name());

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
        .filter(ResourceLookupKeys.accountId, accountId)
        .filter(ResourceLookupKeys.resourceId, resourceId)
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
                                .filter(ResourceLookupKeys.resourceName, resourceLookup.getResourceName())
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

  @Override
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
      logger.warn("Failing to create ResourceLookupEntry for Record: {}", record);
    }
  }

  @Override
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
      logger.warn("Failing to create ResourceLookupEntry for Record: {}", record);
    }
  }

  @Override
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

    if (resourceTypeSet.contains(record.getEntityType())
        || resourceTypeSet.contains(record.getAffectedResourceType())) {
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
    return listResourceLookupRecordsWithTagsInternal(
        accountId, filter, limit, offset, supportedTagEntityTypes.toArray());
  }

  private PageResponse<ResourceLookup> listResourceLookupRecordsWithTagsInternal(
      String accountId, String filter, String limit, String offset, Object[] entityTypes) {
    PageRequest<ResourceLookup> pageRequest = new PageRequest<>();

    pageRequest.addFilter(ResourceLookupKeys.accountId, EQ, accountId);
    pageRequest.setOffset("0");
    pageRequest.setLimit(String.valueOf(Integer.MAX_VALUE));
    pageRequest.addFilter(ResourceLookupKeys.resourceType, IN, entityTypes);
    pageRequest.addOrder(ResourceLookupKeys.resourceName, ASC);
    resourceLookupFilterHelper.addResourceLookupFiltersToPageRequest(pageRequest, filter);

    PageResponse<ResourceLookup> pageResponse = wingsPersistence.query(ResourceLookup.class, pageRequest);
    List<ResourceLookup> filteredResourceLookups = applyAuthFilters(pageResponse.getResponse());

    List<ResourceLookup> response;
    int total = filteredResourceLookups.size();
    int offsetValue = isNotBlank(offset) ? Integer.parseInt(offset) : 0;
    int limitValue = isNotBlank(limit) ? Integer.parseInt(limit) : PageRequest.DEFAULT_UNLIMITED;

    if (total <= offsetValue) {
      response = new ArrayList<>();
    } else {
      int endIdx = Math.min(offsetValue + limitValue, total);
      response = filteredResourceLookups.subList(offsetValue, endIdx);
    }

    return aPageResponse()
        .withResponse(response)
        .withTotal(filteredResourceLookups.size())
        .withOffset(offset)
        .withLimit(limit)
        .build();
  }

  private List<ResourceLookup> applyAuthFilters(List<ResourceLookup> resourceLookups) {
    List<ResourceLookup> filteredResourceLookupList = new ArrayList<>();

    if (isEmpty(resourceLookups)) {
      return filteredResourceLookupList;
    }

    resourceLookups.forEach(resourceLookup -> {
      try {
        EntityType entityType = EntityType.valueOf(resourceLookup.getResourceType());

        if (supportedTagEntityTypes.contains(entityType)) {
          harnessTagService.validateTagResourceAccess(resourceLookup.getAppId(), resourceLookup.getAccountId(),
              resourceLookup.getResourceId(), entityType, Action.READ);
          filteredResourceLookupList.add(resourceLookup);
        }

      } catch (Exception ex) {
        // Exception is thrown if the user does not have permissions on the entity
      }
    });

    return filteredResourceLookupList;
  }

  @Override
  public <T> PageResponse<T> listWithTagFilters(
      PageRequest<T> request, String filter, EntityType entityType, boolean withTags) {
    if (isNotBlank(filter)) {
      String accountId = getAccountIdFromPageRequest(request);

      if (isNotBlank(accountId)) {
        PageResponse<ResourceLookup> resourceLookupPageResponse = listResourceLookupRecordsWithTagsInternal(
            accountId, filter, String.valueOf(Integer.MAX_VALUE), "0", new Object[] {entityType});

        List<ResourceLookup> response = resourceLookupPageResponse.getResponse();
        if (isEmpty(response)) {
          return aPageResponse().withResponse(emptyList()).build();
        }

        List<String> resourceIds = response.stream().map(ResourceLookup::getResourceId).collect(Collectors.toList());
        request.addFilter("_id", IN, resourceIds.toArray());
      }
    }

    PageResponse<T> pageResponse;

    switch (entityType) {
      case SERVICE:
        pageResponse = (PageResponse<T>) wingsPersistence.query(Service.class, (PageRequest<Service>) request);
        break;

      case ENVIRONMENT:
        pageResponse = (PageResponse<T>) wingsPersistence.query(Environment.class, (PageRequest<Environment>) request);
        break;

      case WORKFLOW:
        pageResponse = (PageResponse<T>) wingsPersistence.query(Workflow.class, (PageRequest<Workflow>) request);
        break;

      case PIPELINE:
        pageResponse = (PageResponse<T>) wingsPersistence.query(Pipeline.class, (PageRequest<Pipeline>) request);
        break;

      case TRIGGER:
        pageResponse = (PageResponse<T>) wingsPersistence.query(Trigger.class, (PageRequest<Trigger>) request);
        break;

      case DEPLOYMENT_TRIGGER:
        pageResponse =
            (PageResponse<T>) wingsPersistence.query(DeploymentTrigger.class, (PageRequest<DeploymentTrigger>) request);
        break;

      case PROVISIONER:
        pageResponse = (PageResponse<T>) wingsPersistence.query(
            InfrastructureProvisioner.class, (PageRequest<InfrastructureProvisioner>) request);
        break;

      case APPLICATION:
        pageResponse = (PageResponse<T>) wingsPersistence.query(Application.class, (PageRequest<Application>) request);
        break;

      default:
        throw new InvalidRequestException(format("Unhandled entity type %s while getting list", entityType));
    }

    if (withTags) {
      setTagLinks(request, pageResponse, entityType);
    }

    return pageResponse;
  }

  private <T> String getAccountIdFromPageRequest(PageRequest<T> request) {
    List<SearchFilter> filters = request.getFilters();

    if (isEmpty(filters)) {
      return null;
    }

    String accountId = null;
    String appId = null;

    for (SearchFilter searchFilter : filters) {
      if (isNotEmpty(searchFilter.getFieldValues())) {
        if (searchFilter.getFieldName().equals("accountId")) {
          accountId = (String) searchFilter.getFieldValues()[0];
          break;
        } else if (searchFilter.getFieldName().equals("appId")) {
          appId = (String) searchFilter.getFieldValues()[0];
          break;
        }
      }
    }

    if (isNotBlank(accountId)) {
      return accountId;
    }

    return appService.getAccountIdByAppId(appId);
  }

  private <T> void setTagLinks(PageRequest<T> request, PageResponse<T> response, EntityType entityType) {
    String accountId = getAccountIdFromPageRequest(request);
    if (isBlank(accountId)) {
      return;
    }

    switch (entityType) {
      case SERVICE:
      case ENVIRONMENT:
      case WORKFLOW:
      case PIPELINE:
      case TRIGGER:
      case DEPLOYMENT_TRIGGER:
      case PROVISIONER:
      case APPLICATION:
        for (T t : response.getResponse()) {
          ((TagAware) t).setTagLinks(harnessTagService.getTagLinksWithEntityId(accountId, ((UuidAware) t).getUuid()));
        }

        break;

      default:
        throw new InvalidRequestException(format("Unhandled entity type %s while setting tags links", entityType));
    }
  }
}