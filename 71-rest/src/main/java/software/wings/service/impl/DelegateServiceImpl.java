package software.wings.service.impl;

import static com.google.common.base.Charsets.UTF_8;
import static freemarker.template.Configuration.VERSION_2_3_23;
import static io.harness.beans.DelegateTask.Status.ABORTED;
import static io.harness.beans.DelegateTask.Status.ERROR;
import static io.harness.beans.DelegateTask.Status.QUEUED;
import static io.harness.beans.DelegateTask.Status.STARTED;
import static io.harness.beans.DelegateTask.Status.runningStatuses;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.SizeFunction.size;
import static io.harness.data.structure.UUIDGenerator.convertFromBase64;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.beans.DelegateTaskAbortEvent.Builder.aDelegateTaskAbortEvent;
import static io.harness.delegate.beans.DelegateTaskEvent.DelegateTaskEventBuilder.aDelegateTaskEvent;
import static io.harness.delegate.beans.DelegateType.CE_KUBERNETES;
import static io.harness.delegate.beans.DelegateType.DOCKER;
import static io.harness.delegate.beans.DelegateType.ECS;
import static io.harness.delegate.beans.DelegateType.HELM_DELEGATE;
import static io.harness.delegate.beans.DelegateType.KUBERNETES;
import static io.harness.delegate.beans.DelegateType.SHELL_SCRIPT;
import static io.harness.delegate.beans.executioncapability.ExecutionCapability.EvaluationMode;
import static io.harness.delegate.message.ManagerMessageConstants.JRE_VERSION;
import static io.harness.delegate.message.ManagerMessageConstants.MIGRATE;
import static io.harness.delegate.message.ManagerMessageConstants.SELF_DESTRUCT;
import static io.harness.delegate.message.ManagerMessageConstants.USE_CDN;
import static io.harness.delegate.message.ManagerMessageConstants.USE_STORAGE_PROXY;
import static io.harness.eraro.ErrorCode.USAGE_LIMITS_EXCEEDED;
import static io.harness.exception.WingsException.USER;
import static io.harness.govern.Switch.noop;
import static io.harness.k8s.KubernetesConvention.getAccountIdentifier;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_NESTS;
import static io.harness.mongo.MongoUtils.setUnset;
import static io.harness.obfuscate.Obfuscator.obfuscate;
import static io.harness.persistence.HQuery.excludeAuthority;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Comparator.comparingInt;
import static java.util.Comparator.naturalOrder;
import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substringAfter;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.DelegateSequenceConfig.Builder.aDelegateSequenceBuilder;
import static software.wings.beans.Event.Builder.anEvent;
import static software.wings.beans.FeatureName.USE_CDN_FOR_STORAGE_FILES;
import static software.wings.beans.alert.AlertType.NoEligibleDelegates;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Suppliers;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.UncheckedExecutionException;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.github.zafarkhaja.semver.Version;
import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoGridFSException;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import io.harness.beans.DelegateTask;
import io.harness.beans.DelegateTask.DelegateTaskKeys;
import io.harness.beans.FileMetadata;
import io.harness.beans.PageRequest;
import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.configuration.DeployMode;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.ConnectionMode;
import io.harness.delegate.beans.DelegateApproval;
import io.harness.delegate.beans.DelegateConfiguration;
import io.harness.delegate.beans.DelegateConnectionHeartbeat;
import io.harness.delegate.beans.DelegateParams;
import io.harness.delegate.beans.DelegateProfile;
import io.harness.delegate.beans.DelegateProfileParams;
import io.harness.delegate.beans.DelegateRegisterResponse;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateScripts;
import io.harness.delegate.beans.DelegateSyncTaskResponse;
import io.harness.delegate.beans.DelegateTaskAbortEvent;
import io.harness.delegate.beans.DelegateTaskEvent;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskPackage.DelegateTaskPackageBuilder;
import io.harness.delegate.beans.DelegateTaskRank;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.DelegateTaskResponse.ResponseCode;
import io.harness.delegate.beans.DelegateType;
import io.harness.delegate.beans.DuplicateDelegateException;
import io.harness.delegate.beans.ErrorNotifyResponseData;
import io.harness.delegate.beans.NoAvailableDelegatesException;
import io.harness.delegate.beans.NoInstalledDelegatesException;
import io.harness.delegate.beans.RemoteMethodReturnValueData;
import io.harness.delegate.beans.TaskGroup;
import io.harness.delegate.beans.TaskSelectorMap;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.SelectorCapability;
import io.harness.delegate.service.DelegateAgentFileService.FileBucket;
import io.harness.delegate.task.DelegateLogContext;
import io.harness.delegate.task.TaskLogContext;
import io.harness.delegate.task.TaskParameters;
import io.harness.environment.SystemEnvironment;
import io.harness.event.handler.impl.EventPublishHelper;
import io.harness.exception.CriticalExpressionEvaluationException;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.FailureType;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.LimitsExceededException;
import io.harness.exception.UnexpectedException;
import io.harness.expression.ExpressionEvaluator;
import io.harness.expression.ExpressionReflectionUtils;
import io.harness.k8s.model.response.CEK8sDelegatePrerequisite;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.logging.AccountLogContext;
import io.harness.logging.AutoLogContext;
import io.harness.logging.DelegateDriverLogContext;
import io.harness.logging.Misc;
import io.harness.logstreaming.LogStreamingServiceRestClient;
import io.harness.mongo.DelayLogContext;
import io.harness.network.Http;
import io.harness.network.SafeHttpCall;
import io.harness.observer.Subject;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import io.harness.persistence.UuidAware;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.selection.log.BatchDelegateSelectionLog;
import io.harness.serializer.KryoSerializer;
import io.harness.service.intfc.DelegateCallbackRegistry;
import io.harness.service.intfc.DelegateCallbackService;
import io.harness.service.intfc.DelegateSyncService;
import io.harness.service.intfc.DelegateTaskResultsProvider;
import io.harness.service.intfc.DelegateTaskSelectorMapService;
import io.harness.service.intfc.DelegateTaskService;
import io.harness.stream.BoundedInputStream;
import io.harness.version.VersionInfoManager;
import io.harness.waiter.WaitNotifyEngine;
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
import org.jetbrains.annotations.NotNull;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.app.DelegateGrpcConfig;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.CEDelegateStatus;
import software.wings.beans.CEDelegateStatus.CEDelegateStatusBuilder;
import software.wings.beans.Delegate;
import software.wings.beans.Delegate.DelegateKeys;
import software.wings.beans.Delegate.Status;
import software.wings.beans.DelegateConnection;
import software.wings.beans.DelegateScalingGroup;
import software.wings.beans.DelegateSequenceConfig;
import software.wings.beans.DelegateSequenceConfig.DelegateSequenceConfigKeys;
import software.wings.beans.DelegateStatus;
import software.wings.beans.Event.Type;
import software.wings.beans.ExecutionCredential;
import software.wings.beans.FeatureName;
import software.wings.beans.GitConfig;
import software.wings.beans.GitValidationParameters;
import software.wings.beans.HostValidationTaskParameters;
import software.wings.beans.SelectorType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.TaskType;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.DelegateProfileErrorAlert;
import software.wings.beans.alert.DelegatesDownAlert;
import software.wings.beans.alert.NoActiveDelegatesAlert;
import software.wings.beans.alert.NoEligibleDelegatesAlert;
import software.wings.beans.alert.NoInstalledDelegatesAlert;
import software.wings.cdn.CdnConfig;
import software.wings.core.managerConfiguration.ConfigurationController;
import software.wings.delegatetasks.delegatecapability.CapabilityHelper;
import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.dl.WingsPersistence;
import software.wings.expression.ManagerPreExecutionExpressionEvaluator;
import software.wings.expression.SecretFunctor;
import software.wings.expression.SecretManagerFunctor;
import software.wings.features.DelegatesFeature;
import software.wings.features.api.UsageLimitedFeature;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandTaskParameters;
import software.wings.helpers.ext.pcf.request.PcfCommandTaskParameters.PcfCommandTaskParametersBuilder;
import software.wings.helpers.ext.pcf.request.PcfRunPluginCommandRequest;
import software.wings.helpers.ext.url.SubdomainUrlHelperIntfc;
import software.wings.jre.JreConfig;
import software.wings.licensing.LicenseService;
import software.wings.service.impl.EventEmitter.Channel;
import software.wings.service.impl.artifact.ArtifactCollectionUtils;
import software.wings.service.impl.infra.InfraDownloadService;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.AssignDelegateService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.DelegateProfileService;
import software.wings.service.intfc.DelegateSelectionLogsService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManager;

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
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
@Slf4j
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
  private static final int MAX_DELEGATE_META_INFO_ENTRIES = 10000;
  private static final String HARNESS_ECS_DELEGATE = "Harness-ECS-Delegate";
  private static final String DELIMITER = "_";
  private static final int MAX_RETRIES = 2;

  public static final String HARNESS_DELEGATE_VALUES_YAML = HARNESS_DELEGATE + "-values";
  private static final String YAML = ".yaml";
  private static final String UPGRADE_VERSION = "upgradeVersion";
  private static final String ASYNC = "async";
  private static final String SYNC = "sync";
  private static final String STREAM_DELEGATE = "/stream/delegate/";
  private static final String TAR_GZ = ".tar.gz";
  private static final String README = "README";
  private static final String README_TXT = "/README.txt";
  private static final String EMPTY_VERSION = "0.0.0";
  private static final String JRE_DIRECTORY = "jreDirectory";
  private static final String JRE_MAC_DIRECTORY = "jreMacDirectory";
  private static final String JRE_TAR_PATH = "jreTarPath";
  public static final String JRE_VERSION_KEY = "jreVersion";
  private static final String ENV_ENV_VAR = "ENV";
  public static final String TASK_SELECTORS = "TASK_SELECTORS";
  public static final String TASK_CATEGORY = "TASK_CATEGORY";

  static {
    templateConfiguration.setTemplateLoader(new ClassTemplateLoader(DelegateServiceImpl.class, "/delegatetemplates"));
  }

  private static final long VALIDATION_TIMEOUT = TimeUnit.SECONDS.toMillis(12);

  @Inject private WingsPersistence wingsPersistence;
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
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private ArtifactCollectionUtils artifactCollectionUtils;
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
  @Inject private LogStreamingServiceRestClient logStreamingServiceRestClient;

  @Inject @Named(DelegatesFeature.FEATURE_NAME) private UsageLimitedFeature delegatesFeature;
  @Inject @Getter private Subject<DelegateObserver> subject = new Subject<>();

  private LoadingCache<String, String> delegateVersionCache = CacheBuilder.newBuilder()
                                                                  .maximumSize(10000)
                                                                  .expireAfterWrite(1, TimeUnit.MINUTES)
                                                                  .build(new CacheLoader<String, String>() {
                                                                    @Override
                                                                    public String load(String accountId) {
                                                                      return fetchDelegateMetadataFromStorage();
                                                                    }
                                                                  });

  private LoadingCache<String, Optional<Delegate>> delegateCache =
      CacheBuilder.newBuilder()
          .maximumSize(MAX_DELEGATE_META_INFO_ENTRIES)
          .expireAfterWrite(1, TimeUnit.MINUTES)
          .build(new CacheLoader<String, Optional<Delegate>>() {
            @Override
            public Optional<Delegate> load(String delegateId) {
              return Optional.ofNullable(
                  wingsPersistence.createQuery(Delegate.class).filter(DelegateKeys.uuid, delegateId).get());
            }
          });

  private Supplier<Long> taskCountCache = Suppliers.memoizeWithExpiration(this ::fetchTaskCount, 1, TimeUnit.MINUTES);

  private LoadingCache<String, String> logStreamingAccountTokenCache =
      CacheBuilder.newBuilder()
          .maximumSize(1000)
          .expireAfterWrite(5, TimeUnit.MINUTES)
          .build(new CacheLoader<String, String>() {
            @Override
            public String load(String accountId) {
              return retrieveLogStreamingAccountToken(accountId);
            }
          });

  @Override
  public List<Integer> getCountOfDelegatesForAccounts(List<String> accountIds) {
    List<Delegate> delegates =
        wingsPersistence.createQuery(Delegate.class).field(DelegateKeys.accountId).in(accountIds).asList();
    Map<String, Integer> countOfDelegatesPerAccount =
        accountIds.stream().collect(Collectors.toMap(accountId -> accountId, accountId -> 0));
    delegates.forEach(delegate -> {
      int currentCount = countOfDelegatesPerAccount.get(delegate.getAccountId());
      countOfDelegatesPerAccount.put(delegate.getAccountId(), currentCount + 1);
    });
    return accountIds.stream().map(countOfDelegatesPerAccount::get).collect(Collectors.toList());
  }

  @Override
  public PageResponse<Delegate> list(PageRequest<Delegate> pageRequest) {
    return wingsPersistence.query(Delegate.class, pageRequest);
  }

  @Override
  public boolean checkDelegateConnected(String accountId, String delegateId) {
    return delegateConnectionDao.checkDelegateConnected(
        accountId, delegateId, versionInfoManager.getVersionInfo().getVersion());
  }

  @Override
  public List<String> getKubernetesDelegateNames(String accountId) {
    return wingsPersistence.createQuery(Delegate.class)
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
    Delegate delegate = wingsPersistence.createQuery(Delegate.class)
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
    List<DelegateStatus.DelegateInner.DelegateConnectionInner> activelyConnectedDelegates =
        delegateConnectionDao.list(accountId, delegate.getUuid())
            .stream()
            .map(delegateConnection
                -> DelegateStatus.DelegateInner.DelegateConnectionInner.builder()
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
    Query<Delegate> delegateQuery = wingsPersistence.createQuery(Delegate.class)
                                        .filter(DelegateKeys.accountId, accountId)
                                        .project(DelegateKeys.accountId, true)
                                        .project(DelegateKeys.tags, true)
                                        .project(DelegateKeys.delegateName, true)
                                        .project(DelegateKeys.hostName, true)
                                        .project(DelegateKeys.delegateProfileId, true);

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
  public Set<String> retrieveDelegateSelectors(Delegate delegate) {
    Set<String> selectors = delegate.getTags() == null ? new HashSet<>() : new HashSet<>(delegate.getTags());

    selectors.addAll(retrieveDelegateImplicitSelectors(delegate).keySet());

    return selectors;
  }

  private Map<String, SelectorType> retrieveDelegateImplicitSelectors(Delegate delegate) {
    SortedMap<String, SelectorType> selectorTypeMap = new TreeMap<>();

    DelegateProfile delegateProfile =
        delegateProfileService.get(delegate.getAccountId(), delegate.getDelegateProfileId());

    if (isNotBlank(delegate.getHostName())) {
      selectorTypeMap.put(delegate.getHostName().toLowerCase(), SelectorType.HOST_NAME);
    }

    if (isNotBlank(delegate.getDelegateName())) {
      selectorTypeMap.put(delegate.getDelegateName().toLowerCase(), SelectorType.DELEGATE_NAME);
    }

    if (delegateProfile != null && isNotBlank(delegateProfile.getName())) {
      selectorTypeMap.put(delegateProfile.getName().toLowerCase(), SelectorType.PROFILE_NAME);
    }

    if (delegateProfile != null && isNotEmpty(delegateProfile.getSelectors())) {
      for (String selector : delegateProfile.getSelectors()) {
        selectorTypeMap.put(selector.toLowerCase(), SelectorType.PROFILE_SELECTORS);
      }
    }

    return selectorTypeMap;
  }

  @Override
  public List<String> getAvailableVersions(String accountId) {
    DelegateStatus status = getDelegateStatus(accountId);
    return status.getPublishedVersions();
  }

  @Override
  public DelegateStatus getDelegateStatus(String accountId) {
    DelegateConfiguration delegateConfiguration = accountService.getDelegateConfiguration(accountId);

    List<Delegate> delegates = wingsPersistence.createQuery(Delegate.class)
                                   .filter(DelegateKeys.accountId, accountId)
                                   .field(DelegateKeys.status)
                                   .notEqual(Status.DELETED)
                                   .asList();

    Map<String, List<DelegateStatus.DelegateInner.DelegateConnectionInner>> perDelegateConnections =
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

    Map<String, List<DelegateStatus.DelegateInner.DelegateConnectionInner>> activeDelegateConnections =
        delegateConnectionDao.obtainActiveDelegateConnections(accountId);

    List<DelegateScalingGroup> scalingGroups = getDelegateScalingGroups(accountId, activeDelegateConnections);

    return DelegateStatus.builder()
        .publishedVersions(delegateConfiguration.getDelegateVersions())
        .scalingGroups(scalingGroups)
        .delegates(buildInnerDelegates(delegatesWithoutScalingGroup, activeDelegateConnections, false))
        .build();
  }

  @NotNull
  private List<DelegateScalingGroup> getDelegateScalingGroups(String accountId,
      Map<String, List<DelegateStatus.DelegateInner.DelegateConnectionInner>> activeDelegateConnections) {
    List<Delegate> activeDelegates = wingsPersistence.createQuery(Delegate.class)
                                         .filter(DelegateKeys.accountId, accountId)
                                         .field(DelegateKeys.delegateGroupName)
                                         .exists()
                                         .field(DelegateKeys.status)
                                         .hasAnyOf(Arrays.asList(Status.ENABLED, Status.WAITING_FOR_APPROVAL))
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
    return wingsPersistence.createQuery(Delegate.class)
        .filter(DelegateKeys.accountId, accountId)
        .field(DelegateKeys.status)
        .notEqual(Status.DELETED)
        .field(DelegateKeys.delegateGroupName)
        .doesNotExist()
        .asList();
  }

  @NotNull
  private List<DelegateStatus.DelegateInner> buildInnerDelegates(List<Delegate> delegates,
      Map<String, List<DelegateStatus.DelegateInner.DelegateConnectionInner>> perDelegateConnections,
      boolean filterInactiveDelegates) {
    return delegates.stream()
        .filter(delegate -> !filterInactiveDelegates || perDelegateConnections.containsKey(delegate.getUuid()))
        .map(delegate -> {
          List<DelegateStatus.DelegateInner.DelegateConnectionInner> connections =
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
              .implicitSelectors(retrieveDelegateImplicitSelectors(delegate))
              .sampleDelegate(delegate.isSampleDelegate())
              .connections(connections)
              .build();
        })
        .collect(Collectors.toList());
  }

  @Override
  public Delegate get(String accountId, String delegateId, boolean forceRefresh) {
    try {
      if (forceRefresh) {
        delegateCache.refresh(delegateId);
      }
      Delegate delegate = delegateCache.get(delegateId).orElse(null);
      if (delegate != null && (delegate.getAccountId() == null || !delegate.getAccountId().equals(accountId))) {
        // TODO: this is serious, we should not return the delegate if the account is not the expected one
        //       just to be on the safe side, make sure that all such scenarios are first fixed
        log.error("Delegate account id mismatch", new Exception(""));
      }
      return delegate;
    } catch (ExecutionException e) {
      log.error("Execution exception", e);
    } catch (UncheckedExecutionException e) {
      log.error("Delegate not found exception", e);
    }
    return null;
  }

  @Override
  public Delegate update(Delegate delegate) {
    UpdateOperations<Delegate> updateOperations = getDelegateUpdateOperations(delegate);

    if (ECS.equals(delegate.getDelegateType())) {
      return updateEcsDelegate(delegate, true);
    } else {
      log.info("Updating delegate : {}", delegate.getUuid());
      return updateDelegate(delegate, updateOperations);
    }
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
    UpdateOperations<Delegate> updateOperations = wingsPersistence.createUpdateOperations(Delegate.class);
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
    wingsPersistence.update(wingsPersistence.createQuery(Delegate.class)
                                .filter(DelegateKeys.accountId, accountId)
                                .filter(DelegateKeys.uuid, delegateId),
        wingsPersistence.createUpdateOperations(Delegate.class).set(DelegateKeys.description, newDescription));

    return get(accountId, delegateId, true);
  }

  @Override
  public Delegate updateApprovalStatus(String accountId, String delegateId, DelegateApproval action) {
    Delegate.Status newDelegateStatus = mapApprovalActionToDelegateStatus(action);

    Delegate currentDelegate = wingsPersistence.createQuery(Delegate.class)
                                   .filter(DelegateKeys.accountId, accountId)
                                   .filter(DelegateKeys.uuid, delegateId)
                                   .get();

    Query<Delegate> updateQuery = wingsPersistence.createQuery(Delegate.class)
                                      .filter(DelegateKeys.accountId, accountId)
                                      .filter(DelegateKeys.uuid, delegateId)
                                      .filter(DelegateKeys.status, Status.WAITING_FOR_APPROVAL);

    UpdateOperations<Delegate> updateOperations =
        wingsPersistence.createUpdateOperations(Delegate.class).set(DelegateKeys.status, newDelegateStatus);

    log.debug("Updating approval status from {} to {}", currentDelegate.getStatus(), newDelegateStatus);
    Delegate updatedDelegate =
        wingsPersistence.findAndModify(updateQuery, updateOperations, HPersistence.returnNewOptions);

    auditServiceHelper.reportForAuditingUsingAccountId(
        accountId, currentDelegate, updatedDelegate, Type.DELEGATE_APPROVAL);

    if (Status.DELETED == newDelegateStatus) {
      broadcasterFactory.lookup(STREAM_DELEGATE + accountId, true).broadcast(SELF_DESTRUCT + delegateId);
    }

    return updatedDelegate;
  }

  private Status mapApprovalActionToDelegateStatus(DelegateApproval action) {
    if (DelegateApproval.ACTIVATE == action) {
      return Status.ENABLED;
    } else {
      return Status.DELETED;
    }
  }

  @Override
  public Delegate updateHeartbeatForDelegateWithPollingEnabled(Delegate delegate) {
    wingsPersistence.update(wingsPersistence.createQuery(Delegate.class)
                                .filter(DelegateKeys.accountId, delegate.getAccountId())
                                .filter(DelegateKeys.uuid, delegate.getUuid()),
        wingsPersistence.createUpdateOperations(Delegate.class)
            .set(DelegateKeys.lastHeartBeat, currentTimeMillis())
            .set(DelegateKeys.validUntil, Date.from(OffsetDateTime.now().plusDays(Delegate.TTL.toDays()).toInstant())));
    delegateTaskService.touchExecutingTasks(
        delegate.getAccountId(), delegate.getUuid(), delegate.getCurrentlyExecutingDelegateTasks());

    Delegate existingDelegate = get(delegate.getAccountId(), delegate.getUuid(), false);

    if (existingDelegate == null) {
      register(delegate);
      existingDelegate = get(delegate.getAccountId(), delegate.getUuid(), true);
    }

    if (licenseService.isAccountDeleted(existingDelegate.getAccountId())) {
      existingDelegate.setStatus(Status.DELETED);
    }

    existingDelegate.setUseCdn(featureFlagService.isEnabled(USE_CDN_FOR_STORAGE_FILES, delegate.getAccountId()));
    existingDelegate.setUseJreVersion(getTargetJreVersion(delegate.getAccountId()));
    return existingDelegate;
  }

  @Override
  public Delegate updateTags(Delegate delegate) {
    UpdateOperations<Delegate> updateOperations = wingsPersistence.createUpdateOperations(Delegate.class);
    setUnset(updateOperations, DelegateKeys.tags, delegate.getTags());
    log.info("Updating delegate tags : Delegate:{} tags:{}", delegate.getUuid(), delegate.getTags());

    auditServiceHelper.reportForAuditingUsingAccountId(delegate.getAccountId(), null, delegate, Type.UPDATE_TAG);
    log.info("Auditing updation of Tags for delegate={} in account={}", delegate.getUuid(), delegate.getAccountId());

    if (ECS.equals(delegate.getDelegateType())) {
      return updateAllDelegatesIfECSType(delegate, updateOperations, "TAGS");
    } else {
      Delegate updatedDelegate = updateDelegate(delegate, updateOperations);
      if (currentTimeMillis() - updatedDelegate.getLastHeartBeat() < Duration.ofMinutes(2).toMillis()) {
        alertService.delegateEligibilityUpdated(updatedDelegate.getAccountId(), updatedDelegate.getUuid());
      }

      return updatedDelegate;
    }
  }

  @Override
  public Delegate updateScopes(Delegate delegate) {
    UpdateOperations<Delegate> updateOperations = wingsPersistence.createUpdateOperations(Delegate.class);
    setUnset(updateOperations, DelegateKeys.includeScopes, delegate.getIncludeScopes());
    setUnset(updateOperations, DelegateKeys.excludeScopes, delegate.getExcludeScopes());

    log.info("Updating delegate scopes : Delegate:{} includeScopes:{} excludeScopes:{}", delegate.getUuid(),
        delegate.getIncludeScopes(), delegate.getExcludeScopes());

    auditServiceHelper.reportForAuditingUsingAccountId(delegate.getAccountId(), null, delegate, Type.UPDATE_SCOPE);
    log.info(
        "Auditing updation of scope for delegateId={} in accountId={}", delegate.getUuid(), delegate.getAccountId());

    if (ECS.equals(delegate.getDelegateType())) {
      return updateAllDelegatesIfECSType(delegate, updateOperations, "SCOPES");
    } else {
      Delegate updatedDelegate = updateDelegate(delegate, updateOperations);
      if (currentTimeMillis() - updatedDelegate.getLastHeartBeat() < Duration.ofMinutes(2).toMillis()) {
        alertService.delegateEligibilityUpdated(updatedDelegate.getAccountId(), updatedDelegate.getUuid());
      }
      return updatedDelegate;
    }
  }

  private Delegate updateDelegate(Delegate delegate, UpdateOperations<Delegate> updateOperations) {
    Delegate previousDelegate = get(delegate.getAccountId(), delegate.getUuid(), false);

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

    wingsPersistence.update(wingsPersistence.createQuery(Delegate.class)
                                .filter(DelegateKeys.accountId, delegate.getAccountId())
                                .filter(DelegateKeys.uuid, delegate.getUuid()),
        updateOperations);
    delegateTaskService.touchExecutingTasks(
        delegate.getAccountId(), delegate.getUuid(), delegate.getCurrentlyExecutingDelegateTasks());

    eventEmitter.send(Channel.DELEGATES,
        anEvent().withOrgId(delegate.getAccountId()).withUuid(delegate.getUuid()).withType(Type.UPDATE).build());
    return get(delegate.getAccountId(), delegate.getUuid(), true);
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
  public DelegateScripts getDelegateScripts(
      String accountId, String version, String managerHost, String verificationHost) throws IOException {
    ImmutableMap<String, String> scriptParams = getJarAndScriptRunTimeParamMap(ScriptRuntimeParamMapInquiry.builder()
                                                                                   .accountId(accountId)
                                                                                   .version(version)
                                                                                   .managerHost(managerHost)
                                                                                   .verificationHost(verificationHost)
                                                                                   .build());

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
    private String accountId;
    private String version;
    private String managerHost;
    private String verificationHost;
    private String delegateName;
    private String delegateProfile;
    private String delegateType;
    private boolean ceEnabled;
    private boolean ciEnabled;
  }

  private ImmutableMap<String, String> getJarAndScriptRunTimeParamMap(ScriptRuntimeParamMapInquiry inquiry) {
    String latestVersion = null;
    String jarRelativePath;
    String delegateJarDownloadUrl = null;
    String delegateStorageUrl = null;
    String delegateCheckLocation = null;
    boolean jarFileExists = false;
    String delegateDockerImage = "harness/delegate:latest";
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

      ImmutableMap.Builder<String, String> params =
          ImmutableMap.<String, String>builder()
              .put("delegateDockerImage", delegateDockerImage)
              .put("accountId", inquiry.getAccountId())
              .put("accountSecret", account.getAccountKey())
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
              .put("delegateGrpcServicePort", String.valueOf(delegateGrpcConfig.getPort()))
              .put("kubernetesAccountLabel", getAccountIdentifier(inquiry.getAccountId()));
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

      params.put("grpcServiceEnabled", String.valueOf(isCiEnabled));
      if (isCiEnabled) {
        params.put("grpcServiceConnectorPort", String.valueOf(delegateGrpcConfig.getPort()));
        params.put("managerServiceSecret", String.valueOf(mainConfiguration.getPortal().getJwtNextGenManagerSecret()));
      } else {
        params.put("grpcServiceConnectorPort", String.valueOf(0));
        params.put("managerServiceSecret", "null");
      }

      params.put("useCdn", String.valueOf(useCDN));
      params.put("cdnUrl", cdnConfig.getUrl());

      JreConfig jreConfig = getJreConfig(inquiry.getAccountId());

      Preconditions.checkNotNull(jreConfig, "jreConfig cannot be null");

      params.put(JRE_VERSION_KEY, jreConfig.getVersion());
      params.put(JRE_DIRECTORY, jreConfig.getJreDirectory());
      params.put(JRE_MAC_DIRECTORY, jreConfig.getJreMacDirectory());
      params.put(JRE_TAR_PATH, jreConfig.getJreTarPath());
      params.put("enableCE", String.valueOf(inquiry.isCeEnabled()));

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
      jreConfig = JreConfig.builder()
                      .version(jreConfig.getVersion())
                      .jreDirectory(jreConfig.getJreDirectory())
                      .jreMacDirectory(jreConfig.getJreMacDirectory())
                      .jreTarPath(tarPath)
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
      String delegateProfile) throws IOException {
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
        delegateProfile = delegateProfileService.fetchPrimaryProfile(accountId).getUuid();
      }

      ImmutableMap<String, String> scriptParams = getJarAndScriptRunTimeParamMap(ScriptRuntimeParamMapInquiry.builder()
                                                                                     .accountId(accountId)
                                                                                     .version(version)
                                                                                     .managerHost(managerHost)
                                                                                     .verificationHost(verificationUrl)
                                                                                     .delegateName(delegateName)
                                                                                     .delegateProfile(delegateProfile)
                                                                                     .delegateType(SHELL_SCRIPT)
                                                                                     .build());

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
      String delegateProfile) throws IOException {
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
        delegateProfile = delegateProfileService.fetchPrimaryProfile(accountId).getUuid();
      }

      ImmutableMap<String, String> scriptParams = getJarAndScriptRunTimeParamMap(ScriptRuntimeParamMapInquiry.builder()
                                                                                     .accountId(accountId)
                                                                                     .version(version)
                                                                                     .managerHost(managerHost)
                                                                                     .verificationHost(verificationUrl)
                                                                                     .delegateName(delegateName)
                                                                                     .delegateProfile(delegateProfile)
                                                                                     .delegateType(DOCKER)
                                                                                     .build());

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
      String delegateProfile, Boolean isCiEnabled) throws IOException {
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

      ImmutableMap<String, String> scriptParams =
          getJarAndScriptRunTimeParamMap(ScriptRuntimeParamMapInquiry.builder()
                                             .accountId(accountId)
                                             .version(version)
                                             .managerHost(managerHost)
                                             .verificationHost(verificationUrl)
                                             .delegateName(delegateName)
                                             .delegateProfile(delegateProfile == null ? "" : delegateProfile)
                                             .delegateType(KUBERNETES)
                                             .ciEnabled(isCiEnabled)
                                             .build());

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
      String delegateName, String delegateProfile) throws IOException {
    String version;
    if (mainConfiguration.getDeployMode() == DeployMode.KUBERNETES) {
      List<String> delegateVersions = accountService.getDelegateConfiguration(accountId).getDelegateVersions();
      version = delegateVersions.get(delegateVersions.size() - 1);
    } else {
      version = EMPTY_VERSION;
    }

    ImmutableMap<String, String> scriptParams =
        getJarAndScriptRunTimeParamMap(ScriptRuntimeParamMapInquiry.builder()
                                           .accountId(accountId)
                                           .version(version)
                                           .managerHost(managerHost)
                                           .verificationHost(verificationUrl)
                                           .delegateName(delegateName)
                                           .delegateProfile(delegateProfile == null ? "" : delegateProfile)
                                           .delegateType(CE_KUBERNETES)
                                           .ceEnabled(true)
                                           .build());

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
      String delegateName, String delegateProfile) throws IOException {
    String version;

    if (mainConfiguration.getDeployMode() == DeployMode.KUBERNETES) {
      List<String> delegateVersions = accountService.getDelegateConfiguration(accountId).getDelegateVersions();
      version = delegateVersions.get(delegateVersions.size() - 1);
    } else {
      version = EMPTY_VERSION;
    }

    ImmutableMap<String, String> params =
        getJarAndScriptRunTimeParamMap(ScriptRuntimeParamMapInquiry.builder()
                                           .accountId(accountId)
                                           .version(version)
                                           .managerHost(managerHost)
                                           .verificationHost(verificationUrl)
                                           .delegateName(delegateName)
                                           .delegateProfile(delegateProfile == null ? "" : delegateProfile)
                                           .delegateType(HELM_DELEGATE)
                                           .build());

    File yaml = File.createTempFile(HARNESS_DELEGATE_VALUES_YAML, YAML);
    saveProcessedTemplate(params, yaml, "delegate-helm-values.yaml.ftl");

    return yaml;
  }

  @Override
  public File downloadECSDelegate(String managerHost, String verificationUrl, String accountId, boolean awsVpcMode,
      String hostname, String delegateGroupName, String delegateProfile) throws IOException {
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

      ImmutableMap<String, String> scriptParams =
          getJarAndScriptRunTimeParamMap(ScriptRuntimeParamMapInquiry.builder()
                                             .accountId(accountId)
                                             .version(version)
                                             .managerHost(managerHost)
                                             .verificationHost(verificationUrl)
                                             .delegateName(StringUtils.EMPTY)
                                             .delegateProfile(delegateProfile == null ? "" : delegateProfile)
                                             .delegateType(ECS)
                                             .build());
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
      delegateProfile = delegateProfileService.fetchPrimaryProfile(accountId);
      delegate.setDelegateProfileId(delegateProfile.getUuid());
    }

    if (delegateProfile.isApprovalRequired()) {
      delegate.setStatus(Status.WAITING_FOR_APPROVAL);
    } else {
      delegate.setStatus(Status.ENABLED);
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

    // When polling is enabled for delegate, do not perform these event publishing
    if (isDelegateWithoutPollingEnabled(delegate)) {
      eventEmitter.send(Channel.DELEGATES,
          anEvent().withOrgId(delegate.getAccountId()).withUuid(delegate.getUuid()).withType(Type.CREATE).build());
      assignDelegateService.clearConnectionResults(delegate.getAccountId());
    }

    updateWithTokenAndSeqNumIfEcsDelegate(delegate, savedDelegate);
    eventPublishHelper.publishInstalledDelegateEvent(delegate.getAccountId(), delegate.getUuid());
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
    return wingsPersistence.createQuery(Delegate.class).filter(DelegateKeys.accountId, accountId).count();
  }

  private Delegate saveDelegate(Delegate delegate) {
    log.info("Adding delegate {} for account {}", delegate.getHostName(), delegate.getAccountId());
    wingsPersistence.save(delegate);
    log.info("Delegate saved: {}", delegate);
    return delegate;
  }

  @Override
  public void delete(String accountId, String delegateId) {
    log.info("Deleting delegate: {}", delegateId);
    Delegate existingDelegate = wingsPersistence.createQuery(Delegate.class)
                                    .filter(DelegateKeys.accountId, accountId)
                                    .filter(DelegateKeys.uuid, delegateId)
                                    .project(DelegateKeys.ip, true)
                                    .project(DelegateKeys.hostName, true)
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
    }

    wingsPersistence.delete(wingsPersistence.createQuery(Delegate.class)
                                .filter(DelegateKeys.accountId, accountId)
                                .filter(DelegateKeys.uuid, delegateId));
  }

  @Override
  public void retainOnlySelectedDelegatesAndDeleteRest(String accountId, List<String> delegatesToRetain) {
    if (EmptyPredicate.isNotEmpty(delegatesToRetain)) {
      wingsPersistence.delete(wingsPersistence.createQuery(Delegate.class)
                                  .filter(DelegateKeys.accountId, accountId)
                                  .field(DelegateKeys.uuid)
                                  .notIn(delegatesToRetain));
    } else {
      log.info("List of delegates to retain is empty. In order to delete delegates, pass a list of delegate IDs");
    }
  }

  @Override
  public DelegateRegisterResponse register(Delegate delegate) {
    if (licenseService.isAccountDeleted(delegate.getAccountId())) {
      broadcasterFactory.lookup(STREAM_DELEGATE + delegate.getAccountId(), true).broadcast(SELF_DESTRUCT);
      return DelegateRegisterResponse.builder().action(DelegateRegisterResponse.Action.SELF_DESTRUCT).build();
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

    Query<Delegate> delegateQuery = wingsPersistence.createQuery(Delegate.class)
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
    if (existingDelegate != null && existingDelegate.getStatus() == Status.DELETED) {
      broadcasterFactory.lookup(STREAM_DELEGATE + delegate.getAccountId(), true)
          .broadcast(SELF_DESTRUCT + existingDelegate.getUuid());

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
      return DelegateRegisterResponse.builder().action(DelegateRegisterResponse.Action.SELF_DESTRUCT).build();
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

    Query<Delegate> delegateQuery = wingsPersistence.createQuery(Delegate.class)
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
    if (existingDelegate != null && existingDelegate.getStatus() == Status.DELETED) {
      broadcasterFactory.lookup(STREAM_DELEGATE + delegateParams.getAccountId(), true)
          .broadcast(SELF_DESTRUCT + existingDelegate.getUuid());

      return DelegateRegisterResponse.builder().action(DelegateRegisterResponse.Action.SELF_DESTRUCT).build();
    }

    log.info("Registering delegate for Hostname: {} IP: {}", delegateParams.getHostName(), delegateParams.getIp());

    Delegate delegate = Delegate.builder()
                            .uuid(delegateParams.getDelegateId())
                            .accountId(delegateParams.getAccountId())
                            .description(delegateParams.getDescription())
                            .ip(delegateParams.getIp())
                            .hostName(delegateParams.getHostName())
                            .delegateGroupName(delegateParams.getDelegateGroupName())
                            .delegateName(delegateParams.getDelegateName())
                            .delegateProfileId(delegateParams.getDelegateProfileId())
                            .lastHeartBeat(delegateParams.getLastHeartBeat())
                            .version(delegateParams.getVersion())
                            .sequenceNum(delegateParams.getSequenceNum())
                            .delegateType(delegateParams.getDelegateType())
                            .delegateRandomToken(delegateParams.getDelegateRandomToken())
                            .keepAlivePacket(delegateParams.isKeepAlivePacket())
                            .polllingModeEnabled(delegateParams.isPolllingModeEnabled())
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
    Query<DelegateSequenceConfig> delegateSequenceQuery = wingsPersistence.createQuery(DelegateSequenceConfig.class)
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
    Delegate delegate = get(accountId, delegateId, true);

    if (delegate == null || Status.ENABLED != delegate.getStatus()) {
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
    Delegate delegate = get(accountId, delegateId, true);
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

    wingsPersistence.update(wingsPersistence.createQuery(Delegate.class)
                                .filter(DelegateKeys.accountId, accountId)
                                .filter(DelegateKeys.uuid, delegateId),
        wingsPersistence.createUpdateOperations(Delegate.class)
            .set(DelegateKeys.profileResult, fileId)
            .set(DelegateKeys.profileError, error)
            .set(DelegateKeys.profileExecutedAt, clock.millis()));

    if (isNotBlank(previousProfileResult)) {
      fileService.deleteFile(previousProfileResult, FileBucket.PROFILE_RESULTS);
    }
  }

  @Override
  public String getProfileResult(String accountId, String delegateId) {
    Delegate delegate = get(accountId, delegateId, false);

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
  public String queueTask(DelegateTask task) {
    task.getData().setAsync(true);
    if (task.getUuid() == null) {
      task.setUuid(generateUuid());
    }

    try (AutoLogContext ignore1 = new TaskLogContext(task.getUuid(), task.getData().getTaskType(),
             TaskType.valueOf(task.getData().getTaskType()).getTaskGroup().name(), task.getRank(), OVERRIDE_NESTS);
         AutoLogContext ignore2 = new AccountLogContext(task.getAccountId(), OVERRIDE_ERROR)) {
      saveDelegateTask(task, QUEUED);
      log.info("Queueing async task");
      broadcastHelper.broadcastNewDelegateTaskAsync(task);
    }
    return task.getUuid();
  }

  @Override
  public void scheduleSyncTask(DelegateTask task) {
    task.getData().setAsync(false);
    if (task.getUuid() == null) {
      task.setUuid(generateUuid());
    }

    try (AutoLogContext ignore1 = new TaskLogContext(task.getUuid(), task.getData().getTaskType(),
             TaskType.valueOf(task.getData().getTaskType()).getTaskGroup().name(), task.getRank(), OVERRIDE_NESTS);
         AutoLogContext ignore2 = new AccountLogContext(task.getAccountId(), OVERRIDE_ERROR)) {
      saveDelegateTask(task, QUEUED);
      List<String> eligibleDelegateIds = ensureDelegateAvailableToExecuteTask(task);
      if (isEmpty(eligibleDelegateIds)) {
        log.warn(assignDelegateService.getActiveDelegateAssignmentErrorMessage(task));
        if (assignDelegateService.noInstalledDelegates(task.getAccountId())) {
          throw new NoInstalledDelegatesException();
        } else {
          throw new NoAvailableDelegatesException();
        }
      }

      log.info("Processing sync task");
      broadcastHelper.rebroadcastDelegateTask(task);
    }
  }

  @Override
  public <T extends DelegateResponseData> T executeTask(DelegateTask task) {
    scheduleSyncTask(task);
    return delegateSyncService.waitForTask(
        task.getUuid(), task.calcDescription(), Duration.ofMillis(task.getData().getTimeout()));
  }

  @VisibleForTesting
  protected void checkTaskRankRateLimit(DelegateTaskRank rank) {
    if (rank == null) {
      rank = DelegateTaskRank.CRITICAL;
    }

    if (rankLimitReached(rank)) {
      // noop - we will add RateLimitException later
    }
  }

  private boolean rankLimitReached(DelegateTaskRank rank) {
    Long totalTaskCount = taskCountCache.get();
    return totalTaskCount >= obtainRankLimit(rank);
  }

  private long obtainRankLimit(DelegateTaskRank rank) {
    switch (rank) {
      case OPTIONAL:
        return mainConfiguration.getPortal().getOptionalDelegateTaskRejectAtLimit();
      case IMPORTANT:
        return mainConfiguration.getPortal().getImportantDelegateTaskRejectAtLimit();
      case CRITICAL:
        return mainConfiguration.getPortal().getCriticalDelegateTaskRejectAtLimit();
      default:
        throw new InvalidArgumentsException("Unsupported delegate task rank level " + rank);
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

    Delegate delegate = get(accountId, delegateId, forceRefresh);
    if (delegate == null) {
      return delegateId;
    }

    return obtainDelegateName(delegate);
  }

  @VisibleForTesting
  @Override
  public void convertToExecutionCapability(DelegateTask task) {
    Set<ExecutionCapability> selectorCapabilities = new HashSet<>();

    if (isNotEmpty(task.getTags())) {
      SelectorCapability selectorCapability =
          SelectorCapability.builder().selectors(new HashSet<>(task.getTags())).selectorOrigin(TASK_SELECTORS).build();
      selectorCapabilities.add(selectorCapability);
    }

    if (task.getData() != null && task.getData().getTaskType() != null) {
      TaskGroup taskGroup = TaskType.valueOf(task.getData().getTaskType()).getTaskGroup();
      TaskSelectorMap mapFromTaskType = taskSelectorMapService.get(task.getAccountId(), taskGroup);
      if (mapFromTaskType != null && isNotEmpty(mapFromTaskType.getSelectors())) {
        SelectorCapability selectorCapability =
            SelectorCapability.builder()
                .selectors(mapFromTaskType.getSelectors())
                .selectorOrigin(TASK_CATEGORY + ": " + mapFromTaskType.getTaskGroup())
                .build();
        selectorCapabilities.add(selectorCapability);
      }
    }

    if (task.getExecutionCapabilities() == null) {
      task.setExecutionCapabilities(new ArrayList<>(selectorCapabilities));
    } else {
      task.getExecutionCapabilities().addAll(selectorCapabilities);
    }
  }

  @VisibleForTesting
  @Override
  public void saveDelegateTask(DelegateTask task, DelegateTask.Status taskStatus) {
    task.setStatus(taskStatus);
    task.setVersion(getVersion());
    task.setLastBroadcastAt(clock.millis());

    // For forward compatibility set the wait id to the uuid
    if (task.getUuid() == null) {
      task.setUuid(generateUuid());
    }

    if (task.getWaitId() == null) {
      task.setWaitId(task.getUuid());
    }

    // For backward compatibility we base the queue task expiry on the execution timeout
    if (task.getExpiry() == 0) {
      task.setExpiry(currentTimeMillis() + task.getData().getTimeout());
    }

    // order of these three calls is important, first capabilities are created, then appended, then used in
    // pickFirstAttemptDelegate
    generateCapabilitiesForTaskIfFeatureEnabled(task);
    convertToExecutionCapability(task);
    task.setPreAssignedDelegateId(assignDelegateService.pickFirstAttemptDelegate(task));

    // Ensure that broadcast happens at least 5 seconds from current time for async tasks
    if (task.getData().isAsync()) {
      task.setNextBroadcast(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5));
    }

    checkTaskRankRateLimit(task.getRank());

    // TODO: Make this call to make sure there are no secrets in disallowed expressions
    // resolvePreAssignmentExpressions(task, CHECK_FOR_SECRETS);

    wingsPersistence.save(task);
  }

  private Long fetchTaskCount() {
    return wingsPersistence.createQuery(DelegateTask.class, excludeAuthority).count();
  }

  @Override
  public String queueParkedTask(String accountId, String taskId) {
    DelegateTask task = wingsPersistence.createQuery(DelegateTask.class)
                            .filter(DelegateTaskKeys.accountId, accountId)
                            .filter(DelegateTaskKeys.uuid, taskId)
                            .get();

    task.getData().setAsync(true);

    try (AutoLogContext ignore1 = new TaskLogContext(task.getUuid(), task.getData().getTaskType(),
             TaskType.valueOf(task.getData().getTaskType()).getTaskGroup().name(), OVERRIDE_NESTS);
         AutoLogContext ignore2 = new AccountLogContext(task.getAccountId(), OVERRIDE_ERROR)) {
      saveDelegateTask(task, QUEUED);
      log.info("Queueing parked task");
      broadcastHelper.broadcastNewDelegateTaskAsync(task);
    }
    return task.getUuid();
  }

  @Override
  public byte[] getParkedTaskResults(String accountId, String taskId, String driverId) {
    DelegateTaskResultsProvider delegateTaskResultsProvider =
        delegateCallbackRegistry.obtainDelegateTaskResultsProvider(driverId);
    if (delegateTaskResultsProvider == null) {
      return new byte[0];
    }
    return delegateTaskResultsProvider.getDelegateTaskResults(taskId);
  }

  private void generateCapabilitiesForTaskIfFeatureEnabled(DelegateTask task) {
    addMergedParamsForCapabilityCheck(task);

    DelegateTaskPackage delegateTaskPackage = getDelegatePackageWithEncryptionConfig(task);
    CapabilityHelper.embedCapabilitiesInDelegateTask(task,
        delegateTaskPackage == null || isEmpty(delegateTaskPackage.getEncryptionConfigs())
            ? emptyList()
            : delegateTaskPackage.getEncryptionConfigs().values());

    if (isNotEmpty(task.getExecutionCapabilities())) {
      log.info(CapabilityHelper.generateLogStringWithCapabilitiesGenerated(task));
    }
  }

  // For some of the tasks, the necessary factors to do capability check is split across multiple
  // params. So none of the params can provide the execution capability by itself. To work around this,
  // we're adding extra params that combines these split params.
  private void addMergedParamsForCapabilityCheck(DelegateTask task) {
    List<Object> newParams;
    TaskType type = TaskType.valueOf(task.getData().getTaskType());
    Object[] params = task.getData().getParameters();
    switch (type) {
      case HOST_VALIDATION:
        HostValidationTaskParameters hostValidationTaskParameters =
            HostValidationTaskParameters.builder()
                .hostNames((List<String>) params[2])
                .connectionSetting((SettingAttribute) params[3])
                .encryptionDetails((List<EncryptedDataDetail>) params[4])
                .executionCredential((ExecutionCredential) params[5])
                .build();
        newParams = new ArrayList<>(Arrays.asList(hostValidationTaskParameters));
        task.getData().setParameters(newParams.toArray());
        return;
      case PCF_COMMAND_TASK:
        PcfCommandRequest commandRequest = (PcfCommandRequest) params[0];
        if (!(commandRequest instanceof PcfRunPluginCommandRequest)) {
          PcfCommandTaskParametersBuilder parametersBuilder =
              PcfCommandTaskParameters.builder().pcfCommandRequest(commandRequest);
          if (params.length > 1) {
            parametersBuilder.encryptedDataDetails((List<EncryptedDataDetail>) params[1]);
          }
          newParams = new ArrayList<>(Collections.singletonList(parametersBuilder.build()));
          task.getData().setParameters(newParams.toArray());
        }
        return;
      case GIT_COMMAND:
        GitConfig config = (GitConfig) params[1];
        List<EncryptedDataDetail> encryptedDataDetails = (List<EncryptedDataDetail>) params[2];
        Object[] newParamsArr = Arrays.copyOf(params, params.length + 1);
        newParamsArr[newParamsArr.length - 1] =
            GitValidationParameters.builder().gitConfig(config).encryptedDataDetails(encryptedDataDetails).build();
        task.getData().setParameters(newParamsArr);
        return;
      default:
        noop();
    }
  }

  private List<String> ensureDelegateAvailableToExecuteTask(DelegateTask task) {
    if (task == null) {
      log.warn("Delegate task is null");
      throw new InvalidArgumentsException(Pair.of("args", "Delegate task is null"));
    }
    if (task.getAccountId() == null) {
      log.warn("Delegate task has null account ID");
      throw new InvalidArgumentsException(Pair.of("args", "Delegate task has null account ID"));
    }

    BatchDelegateSelectionLog batch = delegateSelectionLogsService.createBatch(task);

    List<String> activeDelegates = assignDelegateService.retrieveActiveDelegates(task.getAccountId(), batch);
    log.info("{} delegates {} are active", activeDelegates.size(), activeDelegates);

    List<String> eligibleDelegates = activeDelegates.stream()
                                         .filter(delegateId -> assignDelegateService.canAssign(batch, delegateId, task))
                                         .collect(toList());

    delegateSelectionLogsService.save(batch);

    if (activeDelegates.isEmpty()) {
      if (assignDelegateService.noInstalledDelegates(task.getAccountId())) {
        log.info("No installed delegates found for the account");
        alertService.openAlert(task.getAccountId(), GLOBAL_APP_ID, AlertType.NoInstalledDelegates,
            NoInstalledDelegatesAlert.builder().accountId(task.getAccountId()).build());
      } else {
        log.info("No delegates are active for the account");
        alertService.openAlert(task.getAccountId(), GLOBAL_APP_ID, AlertType.NoActiveDelegates,
            NoActiveDelegatesAlert.builder().accountId(task.getAccountId()).build());
      }
    } else if (eligibleDelegates.isEmpty()) {
      log.warn("{} delegates active but no delegates are eligible to execute task", activeDelegates.size());

      List<ExecutionCapability> selectorCapabilities =
          task.getExecutionCapabilities().stream().filter(c -> c instanceof SelectorCapability).collect(toList());

      alertService.openAlert(task.getAccountId(), task.getAppId(), NoEligibleDelegates,
          NoEligibleDelegatesAlert.builder()
              .accountId(task.getAccountId())
              .appId(task.getAppId())
              .envId(task.getEnvId())
              .infraMappingId(task.getInfrastructureMappingId())
              .taskGroup(TaskType.valueOf(task.getData().getTaskType()).getTaskGroup())
              .taskType(TaskType.valueOf(task.getData().getTaskType()))
              .executionCapabilities(selectorCapabilities)
              .build());
    }

    log.info("{} delegates {} eligible to execute task", eligibleDelegates.size(), eligibleDelegates);
    return eligibleDelegates;
  }

  @Override
  public DelegateTaskPackage reportConnectionResults(
      String accountId, String delegateId, String taskId, List<DelegateConnectionResult> results) {
    assignDelegateService.saveConnectionResults(results);
    DelegateTask delegateTask = getUnassignedDelegateTask(accountId, taskId, delegateId);
    if (delegateTask == null) {
      return null;
    }

    try (AutoLogContext ignore = new TaskLogContext(taskId, delegateTask.getData().getTaskType(),
             TaskType.valueOf(delegateTask.getData().getTaskType()).getTaskGroup().name(), OVERRIDE_ERROR)) {
      log.info("Delegate completed validating {} task", delegateTask.getData().isAsync() ? ASYNC : SYNC);

      UpdateOperations<DelegateTask> updateOperations =
          wingsPersistence.createUpdateOperations(DelegateTask.class)
              .addToSet(DelegateTaskKeys.validationCompleteDelegateIds, delegateId);
      Query<DelegateTask> updateQuery = wingsPersistence.createQuery(DelegateTask.class)
                                            .filter(DelegateTaskKeys.accountId, delegateTask.getAccountId())
                                            .filter(DelegateTaskKeys.uuid, delegateTask.getUuid())
                                            .filter(DelegateTaskKeys.status, QUEUED)
                                            .field(DelegateTaskKeys.delegateId)
                                            .doesNotExist();
      wingsPersistence.update(updateQuery, updateOperations);

      long requiredDelegateCapabilites = 0;
      if (delegateTask.getExecutionCapabilities() != null) {
        requiredDelegateCapabilites = delegateTask.getExecutionCapabilities()
                                          .stream()
                                          .filter(e -> e.evaluationMode() == ExecutionCapability.EvaluationMode.AGENT)
                                          .count();
      }

      // If all delegate task capabilities were evaluated and they were ok, we can assign the task
      if (requiredDelegateCapabilites == size(results)
          && results.stream().allMatch(DelegateConnectionResult::isValidated)) {
        return assignTask(delegateId, taskId, delegateTask);
      }
    }

    return null;
  }

  @Override
  public DelegateTaskPackage acquireDelegateTask(String accountId, String delegateId, String taskId) {
    try {
      Delegate delegate = get(accountId, delegateId, false);
      if (delegate == null || Status.ENABLED != delegate.getStatus()) {
        log.warn("Delegate rejected to acquire task, because it was not found to be in {} status.", Status.ENABLED);
        return null;
      }

      log.info("Acquiring delegate task");
      DelegateTask delegateTask = getUnassignedDelegateTask(accountId, taskId, delegateId);
      if (delegateTask == null) {
        return null;
      }

      try (AutoLogContext ignore = new TaskLogContext(taskId, delegateTask.getData().getTaskType(),
               TaskType.valueOf(delegateTask.getData().getTaskType()).getTaskGroup().name(), OVERRIDE_ERROR)) {
        BatchDelegateSelectionLog batch = delegateSelectionLogsService.createBatch(delegateTask);
        boolean canAssign = assignDelegateService.canAssign(batch, delegateId, delegateTask);
        delegateSelectionLogsService.save(batch);

        if (!canAssign) {
          log.info("Delegate is not scoped for task");
          ensureDelegateAvailableToExecuteTask(delegateTask); // Raises an alert if there are no eligible delegates.
          return null;
        }

        // PL-9595 - Revalidate all tasks that are already whitelisted before executing them
        if ((featureFlagService.isEnabled(FeatureName.REVALIDATE_WHITELISTED_DELEGATE, accountId)
                && assignDelegateService.isWhitelisted(delegateTask, delegateId))
            || assignDelegateService.shouldValidate(delegateTask, delegateId)) {
          setValidationStarted(delegateId, delegateTask);
          return resolvePreAssignmentExpressions(delegateTask, SecretManagerFunctor.Mode.APPLY);
        } else if (!featureFlagService.isEnabled(FeatureName.REVALIDATE_WHITELISTED_DELEGATE, accountId)
            && assignDelegateService.isWhitelisted(delegateTask, delegateId)) {
          // Directly assign task only when FF is off and task is already whitelisted.
          return assignTask(delegateId, taskId, delegateTask);
        } else {
          log.info("Delegate is blacklisted for task");
          return null;
        }
      }
    } finally {
      log.info("Done with acquire delegate task method");
    }
  }

  @Override
  public void failIfAllDelegatesFailed(String accountId, String delegateId, String taskId) {
    DelegateTask delegateTask = getUnassignedDelegateTask(accountId, taskId, delegateId);
    if (delegateTask == null) {
      log.info("Task not found or was already assigned");
      return;
    }
    try (AutoLogContext ignore = new TaskLogContext(taskId, delegateTask.getData().getTaskType(),
             TaskType.valueOf(delegateTask.getData().getTaskType()).getTaskGroup().name(), OVERRIDE_ERROR)) {
      if (!isValidationComplete(delegateTask)) {
        log.info("Task is still being validated");
        return;
      }
      // Check whether a whitelisted delegate is connected
      List<String> whitelistedDelegates = assignDelegateService.connectedWhitelistedDelegates(delegateTask);
      if (isNotEmpty(whitelistedDelegates)) {
        log.info("Waiting for task to be acquired by a whitelisted delegate: {}", whitelistedDelegates);
        return;
      }

      log.info("No connected whitelisted delegates found for task");
      String errorMessage = generateValidationError(delegateTask);
      log.info(errorMessage);
      DelegateResponseData response;
      if (delegateTask.getData().isAsync()) {
        response = ErrorNotifyResponseData.builder()
                       .failureTypes(EnumSet.of(FailureType.DELEGATE_PROVISIONING))
                       .errorMessage(errorMessage)
                       .build();
      } else {
        response =
            RemoteMethodReturnValueData.builder().exception(new InvalidRequestException(errorMessage, USER)).build();
      }
      processDelegateResponse(accountId, null, taskId,
          DelegateTaskResponse.builder().accountId(accountId).response(response).responseCode(ResponseCode.OK).build());
    }
  }

  private String generateValidationError(DelegateTask delegateTask) {
    String capabilities = "";
    List<ExecutionCapability> executionCapabilities = delegateTask.getExecutionCapabilities();
    if (isNotEmpty(executionCapabilities)) {
      capabilities = (executionCapabilities.size() > 4 ? executionCapabilities.subList(0, 4) : executionCapabilities)
                         .stream()
                         .map(ExecutionCapability::fetchCapabilityBasis)
                         .collect(joining(", "));
      if (executionCapabilities.size() > 4) {
        capabilities += ", and " + (executionCapabilities.size() - 4) + " more...";
      }
    }

    String delegates = null, timedoutDelegates = null;
    Set<String> validationCompleteDelegateIds = delegateTask.getValidationCompleteDelegateIds();
    Set<String> validatingDelegateIds = delegateTask.getValidatingDelegateIds();

    if (isNotEmpty(validationCompleteDelegateIds)) {
      delegates = join(", ",
          validationCompleteDelegateIds.stream()
              .map(delegateId -> {
                Delegate delegate = get(delegateTask.getAccountId(), delegateId, false);
                return delegate == null ? delegateId : delegate.getHostName();
              })
              .collect(toList()));
    } else {
      delegates = "no delegates";
    }

    if (isNotEmpty(validatingDelegateIds)) {
      timedoutDelegates = join(", ",
          validatingDelegateIds.stream()
              .filter(p -> !validationCompleteDelegateIds.contains(p))
              .map(delegateId -> {
                Delegate delegate = get(delegateTask.getAccountId(), delegateId, false);
                return delegate == null ? delegateId : delegate.getHostName();
              })
              .collect(joining()));
    } else {
      timedoutDelegates = "no delegates timedout";
    }

    return format("No eligible delegates could perform the required capabilities for this task: [ %s ]%n"
            + "  -  The capabilities were tested by the following delegates: [ %s ]%n"
            + "  -  Following delegates were validating but never returned: [ %s ]%n"
            + "  -  Other delegates (if any) may have been offline or were not eligible due to tag or scope restrictions.",
        capabilities, delegates, timedoutDelegates);
  }

  @VisibleForTesting
  void setValidationStarted(String delegateId, DelegateTask delegateTask) {
    log.info("Delegate to validate {} task", delegateTask.getData().isAsync() ? ASYNC : SYNC);
    UpdateOperations<DelegateTask> updateOperations = wingsPersistence.createUpdateOperations(DelegateTask.class)
                                                          .addToSet(DelegateTaskKeys.validatingDelegateIds, delegateId);
    Query<DelegateTask> updateQuery = wingsPersistence.createQuery(DelegateTask.class)
                                          .filter(DelegateTaskKeys.accountId, delegateTask.getAccountId())
                                          .filter(DelegateTaskKeys.uuid, delegateTask.getUuid())
                                          .filter(DelegateTaskKeys.status, QUEUED)
                                          .field(DelegateTaskKeys.delegateId)
                                          .doesNotExist();
    wingsPersistence.update(updateQuery, updateOperations);

    wingsPersistence.update(updateQuery.field(DelegateTaskKeys.validationStartedAt).doesNotExist(),
        wingsPersistence.createUpdateOperations(DelegateTask.class)
            .set(DelegateTaskKeys.validationStartedAt, clock.millis()));
  }

  private boolean isValidationComplete(DelegateTask delegateTask) {
    Set<String> validatingDelegates = delegateTask.getValidatingDelegateIds();
    Set<String> completeDelegates = delegateTask.getValidationCompleteDelegateIds();
    boolean allDelegatesFinished = isNotEmpty(validatingDelegates) && isNotEmpty(completeDelegates)
        && completeDelegates.containsAll(validatingDelegates);
    if (allDelegatesFinished) {
      log.info("Validation attempts are complete for task", delegateTask.getUuid());
    }
    boolean validationTimedOut = delegateTask.getValidationStartedAt() != null
        && clock.millis() - delegateTask.getValidationStartedAt() > VALIDATION_TIMEOUT;
    if (validationTimedOut) {
      log.info("Validation timed out for task", delegateTask.getUuid());
    }
    return allDelegatesFinished || validationTimedOut;
  }

  private void clearFromValidationCache(DelegateTask delegateTask) {
    UpdateOperations<DelegateTask> updateOperations = wingsPersistence.createUpdateOperations(DelegateTask.class)
                                                          .unset(DelegateTaskKeys.validatingDelegateIds)
                                                          .unset(DelegateTaskKeys.validationCompleteDelegateIds);
    Query<DelegateTask> updateQuery = wingsPersistence.createQuery(DelegateTask.class)
                                          .filter(DelegateTaskKeys.accountId, delegateTask.getAccountId())
                                          .filter(DelegateTaskKeys.uuid, delegateTask.getUuid())
                                          .filter(DelegateTaskKeys.status, QUEUED)
                                          .field(DelegateTaskKeys.delegateId)
                                          .doesNotExist();
    wingsPersistence.update(updateQuery, updateOperations);
  }

  @VisibleForTesting
  DelegateTask getUnassignedDelegateTask(String accountId, String taskId, String delegateId) {
    DelegateTask delegateTask = wingsPersistence.createQuery(DelegateTask.class)
                                    .filter(DelegateTaskKeys.accountId, accountId)
                                    .filter(DelegateTaskKeys.uuid, taskId)
                                    .get();
    if (delegateTask != null) {
      try (AutoLogContext ignore = new TaskLogContext(taskId, delegateTask.getData().getTaskType(),
               TaskType.valueOf(delegateTask.getData().getTaskType()).getTaskGroup().name(), OVERRIDE_ERROR)) {
        if (delegateTask.getDelegateId() == null && delegateTask.getStatus() == QUEUED) {
          log.info("Found unassigned delegate task");
          return delegateTask;
        } else if (delegateId.equals(delegateTask.getDelegateId())) {
          log.info("Returning already assigned task to delegate from getUnassigned");
          return delegateTask;
        }
        log.info("Task not available for delegate - it was assigned to {} and has status {}",
            delegateTask.getDelegateId(), delegateTask.getStatus());
      }
    } else {
      log.info("Task no longer exists", taskId);
    }
    return null;
  }

  private DelegateTaskPackage resolvePreAssignmentExpressions(
      DelegateTask delegateTask, SecretManagerFunctor.Mode mode) {
    try {
      ManagerPreExecutionExpressionEvaluator managerPreExecutionExpressionEvaluator =
          new ManagerPreExecutionExpressionEvaluator(mode, serviceTemplateService, configService,
              delegateTask.getAppId(), delegateTask.getEnvId(), delegateTask.getServiceTemplateId(),
              artifactCollectionUtils, delegateTask.getArtifactStreamId(), featureFlagService, managerDecryptionService,
              secretManager, delegateTask.getAccountId(), delegateTask.getWorkflowExecutionId(),
              delegateTask.getData().getExpressionFunctorToken());

      List<ExecutionCapability> executionCapabilityList = emptyList();
      if (isNotEmpty(delegateTask.getExecutionCapabilities())) {
        executionCapabilityList = delegateTask.getExecutionCapabilities()
                                      .stream()
                                      .filter(x -> x.evaluationMode() == EvaluationMode.AGENT)
                                      .collect(toList());
      }

      DelegateTaskPackageBuilder delegateTaskPackageBuilder = DelegateTaskPackage.builder()
                                                                  .accountId(delegateTask.getAccountId())
                                                                  .delegateId(delegateTask.getDelegateId())
                                                                  .delegateTaskId(delegateTask.getUuid())
                                                                  .data(delegateTask.getData())
                                                                  .executionCapabilities(executionCapabilityList);

      if (featureFlagService.isEnabled(FeatureName.LOG_STREAMING_INTEGRATION, delegateTask.getAccountId())) {
        try {
          String logStreamingAccountToken = logStreamingAccountTokenCache.get(delegateTask.getAccountId());

          if (isNotBlank(logStreamingAccountToken)) {
            delegateTaskPackageBuilder.logStreamingToken(logStreamingAccountToken);
          }
        } catch (ExecutionException e) {
          log.warn("Unable to retrieve the log streaming service account token, while preparing delegate task package");
        }
      }

      if (delegateTask.getData().getParameters() == null || delegateTask.getData().getParameters().length != 1
          || !(delegateTask.getData().getParameters()[0] instanceof TaskParameters)) {
        return delegateTaskPackageBuilder.build();
      }

      SecretManagerFunctor secretManagerFunctor =
          (SecretManagerFunctor) managerPreExecutionExpressionEvaluator.getSecretManagerFunctor();

      ExpressionReflectionUtils.applyExpression(delegateTask.getData().getParameters()[0], (secretMode, value) -> {
        if (value == null) {
          return null;
        }
        return managerPreExecutionExpressionEvaluator.substitute(value, new HashMap<>());
        // TODO: this code is causing the second issue in DEL-1167
        //        if (secretManagerFunctor != null && secretMode == DISALLOW_SECRETS
        //            && secretManagerFunctor.getEvaluatedSecrets().size() > 0) {
        //          throw new InvalidRequestException(format("Expression %s is not allowed to have secrets.",
        //          substituted));
        //        }
        //        return mode == CHECK_FOR_SECRETS ? value : substituted;
      });

      if (secretManagerFunctor == null) {
        return null;
      }

      delegateTaskPackageBuilder.encryptionConfigs(secretManagerFunctor.getEncryptionConfigs())
          .secretDetails(secretManagerFunctor.getSecretDetails());

      if (isNotEmpty(secretManagerFunctor.getEvaluatedSecrets())) {
        delegateTaskPackageBuilder.secrets(
            secretManagerFunctor.getEvaluatedSecrets().values().stream().collect(toSet()));
      }

      return delegateTaskPackageBuilder.build();
    } catch (CriticalExpressionEvaluationException exception) {
      log.error("Exception in ManagerPreExecutionExpressionEvaluator ", exception);
      Query<DelegateTask> taskQuery = wingsPersistence.createQuery(DelegateTask.class)
                                          .filter(DelegateTaskKeys.accountId, delegateTask.getAccountId())
                                          .filter(DelegateTaskKeys.uuid, delegateTask.getUuid());
      DelegateTaskResponse response =
          DelegateTaskResponse.builder()
              .response(ErrorNotifyResponseData.builder().errorMessage(ExceptionUtils.getMessage(exception)).build())
              .responseCode(ResponseCode.FAILED)
              .accountId(delegateTask.getAccountId())
              .build();
      handleResponse(delegateTask, taskQuery, response);
      return null;
    }
  }

  private DelegateTaskPackage getDelegatePackageWithEncryptionConfig(DelegateTask delegateTask) {
    try {
      if (CapabilityHelper.isTaskParameterType(delegateTask.getData())) {
        return resolvePreAssignmentExpressions(delegateTask, SecretManagerFunctor.Mode.DRY_RUN);
      } else {
        // TODO: Ideally we should not land here, as we should always be passing TaskParameter only for
        // TODO: delegate task. But for now, this is needed. (e.g. Tasks containing Jenkinsonfig, BambooConfig etc.)
        Map<String, EncryptionConfig> encryptionConfigMap =
            CapabilityHelper.fetchEncryptionDetailsListFromParameters(delegateTask.getData());

        return DelegateTaskPackage.builder()
            .accountId(delegateTask.getAccountId())
            .delegateId(delegateTask.getDelegateId())
            .delegateTaskId(delegateTask.getUuid())
            .data(delegateTask.getData())
            .encryptionConfigs(encryptionConfigMap)
            .build();
      }
    } catch (CriticalExpressionEvaluationException exception) {
      log.error("Exception in ManagerPreExecutionExpressionEvaluator ", exception);
      Query<DelegateTask> taskQuery = wingsPersistence.createQuery(DelegateTask.class)
                                          .filter(DelegateTaskKeys.accountId, delegateTask.getAccountId())
                                          .filter(DelegateTaskKeys.uuid, delegateTask.getUuid());
      DelegateTaskResponse response =
          DelegateTaskResponse.builder()
              .response(ErrorNotifyResponseData.builder().errorMessage(ExceptionUtils.getMessage(exception)).build())
              .responseCode(ResponseCode.FAILED)
              .accountId(delegateTask.getAccountId())
              .build();
      handleResponse(delegateTask, taskQuery, response);
      return null;
    }
  }

  private void handleInprocResponse(DelegateTask delegateTask, DelegateTaskResponse response) {
    if (delegateTask.getData().isAsync()) {
      String waitId = delegateTask.getWaitId();
      if (waitId != null) {
        waitNotifyEngine.doneWith(waitId, response.getResponse());
      } else {
        log.error("Async task has no wait ID");
      }
    } else {
      wingsPersistence.save(DelegateSyncTaskResponse.builder()
                                .uuid(delegateTask.getUuid())
                                .responseData(kryoSerializer.asDeflatedBytes(response.getResponse()))
                                .build());
    }
  }

  @Override
  public void handleResponse(DelegateTask delegateTask, Query<DelegateTask> taskQuery, DelegateTaskResponse response) {
    if (delegateTask.getDriverId() == null) {
      handleInprocResponse(delegateTask, response);
    } else {
      handleDriverResponse(delegateTask, response);
    }

    if (taskQuery != null) {
      wingsPersistence.deleteOnServer(taskQuery);
    }
  }

  @VisibleForTesting
  void handleDriverResponse(DelegateTask delegateTask, DelegateTaskResponse response) {
    if (delegateTask == null || response == null) {
      return;
    }

    DelegateCallbackService delegateCallbackService =
        delegateCallbackRegistry.obtainDelegateCallbackService(delegateTask.getDriverId());
    if (delegateCallbackService == null) {
      return;
    }

    try (DelegateDriverLogContext driverLogContext =
             new DelegateDriverLogContext(delegateTask.getDriverId(), OVERRIDE_ERROR);
         TaskLogContext taskLogContext = new TaskLogContext(delegateTask.getUuid(), OVERRIDE_ERROR)) {
      if (delegateTask.getData().isAsync()) {
        log.info("Publishing async task response...");
        delegateCallbackService.publishAsyncTaskResponse(
            delegateTask.getUuid(), kryoSerializer.asDeflatedBytes(response.getResponse()));
      } else {
        log.info("Publishing sync task response...");
        delegateCallbackService.publishSyncTaskResponse(
            delegateTask.getUuid(), kryoSerializer.asDeflatedBytes(response.getResponse()));
      }
    } catch (Exception ex) {
      log.error("Failed publishing task response for task", ex);
    }
  }

  @Override
  public boolean validateThatDelegateNameIsUnique(String accountId, String delegateName) {
    Delegate delegate = wingsPersistence.createQuery(Delegate.class)
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

  @VisibleForTesting
  DelegateTaskPackage assignTask(String delegateId, String taskId, DelegateTask delegateTask) {
    // Clear pending validations. No longer need to track since we're assigning.
    clearFromValidationCache(delegateTask);

    log.info("Assigning {} task to delegate", delegateTask.getData().isAsync() ? ASYNC : SYNC);
    Query<DelegateTask> query = wingsPersistence.createQuery(DelegateTask.class)
                                    .filter(DelegateTaskKeys.accountId, delegateTask.getAccountId())
                                    .filter(DelegateTaskKeys.uuid, taskId)
                                    .filter(DelegateTaskKeys.status, QUEUED)
                                    .field(DelegateTaskKeys.delegateId)
                                    .doesNotExist()
                                    .project(DelegateTaskKeys.data_parameters, false);
    UpdateOperations<DelegateTask> updateOperations =
        wingsPersistence.createUpdateOperations(DelegateTask.class)
            .set(DelegateTaskKeys.delegateId, delegateId)
            .set(DelegateTaskKeys.status, STARTED)
            .set(DelegateTaskKeys.expiry, currentTimeMillis() + delegateTask.getData().getTimeout());
    DelegateTask task =
        wingsPersistence.findAndModifySystemData(query, updateOperations, HPersistence.returnNewOptions);
    // If the task wasn't updated because delegateId already exists then query for the task with the delegateId in
    // case client is retrying the request
    if (task != null) {
      try (
          DelayLogContext ignore = new DelayLogContext(task.getLastUpdatedAt() - task.getCreatedAt(), OVERRIDE_ERROR)) {
        log.info("Task assigned to delegate");
      }
      task.getData().setParameters(delegateTask.getData().getParameters());

      BatchDelegateSelectionLog batch = delegateSelectionLogsService.createBatch(delegateTask);
      delegateSelectionLogsService.logTaskAssigned(batch, task.getAccountId(), delegateId);
      delegateSelectionLogsService.save(batch);

      return resolvePreAssignmentExpressions(task, SecretManagerFunctor.Mode.APPLY);
    }
    task = wingsPersistence.createQuery(DelegateTask.class)
               .filter(DelegateTaskKeys.accountId, delegateTask.getAccountId())
               .filter(DelegateTaskKeys.uuid, taskId)
               .filter(DelegateTaskKeys.status, STARTED)
               .filter(DelegateTaskKeys.delegateId, delegateId)
               .project(DelegateTaskKeys.data_parameters, false)
               .get();
    if (task == null) {
      log.info("Task no longer available for delegate");
      return null;
    }

    task.getData().setParameters(delegateTask.getData().getParameters());
    log.info("Returning previously assigned task to delegate");
    return resolvePreAssignmentExpressions(task, SecretManagerFunctor.Mode.APPLY);
  }

  @Override
  public void clearCache(String accountId, String delegateId) {
    assignDelegateService.clearConnectionResults(accountId, delegateId);
  }

  @Override
  public void processDelegateResponse(
      String accountId, String delegateId, String taskId, DelegateTaskResponse response) {
    if (response == null) {
      throw new InvalidArgumentsException(Pair.of("args", "response cannot be null"));
    }

    log.info("Response received for task with responseCode [{}]", response.getResponseCode());

    Query<DelegateTask> taskQuery = wingsPersistence.createQuery(DelegateTask.class)
                                        .filter(DelegateTaskKeys.accountId, response.getAccountId())
                                        .filter(DelegateTaskKeys.uuid, taskId);

    DelegateTask delegateTask = taskQuery.get();

    if (delegateTask != null) {
      try (AutoLogContext ignore = new TaskLogContext(taskId, delegateTask.getData().getTaskType(),
               TaskType.valueOf(delegateTask.getData().getTaskType()).getTaskGroup().name(), OVERRIDE_ERROR)) {
        if (!StringUtils.equals(delegateTask.getVersion(), getVersion())) {
          log.warn("Version mismatch for task. [managerVersion {}, taskVersion {}]", getVersion(),
              delegateTask.getVersion());
        }

        if (response.getResponseCode() == ResponseCode.RETRY_ON_OTHER_DELEGATE) {
          log.info("Delegate returned retryable error for task");

          Set<String> alreadyTriedDelegates = delegateTask.getAlreadyTriedDelegates();
          List<String> remainingConnectedDelegates =
              assignDelegateService.connectedWhitelistedDelegates(delegateTask)
                  .stream()
                  .filter(item -> !delegateId.equals(item))
                  .filter(item -> isEmpty(alreadyTriedDelegates) || !alreadyTriedDelegates.contains(item))
                  .collect(toList());

          if (!remainingConnectedDelegates.isEmpty()) {
            log.info("Requeueing task");

            wingsPersistence.update(taskQuery,
                wingsPersistence.createUpdateOperations(DelegateTask.class)
                    .unset(DelegateTaskKeys.delegateId)
                    .unset(DelegateTaskKeys.validationStartedAt)
                    .unset(DelegateTaskKeys.lastBroadcastAt)
                    .unset(DelegateTaskKeys.validatingDelegateIds)
                    .unset(DelegateTaskKeys.validationCompleteDelegateIds)
                    .set(DelegateTaskKeys.broadcastCount, 1)
                    .set(DelegateTaskKeys.status, QUEUED)
                    .addToSet(DelegateTaskKeys.alreadyTriedDelegates, delegateId));
            return;
          } else {
            log.info("Task has been tried on all the connected delegates. Proceeding with error.");
          }
        }
        handleResponse(delegateTask, taskQuery, response);
        assignDelegateService.refreshWhitelist(delegateTask, delegateId);
      }
    } else {
      log.warn("No delegate task found");
    }
  }

  @Override
  public boolean filter(String accountId, String delegateId) {
    Delegate delegate = get(accountId, delegateId, false);
    return delegate != null && StringUtils.equals(delegate.getAccountId(), accountId);
  }

  @Override
  public boolean filter(String delegateId, DelegateTaskAbortEvent taskAbortEvent) {
    return wingsPersistence.createQuery(DelegateTask.class)
               .filter(DelegateTaskKeys.accountId, taskAbortEvent.getAccountId())
               .filter(DelegateTaskKeys.uuid, taskAbortEvent.getDelegateTaskId())
               .filter(DelegateTaskKeys.delegateId, delegateId)
               .getKey()
        != null;
  }

  @Override
  public String expireTask(String accountId, String delegateTaskId) {
    String errorMessage = null;
    try (AutoLogContext ignore1 = new TaskLogContext(delegateTaskId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      if (delegateTaskId == null) {
        log.warn("Delegate task id was null", new IllegalArgumentException());
        return errorMessage;
      }
      log.info("Expiring delegate task");
      Query<DelegateTask> delegateTaskQuery = getRunningTaskQuery(accountId, delegateTaskId);

      DelegateTask delegateTask = delegateTaskQuery.get();
      if (delegateTask != null) {
        try (AutoLogContext ignore3 = new TaskLogContext(delegateTaskId, delegateTask.getData().getTaskType(),
                 TaskType.valueOf(delegateTask.getData().getTaskType()).getTaskGroup().name(), OVERRIDE_ERROR)) {
          errorMessage = "Task expired. " + assignDelegateService.getActiveDelegateAssignmentErrorMessage(delegateTask);
          log.info("Marking task as expired: {}", errorMessage);

          if (isNotBlank(delegateTask.getWaitId())) {
            waitNotifyEngine.doneWith(
                delegateTask.getWaitId(), ErrorNotifyResponseData.builder().errorMessage(errorMessage).build());
          }
        }
      }

      endTask(accountId, delegateTaskId, delegateTaskQuery, ERROR);
    }
    return errorMessage;
  }

  @Override
  public DelegateTask abortTask(String accountId, String delegateTaskId) {
    try (AutoLogContext ignore1 = new TaskLogContext(delegateTaskId, OVERRIDE_ERROR);
         AutoLogContext ignore2 = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      if (delegateTaskId == null) {
        log.warn("Delegate task id was null", new IllegalArgumentException());
        return null;
      }
      log.info("Aborting delegate task");

      wingsPersistence.save(
          DelegateSyncTaskResponse.builder()
              .uuid(delegateTaskId)
              .responseData(kryoSerializer.asDeflatedBytes(
                  ErrorNotifyResponseData.builder().errorMessage("Delegate task was aborted").build()))
              .build());

      return endTask(accountId, delegateTaskId, getRunningTaskQuery(accountId, delegateTaskId), ABORTED);
    }
  }

  private DelegateTask endTask(
      String accountId, String delegateTaskId, Query<DelegateTask> delegateTaskQuery, DelegateTask.Status status) {
    UpdateOperations updateOperations =
        wingsPersistence.createUpdateOperations(DelegateTask.class).set(DelegateTaskKeys.status, status);

    DelegateTask oldTask =
        wingsPersistence.findAndModify(delegateTaskQuery, updateOperations, HPersistence.returnOldOptions);

    broadcasterFactory.lookup(STREAM_DELEGATE + accountId, true)
        .broadcast(aDelegateTaskAbortEvent().withAccountId(accountId).withDelegateTaskId(delegateTaskId).build());

    return oldTask;
  }

  private Query<DelegateTask> getRunningTaskQuery(String accountId, String delegateTaskId) {
    return wingsPersistence.createQuery(DelegateTask.class)
        .filter(DelegateTaskKeys.uuid, delegateTaskId)
        .filter(DelegateTaskKeys.accountId, accountId)
        .filter(DelegateTaskKeys.data_async, Boolean.TRUE)
        .field(DelegateTaskKeys.status)
        .in(runningStatuses());
  }

  @Override
  public List<DelegateTaskEvent> getDelegateTaskEvents(String accountId, String delegateId, boolean syncOnly) {
    List<DelegateTaskEvent> delegateTaskEvents = new ArrayList<>(getQueuedEvents(accountId, true));
    if (!syncOnly) {
      delegateTaskEvents.addAll(getQueuedEvents(accountId, false));
      delegateTaskEvents.addAll(getAbortedEvents(accountId, delegateId));
    }

    log.info("Dispatched delegateTaskIds: {}",
        join(",", delegateTaskEvents.stream().map(DelegateTaskEvent::getDelegateTaskId).collect(toList())));

    return delegateTaskEvents;
  }

  private List<DelegateTaskEvent> getQueuedEvents(String accountId, boolean sync) {
    Query<DelegateTask> delegateTaskQuery =
        wingsPersistence.createQuery(DelegateTask.class)
            .filter(DelegateTaskKeys.accountId, accountId)
            .filter(DelegateTaskKeys.version, versionInfoManager.getVersionInfo().getVersion())
            .filter(DelegateTaskKeys.status, QUEUED)
            .filter(DelegateTaskKeys.data_async, !sync)
            .field(DelegateTaskKeys.delegateId)
            .doesNotExist()
            .field(DelegateTaskKeys.expiry)
            .greaterThan(currentTimeMillis());

    return delegateTaskQuery.asKeyList()
        .stream()
        .map(taskKey
            -> aDelegateTaskEvent()
                   .withAccountId(accountId)
                   .withDelegateTaskId(taskKey.getId().toString())
                   .withSync(sync)
                   .build())
        .collect(toList());
  }

  private List<DelegateTaskEvent> getAbortedEvents(String accountId, String delegateId) {
    Query<DelegateTask> abortedQuery = wingsPersistence.createQuery(DelegateTask.class)
                                           .filter(DelegateTaskKeys.accountId, accountId)
                                           .filter(DelegateTaskKeys.status, ABORTED)
                                           .filter(DelegateTaskKeys.data_async, Boolean.TRUE)
                                           .filter(DelegateTaskKeys.delegateId, delegateId);

    // Send abort event only once by clearing delegateId
    wingsPersistence.update(
        abortedQuery, wingsPersistence.createUpdateOperations(DelegateTask.class).unset(DelegateTaskKeys.delegateId));

    return abortedQuery.project(DelegateTaskKeys.accountId, true)
        .asList()
        .stream()
        .map(delegateTask
            -> aDelegateTaskAbortEvent()
                   .withAccountId(delegateTask.getAccountId())
                   .withDelegateTaskId(delegateTask.getUuid())
                   .withSync(false)
                   .build())
        .collect(toList());
  }

  private String getVersion() {
    return versionInfoManager.getVersionInfo().getVersion();
  }

  @Override
  public void deleteByAccountId(String accountId) {
    wingsPersistence.delete(wingsPersistence.createQuery(Delegate.class).filter(DelegateKeys.accountId, accountId));
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
  public Optional<DelegateTask> fetchDelegateTask(String accountId, String taskId) {
    return Optional.ofNullable(wingsPersistence.createQuery(DelegateTask.class)
                                   .filter(DelegateTaskKeys.accountId, accountId)
                                   .filter(DelegateTaskKeys.uuid, taskId)
                                   .get());
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
          Delegate delegate = get(accountId, delegateId, false);
          boolean sameShellScriptDelegateLocation = DelegateType.SHELL_SCRIPT.equals(delegate.getDelegateType())
              && (isEmpty(heartbeat.getLocation()) || isEmpty(existingConnection.getLocation())
                     || heartbeat.getLocation().equals(existingConnection.getLocation()));
          if (!sameShellScriptDelegateLocation) {
            log.error(
                "Newer delegate connection found for the delegate id! Will initiate self destruct sequence for the current delegate.");
            destroyTheCurrentDelegate(accountId, delegateId, heartbeat.getDelegateConnectionId(), connectionMode);
            delegateConnectionDao.replaceWithNewerConnection(heartbeat.getDelegateConnectionId(), existingConnection);
          } else {
            log.error("Delegate restarted");
          }

        } else {
          log.error("Delegate restarted");
        }
      }
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
          wingsPersistence.createQuery(DelegateSequenceConfig.class).filter(ID_KEY, config.getUuid());
      wingsPersistence.update(sequenceConfigQuery,
          wingsPersistence.createUpdateOperations(DelegateSequenceConfig.class)
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
        wingsPersistence.createQuery(Delegate.class)
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
        wingsPersistence.createQuery(Delegate.class).filter(DelegateKeys.uuid, delegate.getUuid());

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
        wingsPersistence.createQuery(DelegateSequenceConfig.class)
            .filter(DelegateSequenceConfigKeys.accountId, delegate.getAccountId())
            .filter(DelegateSequenceConfigKeys.hostName, delegate.getHostName());

    return delegateSequenceConfigQuery.project(ID_KEY, true)
        .project(DelegateSequenceConfigKeys.sequenceNum, true)
        .project(DelegateSequenceConfig.LAST_UPDATED_AT_KEY, true)
        .project(DelegateSequenceConfig.ACCOUNT_ID_KEY, true)
        .project(DelegateSequenceConfigKeys.hostName, true)
        .project(DelegateSequenceConfigKeys.delegateToken, true)
        .asList();
  }

  @VisibleForTesting
  DelegateSequenceConfig addNewDelegateSequenceConfigRecord(Delegate delegate) {
    Query<DelegateSequenceConfig> delegateSequenceConfigQuery =
        wingsPersistence.createQuery(DelegateSequenceConfig.class)
            .filter(DelegateSequenceConfig.ACCOUNT_ID_KEY, delegate.getAccountId())
            .filter(DelegateSequenceConfigKeys.hostName, delegate.getHostName());

    List<DelegateSequenceConfig> existingDelegateSequenceConfigs =
        delegateSequenceConfigQuery.project(DelegateSequenceConfigKeys.sequenceNum, true)
            .project(DelegateSequenceConfig.LAST_UPDATED_AT_KEY, true)
            .project(DelegateSequenceConfig.ACCOUNT_ID_KEY, true)
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
          delete(existingInactiveDelegate.getAccountId(), existingInactiveDelegate.getUuid());
        }

        Query<DelegateSequenceConfig> sequenceConfigQuery =
            wingsPersistence.createQuery(DelegateSequenceConfig.class).filter("_id", config.getUuid());
        wingsPersistence.update(sequenceConfigQuery,
            wingsPersistence.createUpdateOperations(DelegateSequenceConfig.class)
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

    wingsPersistence.save(sequenceConfig);
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
    return wingsPersistence.createQuery(Delegate.class, excludeAuthority)
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
        throw new DuplicateDelegateException(delegateId, delegateConnectionId);
      case STREAMING:
        broadcasterFactory.lookup(STREAM_DELEGATE + accountId, true)
            .broadcast(SELF_DESTRUCT + delegateId + "-" + delegateConnectionId);
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
    Query<Delegate> query = wingsPersistence.createQuery(Delegate.class)
                                .filter(DelegateKeys.accountId, accountId)
                                .field(DelegateKeys.uuid)
                                .notIn(delegatesToRetain);

    UpdateOperations<Delegate> updateOps =
        wingsPersistence.createUpdateOperations(Delegate.class).set(DelegateKeys.status, Status.DELETED);
    wingsPersistence.update(query, updateOps);

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
  protected String retrieveLogStreamingAccountToken(String accountId) {
    try {
      return SafeHttpCall.executeWithExceptions(logStreamingServiceRestClient.retrieveAccountToken(
          mainConfiguration.getLogStreamingServiceConfig().getServiceToken(), accountId));
    } catch (Exception ex) {
      log.error("Unable to retrieve log streaming authentication token", ex);
      return null;
    }
  }
}
