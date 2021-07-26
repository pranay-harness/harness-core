package software.wings.service.impl;

import static io.harness.annotations.dev.HarnessTeam.DEL;
import static io.harness.beans.FeatureName.DO_DELEGATE_PHYSICAL_DELETE;
import static io.harness.beans.FeatureName.NEXT_GEN_ENABLED;
import static io.harness.beans.FeatureName.PER_AGENT_CAPABILITIES;
import static io.harness.beans.FeatureName.USE_CDN_FOR_STORAGE_FILES;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.convertFromBase64;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.beans.DelegateType.CE_KUBERNETES;
import static io.harness.delegate.beans.DelegateType.DOCKER;
import static io.harness.delegate.beans.DelegateType.ECS;
import static io.harness.delegate.beans.DelegateType.HELM_DELEGATE;
import static io.harness.delegate.beans.DelegateType.KUBERNETES;
import static io.harness.delegate.beans.DelegateType.SHELL_SCRIPT;
import static io.harness.delegate.beans.K8sPermissionType.NAMESPACE_ADMIN;
import static io.harness.delegate.message.ManagerMessageConstants.JRE_VERSION;
import static io.harness.delegate.message.ManagerMessageConstants.MIGRATE;
import static io.harness.delegate.message.ManagerMessageConstants.SELF_DESTRUCT;
import static io.harness.delegate.message.ManagerMessageConstants.USE_CDN;
import static io.harness.delegate.message.ManagerMessageConstants.USE_STORAGE_PROXY;
import static io.harness.eraro.ErrorCode.USAGE_LIMITS_EXCEEDED;
import static io.harness.eventsframework.EventsFrameworkMetadataConstants.DELETE_ACTION;
import static io.harness.exception.WingsException.USER;
import static io.harness.k8s.KubernetesConvention.getAccountIdentifier;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_NESTS;
import static io.harness.mongo.MongoUtils.setUnset;
import static io.harness.obfuscate.Obfuscator.obfuscate;
import static io.harness.persistence.HQuery.excludeAuthority;

import static software.wings.audit.AuditHeader.Builder.anAuditHeader;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.DelegateSequenceConfig.Builder.aDelegateSequenceBuilder;
import static software.wings.beans.Event.Builder.anEvent;
import static software.wings.beans.User.Builder.anUser;
import static software.wings.utils.Utils.normalizeIdentifier;
import static software.wings.utils.Utils.uuidToIdentifier;

import static freemarker.template.Configuration.VERSION_2_3_23;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Comparator.comparingInt;
import static java.util.Comparator.naturalOrder;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.compare;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import io.harness.annotations.dev.BreakDependencyOn;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.Cd1SetupFields;
import io.harness.beans.DelegateTask;
import io.harness.beans.DelegateTask.Status;
import io.harness.beans.EmbeddedUser;
import io.harness.beans.FeatureName;
import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.capability.CapabilityRequirement;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.capability.CapabilitySubjectPermission.PermissionResult;
import io.harness.capability.CapabilityTaskSelectionDetails;
import io.harness.capability.CapabilityTaskSelectionDetails.CapabilityTaskSelectionDetailsKeys;
import io.harness.capability.service.CapabilityService;
import io.harness.configuration.DeployMode;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.AvailableDelegateSizes;
import io.harness.delegate.beans.ConnectionMode;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.Delegate.DelegateKeys;
import io.harness.delegate.beans.DelegateApproval;
import io.harness.delegate.beans.DelegateConfiguration;
import io.harness.delegate.beans.DelegateConnectionDetails;
import io.harness.delegate.beans.DelegateConnectionHeartbeat;
import io.harness.delegate.beans.DelegateEntityOwner;
import io.harness.delegate.beans.DelegateGroup;
import io.harness.delegate.beans.DelegateGroup.DelegateGroupKeys;
import io.harness.delegate.beans.DelegateGroupStatus;
import io.harness.delegate.beans.DelegateInitializationDetails;
import io.harness.delegate.beans.DelegateInstanceStatus;
import io.harness.delegate.beans.DelegateParams;
import io.harness.delegate.beans.DelegateProfile;
import io.harness.delegate.beans.DelegateProfileParams;
import io.harness.delegate.beans.DelegateRegisterResponse;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateScripts;
import io.harness.delegate.beans.DelegateSetupDetails;
import io.harness.delegate.beans.DelegateSize;
import io.harness.delegate.beans.DelegateSizeDetails;
import io.harness.delegate.beans.DelegateType;
import io.harness.delegate.beans.DuplicateDelegateException;
import io.harness.delegate.beans.FileBucket;
import io.harness.delegate.beans.FileMetadata;
import io.harness.delegate.beans.K8sConfigDetails;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.SelectorCapability;
import io.harness.delegate.events.DelegateGroupDeleteEvent;
import io.harness.delegate.events.DelegateGroupUpsertEvent;
import io.harness.delegate.task.DelegateLogContext;
import io.harness.delegate.utils.DelegateEntityOwnerHelper;
import io.harness.environment.SystemEnvironment;
import io.harness.event.handler.impl.EventPublishHelper;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.EventsFrameworkMetadataConstants;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.entity_crud.EntityChangeDTO;
import io.harness.eventsframework.producer.Message;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.LimitsExceededException;
import io.harness.exception.UnexpectedException;
import io.harness.expression.ExpressionEvaluator;
import io.harness.ff.FeatureFlagService;
import io.harness.globalcontex.DelegateTokenGlobalContextData;
import io.harness.k8s.model.response.CEK8sDelegatePrerequisite;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.logging.AutoLogContext;
import io.harness.logging.Misc;
import io.harness.manage.GlobalContextManager;
import io.harness.network.Http;
import io.harness.ng.core.utils.NGUtils;
import io.harness.observer.Subject;
import io.harness.outbox.api.OutboxService;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import io.harness.persistence.UuidAware;
import io.harness.serializer.JsonUtils;
import io.harness.serializer.KryoSerializer;
import io.harness.service.intfc.DelegateCache;
import io.harness.service.intfc.DelegateCallbackRegistry;
import io.harness.service.intfc.DelegateInsightsService;
import io.harness.service.intfc.DelegateProfileObserver;
import io.harness.service.intfc.DelegateSetupService;
import io.harness.service.intfc.DelegateSyncService;
import io.harness.service.intfc.DelegateTaskSelectorMapService;
import io.harness.service.intfc.DelegateTaskService;
import io.harness.service.intfc.DelegateTokenService;
import io.harness.stream.BoundedInputStream;
import io.harness.version.VersionInfoManager;
import io.harness.waiter.WaitNotifyEngine;

import software.wings.app.DelegateGrpcConfig;
import software.wings.app.MainConfiguration;
import software.wings.audit.AuditHeader;
import software.wings.beans.Account;
import software.wings.beans.CEDelegateStatus;
import software.wings.beans.CEDelegateStatus.CEDelegateStatusBuilder;
import software.wings.beans.DelegateConnection;
import software.wings.beans.DelegateScalingGroup;
import software.wings.beans.DelegateSequenceConfig;
import software.wings.beans.DelegateSequenceConfig.DelegateSequenceConfigKeys;
import software.wings.beans.DelegateStatus;
import software.wings.beans.Event.Type;
import software.wings.beans.HttpMethod;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.DelegateProfileErrorAlert;
import software.wings.beans.alert.DelegatesDownAlert;
import software.wings.cdn.CdnConfig;
import software.wings.common.AuditHelper;
import software.wings.core.managerConfiguration.ConfigurationController;
import software.wings.expression.SecretFunctor;
import software.wings.features.DelegatesFeature;
import software.wings.features.api.UsageLimitedFeature;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.helpers.ext.url.SubdomainUrlHelperIntfc;
import software.wings.jre.JreConfig;
import software.wings.licensing.LicenseService;
import software.wings.service.impl.EventEmitter.Channel;
import software.wings.service.impl.infra.InfraDownloadService;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.AssignDelegateService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.DelegateProfileService;
import software.wings.service.intfc.DelegateSelectionLogsService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.DelegateTaskServiceClassic;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManager;

import com.github.zafarkhaja.semver.Version;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.protobuf.StringValue;
import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoGridFSException;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import io.dropwizard.jersey.validation.JerseyViolationException;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;
import javax.validation.ConstraintViolation;
import javax.validation.executable.ValidateOnExecution;
import javax.ws.rs.core.MediaType;
import lombok.Getter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request.Builder;
import okhttp3.Response;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.atmosphere.cpr.BroadcasterFactory;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.hibernate.validator.internal.engine.path.PathImpl;
import org.jetbrains.annotations.NotNull;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Singleton
@ValidateOnExecution
@Slf4j
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
@BreakDependencyOn("software.wings.service.intfc.AccountService")
@OwnedBy(DEL)
public class DelegateServiceImpl implements DelegateService {
  /**
   * The constant DELEGATE_DIR.
   */
  private static final String HARNESS_DELEGATE = "harness-delegate";
  public static final String DELEGATE_DIR = HARNESS_DELEGATE;
  public static final String DOCKER_DELEGATE = HARNESS_DELEGATE + "-docker";
  public static final String KUBERNETES_DELEGATE = HARNESS_DELEGATE + "-kubernetes";
  public static final String ECS_DELEGATE = HARNESS_DELEGATE + "-ecs";
  private static final Configuration templateConfiguration = new Configuration(VERSION_2_3_23);
  private static final String HARNESS_ECS_DELEGATE = "Harness-ECS-Delegate";
  private static final String DELIMITER = "_";
  private static final int MAX_RETRIES = 2;
  public static final String NG_CLUSTER_ADMIN_YAML = "-ng-cluster-admin.yaml.ftl";
  public static final String NG_CLUSTER_VIEWER_YAML = "-ng-cluster-viewer.yaml.ftl";
  public static final String NG_NAMESPACE_ADMIN_YAML = "-ng-namespace-admin.yaml.ftl";

  public static final String HARNESS_DELEGATE_VALUES_YAML = HARNESS_DELEGATE + "-values";
  private static final String YAML = ".yaml";
  private static final String UPGRADE_VERSION = "upgradeVersion";
  private static final String STREAM_DELEGATE = "/stream/delegate/";
  private static final String TAR_GZ = ".tar.gz";
  private static final String README = "README";
  private static final String README_TXT = "/README.txt";
  private static final String EMPTY_VERSION = "0.0.0";
  private static final String JRE_DIRECTORY = "jreDirectory";
  private static final String JRE_MAC_DIRECTORY = "jreMacDirectory";
  private static final String JRE_TAR_PATH = "jreTarPath";
  private static final String ALPN_JAR_PATH = "alpnJarPath";
  public static final String JRE_VERSION_KEY = "jreVersion";
  private static final String ENV_ENV_VAR = "ENV";
  public static final String TASK_SELECTORS = "Task Selectors";
  public static final String TASK_CATEGORY_MAP = "Task Category Map";
  public static final double RESERVED_CPU_FOR_NG_KUBERNETES = 0.05;
  public static final int RESERVED_MEMORY_FOR_NG_KUBERNETES = 100;

  static {
    templateConfiguration.setTemplateLoader(new ClassTemplateLoader(DelegateServiceImpl.class, "/delegatetemplates"));
  }

  private static final int WATCHER_RAM_IN_MB = 500;
  // Calculated as 30% of total RAM for delegate + watcher, in LAPTOP delegate which was 1250 (500 watcher + 250
  // base delegate memory + 250 to handle 50 tasks + 250 for ramp down for old version delegate during release)
  private static final int POD_BASE_RAM_IN_MB = 400;

  @Inject private HPersistence persistence;
  @Inject private WaitNotifyEngine waitNotifyEngine;
  @Inject private AccountService accountService;
  @Inject private LicenseService licenseService;
  @Inject private MainConfiguration mainConfiguration;
  @Inject private EventEmitter eventEmitter;
  @Inject private BroadcasterFactory broadcasterFactory;
  @Inject private AssignDelegateService assignDelegateService;
  @Inject private AlertService alertService;
  @Inject private Clock clock;
  @Inject private VersionInfoManager versionInfoManager;
  @Inject private Injector injector;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private InfraDownloadService infraDownloadService;
  @Inject private DelegateProfileService delegateProfileService;
  @Inject private ManagerDecryptionService managerDecryptionService;
  @Inject private SecretManager secretManager;
  @Inject private ExpressionEvaluator evaluator;
  @Inject private FileService fileService;
  @Inject private EventPublishHelper eventPublishHelper;
  @Inject private ConfigService configService;
  @Inject private PersistentLocker persistentLocker;
  @Inject private DelegateTaskBroadcastHelper broadcastHelper;
  @Inject private AuditServiceHelper auditServiceHelper;
  @Inject private SubdomainUrlHelperIntfc subdomainUrlHelper;
  @Inject private ConfigurationController configurationController;
  @Inject private DelegateSelectionLogsService delegateSelectionLogsService;
  @Inject private DelegateConnectionDao delegateConnectionDao;
  @Inject private SystemEnvironment sysenv;
  @Inject private DelegateSyncService delegateSyncService;
  @Inject private DelegateTaskService delegateTaskService;
  @Inject private KryoSerializer kryoSerializer;
  @Inject private DelegateCallbackRegistry delegateCallbackRegistry;
  @Inject private EmailNotificationService emailNotificationService;
  @Inject private DelegateGrpcConfig delegateGrpcConfig;
  @Inject private DelegateTaskSelectorMapService taskSelectorMapService;
  @Inject private SettingsService settingsService;
  @Inject private DelegateCache delegateCache;
  @Inject private CapabilityService capabilityService;
  @Inject private DelegateInsightsService delegateInsightsService;
  @Inject private DelegateSetupService delegateSetupService;
  @Inject private AuditHelper auditHelper;
  @Inject private DelegateTokenService delegateTokenService;
  @Inject private DelegateTaskServiceClassic delegateTaskServiceClassic;
  @Inject @Named(EventsFrameworkConstants.ENTITY_CRUD) private Producer eventProducer;

  @Inject @Named(DelegatesFeature.FEATURE_NAME) private UsageLimitedFeature delegatesFeature;
  @Inject @Getter private Subject<DelegateObserver> subject = new Subject<>();
  @Getter private Subject<DelegateProfileObserver> delegateProfileSubject = new Subject<>();
  @Inject @Getter private Subject<DelegateTaskStatusObserver> delegateTaskStatusObserverSubject;
  @Inject private OutboxService outboxService;

  private LoadingCache<String, String> delegateVersionCache = CacheBuilder.newBuilder()
                                                                  .maximumSize(10000)
                                                                  .expireAfterWrite(1, TimeUnit.MINUTES)
                                                                  .build(new CacheLoader<String, String>() {
                                                                    @Override
                                                                    public String load(String accountId) {
                                                                      return fetchDelegateMetadataFromStorage();
                                                                    }
                                                                  });

  @Override
  public List<Integer> getCountOfDelegatesForAccounts(List<String> accountIds) {
    List<Delegate> delegates = persistence.createQuery(Delegate.class)
                                   .field(DelegateKeys.accountId)
                                   .in(accountIds)
                                   .field(DelegateKeys.status)
                                   .notEqual(DelegateInstanceStatus.DELETED)
                                   .asList();
    Map<String, Integer> countOfDelegatesPerAccount =
        accountIds.stream().collect(toMap(accountId -> accountId, accountId -> 0));
    delegates.forEach(delegate -> {
      int currentCount = countOfDelegatesPerAccount.get(delegate.getAccountId());
      countOfDelegatesPerAccount.put(delegate.getAccountId(), currentCount + 1);
    });
    return accountIds.stream().map(countOfDelegatesPerAccount::get).collect(toList());
  }

  @Override
  public PageResponse<Delegate> list(PageRequest<Delegate> pageRequest) {
    return persistence.query(Delegate.class, pageRequest);
  }

  @Override
  public boolean checkDelegateConnected(String accountId, String delegateId) {
    return delegateConnectionDao.checkDelegateConnected(
        accountId, delegateId, versionInfoManager.getVersionInfo().getVersion());
  }

  @Override
  public List<String> getKubernetesDelegateNames(String accountId) {
    return persistence.createQuery(Delegate.class)
        .filter(DelegateKeys.accountId, accountId)
        .field(DelegateKeys.delegateName)
        .exists()
        .project(DelegateKeys.delegateName, true)
        .asList()
        .stream()
        .map(Delegate::getDelegateName)
        .distinct()
        .sorted(naturalOrder())
        .collect(toList());
  }

  @Override
  public CEDelegateStatus validateCEDelegate(String accountId, String delegateName) {
    Delegate delegate = persistence.createQuery(Delegate.class)
                            .filter(DelegateKeys.accountId, accountId)
                            .field(DelegateKeys.delegateName)
                            .exists()
                            .filter(DelegateKeys.delegateName, delegateName)
                            .get();

    if (delegate == null) {
      return CEDelegateStatus.builder().found(false).build();
    }

    CEDelegateStatusBuilder ceDelegateStatus = CEDelegateStatus.builder()
                                                   .found(true)
                                                   .ceEnabled(delegate.isCeEnabled())
                                                   .delegateName(delegate.getDelegateName())
                                                   .delegateType(delegate.getDelegateType())
                                                   .uuid(delegate.getUuid())
                                                   .lastHeartBeat(delegate.getLastHeartBeat())
                                                   .status(delegate.getStatus())
                                                   .build()
                                                   .toBuilder();

    // check delegate connections, if it's active
    List<DelegateConnectionDetails> activelyConnectedDelegates =
        delegateConnectionDao.list(accountId, delegate.getUuid())
            .stream()
            .map(delegateConnection
                -> DelegateConnectionDetails.builder()
                       .uuid(delegateConnection.getUuid())
                       .lastHeartbeat(delegateConnection.getLastHeartbeat())
                       .version(delegateConnection.getVersion())
                       .build())
            .collect(toList());
    if (activelyConnectedDelegates.isEmpty()) {
      return ceDelegateStatus.build();
    }

    // verify metrics server and ce permissions
    final CEK8sDelegatePrerequisite cek8sDelegatePrerequisite =
        settingsService.validateCEDelegateSetting(accountId, delegateName);

    return ceDelegateStatus.connections(activelyConnectedDelegates)
        .metricsServerCheck(cek8sDelegatePrerequisite.getMetricsServer())
        .permissionRuleList(cek8sDelegatePrerequisite.getPermissions())
        .build();
  }

  @Override
  public Set<String> getAllDelegateSelectors(String accountId) {
    Query<Delegate> delegateQuery =
        persistence.createQuery(Delegate.class)
            .filter(DelegateKeys.accountId, accountId)
            .field(DelegateKeys.ng)
            .notEqual(true) // notEqual is required to cover all existing delegates that will not have the ng flag set
            .field(DelegateKeys.status)
            .notEqual(DelegateInstanceStatus.DELETED)
            .project(DelegateKeys.accountId, true)
            .project(DelegateKeys.tags, true)
            .project(DelegateKeys.delegateName, true)
            .project(DelegateKeys.hostName, true)
            .project(DelegateKeys.delegateProfileId, true)
            .project(DelegateKeys.delegateGroupId, true);

    try (HIterator<Delegate> delegates = new HIterator<>(delegateQuery.fetch())) {
      if (delegates.hasNext()) {
        Set<String> selectors = new HashSet<>();

        for (Delegate delegate : delegates) {
          selectors.addAll(retrieveDelegateSelectors(delegate));
        }
        return selectors;
      }
    }
    return emptySet();
  }

  @Override
  public Set<String> getAllDelegateSelectorsUpTheHierarchy(
      final String accountId, final String orgId, final String projectId) {
    final Query<DelegateGroup> delegateGroupQuery = persistence.createQuery(DelegateGroup.class)
                                                        .filter(DelegateGroupKeys.accountId, accountId)
                                                        .filter(DelegateGroupKeys.ng, true);

    final DelegateEntityOwner owner = DelegateEntityOwnerHelper.buildOwner(orgId, projectId);

    delegateGroupQuery.field(DelegateKeys.owner_identifier)
        .in(Arrays.asList(null, orgId, owner != null ? owner.getIdentifier() : null));

    final List<DelegateGroup> delegateGroups =
        delegateGroupQuery.field(DelegateGroupKeys.status).notEqual(DelegateGroupStatus.DELETED).asList();

    return delegateGroups.stream()
        .map(group -> {
          Set<String> groupSelectors = new HashSet<>();
          groupSelectors.addAll(delegateSetupService.retrieveDelegateGroupImplicitSelectors(group).keySet());

          if (isNotEmpty(group.getTags())) {
            groupSelectors.addAll(group.getTags());
          }

          return groupSelectors;
        })
        .flatMap(Collection::stream)
        .collect(toSet());
  }

  @Override
  public Set<String> retrieveDelegateSelectors(Delegate delegate) {
    Set<String> selectors = delegate.getTags() == null ? new HashSet<>() : new HashSet<>(delegate.getTags());

    selectors.addAll(delegateSetupService.retrieveDelegateImplicitSelectors(delegate).keySet());

    return selectors;
  }

  @Override
  public List<String> getAvailableVersions(String accountId) {
    DelegateStatus status = getDelegateStatus(accountId);
    return status.getPublishedVersions();
  }

  @Override
  public Double getConnectedRatioWithPrimary(String targetVersion) {
    long primary =
        delegateConnectionDao.numberOfActiveDelegateConnectionsPerVersion(configurationController.getPrimaryVersion());

    // If we do not have any delegates in the primary version, lets unblock the deployment,
    // that will be very rare and we are in trouble anyways, let report 1 to let the new deployment go.
    if (primary == 0) {
      return 1.0;
    }

    long target = delegateConnectionDao.numberOfActiveDelegateConnectionsPerVersion(targetVersion);

    return (double) target / (double) primary;
  }

  @Override
  public DelegateSetupDetails validateKubernetesYaml(String accountId, DelegateSetupDetails delegateSetupDetails) {
    validateSetupDetails(delegateSetupDetails);
    delegateSetupDetails.setSessionIdentifier(generateUuid());
    return delegateSetupDetails;
  }

  @Override
  public File generateKubernetesYaml(String accountId, DelegateSetupDetails delegateSetupDetails, String managerHost,
      String verificationServiceUrl, MediaType fileFormat) throws IOException {
    validateSetupDetails(delegateSetupDetails);
    if (isBlank(delegateSetupDetails.getSessionIdentifier())) {
      throw new InvalidRequestException("Session identifier must be provided.", USER);
    }

    File kubernetesDelegateFile = File.createTempFile(KUBERNETES_DELEGATE, ".tar");

    try (TarArchiveOutputStream out = new TarArchiveOutputStream(new FileOutputStream(kubernetesDelegateFile))) {
      out.putArchiveEntry(new TarArchiveEntry(KUBERNETES_DELEGATE + "/"));
      out.closeArchiveEntry();

      String version;
      if (mainConfiguration.getDeployMode() == DeployMode.KUBERNETES) {
        List<String> delegateVersions = accountService.getDelegateConfiguration(accountId).getDelegateVersions();
        version = delegateVersions.get(delegateVersions.size() - 1);
      } else {
        version = EMPTY_VERSION;
      }

      boolean isCiEnabled = featureFlagService.isEnabled(NEXT_GEN_ENABLED, accountId);

      DelegateSizeDetails sizeDetails = fetchAvailableSizes()
                                            .stream()
                                            .filter(size -> size.getSize() == delegateSetupDetails.getSize())
                                            .findFirst()
                                            .orElse(null);

      DelegateGroup delegateGroup =
          upsertDelegateGroup(delegateSetupDetails.getName(), accountId, delegateSetupDetails);

      ImmutableMap<String, String> scriptParams = getJarAndScriptRunTimeParamMap(
          ScriptRuntimeParamMapInquiry.builder()
              .accountId(accountId)
              .version(version)
              .managerHost(managerHost)
              .verificationHost(verificationServiceUrl)
              .delegateName(delegateSetupDetails.getName())
              .delegateProfile(delegateSetupDetails.getDelegateConfigurationId() == null
                      ? ""
                      : delegateSetupDetails.getDelegateConfigurationId())
              .delegateType(KUBERNETES)
              .ciEnabled(isCiEnabled)
              .delegateSessionIdentifier(delegateSetupDetails.getSessionIdentifier())
              .delegateOrgIdentifier(delegateSetupDetails.getOrgIdentifier())
              .delegateProjectIdentifier(delegateSetupDetails.getProjectIdentifier())
              .delegateDescription(delegateSetupDetails.getDescription())
              .delegateSize(sizeDetails.getSize().name())
              .delegateTaskLimit(sizeDetails.getTaskLimit() / sizeDetails.getReplicas())
              .delegateReplicas(sizeDetails.getReplicas())
              .delegateRam(sizeDetails.getRam() / sizeDetails.getReplicas())
              .delegateCpu(sizeDetails.getCpu() / sizeDetails.getReplicas())
              .delegateRequestsRam(sizeDetails.getRam() / sizeDetails.getReplicas() - RESERVED_MEMORY_FOR_NG_KUBERNETES)
              .delegateRequestsCpu(sizeDetails.getCpu() / sizeDetails.getReplicas() - RESERVED_CPU_FOR_NG_KUBERNETES)
              .delegateGroupId(delegateGroup.getUuid())
              .delegateNamespace(delegateSetupDetails.getK8sConfigDetails().getNamespace())
              .logStreamingServiceBaseUrl(mainConfiguration.getLogStreamingServiceConfig().getBaseUrl())
              .build(),
          true);

      File yaml = File.createTempFile(HARNESS_DELEGATE, YAML);
      String templateName = obtainK8sTemplateNameFromConfig(delegateSetupDetails.getK8sConfigDetails());
      saveProcessedTemplate(scriptParams, yaml, templateName);
      yaml = new File(yaml.getAbsolutePath());

      if (fileFormat != null && fileFormat.equals(MediaType.TEXT_PLAIN_TYPE)) {
        return yaml;
      }

      TarArchiveEntry yamlTarArchiveEntry =
          new TarArchiveEntry(yaml, KUBERNETES_DELEGATE + "/" + HARNESS_DELEGATE + YAML);
      out.putArchiveEntry(yamlTarArchiveEntry);
      try (FileInputStream fis = new FileInputStream(yaml)) {
        IOUtils.copy(fis, out);
      }
      out.closeArchiveEntry();

      addReadmeFile(out);

      out.flush();
      out.finish();
    }

    File gzipKubernetesDelegateFile = File.createTempFile(DELEGATE_DIR, TAR_GZ);
    compressGzipFile(kubernetesDelegateFile, gzipKubernetesDelegateFile);

    return gzipKubernetesDelegateFile;
  }

  private String obtainK8sTemplateNameFromConfig(K8sConfigDetails k8sConfigDetails) {
    if (k8sConfigDetails == null || k8sConfigDetails.getK8sPermissionType() == null) {
      return HARNESS_DELEGATE + NG_CLUSTER_ADMIN_YAML;
    }

    switch (k8sConfigDetails.getK8sPermissionType()) {
      case CLUSTER_VIEWER:
        return HARNESS_DELEGATE + NG_CLUSTER_VIEWER_YAML;
      case NAMESPACE_ADMIN:
        return HARNESS_DELEGATE + NG_NAMESPACE_ADMIN_YAML;
      case CLUSTER_ADMIN:
      default:
        return HARNESS_DELEGATE + NG_CLUSTER_ADMIN_YAML;
    }
  }

  private void validateSetupDetails(DelegateSetupDetails delegateSetupDetails) {
    if (isBlank(delegateSetupDetails.getDelegateConfigurationId())) {
      throw new InvalidRequestException("Delegate Configuration must be provided.", USER);
    }

    if (isBlank(delegateSetupDetails.getName())) {
      throw new InvalidRequestException("Delegate Name must be provided.", USER);
    }

    if (delegateSetupDetails.getSize() == null) {
      throw new InvalidRequestException("Delegate Size must be provided.", USER);
    }

    K8sConfigDetails k8sConfigDetails = delegateSetupDetails.getK8sConfigDetails();
    if (k8sConfigDetails == null || k8sConfigDetails.getK8sPermissionType() == null) {
      throw new InvalidRequestException("K8s permission type must be provided.", USER);
    } else if (k8sConfigDetails.getK8sPermissionType() == NAMESPACE_ADMIN && isBlank(k8sConfigDetails.getNamespace())) {
      throw new InvalidRequestException("K8s namespace must be provided for this type of permission.", USER);
    }
  }

  @Override
  public DelegateStatus getDelegateStatus(String accountId) {
    DelegateConfiguration delegateConfiguration = accountService.getDelegateConfiguration(accountId);

    List<Delegate> delegates = persistence.createQuery(Delegate.class)
                                   .filter(DelegateKeys.accountId, accountId)
                                   .field(DelegateKeys.status)
                                   .notEqual(DelegateInstanceStatus.DELETED)
                                   .asList();

    Map<String, List<DelegateConnectionDetails>> perDelegateConnections =
        delegateConnectionDao.obtainActiveDelegateConnections(accountId);

    return DelegateStatus.builder()
        .publishedVersions(delegateConfiguration.getDelegateVersions())
        .delegates(buildInnerDelegates(delegates, perDelegateConnections, false))
        .build();
  }

  @Override
  public DelegateStatus getDelegateStatusWithScalingGroups(String accountId) {
    DelegateConfiguration delegateConfiguration = accountService.getDelegateConfiguration(accountId);

    List<Delegate> delegatesWithoutScalingGroup = getDelegatesWithoutScalingGroup(accountId);

    Map<String, List<DelegateConnectionDetails>> activeDelegateConnections =
        delegateConnectionDao.obtainActiveDelegateConnections(accountId);

    List<DelegateScalingGroup> scalingGroups = getDelegateScalingGroups(accountId, activeDelegateConnections);

    return DelegateStatus.builder()
        .publishedVersions(delegateConfiguration.getDelegateVersions())
        .scalingGroups(scalingGroups)
        .delegates(buildInnerDelegates(delegatesWithoutScalingGroup, activeDelegateConnections, false))
        .build();
  }

  @NotNull
  private List<DelegateScalingGroup> getDelegateScalingGroups(
      String accountId, Map<String, List<DelegateConnectionDetails>> activeDelegateConnections) {
    List<Delegate> activeDelegates =
        persistence.createQuery(Delegate.class)
            .filter(DelegateKeys.accountId, accountId)
            .field(DelegateKeys.ng)
            .notEqual(true) // notEqual is required to cover all existing delegates that will not have the ng flag set
            .field(DelegateKeys.delegateGroupName)
            .exists()
            .field(DelegateKeys.status)
            .hasAnyOf(Arrays.asList(DelegateInstanceStatus.ENABLED, DelegateInstanceStatus.WAITING_FOR_APPROVAL))
            .asList();

    return activeDelegates.stream()
        .collect(groupingBy(Delegate::getDelegateGroupName))
        .entrySet()
        .stream()
        .map(entry
            -> DelegateScalingGroup.builder()
                   .groupName(entry.getKey())
                   .delegates(buildInnerDelegates(entry.getValue(), activeDelegateConnections, true))
                   .build())
        .collect(toList());
  }

  private List<Delegate> getDelegatesWithoutScalingGroup(String accountId) {
    return persistence.createQuery(Delegate.class)
        .filter(DelegateKeys.accountId, accountId)
        .field(DelegateKeys.ng)
        .notEqual(true) // notEqual is required to cover all existing delegates that will not have the ng flag set
        .field(DelegateKeys.delegateGroupId)
        .doesNotExist()
        .field(DelegateKeys.delegateGroupName)
        .doesNotExist()
        .field(DelegateKeys.status)
        .notEqual(DelegateInstanceStatus.DELETED)
        .asList();
  }

  @NotNull
  private List<DelegateStatus.DelegateInner> buildInnerDelegates(List<Delegate> delegates,
      Map<String, List<DelegateConnectionDetails>> perDelegateConnections, boolean filterInactiveDelegates) {
    return delegates.stream()
        .filter(delegate -> !filterInactiveDelegates || perDelegateConnections.containsKey(delegate.getUuid()))
        .map(delegate -> {
          List<DelegateConnectionDetails> connections =
              perDelegateConnections.computeIfAbsent(delegate.getUuid(), uuid -> emptyList());
          return DelegateStatus.DelegateInner.builder()
              .uuid(delegate.getUuid())
              .delegateName(delegate.getDelegateName())
              .description(delegate.getDescription())
              .hostName(delegate.getHostName())
              .delegateGroupName(delegate.getDelegateGroupName())
              .ip(delegate.getIp())
              .status(delegate.getStatus())
              .lastHeartBeat(delegate.getLastHeartBeat())
              // currently, we do not return stale connections, but if we do this must filter them out
              .activelyConnected(!connections.isEmpty())
              .delegateProfileId(delegate.getDelegateProfileId())
              .delegateType(delegate.getDelegateType())
              .polllingModeEnabled(delegate.isPolllingModeEnabled())
              .proxy(delegate.isProxy())
              .ceEnabled(delegate.isCeEnabled())
              .excludeScopes(delegate.getExcludeScopes())
              .includeScopes(delegate.getIncludeScopes())
              .tags(delegate.getTags())
              .profileExecutedAt(delegate.getProfileExecutedAt())
              .profileError(delegate.isProfileError())
              .implicitSelectors(delegateSetupService.retrieveDelegateImplicitSelectors(delegate))
              .sampleDelegate(delegate.isSampleDelegate())
              .connections(connections)
              .build();
        })
        .collect(toList());
  }

  @Override
  public Delegate update(Delegate delegate) {
    Delegate originalDelegate = delegateCache.get(delegate.getAccountId(), delegate.getUuid(), false);
    boolean newProfileApplied = originalDelegate != null
        && compare(originalDelegate.getDelegateProfileId(), delegate.getDelegateProfileId()) != 0;

    UpdateOperations<Delegate> updateOperations = getDelegateUpdateOperations(delegate);

    Delegate updatedDelegate = null;
    if (ECS.equals(delegate.getDelegateType())) {
      updatedDelegate = updateEcsDelegate(delegate, true);
    } else {
      log.info("Updating delegate : {}", delegate.getUuid());
      updatedDelegate = updateDelegate(delegate, updateOperations);
    }

    if (newProfileApplied) {
      delegateProfileSubject.fireInform(DelegateProfileObxxxxxxxx:onProfileApplied, delegate.getAccountId(),
          delegate.getUuid(), delegate.getDelegateProfileId());
      auditServiceHelper.reportForAuditingUsingAccountId(
          delegate.getAccountId(), originalDelegate, updatedDelegate, Type.UPDATE);
      DelegateProfile profile = delegateProfileService.get(delegate.getAccountId(), delegate.getDelegateProfileId());
      auditServiceHelper.reportForAuditingUsingAccountId(delegate.getAccountId(), null, profile, Type.APPLY);
    }

    return updatedDelegate;
  }

  private Delegate updateEcsDelegate(Delegate delegate, boolean updateEntireEcsCluster) {
    UpdateOperations<Delegate> updateOperations = getDelegateUpdateOperations(delegate);
    if (updateEntireEcsCluster) {
      return updateAllDelegatesIfECSType(delegate, updateOperations, "ALL");
    } else {
      log.info("Updating ECS delegate : {}", delegate.getUuid());
      if (isDelegateWithoutPollingEnabled(delegate)) {
        // This updates delegates, as well as delegateConnection and taksBeingExecuted on delegate
        return updateDelegate(delegate, updateOperations);
      } else {
        // only update lastHeartbeatAt
        return updateHeartbeatForDelegateWithPollingEnabled(delegate);
      }
    }
  }

  private UpdateOperations<Delegate> getDelegateUpdateOperations(Delegate delegate) {
    UpdateOperations<Delegate> updateOperations = persistence.createUpdateOperations(Delegate.class);
    setUnset(updateOperations, DelegateKeys.ip, delegate.getIp());
    if (delegate.getStatus() != null) {
      updateOperations.set(DelegateKeys.status, delegate.getStatus());
    }
    setUnset(updateOperations, DelegateKeys.lastHeartBeat, delegate.getLastHeartBeat());
    setUnset(updateOperations, DelegateKeys.validUntil,
        Date.from(OffsetDateTime.now().plusDays(Delegate.TTL.toDays()).toInstant()));
    setUnset(updateOperations, DelegateKeys.version, delegate.getVersion());
    setUnset(updateOperations, DelegateKeys.description, delegate.getDescription());
    if (delegate.getDelegateType() != null) {
      setUnset(updateOperations, DelegateKeys.delegateType, delegate.getDelegateType());
    }
    setUnset(updateOperations, DelegateKeys.delegateProfileId, delegate.getDelegateProfileId());
    setUnset(updateOperations, DelegateKeys.sampleDelegate, delegate.isSampleDelegate());
    setUnset(updateOperations, DelegateKeys.polllingModeEnabled, delegate.isPolllingModeEnabled());
    setUnset(updateOperations, DelegateKeys.proxy, delegate.isProxy());
    setUnset(updateOperations, DelegateKeys.ceEnabled, delegate.isCeEnabled());
    return updateOperations;
  }

  @Override
  public Delegate updateDescription(String accountId, String delegateId, String newDescription) {
    log.info("Updating delegate description", delegateId);
    persistence.update(persistence.createQuery(Delegate.class)
                           .filter(DelegateKeys.accountId, accountId)
                           .filter(DelegateKeys.uuid, delegateId),
        persistence.createUpdateOperations(Delegate.class).set(DelegateKeys.description, newDescription));

    return delegateCache.get(accountId, delegateId, true);
  }

  @Override
  public Delegate updateApprovalStatus(String accountId, String delegateId, DelegateApproval action)
      throws InvalidRequestException {
    DelegateInstanceStatus newDelegateStatus = mapApprovalActionToDelegateStatus(action);
    Type actionEventType = mapActionToEventType(action);

    Delegate currentDelegate = persistence.createQuery(Delegate.class)
                                   .filter(DelegateKeys.accountId, accountId)
                                   .filter(DelegateKeys.uuid, delegateId)
                                   .get();
    if (currentDelegate == null) {
      throw new InvalidRequestException("Unable to fetch delegate with delegate ID " + delegateId);
    }
    if (currentDelegate.getStatus() != DelegateInstanceStatus.WAITING_FOR_APPROVAL) {
      throw new InvalidRequestException("Delegate is already in state " + currentDelegate.getStatus().name());
    }

    Query<Delegate> updateQuery = persistence.createQuery(Delegate.class)
                                      .filter(DelegateKeys.accountId, accountId)
                                      .filter(DelegateKeys.uuid, delegateId)
                                      .filter(DelegateKeys.status, DelegateInstanceStatus.WAITING_FOR_APPROVAL);

    UpdateOperations<Delegate> updateOperations =
        persistence.createUpdateOperations(Delegate.class).set(DelegateKeys.status, newDelegateStatus);

    log.debug("Updating approval status from {} to {}", currentDelegate.getStatus(), newDelegateStatus);
    Delegate updatedDelegate = persistence.findAndModify(updateQuery, updateOperations, HPersistence.returnNewOptions);

    auditServiceHelper.reportForAuditingUsingAccountId(accountId, currentDelegate, updatedDelegate, actionEventType);

    if (DelegateInstanceStatus.DELETED == newDelegateStatus) {
      broadcasterFactory.lookup(STREAM_DELEGATE + accountId, true).broadcast(SELF_DESTRUCT + delegateId);
      log.warn("Sent self destruct command to rejected delegate {}.", delegateId);
    }

    return updatedDelegate;
  }

  private DelegateInstanceStatus mapApprovalActionToDelegateStatus(DelegateApproval action) {
    if (DelegateApproval.ACTIVATE == action) {
      return DelegateInstanceStatus.ENABLED;
    } else {
      return DelegateInstanceStatus.DELETED;
    }
  }

  private Type mapActionToEventType(DelegateApproval action) {
    if (DelegateApproval.ACTIVATE == action) {
      return Type.DELEGATE_APPROVAL;
    } else {
      return Type.DELEGATE_REJECTION;
    }
  }

  @Override
  public Delegate updateHeartbeatForDelegateWithPollingEnabled(Delegate delegate) {
    persistence.update(persistence.createQuery(Delegate.class)
                           .filter(DelegateKeys.accountId, delegate.getAccountId())
                           .filter(DelegateKeys.uuid, delegate.getUuid()),
        persistence.createUpdateOperations(Delegate.class)
            .set(DelegateKeys.lastHeartBeat, currentTimeMillis())
            .set(DelegateKeys.validUntil, Date.from(OffsetDateTime.now().plusDays(Delegate.TTL.toDays()).toInstant())));
    delegateTaskService.touchExecutingTasks(
        delegate.getAccountId(), delegate.getUuid(), delegate.getCurrentlyExecutingDelegateTasks());

    Delegate existingDelegate = delegateCache.get(delegate.getAccountId(), delegate.getUuid(), false);

    if (existingDelegate == null) {
      register(delegate);
      existingDelegate = delegateCache.get(delegate.getAccountId(), delegate.getUuid(), true);
    }

    if (licenseService.isAccountDeleted(existingDelegate.getAccountId())) {
      existingDelegate.setStatus(DelegateInstanceStatus.DELETED);
    }

    existingDelegate.setUseCdn(featureFlagService.isEnabled(USE_CDN_FOR_STORAGE_FILES, delegate.getAccountId()));
    existingDelegate.setUseJreVersion(getTargetJreVersion(delegate.getAccountId()));
    return existingDelegate;
  }

  @Override
  public Delegate updateTags(Delegate delegate) {
    UpdateOperations<Delegate> updateOperations = persistence.createUpdateOperations(Delegate.class);
    setUnset(updateOperations, DelegateKeys.tags, delegate.getTags());
    log.info("Updating delegate tags : Delegate:{} tags:{}", delegate.getUuid(), delegate.getTags());

    auditServiceHelper.reportForAuditingUsingAccountId(delegate.getAccountId(), null, delegate, Type.UPDATE_TAG);
    log.info("Auditing updation of Tags for delegate={} in account={}", delegate.getUuid(), delegate.getAccountId());

    Delegate updatedDelegate = null;
    if (ECS.equals(delegate.getDelegateType())) {
      updatedDelegate = updateAllDelegatesIfECSType(delegate, updateOperations, "TAGS");
    } else {
      updatedDelegate = updateDelegate(delegate, updateOperations);
      if (currentTimeMillis() - updatedDelegate.getLastHeartBeat() < Duration.ofMinutes(2).toMillis()) {
        alertService.delegateEligibilityUpdated(updatedDelegate.getAccountId(), updatedDelegate.getUuid());
      }
    }

    if (updatedDelegate != null && featureFlagService.isEnabled(PER_AGENT_CAPABILITIES, delegate.getAccountId())) {
      regenerateCapabilityPermissions(delegate.getAccountId(), delegate.getUuid());
    }

    return updatedDelegate;
  }

  @Override
  public Delegate updateScopes(Delegate delegate) {
    UpdateOperations<Delegate> updateOperations = persistence.createUpdateOperations(Delegate.class);
    setUnset(updateOperations, DelegateKeys.includeScopes, delegate.getIncludeScopes());
    setUnset(updateOperations, DelegateKeys.excludeScopes, delegate.getExcludeScopes());

    log.info("Updating delegate scopes : Delegate:{} includeScopes:{} excludeScopes:{}", delegate.getUuid(),
        delegate.getIncludeScopes(), delegate.getExcludeScopes());

    auditServiceHelper.reportForAuditingUsingAccountId(delegate.getAccountId(), null, delegate, Type.UPDATE_SCOPE);
    log.info(
        "Auditing updation of scope for delegateId={} in accountId={}", delegate.getUuid(), delegate.getAccountId());

    Delegate updatedDelegate = null;
    if (ECS.equals(delegate.getDelegateType())) {
      updatedDelegate = updateAllDelegatesIfECSType(delegate, updateOperations, "SCOPES");
    } else {
      updatedDelegate = updateDelegate(delegate, updateOperations);
      if (currentTimeMillis() - updatedDelegate.getLastHeartBeat() < Duration.ofMinutes(2).toMillis()) {
        alertService.delegateEligibilityUpdated(updatedDelegate.getAccountId(), updatedDelegate.getUuid());
      }
    }

    if (updatedDelegate != null && featureFlagService.isEnabled(PER_AGENT_CAPABILITIES, delegate.getAccountId())) {
      regenerateCapabilityPermissions(delegate.getAccountId(), delegate.getUuid());
    }

    return updatedDelegate;
  }

  private Delegate updateDelegate(Delegate delegate, UpdateOperations<Delegate> updateOperations) {
    Delegate previousDelegate = delegateCache.get(delegate.getAccountId(), delegate.getUuid(), false);

    if (previousDelegate != null && isBlank(delegate.getDelegateProfileId())) {
      updateOperations.unset(DelegateKeys.profileResult)
          .unset(DelegateKeys.profileError)
          .unset(DelegateKeys.profileExecutedAt);

      DelegateProfileErrorAlert alertData = DelegateProfileErrorAlert.builder()
                                                .accountId(delegate.getAccountId())
                                                .hostName(delegate.getHostName())
                                                .obfuscatedIpAddress(obfuscate(delegate.getIp()))
                                                .build();
      alertService.closeAlert(delegate.getAccountId(), GLOBAL_APP_ID, AlertType.DelegateProfileError, alertData);

      if (isNotBlank(previousDelegate.getProfileResult())) {
        try {
          fileService.deleteFile(previousDelegate.getProfileResult(), FileBucket.PROFILE_RESULTS);
        } catch (MongoGridFSException e) {
          log.warn("Didn't find profile result file: {}", previousDelegate.getProfileResult());
        }
      }
    }

    persistence.update(persistence.createQuery(Delegate.class)
                           .filter(DelegateKeys.accountId, delegate.getAccountId())
                           .filter(DelegateKeys.uuid, delegate.getUuid()),
        updateOperations);
    delegateTaskService.touchExecutingTasks(
        delegate.getAccountId(), delegate.getUuid(), delegate.getCurrentlyExecutingDelegateTasks());

    eventEmitter.send(Channel.DELEGATES,
        anEvent().withOrgId(delegate.getAccountId()).withUuid(delegate.getUuid()).withType(Type.UPDATE).build());
    return delegateCache.get(delegate.getAccountId(), delegate.getUuid(), true);
  }

  private String processTemplate(Map<String, String> scriptParams, String template) throws IOException {
    try (StringWriter stringWriter = new StringWriter()) {
      templateConfiguration.getTemplate(template).process(scriptParams, stringWriter);
      return stringWriter.toString();
    } catch (TemplateException ex) {
      throw new UnexpectedException("This templates are included in the jar, they should be safe to process", ex);
    }
  }

  @Override
  public DelegateScripts getDelegateScriptsNg(String accountId, String version, String managerHost,
      String verificationHost, DelegateSize delegateSize) throws IOException {
    DelegateSizeDetails sizeDetails =
        fetchAvailableSizes().stream().filter(size -> size.getSize() == delegateSize).findFirst().orElse(null);
    String delegateXmx = "-Xmx4096m";
    if (sizeDetails != null) {
      delegateXmx =
          "-Xmx" + (sizeDetails.getRam() / sizeDetails.getReplicas() - WATCHER_RAM_IN_MB - POD_BASE_RAM_IN_MB) + "m";
    }

    ImmutableMap<String, String> scriptParams = getJarAndScriptRunTimeParamMap(
        ScriptRuntimeParamMapInquiry.builder()
            .accountId(accountId)
            .version(version)
            .managerHost(managerHost)
            .verificationHost(verificationHost)
            .logStreamingServiceBaseUrl(mainConfiguration.getLogStreamingServiceConfig().getBaseUrl())
            .delegateXmx(delegateXmx)
            .build(),
        true);

    DelegateScripts delegateScripts = DelegateScripts.builder().version(version).doUpgrade(false).build();
    if (isNotEmpty(scriptParams)) {
      String upgradeToVersion = scriptParams.get(UPGRADE_VERSION);
      log.info("Upgrading delegate to version: {}", upgradeToVersion);
      boolean doUpgrade;
      if (mainConfiguration.getDeployMode() == DeployMode.KUBERNETES) {
        doUpgrade = true;
      } else {
        doUpgrade = !(Version.valueOf(version).equals(Version.valueOf(upgradeToVersion)));
      }
      delegateScripts.setDoUpgrade(doUpgrade);
      delegateScripts.setVersion(upgradeToVersion);

      delegateScripts.setStartScript(processTemplate(scriptParams, "start.sh.ftl"));
      delegateScripts.setDelegateScript(processTemplate(scriptParams, "delegate.sh.ftl"));
      delegateScripts.setStopScript(processTemplate(scriptParams, "stop.sh.ftl"));
      delegateScripts.setSetupProxyScript(processTemplate(scriptParams, "setup-proxy.sh.ftl"));
    }
    return delegateScripts;
  }

  @Override
  public DelegateScripts getDelegateScripts(String accountId, String version, String managerHost,
      String verificationHost, String delegateName) throws IOException {
    String delegateTokenName = EMPTY;
    if (featureFlagService.isEnabled(FeatureName.USE_CUSTOM_DELEGATE_TOKENS, accountId)) {
      DelegateTokenGlobalContextData delegateTokenGlobalContextData =
          GlobalContextManager.get(DelegateTokenGlobalContextData.TOKEN_NAME);
      if (delegateTokenGlobalContextData != null) {
        delegateTokenName = delegateTokenGlobalContextData.getTokenName();
      } else {
        log.warn("DelegateTokenGlobalContextData was found null in getDelegateScripts()");
      }
    }

    ImmutableMap<String, String> scriptParams = getJarAndScriptRunTimeParamMap(
        ScriptRuntimeParamMapInquiry.builder()
            .accountId(accountId)
            .version(version)
            .managerHost(managerHost)
            .verificationHost(verificationHost)
            .logStreamingServiceBaseUrl(mainConfiguration.getLogStreamingServiceConfig().getBaseUrl())
            .delegateTokenName(delegateTokenName)
            .delegateName(StringUtils.defaultString(delegateName))
            .build(),
        false);

    DelegateScripts delegateScripts = DelegateScripts.builder().version(version).doUpgrade(false).build();
    if (isNotEmpty(scriptParams)) {
      String upgradeToVersion = scriptParams.get(UPGRADE_VERSION);
      log.info("Upgrading delegate to version: {}", upgradeToVersion);
      boolean doUpgrade;
      if (mainConfiguration.getDeployMode() == DeployMode.KUBERNETES) {
        doUpgrade = true;
      } else {
        doUpgrade = !(Version.valueOf(version).equals(Version.valueOf(upgradeToVersion)));
      }
      delegateScripts.setDoUpgrade(doUpgrade);
      delegateScripts.setVersion(upgradeToVersion);

      delegateScripts.setStartScript(processTemplate(scriptParams, "start.sh.ftl"));
      delegateScripts.setDelegateScript(processTemplate(scriptParams, "delegate.sh.ftl"));
      delegateScripts.setStopScript(processTemplate(scriptParams, "stop.sh.ftl"));
      delegateScripts.setSetupProxyScript(processTemplate(scriptParams, "setup-proxy.sh.ftl"));
    }
    return delegateScripts;
  }

  @Override
  public String getLatestDelegateVersion(String accountId) {
    String delegateMatadata = null;
    try {
      delegateMatadata = delegateVersionCache.get(accountId);
    } catch (ExecutionException e) {
      log.error("Execution exception", e);
    }
    return substringBefore(delegateMatadata, " ").trim();
  }

  private String fetchDelegateMetadataFromStorage() {
    String delegateMetadataUrl = subdomainUrlHelper.getDelegateMetadataUrl(null, null, null);
    try {
      log.info("Fetching delegate metadata from storage: {}", delegateMetadataUrl);
      String result = Http.getResponseStringFromUrl(delegateMetadataUrl, 10, 10).trim();
      log.info("Received from storage: {}", result);
      return result;
    } catch (IOException e) {
      log.warn("Exception in fetching delegate version", e);
    }
    return null;
  }

  @Value
  @lombok.Builder
  public static class ScriptRuntimeParamMapInquiry {
    private String delegateXmx;
    private String accountId;
    private String version;
    private String managerHost;
    private String verificationHost;
    private String delegateName;
    private String delegateProfile;
    private String delegateGroupId;
    private String delegateType;
    private boolean ceEnabled;
    private boolean ciEnabled;
    private String logStreamingServiceBaseUrl;
    private String delegateSessionIdentifier;
    private String delegateOrgIdentifier;
    private String delegateProjectIdentifier;
    private String delegateDescription;
    private String delegateSize;
    private int delegateTaskLimit;
    private int delegateReplicas;
    private int delegateRam;
    private double delegateCpu;
    private int delegateRequestsRam;
    private double delegateRequestsCpu;
    private String delegateNamespace;
    private String delegateTokenName;
  }

  private ImmutableMap<String, String> getJarAndScriptRunTimeParamMap(
      ScriptRuntimeParamMapInquiry inquiry, boolean isNg) {
    String latestVersion = null;
    String jarRelativePath;
    String delegateJarDownloadUrl = null;
    String delegateStorageUrl = null;
    String delegateCheckLocation = null;
    boolean jarFileExists = false;
    String delegateDockerImage = isNg ? "harness/delegate:ng" : "harness/delegate:latest";
    CdnConfig cdnConfig = mainConfiguration.getCdnConfig();
    boolean useCDN =
        featureFlagService.isEnabled(USE_CDN_FOR_STORAGE_FILES, inquiry.getAccountId()) && cdnConfig != null;

    boolean isCiEnabled = inquiry.isCiEnabled()
        && isNotEmpty(mainConfiguration.getPortal().getJwtNextGenManagerSecret())
        && nonNull(delegateGrpcConfig.getPort());

    try {
      String delegateMetadataUrl = subdomainUrlHelper.getDelegateMetadataUrl(
          inquiry.getAccountId(), inquiry.getManagerHost(), mainConfiguration.getDeployMode().name());
      delegateStorageUrl = delegateMetadataUrl.substring(0, delegateMetadataUrl.lastIndexOf('/'));
      delegateCheckLocation = delegateMetadataUrl.substring(delegateMetadataUrl.lastIndexOf('/') + 1);

      if (mainConfiguration.getDeployMode() == DeployMode.KUBERNETES) {
        log.info("Multi-Version is enabled");
        latestVersion = inquiry.getVersion();
        String minorVersion = Optional.ofNullable(getMinorVersion(inquiry.getVersion())).orElse(0).toString();
        delegateJarDownloadUrl = infraDownloadService.getDownloadUrlForDelegate(minorVersion, inquiry.getAccountId());
        if (useCDN) {
          delegateStorageUrl = cdnConfig.getUrl();
          log.info("Using CDN delegateStorageUrl " + delegateStorageUrl);
        }
      } else {
        log.info("Delegate metadata URL is " + delegateMetadataUrl);
        String delegateMatadata = delegateVersionCache.get(inquiry.getAccountId());
        log.info("Delegate metadata: [{}]", delegateMatadata);
        latestVersion = substringBefore(delegateMatadata, " ").trim();
        jarRelativePath = substringAfter(delegateMatadata, " ").trim();
        delegateJarDownloadUrl = delegateStorageUrl + "/" + jarRelativePath;
      }
      if ("local".equals(getEnv()) || DeployMode.isOnPrem(mainConfiguration.getDeployMode().name())) {
        jarFileExists = true;
      } else {
        int responseCode = -1;
        try (Response response = Http.getUnsafeOkHttpClient(delegateJarDownloadUrl, 10, 10)
                                     .newCall(new Builder().url(delegateJarDownloadUrl).head().build())
                                     .execute()) {
          responseCode = response.code();
        }
        log.info("HEAD on downloadUrl got statusCode {}", responseCode);
        jarFileExists = responseCode == 200;
        log.info("jarFileExists [{}]", jarFileExists);
      }
    } catch (IOException | ExecutionException e) {
      log.warn("Unable to fetch delegate version information", e);
      log.warn("CurrentVersion: [{}], LatestVersion=[{}], delegateJarDownloadUrl=[{}]", inquiry.getVersion(),
          latestVersion, delegateJarDownloadUrl);
    }

    log.info("Found delegate latest version: [{}] url: [{}]", latestVersion, delegateJarDownloadUrl);
    if (jarFileExists) {
      String watcherMetadataUrl;
      String watcherStorageUrl;
      String watcherCheckLocation;
      String remoteWatcherUrlCdn;

      if (useCDN) {
        watcherMetadataUrl = infraDownloadService.getCdnWatcherMetaDataFileUrl();
      } else {
        watcherMetadataUrl = subdomainUrlHelper.getWatcherMetadataUrl(
            inquiry.getAccountId(), inquiry.getManagerHost(), mainConfiguration.getDeployMode().name());
      }
      remoteWatcherUrlCdn = infraDownloadService.getCdnWatcherBaseUrl();
      watcherStorageUrl = watcherMetadataUrl.substring(0, watcherMetadataUrl.lastIndexOf('/'));
      watcherCheckLocation = watcherMetadataUrl.substring(watcherMetadataUrl.lastIndexOf('/') + 1);

      Account account = accountService.get(inquiry.getAccountId());

      String hexkey =
          format("%040x", new BigInteger(1, inquiry.getAccountId().substring(0, 6).getBytes(Charsets.UTF_8)))
              .replaceFirst("^0+(?!$)", "");

      if (mainConfiguration.getDeployMode() == DeployMode.KUBERNETES_ONPREM) {
        delegateDockerImage = mainConfiguration.getPortal().getDelegateDockerImage();
      }

      String accountSecret = account.getAccountKey();
      if (isNotBlank(inquiry.getDelegateTokenName())) {
        accountSecret = delegateTokenService.getTokenValue(inquiry.getAccountId(), inquiry.getDelegateTokenName());
      }

      ImmutableMap.Builder<String, String> params =
          ImmutableMap.<String, String>builder()
              .put("delegateDockerImage", delegateDockerImage)
              .put("accountId", inquiry.getAccountId())
              .put("accountSecret", accountSecret)
              .put("hexkey", hexkey)
              .put(UPGRADE_VERSION, latestVersion)
              .put("managerHostAndPort", inquiry.getManagerHost())
              .put("verificationHostAndPort", inquiry.getVerificationHost())
              .put("watcherStorageUrl", watcherStorageUrl)
              .put("watcherCheckLocation", watcherCheckLocation)
              .put("remoteWatcherUrlCdn", remoteWatcherUrlCdn)
              .put("delegateStorageUrl", delegateStorageUrl)
              .put("delegateCheckLocation", delegateCheckLocation)
              .put("deployMode", mainConfiguration.getDeployMode().name())
              .put("ciEnabled", String.valueOf(isCiEnabled))
              .put("kubectlVersion", mainConfiguration.getKubectlVersion())
              .put("scmVersion", mainConfiguration.getScmVersion())
              .put("delegateGrpcServicePort", String.valueOf(delegateGrpcConfig.getPort()))
              .put("kubernetesAccountLabel", getAccountIdentifier(inquiry.getAccountId()));

      if (mainConfiguration.getDeployMode() == DeployMode.KUBERNETES_ONPREM) {
        params.put("managerTarget", mainConfiguration.getGrpcOnpremDelegateClientConfig().getTarget());
        params.put("managerAuthority", mainConfiguration.getGrpcOnpremDelegateClientConfig().getAuthority());
      }

      if (isNotBlank(inquiry.getDelegateName())) {
        params.put("delegateName", inquiry.getDelegateName());
      }

      if (isNotBlank(mainConfiguration.getOcVersion())) {
        params.put("ocVersion", mainConfiguration.getOcVersion());
      }

      if (inquiry.getDelegateProfile() != null) {
        params.put("delegateProfile", inquiry.getDelegateProfile());
      }

      if (inquiry.getDelegateType() != null) {
        params.put("delegateType", inquiry.getDelegateType());
      }

      if (inquiry.getLogStreamingServiceBaseUrl() != null) {
        params.put("logStreamingServiceBaseUrl", inquiry.getLogStreamingServiceBaseUrl());
      }

      params.put("grpcServiceEnabled", String.valueOf(isCiEnabled));
      if (isCiEnabled) {
        params.put("grpcServiceConnectorPort", String.valueOf(delegateGrpcConfig.getPort()));
      } else {
        params.put("grpcServiceConnectorPort", String.valueOf(0));
      }

      params.put("useCdn", String.valueOf(useCDN));
      params.put("cdnUrl", cdnConfig.getUrl());

      if (isNotBlank(inquiry.getDelegateXmx())) {
        params.put("delegateXmx", inquiry.getDelegateXmx());
      } else {
        params.put("delegateXmx", "-Xmx4096m");
      }

      JreConfig jreConfig = getJreConfig(inquiry.getAccountId());

      Preconditions.checkNotNull(jreConfig, "jreConfig cannot be null");

      params.put(JRE_VERSION_KEY, jreConfig.getVersion());
      params.put(JRE_DIRECTORY, jreConfig.getJreDirectory());
      params.put(JRE_MAC_DIRECTORY, jreConfig.getJreMacDirectory());
      params.put(JRE_TAR_PATH, jreConfig.getJreTarPath());
      params.put(ALPN_JAR_PATH, jreConfig.getAlpnJarPath());
      params.put("enableCE", String.valueOf(inquiry.isCeEnabled()));

      if (isNotBlank(inquiry.getDelegateSessionIdentifier())) {
        params.put("delegateSessionIdentifier", inquiry.getDelegateSessionIdentifier());
      }

      if (isNotBlank(inquiry.getDelegateOrgIdentifier())) {
        params.put("delegateOrgIdentifier", inquiry.getDelegateOrgIdentifier());
      } else {
        params.put("delegateOrgIdentifier", EMPTY);
      }

      if (isNotBlank(inquiry.getDelegateProjectIdentifier())) {
        params.put("delegateProjectIdentifier", inquiry.getDelegateProjectIdentifier());
      } else {
        params.put("delegateProjectIdentifier", EMPTY);
      }

      if (isNotBlank(inquiry.getDelegateDescription())) {
        params.put("delegateDescription", inquiry.getDelegateDescription());
      } else {
        params.put("delegateDescription", EMPTY);
      }

      if (isNotBlank(inquiry.getDelegateSize())) {
        params.put("delegateSize", inquiry.getDelegateSize());
      }

      if (inquiry.getDelegateTaskLimit() != 0) {
        params.put("delegateTaskLimit", String.valueOf(inquiry.getDelegateTaskLimit()));
      }

      if (inquiry.getDelegateReplicas() != 0) {
        params.put("delegateReplicas", String.valueOf(inquiry.getDelegateReplicas()));
      }

      if (inquiry.getDelegateRam() != 0) {
        params.put("delegateRam", String.valueOf(inquiry.getDelegateRam()));
      }

      if (inquiry.getDelegateCpu() != 0) {
        params.put("delegateCpu", String.valueOf(inquiry.getDelegateCpu()));
      }

      if (inquiry.getDelegateRequestsRam() != 0) {
        params.put("delegateRequestsRam", String.valueOf(inquiry.getDelegateRequestsRam()));
      }

      if (inquiry.getDelegateRequestsCpu() != 0) {
        params.put("delegateRequestsCpu", String.valueOf(inquiry.getDelegateRequestsCpu()));
      }

      if (isNotBlank(inquiry.getDelegateGroupId())) {
        params.put("delegateGroupId", inquiry.getDelegateGroupId());
      } else {
        params.put("delegateGroupId", "");
      }

      if (isNotBlank(inquiry.getDelegateNamespace())) {
        params.put("delegateNamespace", inquiry.getDelegateNamespace());
      } else {
        params.put("delegateNamespace", HARNESS_DELEGATE);
      }

      return params.build();
    }

    String msg = "Failed to get jar and script runtime params. jarFileExists: " + jarFileExists;
    log.warn(msg);
    return null;
  }

  protected String getEnv() {
    return Optional.ofNullable(sysenv.get(ENV_ENV_VAR)).orElse("local");
  }

  /**
   * Returns JreConfig for a given account Id on the basis of UPGRADE_JRE and USE_CDN_FOR_STORAGE_FILES FeatureFlags.
   *
   * @param accountId
   * @return
   */
  private JreConfig getJreConfig(String accountId) {
    boolean useCDN = featureFlagService.isEnabled(USE_CDN_FOR_STORAGE_FILES, accountId);

    String jreVersion = mainConfiguration.getCurrentJre();
    JreConfig jreConfig = mainConfiguration.getJreConfigs().get(jreVersion);
    CdnConfig cdnConfig = mainConfiguration.getCdnConfig();

    if (useCDN && cdnConfig != null) {
      String tarPath = cdnConfig.getCdnJreTarPaths().get(jreVersion);
      String alpnJarPath = cdnConfig.getAlpnJarPath();
      jreConfig = JreConfig.builder()
                      .version(jreConfig.getVersion())
                      .jreDirectory(jreConfig.getJreDirectory())
                      .jreMacDirectory(jreConfig.getJreMacDirectory())
                      .jreTarPath(tarPath)
                      .alpnJarPath(alpnJarPath)
                      .build();
    }
    return jreConfig;
  }

  /**
   * Returns delegate's JRE version for a given account Id
   *
   * @param accountId
   * @return
   */
  private String getTargetJreVersion(String accountId) {
    return getJreConfig(accountId).getVersion();
  }

  private Integer getMinorVersion(String delegateVersion) {
    Integer delegateVersionNumber = null;
    if (isNotBlank(delegateVersion)) {
      try {
        delegateVersionNumber = Integer.parseInt(delegateVersion.substring(delegateVersion.lastIndexOf('.') + 1));
      } catch (NumberFormatException e) {
        // Leave it null
      }
    }
    return delegateVersionNumber;
  }

  @Override
  public File downloadScripts(String managerHost, String verificationUrl, String accountId, String delegateName,
      String delegateProfile, String tokenName) throws IOException {
    File delegateFile = File.createTempFile(DELEGATE_DIR, ".tar");

    try (TarArchiveOutputStream out = new TarArchiveOutputStream(new FileOutputStream(delegateFile))) {
      out.putArchiveEntry(new TarArchiveEntry(DELEGATE_DIR + "/"));
      out.closeArchiveEntry();

      String version;
      if (mainConfiguration.getDeployMode() == DeployMode.KUBERNETES) {
        List<String> delegateVersions = accountService.getDelegateConfiguration(accountId).getDelegateVersions();
        version = delegateVersions.get(delegateVersions.size() - 1);
      } else {
        version = EMPTY_VERSION;
      }

      if (isBlank(delegateProfile) || delegateProfileService.get(accountId, delegateProfile) == null) {
        delegateProfile = delegateProfileService.fetchCgPrimaryProfile(accountId).getUuid();
      }

      ImmutableMap<String, String> scriptParams = getJarAndScriptRunTimeParamMap(
          ScriptRuntimeParamMapInquiry.builder()
              .accountId(accountId)
              .version(version)
              .managerHost(managerHost)
              .verificationHost(verificationUrl)
              .delegateName(delegateName)
              .delegateProfile(delegateProfile)
              .delegateType(SHELL_SCRIPT)
              .logStreamingServiceBaseUrl(mainConfiguration.getLogStreamingServiceConfig().getBaseUrl())
              .delegateTokenName(tokenName)
              .build(),
          false);

      if (isEmpty(scriptParams)) {
        throw new InvalidArgumentsException(Pair.of("scriptParams", "Failed to get jar and script runtime params."));
      }

      File start = File.createTempFile("start", ".sh");
      saveProcessedTemplate(scriptParams, start, "start.sh.ftl");
      start = new File(start.getAbsolutePath());
      TarArchiveEntry startTarArchiveEntry = new TarArchiveEntry(start, DELEGATE_DIR + "/start.sh");
      startTarArchiveEntry.setMode(0755);
      out.putArchiveEntry(startTarArchiveEntry);
      try (FileInputStream fis = new FileInputStream(start)) {
        IOUtils.copy(fis, out);
      }
      out.closeArchiveEntry();

      File delegate = File.createTempFile("delegate", ".sh");
      saveProcessedTemplate(scriptParams, delegate, "delegate.sh.ftl");
      delegate = new File(delegate.getAbsolutePath());
      TarArchiveEntry delegateTarArchiveEntry = new TarArchiveEntry(delegate, DELEGATE_DIR + "/delegate.sh");
      delegateTarArchiveEntry.setMode(0755);
      out.putArchiveEntry(delegateTarArchiveEntry);
      try (FileInputStream fis = new FileInputStream(delegate)) {
        IOUtils.copy(fis, out);
      }
      out.closeArchiveEntry();

      File stop = File.createTempFile("stop", ".sh");
      saveProcessedTemplate(scriptParams, stop, "stop.sh.ftl");
      stop = new File(stop.getAbsolutePath());
      TarArchiveEntry stopTarArchiveEntry = new TarArchiveEntry(stop, DELEGATE_DIR + "/stop.sh");
      stopTarArchiveEntry.setMode(0755);
      out.putArchiveEntry(stopTarArchiveEntry);
      try (FileInputStream fis = new FileInputStream(stop)) {
        IOUtils.copy(fis, out);
      }
      out.closeArchiveEntry();

      File setupProxy = File.createTempFile("setup-proxy", ".sh");
      saveProcessedTemplate(scriptParams, setupProxy, "setup-proxy.sh.ftl");
      setupProxy = new File(setupProxy.getAbsolutePath());
      TarArchiveEntry setupProxyTarArchiveEntry = new TarArchiveEntry(setupProxy, DELEGATE_DIR + "/setup-proxy.sh");
      setupProxyTarArchiveEntry.setMode(0755);
      out.putArchiveEntry(setupProxyTarArchiveEntry);
      try (FileInputStream fis = new FileInputStream(setupProxy)) {
        IOUtils.copy(fis, out);
      }
      out.closeArchiveEntry();

      File readme = File.createTempFile(README, ".txt");
      saveProcessedTemplate(emptyMap(), readme, "readme.txt.ftl");
      readme = new File(readme.getAbsolutePath());
      TarArchiveEntry readmeTarArchiveEntry = new TarArchiveEntry(readme, DELEGATE_DIR + README_TXT);
      out.putArchiveEntry(readmeTarArchiveEntry);
      try (FileInputStream fis = new FileInputStream(readme)) {
        IOUtils.copy(fis, out);
      }
      out.closeArchiveEntry();

      out.flush();
      out.finish();
    }

    File gzipDelegateFile = File.createTempFile(DELEGATE_DIR, TAR_GZ);
    compressGzipFile(delegateFile, gzipDelegateFile);
    return gzipDelegateFile;
  }

  private void saveProcessedTemplate(Map<String, String> scriptParams, File start, String template) throws IOException {
    try (OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(start), UTF_8)) {
      templateConfiguration.getTemplate(template).process(scriptParams, fileWriter);
    } catch (TemplateException ex) {
      throw new UnexpectedException("This templates are included in the jar, they should be safe to process", ex);
    }
  }

  private static void compressGzipFile(File file, File gzipFile) {
    try (FileInputStream fis = new FileInputStream(file); FileOutputStream fos = new FileOutputStream(gzipFile);
         GZIPOutputStream gzipOS = new GZIPOutputStream(fos)) {
      byte[] buffer = new byte[1024];
      int len;
      while ((len = fis.read(buffer)) != -1) {
        gzipOS.write(buffer, 0, len);
      }
    } catch (IOException e) {
      log.error("Error gzipping file.", e);
    }
  }

  @Override
  public File downloadDocker(String managerHost, String verificationUrl, String accountId, String delegateName,
      String delegateProfile, String tokenName) throws IOException {
    File dockerDelegateFile = File.createTempFile(DOCKER_DELEGATE, ".tar");

    try (TarArchiveOutputStream out = new TarArchiveOutputStream(new FileOutputStream(dockerDelegateFile))) {
      out.putArchiveEntry(new TarArchiveEntry(DOCKER_DELEGATE + "/"));
      out.closeArchiveEntry();

      String version;
      if (mainConfiguration.getDeployMode() == DeployMode.KUBERNETES) {
        List<String> delegateVersions = accountService.getDelegateConfiguration(accountId).getDelegateVersions();
        version = delegateVersions.get(delegateVersions.size() - 1);
      } else {
        version = EMPTY_VERSION;
      }

      if (isBlank(delegateProfile) || delegateProfileService.get(accountId, delegateProfile) == null) {
        delegateProfile = delegateProfileService.fetchCgPrimaryProfile(accountId).getUuid();
      }

      ImmutableMap<String, String> scriptParams = getJarAndScriptRunTimeParamMap(
          ScriptRuntimeParamMapInquiry.builder()
              .accountId(accountId)
              .version(version)
              .managerHost(managerHost)
              .verificationHost(verificationUrl)
              .delegateName(delegateName)
              .delegateProfile(delegateProfile)
              .delegateType(DOCKER)
              .logStreamingServiceBaseUrl(mainConfiguration.getLogStreamingServiceConfig().getBaseUrl())
              .delegateTokenName(tokenName)
              .build(),
          false);

      if (isEmpty(scriptParams)) {
        throw new InvalidArgumentsException(Pair.of("scriptParams", "Failed to get jar and script runtime params."));
      }

      String templateName;
      if (isBlank(delegateName)) {
        templateName = "launch-" + HARNESS_DELEGATE + "-without-name.sh.ftl";
      } else {
        templateName = "launch-" + HARNESS_DELEGATE + ".sh.ftl";
      }

      File launch = File.createTempFile("launch-" + HARNESS_DELEGATE, ".sh");
      saveProcessedTemplate(scriptParams, launch, templateName);
      launch = new File(launch.getAbsolutePath());
      TarArchiveEntry launchTarArchiveEntry =
          new TarArchiveEntry(launch, DOCKER_DELEGATE + "/launch-" + HARNESS_DELEGATE + ".sh");
      launchTarArchiveEntry.setMode(0755);
      out.putArchiveEntry(launchTarArchiveEntry);
      try (FileInputStream fis = new FileInputStream(launch)) {
        IOUtils.copy(fis, out);
      }
      out.closeArchiveEntry();

      File readme = File.createTempFile(README, ".txt");
      saveProcessedTemplate(emptyMap(), readme, "readme-docker.txt.ftl");
      readme = new File(readme.getAbsolutePath());
      TarArchiveEntry readmeTarArchiveEntry = new TarArchiveEntry(readme, DOCKER_DELEGATE + README_TXT);

      out.putArchiveEntry(readmeTarArchiveEntry);
      try (FileInputStream fis = new FileInputStream(readme)) {
        IOUtils.copy(fis, out);
      }
      out.closeArchiveEntry();

      out.flush();
      out.finish();
    }

    File gzipDockerDelegateFile = File.createTempFile(DELEGATE_DIR, TAR_GZ);
    compressGzipFile(dockerDelegateFile, gzipDockerDelegateFile);
    return gzipDockerDelegateFile;
  }

  @Override
  public File downloadKubernetes(String managerHost, String verificationUrl, String accountId, String delegateName,
      String delegateProfile, String tokenName) throws IOException {
    File kubernetesDelegateFile = File.createTempFile(KUBERNETES_DELEGATE, ".tar");

    try (TarArchiveOutputStream out = new TarArchiveOutputStream(new FileOutputStream(kubernetesDelegateFile))) {
      out.putArchiveEntry(new TarArchiveEntry(KUBERNETES_DELEGATE + "/"));
      out.closeArchiveEntry();

      String version;
      if (mainConfiguration.getDeployMode() == DeployMode.KUBERNETES) {
        List<String> delegateVersions = accountService.getDelegateConfiguration(accountId).getDelegateVersions();
        version = delegateVersions.get(delegateVersions.size() - 1);
      } else {
        version = EMPTY_VERSION;
      }
      boolean isCiEnabled = featureFlagService.isEnabled(NEXT_GEN_ENABLED, accountId);
      ImmutableMap<String, String> scriptParams = getJarAndScriptRunTimeParamMap(
          ScriptRuntimeParamMapInquiry.builder()
              .accountId(accountId)
              .version(version)
              .managerHost(managerHost)
              .verificationHost(verificationUrl)
              .delegateName(delegateName)
              .delegateProfile(delegateProfile == null ? "" : delegateProfile)
              .delegateType(KUBERNETES)
              .ciEnabled(isCiEnabled)
              .logStreamingServiceBaseUrl(mainConfiguration.getLogStreamingServiceConfig().getBaseUrl())
              .delegateTokenName(tokenName)
              .build(),
          false);

      File yaml = File.createTempFile(HARNESS_DELEGATE, YAML);
      saveProcessedTemplate(scriptParams, yaml, HARNESS_DELEGATE + ".yaml.ftl");
      yaml = new File(yaml.getAbsolutePath());
      TarArchiveEntry yamlTarArchiveEntry =
          new TarArchiveEntry(yaml, KUBERNETES_DELEGATE + "/" + HARNESS_DELEGATE + YAML);
      out.putArchiveEntry(yamlTarArchiveEntry);
      try (FileInputStream fis = new FileInputStream(yaml)) {
        IOUtils.copy(fis, out);
      }
      out.closeArchiveEntry();

      addReadmeFile(out);

      out.flush();
      out.finish();
    }

    File gzipKubernetesDelegateFile = File.createTempFile(DELEGATE_DIR, TAR_GZ);
    compressGzipFile(kubernetesDelegateFile, gzipKubernetesDelegateFile);

    return gzipKubernetesDelegateFile;
  }

  @Override
  public File downloadCeKubernetesYaml(String managerHost, String verificationUrl, String accountId,
      String delegateName, String delegateProfile, String tokenName) throws IOException {
    String version;
    if (mainConfiguration.getDeployMode() == DeployMode.KUBERNETES) {
      List<String> delegateVersions = accountService.getDelegateConfiguration(accountId).getDelegateVersions();
      version = delegateVersions.get(delegateVersions.size() - 1);
    } else {
      version = EMPTY_VERSION;
    }

    ImmutableMap<String, String> scriptParams = getJarAndScriptRunTimeParamMap(
        ScriptRuntimeParamMapInquiry.builder()
            .accountId(accountId)
            .version(version)
            .managerHost(managerHost)
            .verificationHost(verificationUrl)
            .delegateName(delegateName)
            .delegateProfile(delegateProfile == null ? "" : delegateProfile)
            .delegateType(CE_KUBERNETES)
            .ceEnabled(true)
            .logStreamingServiceBaseUrl(mainConfiguration.getLogStreamingServiceConfig().getBaseUrl())
            .delegateTokenName(tokenName)
            .build(),
        false);

    File yaml = File.createTempFile(HARNESS_DELEGATE, YAML);
    saveProcessedTemplate(scriptParams, yaml,
        HARNESS_DELEGATE + "-ce"
            + ".yaml.ftl");
    return new File(yaml.getAbsolutePath());
  }

  private void addReadmeFile(TarArchiveOutputStream out) throws IOException {
    File readme = File.createTempFile(README, ".txt");
    saveProcessedTemplate(emptyMap(), readme, "readme-kubernetes.txt.ftl");
    readme = new File(readme.getAbsolutePath());
    TarArchiveEntry readmeTarArchiveEntry = new TarArchiveEntry(readme, KUBERNETES_DELEGATE + README_TXT);
    out.putArchiveEntry(readmeTarArchiveEntry);
    try (FileInputStream fis = new FileInputStream(readme)) {
      IOUtils.copy(fis, out);
    }
    out.closeArchiveEntry();
  }

  @Override
  public File downloadDelegateValuesYamlFile(String managerHost, String verificationUrl, String accountId,
      String delegateName, String delegateProfile, String tokenName) throws IOException {
    String version;

    if (mainConfiguration.getDeployMode() == DeployMode.KUBERNETES) {
      List<String> delegateVersions = accountService.getDelegateConfiguration(accountId).getDelegateVersions();
      version = delegateVersions.get(delegateVersions.size() - 1);
    } else {
      version = EMPTY_VERSION;
    }

    ImmutableMap<String, String> params = getJarAndScriptRunTimeParamMap(
        ScriptRuntimeParamMapInquiry.builder()
            .accountId(accountId)
            .version(version)
            .managerHost(managerHost)
            .verificationHost(verificationUrl)
            .delegateName(delegateName)
            .delegateProfile(delegateProfile == null ? "" : delegateProfile)
            .delegateType(HELM_DELEGATE)
            .logStreamingServiceBaseUrl(mainConfiguration.getLogStreamingServiceConfig().getBaseUrl())
            .delegateTokenName(tokenName)
            .build(),
        false);

    File yaml = File.createTempFile(HARNESS_DELEGATE_VALUES_YAML, YAML);
    saveProcessedTemplate(params, yaml, "delegate-helm-values.yaml.ftl");

    return yaml;
  }

  @Override
  public File downloadECSDelegate(String managerHost, String verificationUrl, String accountId, boolean awsVpcMode,
      String hostname, String delegateGroupName, String delegateProfile, String tokenName) throws IOException {
    File ecsDelegateFile = File.createTempFile(ECS_DELEGATE, ".tar");

    try (TarArchiveOutputStream out = new TarArchiveOutputStream(new FileOutputStream(ecsDelegateFile))) {
      out.putArchiveEntry(new TarArchiveEntry(ECS_DELEGATE + "/"));
      out.closeArchiveEntry();

      String version;
      if (mainConfiguration.getDeployMode() == DeployMode.KUBERNETES) {
        List<String> delegateVersions = accountService.getDelegateConfiguration(accountId).getDelegateVersions();
        version = delegateVersions.get(delegateVersions.size() - 1);
      } else {
        version = EMPTY_VERSION;
      }

      DelegateGroup delegateGroup = upsertDelegateGroup(delegateGroupName, accountId, null);

      ImmutableMap<String, String> scriptParams = getJarAndScriptRunTimeParamMap(
          ScriptRuntimeParamMapInquiry.builder()
              .accountId(accountId)
              .version(version)
              .managerHost(managerHost)
              .verificationHost(verificationUrl)
              .delegateName(StringUtils.EMPTY)
              .delegateProfile(delegateProfile == null ? "" : delegateProfile)
              .delegateType(ECS)
              .delegateGroupId(delegateGroup.getUuid())
              .logStreamingServiceBaseUrl(mainConfiguration.getLogStreamingServiceConfig().getBaseUrl())
              .delegateTokenName(tokenName)
              .build(),
          false);

      scriptParams = updateMapForEcsDelegate(awsVpcMode, hostname, delegateGroupName, scriptParams);

      // Add Task Spec Json file
      File yaml = File.createTempFile("ecs-spec", ".json");
      saveProcessedTemplate(scriptParams, yaml, "harness-ecs-delegate.json.ftl");
      yaml = new File(yaml.getAbsolutePath());
      TarArchiveEntry yamlTarArchiveEntry = new TarArchiveEntry(yaml, ECS_DELEGATE + "/ecs-task-spec.json");
      out.putArchiveEntry(yamlTarArchiveEntry);
      try (FileInputStream fis = new FileInputStream(yaml)) {
        IOUtils.copy(fis, out);
      }
      out.closeArchiveEntry();

      // Add Task "Service Spec Json for awsvpc mode" file
      File serviceJson = File.createTempFile("ecs-service-spec", ".json");
      saveProcessedTemplate(scriptParams, serviceJson, "harness-ecs-delegate-service.json.ftl");
      serviceJson = new File(serviceJson.getAbsolutePath());
      TarArchiveEntry serviceJsonTarArchiveEntry =
          new TarArchiveEntry(serviceJson, ECS_DELEGATE + "/service-spec-for-awsvpc-mode.json");
      out.putArchiveEntry(serviceJsonTarArchiveEntry);
      try (FileInputStream fis = new FileInputStream(serviceJson)) {
        IOUtils.copy(fis, out);
      }
      out.closeArchiveEntry();

      // Add Readme file
      File readme = File.createTempFile(README, ".txt");
      saveProcessedTemplate(emptyMap(), readme, "readme-ecs.txt.ftl");
      readme = new File(readme.getAbsolutePath());
      TarArchiveEntry readmeTarArchiveEntry = new TarArchiveEntry(readme, ECS_DELEGATE + README_TXT);
      out.putArchiveEntry(readmeTarArchiveEntry);
      try (FileInputStream fis = new FileInputStream(readme)) {
        IOUtils.copy(fis, out);
      }

      out.closeArchiveEntry();

      out.flush();
      out.finish();
    }

    File gzipEcsDelegateFile = File.createTempFile(DELEGATE_DIR, TAR_GZ);
    compressGzipFile(ecsDelegateFile, gzipEcsDelegateFile);

    return gzipEcsDelegateFile;
  }

  private ImmutableMap<String, String> updateMapForEcsDelegate(
      boolean awsVpcMode, String hostname, String delegateGroupName, Map<String, String> scriptParams) {
    Map<String, String> map = new HashMap<>(scriptParams);
    // AWSVPC mode, hostname must be null
    if (awsVpcMode) {
      map.put("networkModeForTask", "\"networkMode\": \"awsvpc\",");
      map.put("hostnameForDelegate", StringUtils.EMPTY);
    } else {
      map.put("networkModeForTask", StringUtils.EMPTY);
      if (isBlank(hostname)) {
        // hostname not provided, use as null, so dockerId will become hostname in ecs
        hostname = HARNESS_ECS_DELEGATE;
      }
      map.put("hostnameForDelegate", "\"hostname\": \"" + hostname + "\",");
    }

    map.put("delegateGroupName", delegateGroupName);

    return ImmutableMap.copyOf(map);
  }

  @Override
  public Delegate add(Delegate delegate) {
    Delegate savedDelegate;
    String accountId = delegate.getAccountId();

    DelegateProfile delegateProfile = delegateProfileService.get(accountId, delegate.getDelegateProfileId());
    if (delegateProfile == null) {
      if (delegate.isNg()) {
        delegateProfile = delegateProfileService.fetchNgPrimaryProfile(accountId, delegate.getOwner());
      } else {
        delegateProfile = delegateProfileService.fetchCgPrimaryProfile(accountId);
      }
      delegate.setDelegateProfileId(delegateProfile.getUuid());
    }

    if (delegateProfile.isApprovalRequired()) {
      delegate.setStatus(DelegateInstanceStatus.WAITING_FOR_APPROVAL);
    } else {
      delegate.setStatus(DelegateInstanceStatus.ENABLED);
    }

    int maxUsageAllowed = delegatesFeature.getMaxUsageAllowedForAccount(accountId);
    if (maxUsageAllowed != Integer.MAX_VALUE) {
      try (AcquiredLock ignored =
               persistentLocker.acquireLock("delegateCountLock-" + accountId, Duration.ofMinutes(3))) {
        long currentDelegateCount = getTotalNumberOfDelegates(accountId);
        if (currentDelegateCount < maxUsageAllowed) {
          savedDelegate = saveDelegate(delegate);
        } else {
          throw new LimitsExceededException(
              format("Can not add delegate to the account. Maximum [%d] delegates are supported", maxUsageAllowed),
              USAGE_LIMITS_EXCEEDED, USER);
        }
      }
    } else {
      savedDelegate = saveDelegate(delegate);
    }

    log.info("Delegate saved: {}", savedDelegate);

    auditServiceHelper.reportForAuditingUsingAccountId(
        accountId, savedDelegate, savedDelegate, Type.DELEGATE_REGISTRATION);

    // When polling is enabled for delegate, do not perform these event publishing
    if (isDelegateWithoutPollingEnabled(delegate)) {
      eventEmitter.send(Channel.DELEGATES,
          anEvent().withOrgId(delegate.getAccountId()).withUuid(delegate.getUuid()).withType(Type.CREATE).build());
      assignDelegateService.clearConnectionResults(delegate.getAccountId());
    }

    updateWithTokenAndSeqNumIfEcsDelegate(delegate, savedDelegate);
    eventPublishHelper.publishInstalledDelegateEvent(delegate.getAccountId(), delegate.getUuid());

    if (savedDelegate != null && featureFlagService.isEnabled(PER_AGENT_CAPABILITIES, savedDelegate.getAccountId())) {
      regenerateCapabilityPermissions(savedDelegate.getAccountId(), savedDelegate.getUuid());
    }

    try {
      if (savedDelegate.isCeEnabled()) {
        subject.fireInform(DelegateObxxxxxxxx:onAdded, savedDelegate);
      }
    } catch (Exception e) {
      log.error("Encountered exception while informing the observers of Delegate.", e);
    }
    return savedDelegate;
  }

  private long getTotalNumberOfDelegates(String accountId) {
    return persistence.createQuery(Delegate.class)
        .filter(DelegateKeys.accountId, accountId)
        .field(DelegateKeys.status)
        .notEqual(DelegateInstanceStatus.DELETED)
        .count();
  }

  private Delegate saveDelegate(Delegate delegate) {
    log.info("Adding delegate {} for account {}", delegate.getHostName(), delegate.getAccountId());
    persistence.save(delegate);
    log.info("Delegate saved: {}", delegate);
    return delegate;
  }

  @Override
  public void delete(String accountId, String delegateId, boolean forceDelete) throws InvalidRequestException {
    Delegate existingDelegate = persistence.createQuery(Delegate.class)
                                    .filter(DelegateKeys.accountId, accountId)
                                    .filter(DelegateKeys.uuid, delegateId)
                                    .project(DelegateKeys.ip, true)
                                    .project(DelegateKeys.hostName, true)
                                    .project(DelegateKeys.owner, true)
                                    .get();

    if (existingDelegate != null) {
      // before deleting delegate, check if any alert is open for delegate, if yes, close it.
      alertService.closeAlert(accountId, GLOBAL_APP_ID, AlertType.DelegatesDown,
          DelegatesDownAlert.builder()
              .accountId(accountId)
              .obfuscatedIpAddress(obfuscate(existingDelegate.getIp()))
              .hostName(existingDelegate.getHostName())
              .build());
      alertService.closeAlert(accountId, GLOBAL_APP_ID, AlertType.DelegateProfileError,
          DelegateProfileErrorAlert.builder()
              .accountId(accountId)
              .obfuscatedIpAddress(obfuscate(existingDelegate.getIp()))
              .hostName(existingDelegate.getHostName())
              .build());
    } else {
      throw new InvalidRequestException("Unable to fetch delegate with delegate id " + delegateId);
    }

    if (featureFlagService.isEnabled(DO_DELEGATE_PHYSICAL_DELETE, accountId) || forceDelete) {
      Query<Delegate> delegateQuery = persistence.createQuery(Delegate.class)
                                          .filter(DelegateKeys.accountId, accountId)
                                          .filter(DelegateKeys.uuid, delegateId);
      boolean deleted = persistence.delete(delegateQuery);
      if (!deleted) {
        throw new InvalidRequestException("Unable to perform delete on delegate delegate id " + delegateId);
      }
      log.info("Delegate: {} deleted.", delegateId);
    } else {
      Query<Delegate> updateQuery = persistence.createQuery(Delegate.class)
                                        .filter(DelegateKeys.accountId, accountId)
                                        .filter(DelegateKeys.uuid, delegateId);

      UpdateOperations<Delegate> updateOperations =
          persistence.createUpdateOperations(Delegate.class)
              .set(DelegateKeys.status, DelegateInstanceStatus.DELETED)
              .set(
                  DelegateKeys.validUntil, Date.from(OffsetDateTime.now().plusDays(Delegate.TTL.toDays()).toInstant()));

      Delegate delegate = persistence.findAndModify(updateQuery, updateOperations, HPersistence.returnNewOptions);
      if (delegate == null || delegate.getStatus() != DelegateInstanceStatus.DELETED) {
        throw new InvalidRequestException("Unable to set status as deleted delegate id " + delegateId);
      }
      log.info("Delegate: {} marked as deleted.", delegateId);

      broadcasterFactory.lookup(STREAM_DELEGATE + accountId, true).broadcast(SELF_DESTRUCT + delegateId);
      log.warn("Sent self destruct command to logically deleted delegate {}.", delegateId);
    }
    auditServiceHelper.reportDeleteForAuditingUsingAccountId(accountId, existingDelegate);
    log.info("Auditing deleting of Delegate for accountId={}", accountId);
  }

  @Override
  public void retainOnlySelectedDelegatesAndDeleteRest(String accountId, List<String> delegatesToRetain) {
    if (EmptyPredicate.isNotEmpty(delegatesToRetain)) {
      persistence.delete(persistence.createQuery(Delegate.class)
                             .filter(DelegateKeys.accountId, accountId)
                             .field(DelegateKeys.uuid)
                             .notIn(delegatesToRetain));
    } else {
      log.info("List of delegates to retain is empty. In order to delete delegates, pass a list of delegate IDs");
    }
  }

  @Override
  public void deleteDelegateGroup(String accountId, String delegateGroupId, boolean forceDelete) {
    log.info("Deleting delegate group: {} and all belonging delegates.", delegateGroupId);
    List<Delegate> groupDelegates = persistence.createQuery(Delegate.class)
                                        .filter(DelegateKeys.accountId, accountId)
                                        .filter(DelegateKeys.delegateGroupId, delegateGroupId)
                                        .project(DelegateKeys.owner, true)
                                        .asList();

    for (Delegate delegate : groupDelegates) {
      try {
        delete(accountId, delegate.getUuid(), forceDelete);
      } catch (InvalidRequestException exception) {
        log.error("Unable to delete delegate ", exception);
      }
    }
    DelegateGroup delegateGroup = persistence.createQuery(DelegateGroup.class)
                                      .filter(DelegateGroupKeys.accountId, accountId)
                                      .filter(DelegateGroupKeys.uuid, delegateGroupId)
                                      .get();
    if (delegateGroup == null) {
      return;
    }

    if (featureFlagService.isEnabled(DO_DELEGATE_PHYSICAL_DELETE, accountId) || forceDelete) {
      persistence.delete(persistence.createQuery(DelegateGroup.class)
                             .filter(DelegateGroupKeys.accountId, accountId)
                             .filter(DelegateGroupKeys.uuid, delegateGroupId));
      log.info("Delegate group: {} and all belonging delegates have been deleted.", delegateGroupId);
    } else {
      Query<DelegateGroup> updateQuery = persistence.createQuery(DelegateGroup.class)
                                             .filter(DelegateGroupKeys.accountId, accountId)
                                             .filter(DelegateGroupKeys.uuid, delegateGroupId);

      UpdateOperations<DelegateGroup> updateOperations =
          persistence.createUpdateOperations(DelegateGroup.class)
              .set(DelegateGroupKeys.status, DelegateGroupStatus.DELETED)
              .set(DelegateGroupKeys.validUntil,
                  Date.from(OffsetDateTime.now()
                                .plusDays(Delegate.TTL.toDays()) // Delegate.TTL is used to make sure TTL duration is
                                                                 // aligned between group and delegate
                                .toInstant()));

      persistence.findAndModify(updateQuery, updateOperations, HPersistence.returnNewOptions);
      log.info("Delegate group: {} and all belonging delegates have been marked as deleted.", delegateGroupId);
    }

    String orgIdentifier = delegateGroup.getOwner() != null
        ? DelegateEntityOwnerHelper.extractOrgIdFromOwnerIdentifier(delegateGroup.getOwner().getIdentifier())
        : null;

    String projectIdentifier = delegateGroup.getOwner() != null
        ? DelegateEntityOwnerHelper.extractProjectIdFromOwnerIdentifier(delegateGroup.getOwner().getIdentifier())
        : null;

    outboxService.save(
        DelegateGroupDeleteEvent.builder()
            .accountIdentifier(accountId)
            .orgIdentifier(orgIdentifier)
            .projectIdentifier(projectIdentifier)
            .delegateGroupId(delegateGroupId)
            .delegateSetupDetails(DelegateSetupDetails.builder()
                                      .delegateConfigurationId(delegateGroup.getDelegateConfigurationId())
                                      .description(delegateGroup.getDescription())
                                      .k8sConfigDetails(delegateGroup.getK8sConfigDetails())
                                      .name(delegateGroup.getName())
                                      .size(delegateGroup.getSizeDetails().getSize())
                                      .orgIdentifier(orgIdentifier)
                                      .projectIdentifier(projectIdentifier)
                                      .build())
            .build());

    DelegateEntityOwner owner = isNotEmpty(groupDelegates) ? groupDelegates.get(0).getOwner() : null;
    publishDelegateChangeEventViaEventFramework(accountId, delegateGroupId, owner, DELETE_ACTION);
  }

  @Override
  public void deleteDelegateGroupV2(
      String accountId, String orgId, String projectId, String identifier, boolean forceDelete) {
    log.info("Deleting delegate group: {} and all belonging delegates.", identifier);
    DelegateGroup delegateGroup =
        persistence.createQuery(DelegateGroup.class)
            .filter(DelegateGroupKeys.accountId, accountId)
            .filter(DelegateGroupKeys.owner, DelegateEntityOwnerHelper.buildOwner(orgId, projectId))
            .filter(DelegateGroupKeys.identifier, identifier)
            .get();

    if (delegateGroup == null) {
      log.info("Delegate group doesn't exist or it is already deleted.");
      return;
    }

    String delegateGroupUuid = delegateGroup.getUuid();
    List<Delegate> groupDelegates = persistence.createQuery(Delegate.class)
                                        .filter(DelegateKeys.accountId, accountId)
                                        .filter(DelegateKeys.delegateGroupId, delegateGroupUuid)
                                        .project(DelegateKeys.owner, true)
                                        .asList();

    for (Delegate delegate : groupDelegates) {
      delete(accountId, delegate.getUuid(), forceDelete);
    }

    if (featureFlagService.isEnabled(DO_DELEGATE_PHYSICAL_DELETE, accountId) || forceDelete) {
      persistence.delete(persistence.createQuery(DelegateGroup.class)
                             .filter(DelegateGroupKeys.accountId, accountId)
                             .filter(DelegateGroupKeys.uuid, delegateGroupUuid));
      log.info("Delegate group: {} and all belonging delegates have been deleted.", delegateGroupUuid);
    } else {
      Query<DelegateGroup> updateQuery = persistence.createQuery(DelegateGroup.class)
                                             .filter(DelegateGroupKeys.accountId, accountId)
                                             .filter(DelegateGroupKeys.uuid, delegateGroupUuid);

      UpdateOperations<DelegateGroup> updateOperations =
          persistence.createUpdateOperations(DelegateGroup.class)
              .set(DelegateGroupKeys.status, DelegateGroupStatus.DELETED)
              .set(DelegateGroupKeys.validUntil,
                  Date.from(OffsetDateTime.now()
                                .plusDays(Delegate.TTL.toDays()) // Delegate.TTL is used to make sure TTL duration is
                                // aligned between group and delegate
                                .toInstant()));

      persistence.findAndModify(updateQuery, updateOperations, HPersistence.returnNewOptions);
      log.info("Delegate group: {} and all belonging delegates have been marked as deleted.", identifier);
    }

    outboxService.save(
        DelegateGroupDeleteEvent.builder()
            .accountIdentifier(accountId)
            .orgIdentifier(orgId)
            .projectIdentifier(projectId)
            .delegateGroupId(delegateGroupUuid)
            .delegateSetupDetails(DelegateSetupDetails.builder()
                                      .delegateConfigurationId(delegateGroup.getDelegateConfigurationId())
                                      .description(delegateGroup.getDescription())
                                      .k8sConfigDetails(delegateGroup.getK8sConfigDetails())
                                      .name(delegateGroup.getName())
                                      .size(delegateGroup.getSizeDetails().getSize())
                                      .orgIdentifier(orgId)
                                      .projectIdentifier(projectId)
                                      .build())
            .build());

    DelegateEntityOwner owner = isNotEmpty(groupDelegates) ? groupDelegates.get(0).getOwner() : null;
    publishDelegateChangeEventViaEventFramework(accountId, delegateGroupUuid, owner, DELETE_ACTION);
  }

  private void publishDelegateChangeEventViaEventFramework(
      String accountId, String delegateGroupId, DelegateEntityOwner owner, String action) {
    try {
      EntityChangeDTO.Builder entityChangeDTOBuilder = EntityChangeDTO.newBuilder()
                                                           .setAccountIdentifier(StringValue.of(accountId))
                                                           .setIdentifier(StringValue.of(delegateGroupId));

      if (owner != null) {
        String orgIdentifier = DelegateEntityOwnerHelper.extractOrgIdFromOwnerIdentifier(owner.getIdentifier());
        if (isNotBlank(orgIdentifier)) {
          entityChangeDTOBuilder.setOrgIdentifier(StringValue.of(orgIdentifier));
        }

        String projectIdentifier = DelegateEntityOwnerHelper.extractProjectIdFromOwnerIdentifier(owner.getIdentifier());
        if (isNotBlank(projectIdentifier)) {
          entityChangeDTOBuilder.setProjectIdentifier(StringValue.of(projectIdentifier));
        }
      }

      eventProducer.send(
          Message.newBuilder()
              .putAllMetadata(ImmutableMap.of("accountId", accountId, EventsFrameworkMetadataConstants.ENTITY_TYPE,
                  EventsFrameworkMetadataConstants.DELEGATE_ENTITY, EventsFrameworkMetadataConstants.ACTION, action))
              .setData(entityChangeDTOBuilder.build().toByteString())
              .build());
    } catch (Exception ex) {
      log.error(String.format(
          "Failed to publish delegate group %s event for accountId %s via event framework.", action, accountId));
    }
  }

  @Override
  public DelegateRegisterResponse register(Delegate delegate) {
    if (licenseService.isAccountDeleted(delegate.getAccountId())) {
      broadcasterFactory.lookup(STREAM_DELEGATE + delegate.getAccountId(), true).broadcast(SELF_DESTRUCT);
      log.warn("Sending self destruct command from register delegate because the account is deleted.");
      return DelegateRegisterResponse.builder().action(DelegateRegisterResponse.Action.SELF_DESTRUCT).build();
    }

    if (isNotBlank(delegate.getDelegateGroupId())) {
      DelegateGroup delegateGroup = persistence.get(DelegateGroup.class, delegate.getDelegateGroupId());

      if (delegateGroup != null && DelegateGroupStatus.DELETED == delegateGroup.getStatus()) {
        log.warn("Sending self destruct command from register delegate because the delegate group is deleted.");
        return DelegateRegisterResponse.builder().action(DelegateRegisterResponse.Action.SELF_DESTRUCT).build();
      }
    }

    boolean useCdn = featureFlagService.isEnabled(USE_CDN_FOR_STORAGE_FILES, delegate.getAccountId());
    broadcasterFactory.lookup(STREAM_DELEGATE + delegate.getAccountId(), true)
        .broadcast(useCdn ? USE_CDN : USE_STORAGE_PROXY);

    String delegateTargetJreVersion = getTargetJreVersion(delegate.getAccountId());
    StringBuilder jreMessage = new StringBuilder().append(JRE_VERSION).append(delegateTargetJreVersion);
    broadcasterFactory.lookup(STREAM_DELEGATE + delegate.getAccountId(), true).broadcast(jreMessage.toString());
    log.debug("Sending message to delegate: {}", jreMessage);

    if (accountService.isAccountMigrated(delegate.getAccountId())) {
      String migrateMsg = MIGRATE + accountService.get(delegate.getAccountId()).getMigratedToClusterUrl();
      broadcasterFactory.lookup(STREAM_DELEGATE + delegate.getAccountId(), true).broadcast(migrateMsg);
      return DelegateRegisterResponse.builder()
          .action(DelegateRegisterResponse.Action.MIGRATE)
          .migrateUrl(accountService.get(delegate.getAccountId()).getMigratedToClusterUrl())
          .build();
    }

    Query<Delegate> delegateQuery = persistence.createQuery(Delegate.class)
                                        .filter(DelegateKeys.accountId, delegate.getAccountId())
                                        .filter(DelegateKeys.hostName, delegate.getHostName());
    // For delegates running in a kubernetes cluster we include lowercase account ID in the hostname to identify it.
    // We ignore IP address because that can change with every restart of the pod.
    if (!delegate.getHostName().contains(getAccountIdentifier(delegate.getAccountId()))) {
      delegateQuery.filter(DelegateKeys.ip, delegate.getIp());
    }

    Delegate existingDelegate = delegateQuery.project(DelegateKeys.status, true)
                                    .project(DelegateKeys.delegateProfileId, true)
                                    .project(DelegateKeys.description, true)
                                    .get();
    if (existingDelegate != null && existingDelegate.getStatus() == DelegateInstanceStatus.DELETED) {
      broadcasterFactory.lookup(STREAM_DELEGATE + delegate.getAccountId(), true)
          .broadcast(SELF_DESTRUCT + existingDelegate.getUuid());
      log.warn(
          "Sending self destruct command from register delegate because the existing delegate has status deleted.");
      return DelegateRegisterResponse.builder().action(DelegateRegisterResponse.Action.SELF_DESTRUCT).build();
    }

    log.info("Registering delegate for Hostname: {} IP: {}", delegate.getHostName(), delegate.getIp());

    if (ECS.equals(delegate.getDelegateType())) {
      return registerResponseFromDelegate(handleEcsDelegateRequest(delegate));
    } else {
      return registerResponseFromDelegate(upsertDelegateOperation(existingDelegate, delegate));
    }
  }

  @Override
  public DelegateRegisterResponse register(DelegateParams delegateParams) {
    if (licenseService.isAccountDeleted(delegateParams.getAccountId())) {
      broadcasterFactory.lookup(STREAM_DELEGATE + delegateParams.getAccountId(), true).broadcast(SELF_DESTRUCT);
      log.warn("Sending self destruct command from register delegate parameters because the account is deleted.");
      return DelegateRegisterResponse.builder().action(DelegateRegisterResponse.Action.SELF_DESTRUCT).build();
    }

    if (isNotBlank(delegateParams.getDelegateGroupId())) {
      DelegateGroup delegateGroup = persistence.get(DelegateGroup.class, delegateParams.getDelegateGroupId());

      if (delegateGroup != null && DelegateGroupStatus.DELETED == delegateGroup.getStatus()) {
        log.warn(
            "Sending self destruct command from register delegate parameters because the delegate group is deleted.");
        return DelegateRegisterResponse.builder().action(DelegateRegisterResponse.Action.SELF_DESTRUCT).build();
      }
    }

    boolean useCdn = featureFlagService.isEnabled(USE_CDN_FOR_STORAGE_FILES, delegateParams.getAccountId());
    broadcasterFactory.lookup(STREAM_DELEGATE + delegateParams.getAccountId(), true)
        .broadcast(useCdn ? USE_CDN : USE_STORAGE_PROXY);

    String delegateTargetJreVersion = getTargetJreVersion(delegateParams.getAccountId());
    StringBuilder jreMessage = new StringBuilder().append(JRE_VERSION).append(delegateTargetJreVersion);
    broadcasterFactory.lookup(STREAM_DELEGATE + delegateParams.getAccountId(), true).broadcast(jreMessage.toString());
    log.info("Sending message to delegate: {}", jreMessage);

    if (accountService.isAccountMigrated(delegateParams.getAccountId())) {
      String migrateMsg = MIGRATE + accountService.get(delegateParams.getAccountId()).getMigratedToClusterUrl();
      broadcasterFactory.lookup(STREAM_DELEGATE + delegateParams.getAccountId(), true).broadcast(migrateMsg);
      return DelegateRegisterResponse.builder()
          .action(DelegateRegisterResponse.Action.MIGRATE)
          .migrateUrl(accountService.get(delegateParams.getAccountId()).getMigratedToClusterUrl())
          .build();
    }

    Query<Delegate> delegateQuery = persistence.createQuery(Delegate.class)
                                        .filter(DelegateKeys.accountId, delegateParams.getAccountId())
                                        .filter(DelegateKeys.hostName, delegateParams.getHostName());
    // For delegates running in a kubernetes cluster we include lowercase account ID in the hostname to identify it.
    // We ignore IP address because that can change with every restart of the pod.
    if (!delegateParams.getHostName().contains(getAccountIdentifier(delegateParams.getAccountId()))) {
      delegateQuery.filter(DelegateKeys.ip, delegateParams.getIp());
    }

    Delegate existingDelegate = delegateQuery.project(DelegateKeys.status, true)
                                    .project(DelegateKeys.delegateProfileId, true)
                                    .project(DelegateKeys.description, true)
                                    .get();
    if (existingDelegate != null && existingDelegate.getStatus() == DelegateInstanceStatus.DELETED) {
      broadcasterFactory.lookup(STREAM_DELEGATE + delegateParams.getAccountId(), true)
          .broadcast(SELF_DESTRUCT + existingDelegate.getUuid());
      log.warn(
          "Sending self destruct command from register delegate parameters because the existing delegate has status deleted.");
      return DelegateRegisterResponse.builder().action(DelegateRegisterResponse.Action.SELF_DESTRUCT).build();
    }

    log.info("Registering delegate for Hostname: {} IP: {}", delegateParams.getHostName(), delegateParams.getIp());

    DelegateSizeDetails sizeDetails = null;
    if (isNotBlank(delegateParams.getDelegateSize())) {
      sizeDetails = fetchAvailableSizes()
                        .stream()
                        .filter(size -> size.getSize().name().equals(delegateParams.getDelegateSize()))
                        .findFirst()
                        .orElse(null);
    }

    String delegateGroupId = delegateParams.getDelegateGroupId();
    if (isBlank(delegateGroupId) && isNotBlank(delegateParams.getDelegateGroupName())) {
      DelegateGroup delegateGroup =
          upsertDelegateGroup(delegateParams.getDelegateGroupName(), delegateParams.getAccountId(), null);
      delegateGroupId = delegateGroup.getUuid();
    }

    // Check if delegate is NG delegate and set the flag to true, if needed
    boolean isNgDelegate = isNotBlank(delegateParams.getSessionIdentifier());

    DelegateEntityOwner owner =
        DelegateEntityOwnerHelper.buildOwner(delegateParams.getOrgIdentifier(), delegateParams.getProjectIdentifier());

    Delegate delegate =
        Delegate.builder()
            .uuid(delegateParams.getDelegateId())
            .accountId(delegateParams.getAccountId())
            .sessionIdentifier(
                isNotBlank(delegateParams.getSessionIdentifier()) ? delegateParams.getSessionIdentifier() : null)
            .owner(owner)
            .ng(isNgDelegate)
            .sizeDetails(sizeDetails)
            .description(delegateParams.getDescription())
            .ip(delegateParams.getIp())
            .hostName(delegateParams.getHostName())
            .delegateGroupName(delegateParams.getDelegateGroupName())
            .delegateGroupId(isNotBlank(delegateGroupId) ? delegateGroupId : null)
            .delegateName(delegateParams.getDelegateName())
            .delegateProfileId(delegateParams.getDelegateProfileId())
            .lastHeartBeat(delegateParams.getLastHeartBeat())
            .version(delegateParams.getVersion())
            .sequenceNum(delegateParams.getSequenceNum())
            .delegateType(delegateParams.getDelegateType())
            .delegateRandomToken(delegateParams.getDelegateRandomToken())
            .keepAlivePacket(delegateParams.isKeepAlivePacket())
            .polllingModeEnabled(delegateParams.isPollingModeEnabled())
            .proxy(delegateParams.isProxy())
            .sampleDelegate(delegateParams.isSampleDelegate())
            .currentlyExecutingDelegateTasks(delegateParams.getCurrentlyExecutingDelegateTasks())
            .ceEnabled(delegateParams.isCeEnabled())
            .build();
    if (ECS.equals(delegateParams.getDelegateType())) {
      return registerResponseFromDelegate(handleEcsDelegateRequest(delegate));
    } else {
      return registerResponseFromDelegate(upsertDelegateOperation(existingDelegate, delegate));
    }
  }

  @VisibleForTesting
  Delegate upsertDelegateOperation(Delegate existingDelegate, Delegate delegate) {
    long delegateHeartbeat = delegate.getLastHeartBeat();
    long now = clock.millis();
    long skew = Math.abs(now - delegateHeartbeat);
    if (skew > TimeUnit.MINUTES.toMillis(2L)) {
      log.warn("Delegate {} has clock skew of {}", delegate.getUuid(), Misc.getDurationString(skew));
    }
    delegate.setLastHeartBeat(now);
    delegate.setValidUntil(Date.from(OffsetDateTime.now().plusDays(Delegate.TTL.toDays()).toInstant()));
    Delegate registeredDelegate;
    if (existingDelegate == null) {
      log.info("No existing delegate, adding for account {}: Hostname: {} IP: {}", delegate.getAccountId(),
          delegate.getHostName(), delegate.getIp());

      createAuditHeaderForDelegateRegistration(delegate.getHostName());

      registeredDelegate = add(delegate);
    } else {
      log.info("Delegate exists, updating: {}", delegate.getUuid());
      delegate.setUuid(existingDelegate.getUuid());
      delegate.setStatus(existingDelegate.getStatus());
      delegate.setDelegateProfileId(existingDelegate.getDelegateProfileId());
      if (isEmpty(delegate.getDescription())) {
        delegate.setDescription(existingDelegate.getDescription());
      }
      if (ECS.equals(delegate.getDelegateType())) {
        registeredDelegate = updateEcsDelegate(delegate, false);
      } else {
        registeredDelegate = update(delegate);
      }
    }

    // Not needed to be done when polling is enabled for delegate
    if (isDelegateWithoutPollingEnabled(delegate)) {
      // Broadcast Message containing, DelegateId and SeqNum (if applicable)
      StringBuilder message = new StringBuilder(128).append("[X]").append(delegate.getUuid());
      updateBroadcastMessageIfEcsDelegate(message, delegate, registeredDelegate);
      broadcasterFactory.lookup(STREAM_DELEGATE + delegate.getAccountId(), true).broadcast(message.toString());

      // TODO: revisit this call, it seems overkill
      alertService.delegateAvailabilityUpdated(registeredDelegate.getAccountId());
      alertService.delegateEligibilityUpdated(registeredDelegate.getAccountId(), registeredDelegate.getUuid());
    }

    return registeredDelegate;
  }

  private AuditHeader createAuditHeaderForDelegateRegistration(String delegateHostName) {
    AuditHeader.Builder builder = anAuditHeader();
    builder.withCreatedAt(System.currentTimeMillis())
        .withCreatedBy(EmbeddedUser.builder().name(delegateHostName).uuid("delegate").build())
        .withRemoteUser(anUser().name(delegateHostName).uuid("delegate").build())
        .withRequestMethod(HttpMethod.POST)
        .withRequestTime(System.currentTimeMillis())
        .withUrl("/agent/delegates");

    return auditHelper.create(builder.build());
  }

  private void updateBroadcastMessageIfEcsDelegate(
      StringBuilder message, Delegate delegate, Delegate registeredDelegate) {
    if (ECS.equals(delegate.getDelegateType())) {
      String hostName = getDelegateHostNameByRemovingSeqNum(registeredDelegate);
      String seqNum = getDelegateSeqNumFromHostName(registeredDelegate);
      DelegateSequenceConfig sequenceConfig =
          getDelegateSequenceConfig(delegate.getAccountId(), hostName, Integer.parseInt(seqNum));
      registeredDelegate.setDelegateRandomToken(sequenceConfig.getDelegateToken());
      registeredDelegate.setSequenceNum(sequenceConfig.getSequenceNum().toString());
      message.append("[TOKEN]")
          .append(sequenceConfig.getDelegateToken())
          .append("[SEQ]")
          .append(sequenceConfig.getSequenceNum());

      log.info("^^^^SEQ: " + message.toString());
    }
  }

  private DelegateRegisterResponse registerResponseFromDelegate(Delegate delegate) {
    if (delegate == null) {
      return null;
    }

    return DelegateRegisterResponse.builder()
        .delegateId(delegate.getUuid())
        .sequenceNum(delegate.getSequenceNum())
        .delegateRandomToken(delegate.getDelegateRandomToken())
        .build();
  }

  @VisibleForTesting
  DelegateSequenceConfig getDelegateSequenceConfig(String accountId, String hostName, Integer seqNum) {
    Query<DelegateSequenceConfig> delegateSequenceQuery = persistence.createQuery(DelegateSequenceConfig.class)
                                                              .filter(DelegateSequenceConfigKeys.accountId, accountId)
                                                              .filter(DelegateSequenceConfigKeys.hostName, hostName);

    if (seqNum != null) {
      delegateSequenceQuery.filter(DelegateSequenceConfigKeys.sequenceNum, seqNum);
    }

    return delegateSequenceQuery.project(DelegateSequenceConfigKeys.accountId, true)
        .project(DelegateSequenceConfigKeys.sequenceNum, true)
        .project(DelegateSequenceConfigKeys.hostName, true)
        .project(DelegateSequenceConfigKeys.delegateToken, true)
        .get();
  }

  @Override
  public DelegateProfileParams checkForProfile(
      String accountId, String delegateId, String profileId, long lastUpdatedAt) {
    if (configurationController.isNotPrimary()) {
      return null;
    }

    log.info("Checking delegate profile. Previous profile [{}] updated at {}", profileId, lastUpdatedAt);
    Delegate delegate = delegateCache.get(accountId, delegateId, true);

    if (delegate == null || DelegateInstanceStatus.ENABLED != delegate.getStatus()) {
      return null;
    }

    if (isNotBlank(profileId) && isBlank(delegate.getDelegateProfileId())) {
      return DelegateProfileParams.builder().profileId("NONE").build();
    }

    if (isNotBlank(delegate.getDelegateProfileId())) {
      DelegateProfile profile = delegateProfileService.get(accountId, delegate.getDelegateProfileId());
      if (profile != null && (!profile.getUuid().equals(profileId) || profile.getLastUpdatedAt() > lastUpdatedAt)) {
        Map<String, Object> context = new HashMap<>();
        context.put("secrets",
            SecretFunctor.builder()
                .managerDecryptionService(managerDecryptionService)
                .secretManager(secretManager)
                .accountId(accountId)
                .build());
        String scriptContent = evaluator.substitute(profile.getStartupScript(), context);
        return DelegateProfileParams.builder()
            .profileId(profile.getUuid())
            .name(profile.getName())
            .profileLastUpdatedAt(profile.getLastUpdatedAt())
            .scriptContent(scriptContent)
            .build();
      }
    }
    return null;
  }

  @Override
  public void saveProfileResult(String accountId, String delegateId, boolean error, FileBucket fileBucket,
      InputStream uploadedInputStream, FormDataContentDisposition fileDetail) {
    Delegate delegate = delegateCache.get(accountId, delegateId, true);
    DelegateProfileErrorAlert alertData = DelegateProfileErrorAlert.builder()
                                              .accountId(accountId)
                                              .hostName(delegate.getHostName())
                                              .obfuscatedIpAddress(obfuscate(delegate.getIp()))
                                              .build();
    if (error) {
      alertService.openAlert(accountId, GLOBAL_APP_ID, AlertType.DelegateProfileError, alertData);
    } else {
      alertService.closeAlert(accountId, GLOBAL_APP_ID, AlertType.DelegateProfileError, alertData);
    }

    FileMetadata fileMetadata = FileMetadata.builder()
                                    .fileName(new File(fileDetail.getFileName()).getName())
                                    .accountId(accountId)
                                    .fileUuid(generateUuid())
                                    .build();
    String fileId = fileService.saveFile(fileMetadata,
        new BoundedInputStream(uploadedInputStream, mainConfiguration.getFileUploadLimits().getProfileResultLimit()),
        fileBucket);

    String previousProfileResult = delegate.getProfileResult();

    persistence.update(persistence.createQuery(Delegate.class)
                           .filter(DelegateKeys.accountId, accountId)
                           .filter(DelegateKeys.uuid, delegateId),
        persistence.createUpdateOperations(Delegate.class)
            .set(DelegateKeys.profileResult, fileId)
            .set(DelegateKeys.profileError, error)
            .set(DelegateKeys.profileExecutedAt, clock.millis()));

    if (isNotBlank(previousProfileResult)) {
      fileService.deleteFile(previousProfileResult, FileBucket.PROFILE_RESULTS);
    }
  }

  @Override
  public String getProfileResult(String accountId, String delegateId) {
    Delegate delegate = delegateCache.get(accountId, delegateId, false);

    String profileResultFileId = delegate.getProfileResult();

    if (isBlank(profileResultFileId)) {
      return "No profile result available for " + delegate.getHostName();
    }

    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      fileService.downloadToStream(profileResultFileId, os, FileBucket.PROFILE_RESULTS);
      os.flush();
      return new String(os.toByteArray(), UTF_8);
    } catch (Exception e) {
      throw new GeneralException("Profile execution log temporarily unavailable. Try again in a few moments.");
    }
  }

  @Override
  public String obtainDelegateName(Delegate delegate) {
    if (delegate == null) {
      return "";
    }

    String delegateName = delegate.getDelegateName();
    if (!isBlank(delegateName)) {
      return delegateName;
    }

    String hostName = delegate.getHostName();
    if (!isBlank(hostName)) {
      return hostName;
    }

    return delegate.getUuid();
  }

  @Override
  public String obtainDelegateName(String accountId, String delegateId, boolean forceRefresh) {
    if (isBlank(accountId) || isBlank(delegateId)) {
      return "";
    }

    Delegate delegate = delegateCache.get(accountId, delegateId, forceRefresh);
    if (delegate == null) {
      return delegateId;
    }

    return obtainDelegateName(delegate);
  }

  @Override
  public List<String> obtainDelegateIds(String accountId, String sessionIdentifier) {
    try {
      return persistence.createQuery(Delegate.class)
          .filter(DelegateKeys.accountId, accountId)
          .filter(DelegateKeys.sessionIdentifier, sessionIdentifier)
          .asKeyList()
          .stream()
          .map(key -> (String) key.getId())
          .collect(toList());
    } catch (Exception e) {
      log.error("Could not get delegates from DB.", e);
      return null;
    }
  }

  @Override
  public void saveDelegateTask(DelegateTask task, Status status) {
    delegateTaskServiceClassic.saveDelegateTask(task, status);
  }

  @VisibleForTesting
  public List<CapabilityRequirement> createCapabilityRequirementInstances(
      String accountId, List<ExecutionCapability> agentCapabilities) {
    List<CapabilityRequirement> capabilityRequirements = new ArrayList<>();
    for (ExecutionCapability agentCapability : agentCapabilities) {
      CapabilityRequirement capabilityRequirement =
          capabilityService.buildCapabilityRequirement(accountId, agentCapability);

      if (capabilityRequirement != null) {
        capabilityRequirements.add(capabilityRequirement);
      }
    }

    return capabilityRequirements;
  }

  @Override
  public void regenerateCapabilityPermissions(String accountId, String delegateId) {
    List<CapabilityRequirement> capabilityRequirements = capabilityService.getAllCapabilityRequirements(accountId);

    for (CapabilityRequirement capabilityRequirement : capabilityRequirements) {
      if (!isDelegateStillInScope(accountId, delegateId, capabilityRequirement.getUuid())) {
        capabilityService.deleteCapabilitySubjectPermission(accountId, delegateId, capabilityRequirement.getUuid());
        continue;
      }

      // If delegate is in scope, we need to add permission record only if it is not already there
      List<String> existingPermissionDelegateIds =
          capabilityService
              .getAllCapabilityPermissions(capabilityRequirement.getAccountId(), capabilityRequirement.getUuid(), null)
              .stream()
              .map(CapabilitySubjectPermission::getDelegateId)
              .collect(toList());

      if (!existingPermissionDelegateIds.contains(delegateId)) {
        capabilityService.addCapabilityPermissions(
            capabilityRequirement, Arrays.asList(delegateId), PermissionResult.UNCHECKED, true);
      }
    }
  }

  @VisibleForTesting
  public boolean isDelegateStillInScope(String accountId, String delegateId, String capabilityId) {
    List<CapabilityTaskSelectionDetails> taskSelectionDetailsList =
        capabilityService.getAllCapabilityTaskSelectionDetails(accountId, capabilityId);

    if (isEmpty(taskSelectionDetailsList)) {
      return true;
    }

    for (CapabilityTaskSelectionDetails taskSelectionDetails : taskSelectionDetailsList) {
      if (isDelegateInCapabilityScope(accountId, delegateId, taskSelectionDetails)) {
        return true;
      }
    }

    // Since the delegate is not in scope for given capability, we need to mark capability task selection details as
    // blocked, if no other delegates are in scope
    List<String> notDeniedDelegates = capabilityService.getNotDeniedCapabilityPermissions(accountId, capabilityId)
                                          .stream()
                                          .map(CapabilitySubjectPermission::getDelegateId)
                                          .collect(toList());

    for (CapabilityTaskSelectionDetails taskSelectionDetails : taskSelectionDetailsList) {
      if (!notDeniedDelegates.stream().anyMatch(
              delegateIdentifier -> isDelegateInCapabilityScope(accountId, delegateIdentifier, taskSelectionDetails))) {
        // Update task selection details record and mark it as blocked
        Query<CapabilityTaskSelectionDetails> selectionDetailsQuery =
            persistence.createQuery(CapabilityTaskSelectionDetails.class)
                .filter(CapabilityTaskSelectionDetailsKeys.accountId, accountId)
                .filter(CapabilityTaskSelectionDetailsKeys.uuid, taskSelectionDetails.getUuid());

        UpdateOperations<CapabilityTaskSelectionDetails> selectionDetailsUpdateOperations =
            persistence.createUpdateOperations(CapabilityTaskSelectionDetails.class);
        setUnset(selectionDetailsUpdateOperations, CapabilityTaskSelectionDetailsKeys.blocked, true);

        persistence.findAndModify(
            selectionDetailsQuery, selectionDetailsUpdateOperations, HPersistence.returnNewOptions);
      }
    }

    return false;
  }

  @VisibleForTesting
  public boolean isDelegateInCapabilityScope(
      String accountId, String delegateId, CapabilityTaskSelectionDetails taskSelectionDetails) {
    List<ExecutionCapability> selectorCapabilities = new ArrayList<>();
    if (isNotEmpty(taskSelectionDetails.getTaskSelectors())) {
      taskSelectionDetails.getTaskSelectors().forEach(
          (origin, selectors)
              -> selectorCapabilities.add(SelectorCapability.builder()
                                              .capabilityType(CapabilityType.SELECTORS)
                                              .selectorOrigin(origin)
                                              .selectors(selectors)
                                              .build()));
    }

    String appId = null;
    String envId = null;
    String infraMappingId = null;
    if (isNotEmpty(taskSelectionDetails.getTaskSetupAbstractions())) {
      appId = taskSelectionDetails.getTaskSetupAbstractions().get(Cd1SetupFields.APP_ID_FIELD);
      envId = taskSelectionDetails.getTaskSetupAbstractions().get(Cd1SetupFields.ENV_ID_FIELD);
      infraMappingId =
          taskSelectionDetails.getTaskSetupAbstractions().get(Cd1SetupFields.INFRASTRUCTURE_MAPPING_ID_FIELD);
    }

    return assignDelegateService.canAssign(null, delegateId, accountId, appId, envId, infraMappingId,
        taskSelectionDetails.getTaskGroup(), selectorCapabilities, taskSelectionDetails.getTaskSetupAbstractions());
  }

  @Override
  public List<DelegateInitializationDetails> obtainDelegateInitializationDetails(
      String accountId, List<String> delegateIds) {
    List<DelegateInitializationDetails> delegateInitializationDetails = new ArrayList<>();

    delegateIds.forEach(
        delegateId -> delegateInitializationDetails.add(getDelegateInitializationDetails(accountId, delegateId)));

    return delegateInitializationDetails;
  }

  @Override
  public boolean validateThatDelegateNameIsUnique(String accountId, String delegateName) {
    Delegate delegate = persistence.createQuery(Delegate.class)
                            .filter(DelegateKeys.accountId, accountId)
                            .filter(DelegateKeys.delegateName, delegateName)
                            .get();
    if (delegate == null) {
      return true;
    }
    return false;
  }

  @Override
  public void delegateDisconnected(String accountId, String delegateId, String delegateConnectionId) {
    delegateConnectionDao.delegateDisconnected(accountId, delegateConnectionId);
    subject.fireInform(DelegateObxxxxxxxx:onDisconnected, accountId, delegateId);
  }

  @Override
  public void clearCache(String accountId, String delegateId) {
    assignDelegateService.clearConnectionResults(accountId, delegateId);
  }

  @Override
  public boolean filter(String accountId, String delegateId) {
    Delegate delegate = delegateCache.get(accountId, delegateId, false);
    return delegate != null && StringUtils.equals(delegate.getAccountId(), accountId);
  }

  @Override
  public void deleteByAccountId(String accountId) {
    persistence.delete(persistence.createQuery(Delegate.class).filter(DelegateKeys.accountId, accountId));
  }

  //------ Start: ECS Delegate Specific Methods

  /**
   * Delegate keepAlive and Registration requests will be handled here
   */
  @Override
  public Delegate handleEcsDelegateRequest(Delegate delegate) {
    if (delegate.isKeepAlivePacket()) {
      handleEcsDelegateKeepAlivePacket(delegate);
      return null;
    }
    Delegate registeredDelegate = handleEcsDelegateRegistration(delegate);
    updateExistingDelegateWithSequenceConfigData(registeredDelegate);
    registeredDelegate.setUseCdn(featureFlagService.isEnabled(USE_CDN_FOR_STORAGE_FILES, delegate.getAccountId()));
    registeredDelegate.setUseJreVersion(getTargetJreVersion(delegate.getAccountId()));

    return registeredDelegate;
  }

  @Override
  public DelegateGroup upsertDelegateGroup(String name, String accountId, DelegateSetupDetails delegateSetupDetails) {
    boolean isNg = delegateSetupDetails != null;
    String delegateGroupIdentifier = getDelegateGroupIdentifier(name, delegateSetupDetails);

    if (isNg) {
      try {
        delegateSetupDetails.setIdentifier(delegateGroupIdentifier);
        NGUtils.validate(delegateSetupDetails);
      } catch (JerseyViolationException exception) {
        throw new InvalidRequestException(getMessage(exception));
      }
    }

    String description = delegateSetupDetails != null ? delegateSetupDetails.getDescription() : null;
    String delegateConfigurationId =
        delegateSetupDetails != null ? delegateSetupDetails.getDelegateConfigurationId() : null;
    String orgIdentifier = delegateSetupDetails != null ? delegateSetupDetails.getOrgIdentifier() : null;
    String projectIdentifier = delegateSetupDetails != null ? delegateSetupDetails.getProjectIdentifier() : null;
    DelegateSizeDetails sizeDetails = delegateSetupDetails != null
        ? fetchAvailableSizes()
              .stream()
              .filter(size -> size.getSize() == delegateSetupDetails.getSize())
              .findFirst()
              .orElse(null)
        : null;
    K8sConfigDetails k8sConfigDetails =
        delegateSetupDetails != null ? delegateSetupDetails.getK8sConfigDetails() : null;

    Set<String> tags = delegateSetupDetails != null && isNotEmpty(delegateSetupDetails.getTags())
        ? delegateSetupDetails.getTags()
        : null;

    DelegateEntityOwner owner = DelegateEntityOwnerHelper.buildOwner(orgIdentifier, projectIdentifier);

    Query<DelegateGroup> query = this.persistence.createQuery(DelegateGroup.class)
                                     .filter(DelegateGroupKeys.accountId, accountId)
                                     .filter(DelegateGroupKeys.ng, isNg)
                                     .filter(DelegateGroupKeys.owner, owner)
                                     .filter(DelegateGroupKeys.name, name);

    // this statement is here because of identifier migration where we used normalized uuid for existing groups
    DelegateGroup existingEntity = query.get();
    if (existingEntity != null && uuidToIdentifier(existingEntity.getUuid()).equals(existingEntity.getIdentifier())) {
      delegateGroupIdentifier = existingEntity.getIdentifier();
    }

    if (existingEntity != null && existingEntity.getIdentifier() == null) {
      log.warn("Existing delegate group {} has null identifier. New entry will be created with identifier {}",
          existingEntity, delegateGroupIdentifier);
    }

    query.filter(DelegateGroupKeys.identifier, delegateGroupIdentifier);

    UpdateOperations<DelegateGroup> updateOperations =
        this.persistence.createUpdateOperations(DelegateGroup.class)
            .setOnInsert(DelegateGroupKeys.uuid, generateUuid())
            .setOnInsert(DelegateGroupKeys.identifier, delegateGroupIdentifier)
            .set(DelegateGroupKeys.name, name)
            .set(DelegateGroupKeys.accountId, accountId)
            .set(DelegateGroupKeys.ng, isNg);

    if (k8sConfigDetails != null) {
      updateOperations.set(DelegateGroupKeys.delegateType, KUBERNETES);
    }

    setUnset(updateOperations, DelegateGroupKeys.k8sConfigDetails, k8sConfigDetails);
    setUnset(updateOperations, DelegateGroupKeys.owner, owner);
    setUnset(updateOperations, DelegateGroupKeys.description, description);
    setUnset(updateOperations, DelegateGroupKeys.delegateConfigurationId, delegateConfigurationId);
    setUnset(updateOperations, DelegateGroupKeys.sizeDetails, sizeDetails);
    setUnset(updateOperations, DelegateGroupKeys.tags, tags);

    DelegateGroup delegateGroup = persistence.upsert(query, updateOperations, HPersistence.upsertReturnNewOptions);
    outboxService.save(
        DelegateGroupUpsertEvent.builder()
            .accountIdentifier(accountId)
            .orgIdentifier(delegateSetupDetails != null ? delegateSetupDetails.getOrgIdentifier() : null)
            .projectIdentifier(delegateSetupDetails != null ? delegateSetupDetails.getProjectIdentifier() : null)
            .delegateGroupId(delegateGroup.getUuid())
            .delegateSetupDetails(delegateSetupDetails)
            .build());
    return delegateGroup;
  }

  @NotNull
  private String getMessage(JerseyViolationException exception) {
    return "Fields "
        + exception.getConstraintViolations()
              .stream()
              .map(c -> ((PathImpl) c.getPropertyPath()).getLeafNode().getName())
              .reduce("", (i, j) -> i + " <" + j + "> ")
        + " did not pass validation checks: "
        + exception.getConstraintViolations()
              .stream()
              .map(ConstraintViolation::getMessage)
              .reduce("", (i, j) -> i + " <" + j + "> ");
  }

  private String getDelegateGroupIdentifier(String name, DelegateSetupDetails delegateSetupDetails) {
    if (delegateSetupDetails != null && isNotBlank(delegateSetupDetails.getIdentifier())) {
      return delegateSetupDetails.getIdentifier();
    } else if (delegateSetupDetails != null && isBlank(delegateSetupDetails.getIdentifier())) {
      return normalizeIdentifier(delegateSetupDetails.getName());
    } else {
      return normalizeIdentifier(name);
    }
  }

  public void registerHeartbeat(
      String accountId, String delegateId, DelegateConnectionHeartbeat heartbeat, ConnectionMode connectionMode) {
    DelegateConnection previousDelegateConnection = delegateConnectionDao.upsertCurrentConnection(
        accountId, delegateId, heartbeat.getDelegateConnectionId(), heartbeat.getVersion(), heartbeat.getLocation());

    if (previousDelegateConnection == null) {
      DelegateConnection existingConnection = delegateConnectionDao.findAndDeletePreviousConnections(
          accountId, delegateId, heartbeat.getDelegateConnectionId(), heartbeat.getVersion());
      if (existingConnection != null) {
        UUID currentUUID = convertFromBase64(heartbeat.getDelegateConnectionId());
        UUID existingUUID = convertFromBase64(existingConnection.getUuid());
        if (existingUUID.timestamp() > currentUUID.timestamp()) {
          Delegate delegate = delegateCache.get(accountId, delegateId, false);
          boolean notSameLocationForShellScriptDelegate = DelegateType.SHELL_SCRIPT.equals(delegate.getDelegateType())
              && (isNotEmpty(heartbeat.getLocation()) && isNotEmpty(existingConnection.getLocation())
                  && !heartbeat.getLocation().equals(existingConnection.getLocation()));
          if (notSameLocationForShellScriptDelegate) {
            log.error(
                "Newer delegate connection found for the delegate id! Will initiate self destruct sequence for the current delegate.");
            destroyTheCurrentDelegate(accountId, delegateId, heartbeat.getDelegateConnectionId(), connectionMode);
            delegateConnectionDao.replaceWithNewerConnection(heartbeat.getDelegateConnectionId(), existingConnection);
          } else {
            log.error("Two delegates with the same identity");
          }
        } else {
          log.error("Delegate restarted");
        }
      }
    } else if (featureFlagService.isEnabled(PER_AGENT_CAPABILITIES, accountId)
        && previousDelegateConnection.isDisconnected()) {
      subject.fireInform(DelegateObxxxxxxxx:onReconnected, accountId, delegateId);
    }
  }

  /**
   * ECS delegate sends keepAlive request every 20 secs. KeepAlive request is a frequent and light weight
   * mode for indicating that delegate is active.
   * <p>
   * We just update "lastUpdatedAt" field with latest time for DelegateSequenceConfig associated with delegate,
   * so we can found stale config (not updated in last 100 secs) when we need to reuse it for new delegate
   * registration.
   */
  @VisibleForTesting
  void handleEcsDelegateKeepAlivePacket(Delegate delegate) {
    log.info("Handling Keep alive packet ");
    if (isBlank(delegate.getHostName()) || isBlank(delegate.getDelegateRandomToken()) || isBlank(delegate.getUuid())
        || isBlank(delegate.getSequenceNum())) {
      return;
    }

    Delegate existingDelegate =
        getDelegateUsingSequenceNum(delegate.getAccountId(), delegate.getHostName(), delegate.getSequenceNum());
    if (existingDelegate == null) {
      return;
    }

    DelegateSequenceConfig config = getDelegateSequenceConfig(
        delegate.getAccountId(), delegate.getHostName(), Integer.parseInt(delegate.getSequenceNum()));

    if (config != null && config.getDelegateToken().equals(delegate.getDelegateRandomToken())) {
      Query<DelegateSequenceConfig> sequenceConfigQuery =
          persistence.createQuery(DelegateSequenceConfig.class).filter(ID_KEY, config.getUuid());
      persistence.update(sequenceConfigQuery,
          persistence.createUpdateOperations(DelegateSequenceConfig.class)
              .set(DelegateSequenceConfigKeys.delegateToken, delegate.getDelegateRandomToken()));
    }
  }

  /**
   * Handles first time registration or heartbeat request send by delegate
   */
  @VisibleForTesting
  Delegate handleEcsDelegateRegistration(Delegate delegate) {
    // SCENARIO 1: Received delegateId with the request and delegate exists in DB.
    // Just update same existing delegate

    if (delegate.getUuid() != null && isValidSeqNum(delegate.getSequenceNum())
        && checkForValidTokenIfPresent(delegate)) {
      Delegate registeredDelegate = handleECSRegistrationUsingID(delegate);
      if (registeredDelegate != null) {
        return registeredDelegate;
      }
    }

    // can not proceed unless we receive valid token
    if (isBlank(delegate.getDelegateRandomToken()) || "null".equalsIgnoreCase(delegate.getDelegateRandomToken())) {
      throw new GeneralException("Received invalid token from ECS delegate");
    }

    // SCENARIO 2: Delegate passed sequenceNum & delegateToken but not UUID.
    // So delegate was registered earlier but may be got restarted and trying re-register.
    if (isValidSeqNum(delegate.getSequenceNum()) && isNotBlank(delegate.getDelegateRandomToken())) {
      Delegate registeredDelegate = handleECSRegistrationUsingSeqNumAndToken(delegate);
      if (registeredDelegate != null) {
        return registeredDelegate;
      }
    }

    // SCENARIO 3: Create new SequenceNum for delegate.
    // We will reach here in 2 scenarios,
    // 1. Delegate did not pass any sequenceNum or delegateToken. (This is first time delegate is registering after
    // start up or disk file delegate writes to, got deleted).

    // 2. Delegate passed seqNum & delegateToken, but We got DuplicateKeyException in SCENARIO 2
    // In any of these cases, it will be treated as fresh registration and new sequenceNum will be generated.
    return registerDelegateWithNewSequenceGeneration(delegate);
  }

  @VisibleForTesting
  boolean checkForValidTokenIfPresent(Delegate delegate) {
    DelegateSequenceConfig config = getDelegateSequenceConfig(
        delegate.getAccountId(), delegate.getHostName(), Integer.parseInt(delegate.getSequenceNum()));
    return config != null && config.getDelegateToken().equals(delegate.getDelegateRandomToken());
  }

  /**
   * Delegate sent token and seqNum but null UUID.
   * 1. See if DelegateSequenceConfig record with same {accId, SeqNum} has same token as passed by delegate.
   * If yes,
   * - get delegate associated with this DelegateSequenceConfig if exists and update it.
   * - if delegate does not present in db, create a new record (init it with config from similar delegate and
   * create record)
   * <p>
   * IF No,
   * - Means that seqNum has been acquired by another delegate.
   * - Generate a new SeqNum and create delegate record using it (init it with config from similar delegate and
   * create record).
   */
  @VisibleForTesting
  Delegate handleECSRegistrationUsingSeqNumAndToken(Delegate delegate) {
    log.info("Delegate sent seqNum : " + delegate.getSequenceNum() + ", and DelegateToken"
        + delegate.getDelegateRandomToken());

    DelegateSequenceConfig sequenceConfig = getDelegateSequenceConfig(
        delegate.getAccountId(), delegate.getHostName(), Integer.parseInt(delegate.getSequenceNum()));

    Delegate existingDelegate = null;
    boolean delegateConfigMatches = false;
    // SequenceConfig found with same {HostName, AccountId, SequenceNum, DelegateToken}.
    // Its same delegate sending request with valid data. Find actual delegate record using this
    // DelegateSequenceConfig
    if (seqNumAndTokenMatchesConfig(delegate, sequenceConfig)) {
      delegateConfigMatches = true;
      existingDelegate = getDelegateUsingSequenceNum(
          sequenceConfig.getAccountId(), sequenceConfig.getHostName(), sequenceConfig.getSequenceNum().toString());
    }

    // No Existing delegate was found, so create new delegate record on manager side,
    // using {seqNum, delegateToken} passed by delegate.
    if (existingDelegate == null) {
      try {
        DelegateSequenceConfig config = delegateConfigMatches
            ? sequenceConfig
            : generateNewSeqenceConfig(delegate, Integer.parseInt(delegate.getSequenceNum()));

        String hostNameWithSeqNum =
            getHostNameToBeUsedForECSDelegate(config.getHostName(), config.getSequenceNum().toString());
        delegate.setHostName(hostNameWithSeqNum);

        // Init this delegate with {TAG/SCOPE/PROFILE} config reading from similar delegate
        initDelegateWithConfigFromExistingDelegate(delegate);

        return upsertDelegateOperation(null, delegate);
      } catch (DuplicateKeyException e) {
        log.warn(
            "SequenceNum passed by delegate has been assigned to a new delegate. will regenerate new sequenceNum.");
      }
    } else {
      // Existing delegate was found, so just update it.
      return upsertDelegateOperation(existingDelegate, delegate);
    }

    return null;
  }

  @VisibleForTesting
  boolean seqNumAndTokenMatchesConfig(Delegate delegate, DelegateSequenceConfig sequenceConfig) {
    return sequenceConfig != null && sequenceConfig.getSequenceNum() != null
        && isNotBlank(sequenceConfig.getDelegateToken())
        && sequenceConfig.getDelegateToken().equals(delegate.getDelegateRandomToken())
        && sequenceConfig.getSequenceNum().toString().equals(delegate.getSequenceNum());
  }

  /**
   * Get Delegate associated with {AccountId, HostName, SeqNum}
   */
  @VisibleForTesting
  Delegate getDelegateUsingSequenceNum(String accountId, String hostName, String seqNum) {
    Delegate existingDelegate;
    Query<Delegate> delegateQuery =
        persistence.createQuery(Delegate.class)
            .filter(DelegateKeys.accountId, accountId)
            .filter(DelegateSequenceConfigKeys.hostName, getHostNameToBeUsedForECSDelegate(hostName, seqNum));

    existingDelegate = delegateQuery.get();
    return existingDelegate;
  }

  /**
   * Get existing delegate having same {hostName (prefix without seqNum), AccId, type = ECS}
   * Copy {SCOPE/PROFILE/TAG/KEYWORDS/DESCRIPTION} config into new delegate being registered
   */
  @VisibleForTesting
  void initDelegateWithConfigFromExistingDelegate(Delegate delegate) {
    List<Delegate> existingDelegates = getAllDelegatesMatchingGroupName(delegate);
    if (isNotEmpty(existingDelegates)) {
      initNewDelegateWithExistingDelegate(delegate, existingDelegates.get(0));
    }
  }

  /**
   * Delegate send UUID, if record exists, just update same one.
   */
  @VisibleForTesting
  Delegate handleECSRegistrationUsingID(Delegate delegate) {
    Query<Delegate> delegateQuery =
        persistence.createQuery(Delegate.class).filter(DelegateKeys.uuid, delegate.getUuid());

    Delegate existingDelegate = delegateQuery.project(DelegateKeys.hostName, true)
                                    .project(DelegateKeys.status, true)
                                    .project(DelegateKeys.delegateProfileId, true)
                                    .project(DelegateKeys.description, true)
                                    .get();

    if (existingDelegate != null) {
      return upsertDelegateOperation(existingDelegate, delegate);
    }

    return null;
  }

  /**
   * Either
   * 1. find a stale DelegateSeqConfig (not updated for last 100 secs),
   * delete delegate associated with it and use this seqNum for new delegate registration.
   * <p>
   * 2. Else no such config exists from point 1, Create new SequenceConfig and associate with delegate.
   * (In both cases, we copy config {SCOPE/TAG/PROFILE} from existing delegates to this new delegate being registered)
   */
  @VisibleForTesting
  Delegate registerDelegateWithNewSequenceGeneration(Delegate delegate) {
    List<DelegateSequenceConfig> existingDelegateSequenceConfigs = getDelegateSequenceConfigs(delegate);

    // Find Inactive DelegateSequenceConfig with same Acc and hostName and delete associated delegate
    DelegateSequenceConfig config =
        getInactiveDelegateSequenceConfigToReplace(delegate, existingDelegateSequenceConfigs);

    if (config != null) {
      return upsertDelegateOperation(null, delegate);
    }

    // Could not find InactiveDelegateConfig, Create new SequenceConfig
    for (int i = 0; i < 3; i++) {
      try {
        config = addNewDelegateSequenceConfigRecord(delegate);
        String hostNameWithSeqNum =
            getHostNameToBeUsedForECSDelegate(delegate.getHostName(), config.getSequenceNum().toString());
        delegate.setHostName(hostNameWithSeqNum);

        // Init this delegate with TAG/SCOPE/PROFILE/KEYWORDS/DESCRIPTION config reading from similar delegate
        initDelegateWithConfigFromExistingDelegate(delegate);

        return upsertDelegateOperation(null, delegate);
      } catch (Exception e) {
        log.warn("Attempt: " + i + " failed with DuplicateKeyException. Trying again" + e);
      }
    }
    // All 3 attempts of sequenceNum generation for delegate failed. Registration can not be completed.
    // Delegate will need to send request again
    throw new GeneralException("Failed to generate sequence number for Delegate");
  }

  /**
   * This method expects, you have already stripped off seqNum for delegate host name
   */
  @VisibleForTesting
  List<DelegateSequenceConfig> getDelegateSequenceConfigs(Delegate delegate) {
    Query<DelegateSequenceConfig> delegateSequenceConfigQuery =
        persistence.createQuery(DelegateSequenceConfig.class)
            .filter(DelegateSequenceConfigKeys.accountId, delegate.getAccountId())
            .filter(DelegateSequenceConfigKeys.hostName, delegate.getHostName());

    return delegateSequenceConfigQuery.project(ID_KEY, true)
        .project(DelegateSequenceConfigKeys.sequenceNum, true)
        .project(DelegateSequenceConfig.LAST_UPDATED_AT_KEY2, true)
        .project(DelegateSequenceConfig.ACCOUNT_ID_KEY2, true)
        .project(DelegateSequenceConfigKeys.hostName, true)
        .project(DelegateSequenceConfigKeys.delegateToken, true)
        .asList();
  }

  @VisibleForTesting
  DelegateSequenceConfig addNewDelegateSequenceConfigRecord(Delegate delegate) {
    Query<DelegateSequenceConfig> delegateSequenceConfigQuery =
        persistence.createQuery(DelegateSequenceConfig.class)
            .filter(DelegateSequenceConfig.ACCOUNT_ID_KEY2, delegate.getAccountId())
            .filter(DelegateSequenceConfigKeys.hostName, delegate.getHostName());

    List<DelegateSequenceConfig> existingDelegateSequenceConfigs =
        delegateSequenceConfigQuery.project(DelegateSequenceConfigKeys.sequenceNum, true)
            .project(DelegateSequenceConfig.LAST_UPDATED_AT_KEY2, true)
            .project(DelegateSequenceConfig.ACCOUNT_ID_KEY2, true)
            .project(DelegateSequenceConfigKeys.hostName, true)
            .project(DelegateSequenceConfigKeys.delegateToken, true)
            .asList();

    existingDelegateSequenceConfigs = existingDelegateSequenceConfigs.stream()
                                          .sorted(comparingInt(DelegateSequenceConfig::getSequenceNum))
                                          .collect(toList());

    int num = 0;
    for (DelegateSequenceConfig existingDelegateSequenceConfig : existingDelegateSequenceConfigs) {
      if (num < existingDelegateSequenceConfig.getSequenceNum()) {
        break;
      }
      num++;
    }

    delegate.setSequenceNum(String.valueOf(num));
    return generateNewSeqenceConfig(delegate, num);
  }

  @VisibleForTesting
  DelegateSequenceConfig getInactiveDelegateSequenceConfigToReplace(
      Delegate delegate, List<DelegateSequenceConfig> existingDelegateSequenceConfigs) {
    DelegateSequenceConfig config;
    try {
      Optional<DelegateSequenceConfig> optionalConfig =
          existingDelegateSequenceConfigs.stream()
              .filter(sequenceConfig
                  -> sequenceConfig.getLastUpdatedAt() < currentTimeMillis() - TimeUnit.SECONDS.toMillis(100))
              .findFirst();

      if (optionalConfig.isPresent()) {
        config = optionalConfig.get();

        Delegate existingInactiveDelegate = getDelegateUsingSequenceNum(
            delegate.getAccountId(), config.getHostName(), config.getSequenceNum().toString());

        if (existingInactiveDelegate != null) {
          // Before deleting existing one, copy {TAG/PROFILE/SCOPE} config into new delegate being registered
          // This needs to be done here as this may be the only delegate in db.
          initNewDelegateWithExistingDelegate(delegate, existingInactiveDelegate);
          delete(existingInactiveDelegate.getAccountId(), existingInactiveDelegate.getUuid(), true);
        }

        Query<DelegateSequenceConfig> sequenceConfigQuery =
            persistence.createQuery(DelegateSequenceConfig.class).filter("_id", config.getUuid());
        persistence.update(sequenceConfigQuery,
            persistence.createUpdateOperations(DelegateSequenceConfig.class)
                .set(DelegateSequenceConfigKeys.delegateToken, delegate.getDelegateRandomToken()));

        // Update delegate with seqNum and hostName
        delegate.setSequenceNum(config.getSequenceNum().toString());
        String hostNameWithSeqNum =
            getHostNameToBeUsedForECSDelegate(config.getHostName(), config.getSequenceNum().toString());
        delegate.setHostName(hostNameWithSeqNum);

        if (existingInactiveDelegate == null) {
          initDelegateWithConfigFromExistingDelegate(delegate);
        }
        return config;
      }
    } catch (Exception e) {
      log.warn("Failed while updating delegateSequenceConfig with delegateToken: {}, DelegateId: {}",
          delegate.getDelegateRandomToken(), delegate.getUuid());
    }

    return null;
  }

  private DelegateSequenceConfig generateNewSeqenceConfig(Delegate delegate, Integer seqNum) {
    log.info("Adding delegateSequenceConfig For delegate.hostname: {}, With SequenceNum: {}, for account:  {}",
        delegate.getHostName(), delegate.getSequenceNum(), delegate.getAccountId());

    DelegateSequenceConfig sequenceConfig = aDelegateSequenceBuilder()
                                                .withSequenceNum(seqNum)
                                                .withAccountId(delegate.getAccountId())
                                                .withHostName(delegate.getHostName())
                                                .withDelegateToken(delegate.getDelegateRandomToken())
                                                .withAppId(GLOBAL_APP_ID)
                                                .build();

    persistence.save(sequenceConfig);
    log.info("DelegateSequenceConfig saved: {}", sequenceConfig);

    return sequenceConfig;
  }

  private String getHostNameToBeUsedForECSDelegate(String hostName, String seqNum) {
    return hostName + DELIMITER + seqNum;
  }

  /**
   * Copy {SCOPE/TAG/PROFILE/KEYWORDS/DESCRIPTION } into new delegate
   */
  private void initNewDelegateWithExistingDelegate(Delegate delegate, Delegate existingInactiveDelegate) {
    delegate.setExcludeScopes(existingInactiveDelegate.getExcludeScopes());
    delegate.setIncludeScopes(existingInactiveDelegate.getIncludeScopes());
    delegate.setDelegateProfileId(existingInactiveDelegate.getDelegateProfileId());
    delegate.setTags(existingInactiveDelegate.getTags());
    delegate.setKeywords(existingInactiveDelegate.getKeywords());
    delegate.setDescription(existingInactiveDelegate.getDescription());
  }

  private Delegate updateAllDelegatesIfECSType(
      Delegate delegate, UpdateOperations<Delegate> updateOperations, String fieldBeingUpdate) {
    List<Delegate> retVal = new ArrayList<>();
    List<Delegate> delegates = getAllDelegatesMatchingGroupName(delegate);

    if (isEmpty(delegates)) {
      return null;
    }

    alertService.delegateAvailabilityUpdated(delegate.getAccountId());

    for (Delegate delegateToBeUpdated : delegates) {
      try (AutoLogContext ignore = new DelegateLogContext(delegateToBeUpdated.getUuid(), OVERRIDE_NESTS)) {
        if ("SCOPES".equals(fieldBeingUpdate)) {
          log.info("Updating delegate scopes: includeScopes:{} excludeScopes:{}", delegate.getIncludeScopes(),
              delegate.getExcludeScopes());
        } else if ("TAGS".equals(fieldBeingUpdate)) {
          log.info("Updating delegate tags : tags:{}", delegate.getTags());
        } else {
          log.info("Updating ECS delegate");
        }

        Delegate updatedDelegate = updateDelegate(delegateToBeUpdated, updateOperations);
        if (updatedDelegate.getUuid().equals(delegate.getUuid())) {
          retVal.add(updatedDelegate);
        }
        if (currentTimeMillis() - updatedDelegate.getLastHeartBeat() < Duration.ofMinutes(2).toMillis()) {
          alertService.delegateEligibilityUpdated(updatedDelegate.getAccountId(), updatedDelegate.getUuid());
        }
      }
    }

    if (isNotEmpty(retVal)) {
      return retVal.get(0);
    } else {
      return null;
    }
  }

  /**
   * All delegates matching {AccId, HostName Prefix, Type = ECS}
   */
  private List<Delegate> getAllDelegatesMatchingGroupName(Delegate delegate) {
    return persistence.createQuery(Delegate.class, excludeAuthority)
        .filter(DelegateKeys.accountId, delegate.getAccountId())
        .filter(DelegateKeys.delegateType, delegate.getDelegateType())
        .filter(DelegateKeys.delegateGroupName, delegate.getDelegateGroupName())
        .asList();
  }

  private boolean isValidSeqNum(String sequenceNum) {
    try {
      Integer.parseInt(sequenceNum);
    } catch (Exception e) {
      return false;
    }

    return true;
  }

  private boolean isDelegateWithoutPollingEnabled(Delegate delegate) {
    return !delegate.isPolllingModeEnabled();
  }

  private void updateWithTokenAndSeqNumIfEcsDelegate(Delegate delegate, Delegate savedDelegate) {
    if (ECS.equals(delegate.getDelegateType())) {
      savedDelegate.setDelegateRandomToken(delegate.getDelegateRandomToken());
      savedDelegate.setSequenceNum(delegate.getSequenceNum());
    }
  }

  @VisibleForTesting
  void updateExistingDelegateWithSequenceConfigData(Delegate delegate) {
    String hostName = getDelegateHostNameByRemovingSeqNum(delegate);
    String seqNum = getDelegateSeqNumFromHostName(delegate);
    DelegateSequenceConfig config =
        getDelegateSequenceConfig(delegate.getAccountId(), hostName, Integer.parseInt(seqNum));
    delegate.setDelegateRandomToken(config.getDelegateToken());
    delegate.setSequenceNum(String.valueOf(config.getSequenceNum()));
  }

  @VisibleForTesting
  String getDelegateHostNameByRemovingSeqNum(Delegate delegate) {
    return delegate.getHostName().substring(0, delegate.getHostName().lastIndexOf('_'));
  }

  @VisibleForTesting
  String getDelegateSeqNumFromHostName(Delegate delegate) {
    return delegate.getHostName().substring(delegate.getHostName().lastIndexOf('_') + 1);
  }

  private void destroyTheCurrentDelegate(
      String accountId, String delegateId, String delegateConnectionId, ConnectionMode connectionMode) {
    switch (connectionMode) {
      case POLLING:
        log.warn("Sent self destruct command to delegate {}, with connectionId {}.", delegateId, delegateConnectionId);
        throw new DuplicateDelegateException(delegateId, delegateConnectionId);
      case STREAMING:
        broadcasterFactory.lookup(STREAM_DELEGATE + accountId, true)
            .broadcast(SELF_DESTRUCT + delegateId + "-" + delegateConnectionId);
        log.warn("Sent self destruct command to delegate {}, with connectionId {}.", delegateId, delegateConnectionId);
        break;
      default:
        throw new UnexpectedException("Non supported connection mode provided");
    }
  }
  //------ END: ECS Delegate Specific Methods

  //------ START: DelegateFeature Specific methods
  @Override
  public void deleteAllDelegatesExceptOne(String accountId, long shutdownInterval) {
    int retryCount = 0;
    while (true) {
      try {
        Optional<String> delegateToRetain = selectDelegateToRetain(accountId);

        if (delegateToRetain.isPresent()) {
          log.info("Deleting all delegates for account : {} except {}", accountId, delegateToRetain.get());

          retainOnlySelectedDelegatesAndDeleteRestByUuid(
              accountId, Collections.singletonList(delegateToRetain.get()), shutdownInterval);

          log.info("Deleted all delegates for account : {} except {}", accountId, delegateToRetain.get());
        } else {
          log.info("No delegate found to retain for account : {}", accountId);
        }

        break;
      } catch (Exception ex) {
        if (retryCount >= MAX_RETRIES) {
          log.error("Couldn't delete delegates for account: {}. Current Delegate Count : {}", accountId,
              getDelegates(accountId).size(), ex);
          break;
        }
        retryCount++;
      }
    }

    int numDelegates = getDelegates(accountId).size();
    if (numDelegates > delegatesFeature.getMaxUsageAllowedForAccount(accountId)) {
      sendEmailAboutDelegatesOverUsage(accountId, numDelegates);
    }
  }

  @Override
  public List<DelegateSizeDetails> fetchAvailableSizes() {
    try (InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("delegatesizes/sizes.json")) {
      String fileContent = IOUtils.toString(inputStream, UTF_8);
      AvailableDelegateSizes availableDelegateSizes = JsonUtils.asObject(fileContent, AvailableDelegateSizes.class);

      return availableDelegateSizes.getAvailableSizes();
    } catch (Exception e) {
      log.error("Unexpected exception occurred while trying read available delegate sizes from resource file.");
    }

    return null;
  }

  @Override
  public List<String> getConnectedDelegates(String accountId, List<String> delegateIds) {
    return delegateIds.stream()
        .filter(delegateId
            -> delegateConnectionDao.checkDelegateConnected(
                accountId, delegateId, versionInfoManager.getVersionInfo().getVersion()))
        .collect(toList());
  }

  private Optional<String> selectDelegateToRetain(String accountId) {
    return getDelegates(accountId)
        .stream()
        .max(Comparator.comparingLong(Delegate::getLastHeartBeat))
        .map(UuidAware::getUuid);
  }

  private List<Delegate> getDelegates(String accountId) {
    return list(PageRequestBuilder.aPageRequest().addFilter(DelegateKeys.accountId, Operator.EQ, accountId).build())
        .getResponse();
  }

  private void retainOnlySelectedDelegatesAndDeleteRestByUuid(
      String accountId, List<String> delegatesToRetain, long shutdownInterval) throws InterruptedException {
    Query<Delegate> query = persistence.createQuery(Delegate.class)
                                .filter(DelegateKeys.accountId, accountId)
                                .field(DelegateKeys.uuid)
                                .notIn(delegatesToRetain);

    UpdateOperations<Delegate> updateOps =
        persistence.createUpdateOperations(Delegate.class).set(DelegateKeys.status, DelegateInstanceStatus.DELETED);
    persistence.update(query, updateOps);

    // Waiting for shutdownInterval to ensure shutdown msg reach delegates before removing their entries from DB
    Thread.sleep(shutdownInterval);

    retainOnlySelectedDelegatesAndDeleteRest(accountId, delegatesToRetain);
  }

  private void sendEmailAboutDelegatesOverUsage(String accountId, int numDelegates) {
    Account account = accountService.get(accountId);
    String body = format(
        "Account is using more than [%d] delegates. Account Id : [%s], Company Name : [%s], Account Name : [%s], Delegate Count : [%d]",
        delegatesFeature.getMaxUsageAllowedForAccount(accountId), accountId, account.getCompanyName(),
        account.getAccountName(), numDelegates);
    String subjectMail =
        format("Found account with more than %d delegates", delegatesFeature.getMaxUsageAllowedForAccount(accountId));

    emailNotificationService.send(EmailData.builder()
                                      .hasHtml(false)
                                      .body(body)
                                      .subject(subjectMail)
                                      .to(Lists.newArrayList("support@harness.io"))
                                      .build());
  }
  //------ END: DelegateFeature Specific methods

  @VisibleForTesting
  protected DelegateInitializationDetails getDelegateInitializationDetails(String accountId, String delegateId) {
    Delegate delegate = delegateCache.get(accountId, delegateId, true);

    if (delegate.isProfileError()) {
      log.debug("Delegate {} could not be initialized correctly.", delegateId);
      return buildInitializationDetails(false, delegate);
    } else if (delegate.getProfileExecutedAt() > 0) {
      log.debug("Delegate {} was initialized correctly.", delegateId);
      return buildInitializationDetails(true, delegate);
    } else {
      DelegateProfile delegateProfile = delegateProfileService.get(accountId, delegate.getDelegateProfileId());

      if (isBlank(delegateProfile.getStartupScript())) {
        log.debug("Delegate {} was initialized correctly.", delegateId);
        return buildInitializationDetails(true, delegate);
      } else {
        log.debug("Delegate {} finalizing initialization correctly.", delegateId);
        return buildInitializationDetails(false, delegate);
      }
    }
  }

  private DelegateInitializationDetails buildInitializationDetails(boolean initialized, Delegate delegate) {
    return DelegateInitializationDetails.builder()
        .delegateId(delegate.getUuid())
        .hostname(delegate.getHostName())
        .initialized(initialized)
        .profileError(delegate.isProfileError())
        .profileExecutedAt(delegate.getProfileExecutedAt())
        .build();
  }

  @Override
  public String queueTask(DelegateTask task) {
    return delegateTaskServiceClassic.queueTask(task);
  }

  @Override
  public void scheduleSyncTask(DelegateTask task) {
    delegateTaskServiceClassic.scheduleSyncTask(task);
  }

  @Override
  public <T extends DelegateResponseData> T executeTask(DelegateTask task) throws InterruptedException {
    return delegateTaskServiceClassic.executeTask(task);
  }
}
