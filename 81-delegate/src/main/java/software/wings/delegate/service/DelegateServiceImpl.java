package software.wings.delegate.service;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.filesystem.FileIo.acquireLock;
import static io.harness.filesystem.FileIo.isLocked;
import static io.harness.filesystem.FileIo.releaseLock;
import static io.harness.network.Localhost.getLocalHostAddress;
import static io.harness.network.Localhost.getLocalHostName;
import static io.harness.network.SafeHttpCall.execute;
import static io.harness.security.VerificationTokenGenerator.VERIFICATION_SERVICE_SECRET;
import static io.harness.threading.Morpheus.sleep;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.MULTIPART_FORM_DATA;
import static org.apache.commons.io.filefilter.FileFilterUtils.falseFileFilter;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static software.wings.beans.Delegate.Builder.aDelegate;
import static software.wings.common.Constants.SELF_DESTRUCT;
import static software.wings.delegate.app.DelegateApplication.getProcessId;
import static software.wings.delegate.service.InstallUtils.installGoTemplateTool;
import static software.wings.delegate.service.InstallUtils.installKubectl;
import static software.wings.managerclient.ManagerClientFactory.TRUST_ALL_CERTS;
import static software.wings.utils.Misc.getDurationString;
import static software.wings.utils.message.MessageConstants.DELEGATE_DASH;
import static software.wings.utils.message.MessageConstants.DELEGATE_GO_AHEAD;
import static software.wings.utils.message.MessageConstants.DELEGATE_HEARTBEAT;
import static software.wings.utils.message.MessageConstants.DELEGATE_IS_NEW;
import static software.wings.utils.message.MessageConstants.DELEGATE_RESTART_NEEDED;
import static software.wings.utils.message.MessageConstants.DELEGATE_RESUME;
import static software.wings.utils.message.MessageConstants.DELEGATE_SELF_DESTRUCT;
import static software.wings.utils.message.MessageConstants.DELEGATE_SEND_VERSION_HEADER;
import static software.wings.utils.message.MessageConstants.DELEGATE_SHUTDOWN_PENDING;
import static software.wings.utils.message.MessageConstants.DELEGATE_SHUTDOWN_STARTED;
import static software.wings.utils.message.MessageConstants.DELEGATE_STARTED;
import static software.wings.utils.message.MessageConstants.DELEGATE_STOP_ACQUIRING;
import static software.wings.utils.message.MessageConstants.DELEGATE_UPGRADE_NEEDED;
import static software.wings.utils.message.MessageConstants.DELEGATE_UPGRADE_PENDING;
import static software.wings.utils.message.MessageConstants.DELEGATE_UPGRADE_STARTED;
import static software.wings.utils.message.MessageConstants.DELEGATE_VERSION;
import static software.wings.utils.message.MessageConstants.UPGRADING_DELEGATE;
import static software.wings.utils.message.MessageConstants.WATCHER_DATA;
import static software.wings.utils.message.MessageConstants.WATCHER_HEARTBEAT;
import static software.wings.utils.message.MessageConstants.WATCHER_PROCESS;
import static software.wings.utils.message.MessageConstants.WATCHER_VERSION;
import static software.wings.utils.message.MessengerType.DELEGATE;
import static software.wings.utils.message.MessengerType.WATCHER;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import com.ning.http.client.AsyncHttpClient;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.beans.DelegateScripts;
import io.harness.delegate.task.DelegateRunnableTask;
import io.harness.filesystem.FileIo;
import io.harness.network.FibonacciBackOff;
import io.harness.network.Http;
import io.harness.rest.RestResponse;
import io.harness.security.TokenGenerator;
import io.harness.serializer.JsonUtils;
import io.harness.version.VersionInfoManager;
import okhttp3.MediaType;
import okhttp3.MultipartBody.Part;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.atmosphere.wasync.Client;
import org.atmosphere.wasync.Encoder;
import org.atmosphere.wasync.Event;
import org.atmosphere.wasync.Function;
import org.atmosphere.wasync.Options;
import org.atmosphere.wasync.Request.METHOD;
import org.atmosphere.wasync.Request.TRANSPORT;
import org.atmosphere.wasync.RequestBuilder;
import org.atmosphere.wasync.Socket;
import org.atmosphere.wasync.Socket.STATUS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.StartedProcess;
import org.zeroturnaround.exec.stream.LogOutputStream;
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;
import retrofit2.Response;
import software.wings.app.DeployMode;
import software.wings.beans.Delegate;
import software.wings.beans.Delegate.Builder;
import software.wings.beans.Delegate.Status;
import software.wings.beans.DelegateConnectionHeartbeat;
import software.wings.beans.DelegateProfileParams;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskAbortEvent;
import software.wings.beans.DelegateTaskEvent;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.TaskType;
import software.wings.delegate.app.DelegateConfiguration;
import software.wings.delegatetasks.TaskLogContext;
import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.delegatetasks.validation.DelegateValidateTask;
import software.wings.managerclient.ManagerClient;
import software.wings.managerclient.ManagerClientFactory;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.utils.message.Message;
import software.wings.utils.message.MessageService;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.ConnectException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManager;
import javax.validation.constraints.NotNull;

@Singleton
public class DelegateServiceImpl implements DelegateService {
  private static final int MAX_CONNECT_ATTEMPTS = 50;
  private static final int RECONNECT_INTERVAL_SECONDS = 10;
  private static final int POLL_INTERVAL_SECONDS = 3;
  private static final long UPGRADE_TIMEOUT = TimeUnit.HOURS.toMillis(2);
  private static final long HEARTBEAT_TIMEOUT = TimeUnit.MINUTES.toMillis(15);
  private static final long WATCHER_HEARTBEAT_TIMEOUT = TimeUnit.MINUTES.toMillis(10);
  private static final long WATCHER_VERSION_MATCH_TIMEOUT = TimeUnit.MINUTES.toMillis(2);
  public static final String DELEGATE_SEQUENCE_CONFIG_FILE = "./delegate_sequence_config";
  public static final int KEEP_ALIVE_INTERVAL = 20000;

  private static String hostName;

  @Inject private DelegateConfiguration delegateConfiguration;
  @Inject private ManagerClient managerClient;
  @Inject @Named("heartbeatExecutor") private ScheduledExecutorService heartbeatExecutor;
  @Inject @Named("localHeartbeatExecutor") private ScheduledExecutorService localHeartbeatExecutor;
  @Inject @Named("upgradeExecutor") private ScheduledExecutorService upgradeExecutor;
  @Inject @Named("inputExecutor") private ScheduledExecutorService inputExecutor;
  @Inject @Named("taskPollExecutor") private ScheduledExecutorService taskPollExecutor;
  @Inject @Named("installCheckExecutor") private ScheduledExecutorService profileExecutor;
  @Inject @Named("systemExecutor") private ExecutorService systemExecutorService;
  @Inject @Named("asyncExecutor") private ExecutorService asyncExecutorService;
  @Inject @Named("artifactExecutor") private ExecutorService artifactExecutorService;
  @Inject @Named("timeoutExecutor") private ExecutorService timeoutEnforcementService;
  @Inject private ExecutorService syncExecutorService;
  @Inject private SignalService signalService;
  @Inject private MessageService messageService;
  @Inject private Injector injector;
  @Inject private TokenGenerator tokenGenerator;
  @Inject private AsyncHttpClient asyncHttpClient;
  @Inject private Clock clock;
  @Inject private TimeLimiter timeLimiter;
  @Inject private VersionInfoManager versionInfoManager;

  private static final Logger logger = LoggerFactory.getLogger(DelegateServiceImpl.class);
  private final Object waiter = new Object();

  private final Map<String, DelegateTask> currentlyValidatingTasks = new ConcurrentHashMap<>();
  private final Map<String, DelegateTask> currentlyExecutingTasks = new ConcurrentHashMap<>();
  private final Map<String, Future<?>> currentlyValidatingFutures = new ConcurrentHashMap<>();
  private final Map<String, Future<?>> currentlyExecutingFutures = new ConcurrentHashMap<>();

  private final AtomicLong lastHeartbeatSentAt = new AtomicLong(System.currentTimeMillis());
  private final AtomicLong lastHeartbeatReceivedAt = new AtomicLong(System.currentTimeMillis());
  private final AtomicBoolean upgradePending = new AtomicBoolean(false);
  private final AtomicBoolean upgradeNeeded = new AtomicBoolean(false);
  private final AtomicBoolean restartNeeded = new AtomicBoolean(false);
  private final AtomicBoolean acquireTasks = new AtomicBoolean(true);
  private final AtomicBoolean executingProfile = new AtomicBoolean(false);
  private final AtomicBoolean selfDestruct = new AtomicBoolean(false);
  private final AtomicBoolean multiVersionWatcherStarted = new AtomicBoolean(false);
  private final AtomicBoolean pollingForTasks = new AtomicBoolean(false);

  private Socket socket;
  private RequestBuilder request;
  private String upgradeVersion;
  private long startTime;
  private long upgradeStartedAt;
  private long stoppedAcquiringAt;
  private static String delegateId;
  private static String delegateType;
  private static String delegateGroupName;
  private String accountId;
  private long watcherVersionMatchedAt = System.currentTimeMillis();
  private HttpHost httpProxyHost;

  private final String delegateConnectionId = generateUuid();
  private DelegateConnectionHeartbeat connectionHeartbeat;

  private final boolean multiVersion = DeployMode.KUBERNETES.name().equals(System.getenv().get("DEPLOY_MODE"))
      || TRUE.toString().equals(System.getenv().get("MULTI_VERSION"));

  public static String getHostName() {
    return hostName;
  }

  static String getDelegateId() {
    return delegateId;
  }

  @SuppressFBWarnings({"UW_UNCOND_WAIT", "WA_NOT_IN_LOOP", "ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD"})
  @Override
  @SuppressWarnings("unchecked")
  public void run(boolean watched) {
    try {
      hostName = getLocalHostName();
      accountId = delegateConfiguration.getAccountId();
      startTime = clock.millis();

      applyProxyConfigurations();

      connectionHeartbeat = DelegateConnectionHeartbeat.builder()
                                .delegateConnectionId(delegateConnectionId)
                                .version(getVersion())
                                .alive(true)
                                .build();

      if (watched) {
        logger.info("[New] Delegate process started. Sending confirmation");
        messageService.writeMessage(DELEGATE_STARTED);
        startInputCheck();
        logger.info("[New] Waiting for go ahead from watcher");
        Message message = messageService.waitForMessage(DELEGATE_GO_AHEAD, TimeUnit.MINUTES.toMillis(5));
        logger.info(message != null ? "[New] Got go-ahead. Proceeding"
                                    : "[New] Timed out waiting for go-ahead. Proceeding anyway");
        messageService.removeData(DELEGATE_DASH + getProcessId(), DELEGATE_IS_NEW);
        startLocalHeartbeat();
      } else {
        logger.info("Delegate process started");
      }

      installKubectl(delegateConfiguration);
      installGoTemplateTool(delegateConfiguration);

      long start = clock.millis();
      String description = "description here".equals(delegateConfiguration.getDescription())
          ? ""
          : delegateConfiguration.getDescription();

      String delegateName = System.getenv().get("DELEGATE_NAME");
      if (isNotBlank(delegateName)) {
        logger.info("Registering delegate with delegate name: {}", delegateName);
      } else {
        delegateName = "";
      }

      String delegateProfile = System.getenv().get("DELEGATE_PROFILE");
      if (isNotBlank(delegateProfile)) {
        logger.info("Registering delegate with delegate profile: {}", delegateProfile);
      } else {
        delegateProfile = "";
      }

      delegateType = System.getenv().get("DELEGATE_TYPE");

      logger.info("DELEGATE_TYPE is: " + delegateType);
      if (isNotBlank(delegateType)) {
        delegateGroupName = System.getenv().get("DELEGATE_GROUP_NAME");
        logger.info(new StringBuilder(128)
                        .append("Registering delegate with delegate Type: ")
                        .append(delegateType)
                        .append(", DelegateGroupName: ")
                        .append(delegateGroupName)
                        .toString());
      }

      Delegate.Builder builder = aDelegate()
                                     .withIp(getLocalHostAddress())
                                     .withAccountId(accountId)
                                     .withHostName(hostName)
                                     .withDelegateName(delegateName)
                                     .withDelegateGroupName(delegateGroupName)
                                     .withDelegateProfileId(delegateProfile)
                                     .withDescription(description)
                                     .withVersion(getVersion())
                                     .withDelegateType(delegateType);

      delegateId = registerDelegate(builder);
      logger.info("[New] Delegate registered in {} ms", clock.millis() - start);

      SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("SSL");
      sslContext.init(null, TRUST_ALL_CERTS.toArray(new TrustManager[1]), new java.security.SecureRandom());

      if (delegateConfiguration.isPollForTasks()) {
        pollingForTasks.set(true);
      } else {
        Client client = org.atmosphere.wasync.ClientFactory.getDefault().newClient();

        // Stream the request body
        RequestBuilder reqBuilder =
            client.newRequestBuilder()
                .method(METHOD.GET)
                .uri(delegateConfiguration.getManagerUrl().replace("/api/", "/stream/") + "delegate/" + accountId)
                .queryString("delegateId", delegateId)
                .queryString("delegateConnectionId", delegateConnectionId)
                .queryString("token", tokenGenerator.getToken("https", "localhost", 9090, hostName))
                .queryString("sequenceNum", getSequenceNumForEcsDelegate())
                .queryString("delegateToken", getRandomTokenForEcsDelegate())
                .header("Version", getVersion());
        if (delegateConfiguration.isProxy()) {
          reqBuilder.header("X-Atmosphere-WebSocket-Proxy", "true");
        }

        request = reqBuilder
                      .encoder(new Encoder<Delegate, Reader>() { // Do not change this, wasync doesn't like lambdas
                        @Override
                        public Reader encode(Delegate s) {
                          return new StringReader(JsonUtils.asJson(s));
                        }
                      })
                      .transport(TRANSPORT.WEBSOCKET);

        Options clientOptions =
            client.newOptionsBuilder()
                .runtime(asyncHttpClient, true)
                .reconnect(true)
                .reconnectAttempts(new File("start.sh").exists() ? MAX_CONNECT_ATTEMPTS : Integer.MAX_VALUE)
                .pauseBeforeReconnectInSeconds(RECONNECT_INTERVAL_SECONDS)
                .build();
        socket = client.create(clientOptions);
        socket
            .on(Event.MESSAGE,
                new Function<String>() { // Do not change this, wasync doesn't like lambdas
                  @Override
                  public void on(String message) {
                    handleMessageSubmit(message);
                  }
                })
            .on(Event.ERROR,
                new Function<Exception>() { // Do not change this, wasync doesn't like lambdas
                  @Override
                  public void on(Exception e) {
                    handleError(e);
                  }
                })
            .on(Event.REOPENED,
                new Function<Object>() { // Do not change this, wasync doesn't like lambdas
                  @Override
                  public void on(Object o) {
                    handleReopened(o, builder);
                  }
                })
            .on(Event.CLOSE, new Function<Object>() { // Do not change this, wasync doesn't like lambdas
              @Override
              public void on(Object o) {
                handleClose(o);
              }
            });

        socket.open(request.build());

        startHeartbeat(builder, socket);
        startKeepAlivePacket(builder, socket);
      }

      startTaskPolling();
      startHeartbeat();

      if (!multiVersion) {
        startUpgradeCheck(getVersion());
      }

      logger.info("Delegate started");

      startProfileCheck();

      synchronized (waiter) {
        waiter.wait();
      }

      messageService.closeData(DELEGATE_DASH + getProcessId());
      messageService.closeChannel(DELEGATE, getProcessId());

      if (upgradePending.get()) {
        removeDelegateVersionFromCapsule();
        cleanupOldDelegateVersionFromBackup();
      }

    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      logger.error("Exception while starting/running delegate", e);
    } catch (RuntimeException | IOException | NoSuchAlgorithmException | KeyManagementException e) {
      logger.error("Exception while starting/running delegate", e);
    }
  }

  protected void applyProxyConfigurations() {
    String proxyHost = System.getProperty("https.proxyHost");

    if (isNotBlank(proxyHost)) {
      String proxyScheme = System.getProperty("proxyScheme");
      String proxyPort = System.getProperty("https.proxyPort");
      logger.info("Using {} proxy {}:{}", proxyScheme, proxyHost, proxyPort);
      httpProxyHost = new HttpHost(proxyHost, Integer.parseInt(proxyPort), proxyScheme);
      String nonProxyHostsString = System.getProperty("http.nonProxyHosts");
      if (isNotBlank(nonProxyHostsString)) {
        String[] suffixes = nonProxyHostsString.split("\\|");
        List<String> nonProxyHosts = Stream.of(suffixes).map(suffix -> suffix.substring(1)).collect(toList());
        logger.info("No proxy for hosts with suffix in: {}", nonProxyHosts);
      }
    } else {
      logger.info("No proxy settings. Configure in proxy.config if needed");
    }
  }

  private void handleClose(Object o) {
    logger.info("Event:{}, message:[{}]", Event.CLOSE.name(), o.toString());
    pollingForTasks.set(true);
  }

  private void handleReopened(Object o, Builder builder) {
    logger.info("Event:{}, message:[{}]", Event.REOPENED.name(), o.toString());
    pollingForTasks.set(false);
    try {
      socket.fire(
          builder.but().withLastHeartBeat(clock.millis()).withStatus(Status.ENABLED).withConnected(true).build());
    } catch (IOException e) {
      logger.error("Error connecting", e);
    }
  }

  private void handleError(Exception e) {
    logger.info("Event:{}, message:[{}]", Event.ERROR.name(), e.getMessage());
    if (e instanceof SSLException) {
      logger.info("Reopening connection to manager");
      try {
        socket.close();
      } catch (Exception ex) {
        // Ignore
      }
      try {
        FibonacciBackOff.executeForEver(() -> socket.open(request.build()));
      } catch (IOException ex) {
        logger.error("Unable to open socket", e);
      }
    } else if (e instanceof ConnectException) {
      logger.warn("Failed to connect after {} attempts.", MAX_CONNECT_ATTEMPTS);
      restartNeeded.set(true);
    } else {
      logger.error("Exception: " + e.getMessage(), e);
      try {
        socket.close();
      } catch (Exception ex) {
        // Ignore
      }
      restartNeeded.set(true);
    }
  }

  private void handleMessageSubmit(String message) {
    logger.info("^^MSG: " + message);
    systemExecutorService.submit(() -> handleMessage(message));
  }

  private void handleMessage(String message) {
    if (StringUtils.startsWith(message, "[X]")) {
      String receivedId;
      String token = null;
      String sequenceNum = null;
      if (isEcsDelegate()) {
        int indexForToken = message.lastIndexOf("[TOKEN]");
        receivedId = message.substring(3, indexForToken); // Remove the "[X]

      } else {
        receivedId = message.substring(3);
      }
      if (delegateId.equals(receivedId)) {
        long now = clock.millis();
        logger.info("Delegate {} received heartbeat response {} after sending. {} since last response.", receivedId,
            getDurationString(lastHeartbeatSentAt.get(), now), getDurationString(lastHeartbeatReceivedAt.get(), now));

        handleEcsDelegateSpecificMessage(message);

        lastHeartbeatReceivedAt.set(now);
      } else {
        logger.info("Heartbeat response for another delegate received: {}", receivedId);
      }
    } else if (StringUtils.equals(message, SELF_DESTRUCT)) {
      initiateSelfDestruct();
    } else if (!StringUtils.equals(message, "X")) {
      logger.info("Executing: Event:{}, message:[{}]", Event.MESSAGE.name(), message);
      try {
        DelegateTaskEvent delegateTaskEvent = JsonUtils.asObject(message, DelegateTaskEvent.class);
        try (TaskLogContext ignore = new TaskLogContext(delegateTaskEvent.getDelegateTaskId())) {
          if (delegateTaskEvent instanceof DelegateTaskAbortEvent) {
            abortDelegateTask((DelegateTaskAbortEvent) delegateTaskEvent);
          } else {
            dispatchDelegateTask(delegateTaskEvent);
          }
        }
      } catch (Exception e) {
        logger.info(message);
        logger.error("Exception while decoding task", e);
      }
    }
  }

  @Override
  public void pause() {
    if (!delegateConfiguration.isPollForTasks()) {
      socket.close();
    }
  }

  private void resume() {
    try {
      if (!delegateConfiguration.isPollForTasks()) {
        FibonacciBackOff.executeForEver(() -> socket.open(request.build()));
      }
      upgradePending.set(false);
      upgradeNeeded.set(false);
      restartNeeded.set(false);
      acquireTasks.set(true);
    } catch (IOException e) {
      logger.error("Failed to resume.", e);
      stop();
    }
  }

  @Override
  public void stop() {
    synchronized (waiter) {
      waiter.notify();
    }
  }

  @SuppressFBWarnings({"DM_EXIT"})
  private String registerDelegate(Builder builder) {
    updateBuilderIfEcsDelegate(builder);
    AtomicInteger attempts = new AtomicInteger(0);
    while (acquireTasks.get()) {
      RestResponse<Delegate> delegateResponse;
      try {
        attempts.incrementAndGet();
        String attemptString = attempts.get() > 1 ? " (Attempt " + attempts.get() + ")" : "";
        logger.info("Registering delegate" + attemptString);
        delegateResponse = execute(managerClient.registerDelegate(
            accountId, builder.but().withLastHeartBeat(clock.millis()).withStatus(Status.ENABLED).build()));
      } catch (Exception e) {
        String msg = "Unknown error occurred while registering Delegate [" + accountId + "] with manager";
        logger.error(msg, e);
        sleep(ofMinutes(1));
        continue;
      }
      if (delegateResponse == null || delegateResponse.getResource() == null) {
        logger.error(
            "Error occurred while registering delegate with manager for account {}. Please see the manager log for more information",
            accountId);
        sleep(ofMinutes(1));
        continue;
      }

      Delegate delegate = delegateResponse.getResource();
      String delegateId = delegate.getUuid();
      handleEcsDelegateRegistrationResponse(delegate);

      if (StringUtils.equals(delegateId, SELF_DESTRUCT)) {
        initiateSelfDestruct();
        sleep(ofMinutes(1));
        continue;
      }
      builder.withUuid(delegateId).withStatus(delegateResponse.getResource().getStatus());
      VERIFICATION_SERVICE_SECRET.set(delegateResponse.getResource().getVerificationServiceSecret());
      logger.info(
          "Delegate registered with id {} and status {}", delegateId, delegateResponse.getResource().getStatus());
      return delegateId;
    }

    // Didn't register and not acquiring. Exiting.
    System.exit(1);
    return null;
  }

  private void startProfileCheck() {
    profileExecutor.scheduleWithFixedDelay(() -> {
      boolean forCodeFormattingOnly; // This line is here for clang-format
      synchronized (this) {
        checkForProfile();
      }
    }, 0, 3, TimeUnit.MINUTES);
  }

  private void checkForProfile() {
    if (!executingProfile.get() && !isLocked(new File("profile"))) {
      try {
        logger.info("Checking for profile ...");
        DelegateProfileParams profileParams = getProfile();
        boolean resultExists = new File("profile.result").exists();
        String profileId = profileParams == null ? "" : profileParams.getProfileId();
        long updated = profileParams == null || !resultExists ? 0L : profileParams.getProfileLastUpdatedAt();
        RestResponse<DelegateProfileParams> response = timeLimiter.callWithTimeout(
            ()
                -> execute(managerClient.checkForProfile(delegateId, accountId, profileId, updated)),
            15L, TimeUnit.SECONDS, true);
        if (response != null) {
          applyProfile(response.getResource());
        }
      } catch (UncheckedTimeoutException ex) {
        logger.warn("Timed out checking for profile");
      } catch (Exception e) {
        logger.error("Error checking for profile", e);
      }
    }
  }

  private DelegateProfileParams getProfile() {
    File profile = new File("profile");
    if (profile.exists()) {
      try {
        return JsonUtils.asObject(FileUtils.readFileToString(profile, UTF_8), DelegateProfileParams.class);
      } catch (Exception e) {
        logger.error("Error reading profile", e);
      }
    }
    return null;
  }

  private void applyProfile(DelegateProfileParams profile) {
    if (profile != null && executingProfile.compareAndSet(false, true)) {
      File profileFile = new File("profile");
      if (acquireLock(profileFile, ofSeconds(5))) {
        try {
          if ("NONE".equals(profile.getProfileId())) {
            FileUtils.deleteQuietly(profileFile);
            FileUtils.deleteQuietly(new File("profile.result"));
            return;
          }

          logger.info("Updating delegate profile to [{} : {}], last update {} ...", profile.getProfileId(),
              profile.getName(), profile.getProfileLastUpdatedAt());
          String script = profile.getScriptContent();

          Logger scriptLogger = LoggerFactory.getLogger("delegate-profile-" + profile.getProfileId());
          scriptLogger.info("Executing profile script: {}", profile.getName());
          List<String> result = new ArrayList<>();

          ProcessExecutor processExecutor = new ProcessExecutor()
                                                .timeout(10, TimeUnit.MINUTES)
                                                .command("/bin/bash", "-c", script)
                                                .readOutput(true)
                                                .redirectOutput(new LogOutputStream() {
                                                  @Override
                                                  protected void processLine(String line) {
                                                    scriptLogger.info(line);
                                                    result.add(line);
                                                  }
                                                })
                                                .redirectError(new LogOutputStream() {
                                                  @Override
                                                  protected void processLine(String line) {
                                                    scriptLogger.error(line);
                                                    result.add("ERROR: " + line);
                                                  }
                                                });
          int exitCode = processExecutor.execute().getExitValue();
          saveProfile(profile, result);
          uploadProfileResult(exitCode);
          logger.info("Profile applied");
        } catch (IOException e) {
          logger.error(format("Error applying profile [%s]", profile.getName()), e);
        } catch (InterruptedException e) {
          logger.info("Interrupted", e);
        } catch (TimeoutException e) {
          logger.info("Timed out", e);
        } catch (UncheckedTimeoutException ex) {
          logger.error("Timed out sending profile result");
        } catch (Exception e) {
          logger.error("Error applying profile", e);
        } finally {
          executingProfile.set(false);
          if (!releaseLock(profileFile)) {
            logger.error("Failed to release lock {}", profileFile.getPath());
          }
        }
      }
    }
  }

  private void saveProfile(DelegateProfileParams profile, List<String> result) {
    logger.info("Saving profile result");
    try {
      File profileFile = new File("profile");
      if (profileFile.exists()) {
        FileUtils.forceDelete(profileFile);
      }
      FileUtils.touch(profileFile);
      FileUtils.write(profileFile, JsonUtils.asPrettyJson(profile), UTF_8);

      File resultFile = new File("profile.result");
      if (resultFile.exists()) {
        FileUtils.forceDelete(resultFile);
      }
      FileUtils.touch(resultFile);
      FileUtils.writeLines(resultFile, result);
    } catch (IOException e) {
      logger.error(format("Error writing profile [%s]", profile.getName()), e);
    }
  }

  private void uploadProfileResult(int exitCode) throws Exception {
    logger.info("Uploading profile result");
    // create RequestBody instance from file
    File profileResult = new File("profile.result");
    RequestBody requestFile = RequestBody.create(MediaType.parse(MULTIPART_FORM_DATA), profileResult);

    // MultipartBody.Part is used to send also the actual file name
    Part part = Part.createFormData("file", profileResult.getName(), requestFile);
    timeLimiter.callWithTimeout(()
                                    -> execute(managerClient.saveProfileResult(
                                        delegateId, accountId, exitCode != 0, FileBucket.PROFILE_RESULTS, part)),
        15L, TimeUnit.SECONDS, true);
  }

  private void startInputCheck() {
    inputExecutor.scheduleWithFixedDelay(
        messageService.getMessageCheckingRunnable(TimeUnit.SECONDS.toMillis(2), message -> {
          if (UPGRADING_DELEGATE.equals(message.getMessage())) {
            upgradeNeeded.set(false);
          } else if (DELEGATE_STOP_ACQUIRING.equals(message.getMessage())) {
            handleStopAcquiringMessage(message.getFromProcess());
          } else if (DELEGATE_RESUME.equals(message.getMessage())) {
            resume();
          } else if (DELEGATE_SEND_VERSION_HEADER.equals(message.getMessage())) {
            ManagerClientFactory.setSendVersionHeader(Boolean.valueOf(message.getParams().get(0)));
            managerClient = injector.getInstance(ManagerClient.class);
          }
        }), 0, 1, TimeUnit.SECONDS);
  }

  private void handleStopAcquiringMessage(String sender) {
    logger.info("Got stop-acquiring message from watcher {}", sender);
    if (acquireTasks.getAndSet(false)) {
      stoppedAcquiringAt = clock.millis();
      Map<String, Object> shutdownData = new HashMap<>();
      shutdownData.put(DELEGATE_SHUTDOWN_PENDING, true);
      shutdownData.put(DELEGATE_SHUTDOWN_STARTED, stoppedAcquiringAt);
      messageService.putAllData(DELEGATE_DASH + getProcessId(), shutdownData);

      systemExecutorService.submit(() -> {
        long started = clock.millis();
        long now = started;
        while (!currentlyExecutingTasks.isEmpty() && now - started < UPGRADE_TIMEOUT) {
          sleep(ofSeconds(1));
          now = clock.millis();
          logger.info("[Old] Completing {} tasks... ({} seconds elapsed): {}", currentlyExecutingTasks.size(),
              (now - started) / 1000L, currentlyExecutingTasks.keySet());
        }
        logger.info(now - started < UPGRADE_TIMEOUT ? "[Old] Delegate finished with tasks. Pausing"
                                                    : "[Old] Timed out waiting to complete tasks. Pausing");
        signalService.pause();
        logger.info("[Old] Shutting down");

        signalService.stop();
      });
    }
  }

  private void startUpgradeCheck(String version) {
    if (!delegateConfiguration.isDoUpgrade()) {
      logger.info("Auto upgrade is disabled in configuration");
      logger.info("Delegate stays on version: [{}]", version);
      return;
    }

    logger.info("Starting upgrade check at interval {} ms", delegateConfiguration.getHeartbeatIntervalMs());
    upgradeExecutor.scheduleWithFixedDelay(() -> {
      if (upgradePending.get()) {
        logger.info("[Old] Upgrade is pending...");
      } else {
        logger.info("Checking for upgrade");
        try {
          RestResponse<DelegateScripts> restResponse = timeLimiter.callWithTimeout(
              () -> execute(managerClient.getDelegateScripts(accountId, version)), 1L, TimeUnit.MINUTES, true);
          DelegateScripts delegateScripts = restResponse.getResource();
          if (delegateScripts.isDoUpgrade()) {
            upgradePending.set(true);

            upgradeStartedAt = clock.millis();
            Map<String, Object> upgradeData = new HashMap<>();
            upgradeData.put(DELEGATE_UPGRADE_PENDING, true);
            upgradeData.put(DELEGATE_UPGRADE_STARTED, upgradeStartedAt);
            messageService.putAllData(DELEGATE_DASH + getProcessId(), upgradeData);

            logger.info("[Old] Replace run scripts");
            replaceRunScripts(delegateScripts);
            logger.info("[Old] Run scripts downloaded. Upgrading delegate. Stop acquiring async tasks");
            upgradeVersion = delegateScripts.getVersion();
            upgradeNeeded.set(true);
          } else {
            logger.info("Delegate up to date");
          }
        } catch (UncheckedTimeoutException tex) {
          logger.warn("Timed out checking for upgrade");
        } catch (Exception e) {
          upgradePending.set(false);
          upgradeNeeded.set(false);
          acquireTasks.set(true);
          logger.error("Exception while checking for upgrade", e);
        }
      }
    }, 0, delegateConfiguration.getHeartbeatIntervalMs(), TimeUnit.MILLISECONDS);
  }

  private void startTaskPolling() {
    taskPollExecutor.scheduleAtFixedRate(this ::pollForTask, 0, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
  }

  private void pollForTask() {
    if (pollingForTasks.get()) {
      try {
        List<DelegateTaskEvent> taskEvents = timeLimiter.callWithTimeout(
            () -> execute(managerClient.pollTaskEvents(delegateId, accountId)), 15L, TimeUnit.SECONDS, true);
        if (isNotEmpty(taskEvents)) {
          logger.info("Processing DelegateTaskEvents {}", taskEvents);
          for (DelegateTaskEvent taskEvent : taskEvents) {
            try (TaskLogContext ignore = new TaskLogContext(taskEvent.getDelegateTaskId())) {
              if (taskEvent instanceof DelegateTaskAbortEvent) {
                abortDelegateTask((DelegateTaskAbortEvent) taskEvent);
              } else {
                dispatchDelegateTask(taskEvent);
              }
            }
          }
        }
      } catch (UncheckedTimeoutException tex) {
        logger.warn("Timed out fetching delegate task events");
      } catch (InterruptedException ie) {
        logger.warn("Delegate service is being shut down, this task is being interrupted.");
      } catch (Exception e) {
        logger.error("Exception while decoding task", e);
      }
    }
  }

  private void startHeartbeat(Builder builder, Socket socket) {
    logger.info("Starting heartbeat at interval {} ms", delegateConfiguration.getHeartbeatIntervalMs());
    heartbeatExecutor.scheduleAtFixedRate(() -> {
      try {
        systemExecutorService.submit(() -> {
          try {
            sendHeartbeat(builder, socket);
          } catch (Exception ex) {
            logger.error("Exception while sending heartbeat", ex);
          }
        });
      } catch (Exception e) {
        logger.error("Exception while scheduling heartbeat", e);
      }
    }, 0, delegateConfiguration.getHeartbeatIntervalMs(), TimeUnit.MILLISECONDS);
  }

  private void startHeartbeat() {
    logger.info("Starting heartbeat at interval {} ms", delegateConfiguration.getHeartbeatIntervalMs());
    heartbeatExecutor.scheduleAtFixedRate(() -> {
      if (pollingForTasks.get()) {
        try {
          systemExecutorService.submit(() -> {
            try {
              sendHeartbeat();
            } catch (Exception ex) {
              logger.error("Exception while sending heartbeat", ex);
            }
          });
        } catch (Exception e) {
          logger.error("Exception while scheduling heartbeat", e);
        }
      }
    }, 0, delegateConfiguration.getHeartbeatIntervalMs(), TimeUnit.MILLISECONDS);
  }

  private void startKeepAlivePacket(Builder builder, Socket socket) {
    if (!isEcsDelegate()) {
      return;
    }

    // Only perform for ECS delegate.
    logger.info("Starting KeepAlive Packet at interval {} ms", KEEP_ALIVE_INTERVAL);
    heartbeatExecutor.scheduleAtFixedRate(() -> {
      try {
        systemExecutorService.submit(() -> {
          try {
            sendKeepAlivePacket(builder, socket);
          } catch (Exception ex) {
            logger.error("Exception while sending KeepAlive Packet", ex);
          }
        });
      } catch (Exception e) {
        logger.error("Exception while scheduling KeepAlive Packet", e);
      }
    }, 0, KEEP_ALIVE_INTERVAL, TimeUnit.MILLISECONDS);
  }

  private void startLocalHeartbeat() {
    localHeartbeatExecutor.scheduleAtFixedRate(() -> {
      try {
        systemExecutorService.submit(() -> {
          Map<String, Object> statusData = new HashMap<>();
          if (selfDestruct.get()) {
            statusData.put(DELEGATE_SELF_DESTRUCT, true);
          } else {
            statusData.put(DELEGATE_HEARTBEAT, clock.millis());
            statusData.put(DELEGATE_VERSION, getVersion());
            statusData.put(DELEGATE_IS_NEW, false);
            statusData.put(DELEGATE_RESTART_NEEDED, doRestartDelegate());
            statusData.put(DELEGATE_UPGRADE_NEEDED, upgradeNeeded.get());
            statusData.put(DELEGATE_UPGRADE_PENDING, upgradePending.get());
            statusData.put(DELEGATE_SHUTDOWN_PENDING, !acquireTasks.get());
            if (upgradePending.get()) {
              statusData.put(DELEGATE_UPGRADE_STARTED, upgradeStartedAt);
            }
            if (!acquireTasks.get()) {
              statusData.put(DELEGATE_SHUTDOWN_STARTED, stoppedAcquiringAt);
            }
          }
          messageService.putAllData(DELEGATE_DASH + getProcessId(), statusData);
          watchWatcher();
        });
      } catch (Exception e) {
        logger.error("Exception while scheduling local heartbeat", e);
      }
      logCurrentTasks();
    }, 0, 10, TimeUnit.SECONDS);
  }

  private void watchWatcher() {
    long watcherHeartbeat =
        Optional.ofNullable(messageService.getData(WATCHER_DATA, WATCHER_HEARTBEAT, Long.class)).orElse(clock.millis());
    boolean heartbeatTimedOut = clock.millis() - watcherHeartbeat > WATCHER_HEARTBEAT_TIMEOUT;
    if (heartbeatTimedOut) {
      logger.warn("Watcher heartbeat not seen for {} seconds", WATCHER_HEARTBEAT_TIMEOUT / 1000L);
    }
    String watcherVersion = messageService.getData(WATCHER_DATA, WATCHER_VERSION, String.class);
    String expectedVersion = findExpectedWatcherVersion();
    if (StringUtils.equals(expectedVersion, watcherVersion)) {
      watcherVersionMatchedAt = clock.millis();
    }
    boolean versionMatchTimedOut = clock.millis() - watcherVersionMatchedAt > WATCHER_VERSION_MATCH_TIMEOUT;
    if (versionMatchTimedOut) {
      logger.warn("Watcher version mismatched for {} seconds. Version is {} but should be {}",
          WATCHER_VERSION_MATCH_TIMEOUT / 1000L, watcherVersion, expectedVersion);
    }

    boolean multiVersionRestartNeeded =
        multiVersion && clock.millis() - startTime > WATCHER_VERSION_MATCH_TIMEOUT && !new File(getVersion()).exists();

    if (heartbeatTimedOut || versionMatchTimedOut
        || (multiVersionRestartNeeded && multiVersionWatcherStarted.compareAndSet(false, true))) {
      String watcherProcess = messageService.getData(WATCHER_DATA, WATCHER_PROCESS, String.class);
      logger.warn("Watcher process {} needs restart", watcherProcess);
      systemExecutorService.submit(() -> {
        try {
          new ProcessExecutor().command("kill", "-9", watcherProcess).start();
          messageService.closeChannel(WATCHER, watcherProcess);
          sleep(ofSeconds(2));
          // Prevent a second restart attempt right away at next heartbeat by writing the watcher heartbeat and
          // resetting version matched timestamp
          messageService.putData(WATCHER_DATA, WATCHER_HEARTBEAT, clock.millis());
          watcherVersionMatchedAt = clock.millis();
          StartedProcess newWatcher = new ProcessExecutor()
                                          .command("nohup", "./start.sh")
                                          .redirectError(Slf4jStream.of("RestartWatcherScript").asError())
                                          .redirectOutput(Slf4jStream.of("RestartWatcherScript").asInfo())
                                          .readOutput(true)
                                          .setMessageLogger((log, format, arguments) -> log.info(format, arguments))
                                          .start();
          if (multiVersionRestartNeeded && newWatcher.getProcess().isAlive()) {
            sleep(ofSeconds(20L));
            FileUtils.forceDelete(new File("delegate.sh"));
            FileUtils.forceDelete(new File("delegate.jar"));
            restartNeeded.set(true);
          }
        } catch (Exception e) {
          logger.error(format("Error restarting watcher %s", watcherProcess), e);
        }
      });
    }
  }

  private String findExpectedWatcherVersion() {
    try {
      // TODO - if multiVersion, get versions from manager endpoint
      String watcherMetadata = getResponseFromUrl(delegateConfiguration.getWatcherCheckLocation());
      return substringBefore(watcherMetadata, " ").trim();
    } catch (IOException e) {
      logger.warn("Unable to fetch watcher version information", e);
      return null;
    }
  }

  private String getResponseFromUrl(String url) throws IOException {
    return Http.getResponseStringFromUrl(url, httpProxyHost, 10000, 10000);
  }

  private boolean doRestartDelegate() {
    long now = clock.millis();

    return new File("start.sh").exists()
        && (restartNeeded.get() || now - lastHeartbeatSentAt.get() > HEARTBEAT_TIMEOUT
               || now - lastHeartbeatReceivedAt.get() > HEARTBEAT_TIMEOUT);
  }

  private void sendHeartbeat(Builder builder, Socket socket) {
    if (socket.status() == STATUS.OPEN || socket.status() == STATUS.REOPENED) {
      logger.info("Sending heartbeat...");

      // This will Add ECS delegate specific fields if delegateType = "ECS"
      updateBuilderIfEcsDelegate(builder);
      try {
        timeLimiter.callWithTimeout(
            ()
                -> socket.fire(JsonUtils.asJson(
                    builder.but()
                        .withLastHeartBeat(clock.millis())
                        .withConnected(true)
                        .withCurrentlyExecutingDelegateTasks(Lists.newArrayList(currentlyExecutingTasks.values()))
                        .build())),
            15L, TimeUnit.SECONDS, true);
        lastHeartbeatSentAt.set(clock.millis());
      } catch (UncheckedTimeoutException ex) {
        logger.warn("Timed out sending heartbeat");
      } catch (Exception e) {
        logger.error("Error sending heartbeat", e);
      }
    } else {
      logger.warn("Socket is not open");
    }
  }

  private void sendKeepAlivePacket(Builder builder, Socket socket) {
    if (socket.status() == STATUS.OPEN || socket.status() == STATUS.REOPENED) {
      logger.info("Sending keepAlive packet...");
      updateBuilderIfEcsDelegate(builder);
      try {
        timeLimiter.callWithTimeout(
            ()
                -> socket.fire(JsonUtils.asJson(builder.but().withKeepAlivePacket(true).build())),
            15L, TimeUnit.SECONDS, true);
      } catch (UncheckedTimeoutException ex) {
        logger.warn("Timed out sending keep alive packet");
      } catch (Exception e) {
        logger.error("Error sending heartbeat", e);
      }
    } else {
      logger.warn("Socket is not open");
    }
  }

  private void sendHeartbeat() {
    logger.info("Sending heartbeat...");
    try {
      Delegate response = timeLimiter.callWithTimeout(
          () -> execute(managerClient.delegateHeartbeat(delegateId, accountId)), 15L, TimeUnit.SECONDS, true);
      if (delegateId.equals(response.getUuid())) {
        if (Status.DELETED.equals(response.getStatus())) {
          initiateSelfDestruct();
        }
        lastHeartbeatSentAt.set(clock.millis());
        lastHeartbeatReceivedAt.set(clock.millis());
      }

      timeLimiter.callWithTimeout(
          ()
              -> execute(managerClient.doConnectionHeartbeat(delegateId, accountId, connectionHeartbeat)),
          15L, TimeUnit.SECONDS, true);
      lastHeartbeatSentAt.set(clock.millis());

    } catch (UncheckedTimeoutException ex) {
      logger.warn("Timed out sending heartbeat");
    } catch (Exception e) {
      logger.error("Error sending heartbeat", e);
    }
  }

  private void logCurrentTasks() {
    logger.info("Currently validating tasks: {}", currentlyValidatingTasks.size());
    logger.info("Currently validating futures: {}", currentlyValidatingFutures.size());
    logger.info("Currently executing tasks: {}", currentlyExecutingTasks.size());
    logger.info("Currently executing futures: {}", currentlyExecutingFutures.size());
  }

  private void abortDelegateTask(DelegateTaskAbortEvent delegateTaskEvent) {
    logger.info("Aborting task {}", delegateTaskEvent);
    Optional.ofNullable(currentlyValidatingFutures.get(delegateTaskEvent.getDelegateTaskId()))
        .ifPresent(future -> future.cancel(true));
    currentlyValidatingTasks.remove(delegateTaskEvent.getDelegateTaskId());
    currentlyValidatingFutures.remove(delegateTaskEvent.getDelegateTaskId());
    logger.info("Removed {} from validating futures on abort", delegateTaskEvent.getDelegateTaskId());

    Optional.ofNullable(currentlyExecutingFutures.get(delegateTaskEvent.getDelegateTaskId()))
        .ifPresent(future -> future.cancel(true));
    currentlyExecutingTasks.remove(delegateTaskEvent.getDelegateTaskId());
    if (currentlyExecutingFutures.remove(delegateTaskEvent.getDelegateTaskId()) != null) {
      logger.info("Removed {} from executing futures on abort", delegateTaskEvent.getDelegateTaskId());
    }
  }

  private void dispatchDelegateTask(DelegateTaskEvent delegateTaskEvent) {
    logger.info("DelegateTaskEvent received - {}", delegateTaskEvent);

    String delegateTaskId = delegateTaskEvent.getDelegateTaskId();
    if (!acquireTasks.get()) {
      logger.info(
          "[Old] Upgraded process is running. Won't acquire task {} while completing other tasks", delegateTaskId);
      return;
    }

    if (upgradePending.get() && !delegateTaskEvent.isSync()) {
      logger.info("[Old] Upgrade pending, won't acquire async task {}", delegateTaskId);
      return;
    }

    if (delegateTaskId != null) {
      if (currentlyValidatingTasks.containsKey(delegateTaskId)) {
        logger.info("Task [DelegateTaskEvent: {}] already validating. Don't validate again", delegateTaskEvent);
        return;
      }

      if (currentlyExecutingTasks.containsKey(delegateTaskId)) {
        logger.info("Task [DelegateTaskEvent: {}] already acquired. Don't acquire again", delegateTaskEvent);
        return;
      }
    }

    try {
      // Delay response if already working on many tasks
      sleep(ofMillis(100 * Math.min(currentlyExecutingTasks.size() + currentlyValidatingTasks.size(), 10)));

      logger.info("Validating DelegateTask - uuid: {}, accountId: {}", delegateTaskId, accountId);

      DelegateTask delegateTask = execute(managerClient.acquireTask(delegateId, delegateTaskId, accountId));

      if (delegateTask == null) {
        logger.info("DelegateTask not available for validation - uuid: {}, accountId: {}", delegateTaskId,
            delegateTaskEvent.getAccountId());
        return;
      }

      if (isEmpty(delegateTask.getDelegateId())) {
        // Not whitelisted. Perform validation.
        DelegateValidateTask delegateValidateTask = TaskType.valueOf(delegateTask.getTaskType())
                                                        .getDelegateValidateTask(delegateId, delegateTask,
                                                            getPostValidationFunction(delegateTaskEvent, delegateTask));
        injector.injectMembers(delegateValidateTask);
        currentlyValidatingTasks.put(delegateTask.getUuid(), delegateTask);
        ExecutorService executorService = delegateTask.isAsync()
            ? asyncExecutorService
            : delegateTask.getTaskType().contains("BUILD") ? artifactExecutorService : syncExecutorService;
        currentlyValidatingFutures.put(delegateTask.getUuid(), executorService.submit(delegateValidateTask));
        logger.info("Task [{}] submitted for validation", delegateTask.getUuid());
      } else if (delegateId.equals(delegateTask.getDelegateId())) {
        // Whitelisted. Proceed immediately.
        logger.info("Delegate {} whitelisted for task {}, accountId: {}", delegateId, delegateTaskId, accountId);
        executeTask(delegateTask);
      }
    } catch (IOException e) {
      logger.error("Unable to get task for validation", e);
    }
  }

  private Consumer<List<DelegateConnectionResult>> getPostValidationFunction(
      DelegateTaskEvent delegateTaskEvent, @NotNull DelegateTask delegateTask) {
    return delegateConnectionResults -> {
      String taskId = delegateTask.getUuid();
      currentlyValidatingTasks.remove(taskId);
      currentlyValidatingFutures.remove(taskId);
      logger.info("Removed {} from validating futures on post validation", taskId);
      List<DelegateConnectionResult> results = Optional.ofNullable(delegateConnectionResults).orElse(emptyList());
      boolean validated = results.stream().anyMatch(DelegateConnectionResult::isValidated);
      logger.info("Validation {} for task {}", validated ? "succeeded" : "failed", taskId);
      try {
        DelegateTask delegateTaskPostValidation = execute(managerClient.reportConnectionResults(
            delegateId, delegateTaskEvent.getDelegateTaskId(), accountId, results));
        if (delegateTaskPostValidation != null && delegateId.equals(delegateTaskPostValidation.getDelegateId())) {
          logger.info("Got the go-ahead to proceed for task {}.", taskId);
          executeTask(delegateTaskPostValidation);
        } else {
          logger.info("Did not get the go-ahead to proceed for task {}", taskId);
          if (validated) {
            logger.info("Task {} validated but was not assigned", taskId);
          } else {
            int delay = POLL_INTERVAL_SECONDS + 3;
            logger.info("Waiting {} seconds to give other delegates a chance to validate task {}", delay, taskId);
            sleep(ofSeconds(delay));
            try {
              logger.info("Manager check whether to fail task {}", taskId);
              execute(
                  managerClient.failIfAllDelegatesFailed(delegateId, delegateTaskEvent.getDelegateTaskId(), accountId));
            } catch (IOException e) {
              logger.error(format("Unable to tell manager to check whether to fail for task %s", taskId), e);
            }
          }
        }
      } catch (IOException e) {
        logger.error(format("Unable to report validation results. Task %s", taskId), e);
      }
    };
  }

  private void executeTask(@NotNull DelegateTask delegateTask) {
    if (currentlyExecutingTasks.containsKey(delegateTask.getUuid())) {
      logger.info("Already executing task {}", delegateTask.getUuid());
      return;
    }
    logger.info("DelegateTask acquired - uuid: {}, accountId: {}, taskType: {}", delegateTask.getUuid(), accountId,
        delegateTask.getTaskType());
    DelegateRunnableTask delegateRunnableTask =
        TaskType.valueOf(delegateTask.getTaskType())
            .getDelegateRunnableTask(delegateId, delegateTask, getPostExecutionFunction(delegateTask),
                getPreExecutionFunction(delegateTask));
    injector.injectMembers(delegateRunnableTask);
    ExecutorService executorService = delegateTask.isAsync()
        ? asyncExecutorService
        : delegateTask.getTaskType().contains("BUILD") ? artifactExecutorService : syncExecutorService;
    Future taskFuture = executorService.submit(delegateRunnableTask);
    logger.info("Task future in executeTask: {} - done:{}, cancelled:{}", delegateTask.getUuid(), taskFuture.isDone(),
        taskFuture.isCancelled());
    currentlyExecutingFutures.put(delegateTask.getUuid(), taskFuture);
    timeoutEnforcementService.submit(() -> enforceDelegateTaskTimeout(delegateTask));
    logger.info("Task [{}] submitted for execution", delegateTask.getUuid());
  }

  private Supplier<Boolean> getPreExecutionFunction(@NotNull DelegateTask delegateTask) {
    return () -> {
      logger.info("Starting pre-execution for task {}", delegateTask.getUuid());
      if (!currentlyExecutingTasks.containsKey(delegateTask.getUuid())) {
        logger.info("Adding task {} to executing tasks", delegateTask.getUuid());
        currentlyExecutingTasks.put(delegateTask.getUuid(), delegateTask);
        return true;
      } else {
        logger.info("Task {} is already being executed", delegateTask.getUuid());
        return false;
      }
    };
  }

  private Consumer<DelegateTaskResponse> getPostExecutionFunction(@NotNull DelegateTask delegateTask) {
    return taskResponse -> {
      Response<ResponseBody> response = null;
      try {
        logger.info("Sending response for task {} to manager", delegateTask.getUuid());
        response = timeLimiter.callWithTimeout(() -> {
          Response<ResponseBody> resp = null;
          int retries = 3;
          while (retries-- > 0) {
            resp = managerClient.sendTaskStatus(delegateId, delegateTask.getUuid(), accountId, taskResponse).execute();
            if (resp != null && resp.code() >= 200 && resp.code() <= 299) {
              logger.info("Task {} response sent to manager", delegateTask.getUuid());
              return resp;
            } else {
              logger.warn("Response received for sent task {}: {}. {}", delegateTask.getUuid(),
                  resp == null ? "null" : resp.code(), retries > 0 ? "Retrying." : "Giving up.");
              sleep(ofMillis(200));
            }
          }
          return resp;
        }, 30L, TimeUnit.SECONDS, true);
      } catch (UncheckedTimeoutException ex) {
        logger.warn("Timed out sending response to manager");
      } catch (Exception e) {
        logger.error("Unable to send response to manager", e);
      } finally {
        currentlyExecutingTasks.remove(delegateTask.getUuid());
        if (currentlyExecutingFutures.remove(delegateTask.getUuid()) != null) {
          logger.info("Removed {} from executing futures on post execution", delegateTask.getUuid());
        }
        if (response != null && response.errorBody() != null && !response.isSuccessful()) {
          response.errorBody().close();
        }
        if (response != null && response.body() != null && response.isSuccessful()) {
          response.body().close();
        }
      }
    };
  }

  private void enforceDelegateTaskTimeout(DelegateTask delegateTask) {
    long startTime = clock.millis();
    boolean stillRunning = true;
    long timeout = delegateTask.getTimeout() + TimeUnit.SECONDS.toMillis(30L);
    Future taskFuture = null;
    while (stillRunning && clock.millis() - startTime < timeout) {
      logger.info("Task time remaining for {}: {} ms", delegateTask.getUuid(), startTime + timeout - clock.millis());
      sleep(ofSeconds(5));
      taskFuture = currentlyExecutingFutures.get(delegateTask.getUuid());
      if (taskFuture != null) {
        logger.info("Task future: {} - done:{}, cancelled:{}", delegateTask.getUuid(), taskFuture.isDone(),
            taskFuture.isCancelled());
      }
      stillRunning = taskFuture != null && !taskFuture.isDone() && !taskFuture.isCancelled();
    }
    if (stillRunning) {
      logger.error("Task {} timed out after {} milliseconds", delegateTask.getUuid(), timeout);
      Optional.ofNullable(currentlyExecutingFutures.get(delegateTask.getUuid()))
          .ifPresent(future -> future.cancel(true));
    }
    if (taskFuture != null) {
      try {
        timeLimiter.callWithTimeout(taskFuture::get, 5L, TimeUnit.SECONDS, true);
      } catch (UncheckedTimeoutException e) {
        logger.error("Timed out getting task future");
      } catch (CancellationException e) {
        logger.error("Task {} was cancelled", delegateTask.getUuid());
      } catch (Exception e) {
        logger.error("Error from task future {}", delegateTask.getUuid(), e);
      }
    }
    currentlyExecutingTasks.remove(delegateTask.getUuid());
    if (currentlyExecutingFutures.remove(delegateTask.getUuid()) != null) {
      logger.info("Removed {} from executing futures on timeout", delegateTask.getUuid());
    }
  }

  private void replaceRunScripts(DelegateScripts delegateScripts) throws IOException {
    for (String fileName : asList("start.sh", "stop.sh", "delegate.sh", "setup-proxy.sh")) {
      Files.deleteIfExists(Paths.get(fileName));
      File scriptFile = new File(fileName);
      String script = delegateScripts.getScriptByName(fileName);

      if (isNotEmpty(script)) {
        try (BufferedWriter writer = Files.newBufferedWriter(scriptFile.toPath())) {
          writer.write(script, 0, script.length());
          writer.flush();
        }
        logger.info("[Old] Done replacing file [{}]. Set User and Group permission", scriptFile);
        Files.setPosixFilePermissions(scriptFile.toPath(),
            Sets.newHashSet(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_EXECUTE,
                PosixFilePermission.OWNER_WRITE, PosixFilePermission.GROUP_READ, PosixFilePermission.OTHERS_READ));
        logger.info("[Old] Done setting file permissions");
      } else {
        logger.error("[Old] Script for file [{}] was not replaced", scriptFile);
      }
    }
  }

  private void cleanupOldDelegateVersionFromBackup() {
    try {
      cleanup(new File(System.getProperty("user.dir")), getVersion(), upgradeVersion, "backup.");
    } catch (Exception ex) {
      logger.error(format("Failed to clean delegate version [%s] from Backup", upgradeVersion), ex);
    }
  }

  private void removeDelegateVersionFromCapsule() {
    try {
      cleanup(new File(System.getProperty("capsule.dir")).getParentFile(), getVersion(), upgradeVersion, "delegate-");
    } catch (Exception ex) {
      logger.error(format("Failed to clean delegate version [%s] from Capsule", upgradeVersion), ex);
    }
  }

  private void cleanup(File dir, String currentVersion, String newVersion, String pattern) {
    FileUtils.listFilesAndDirs(dir, falseFileFilter(), FileFilterUtils.prefixFileFilter(pattern)).forEach(file -> {
      if (!dir.equals(file) && !file.getName().contains(currentVersion) && !file.getName().contains(newVersion)) {
        logger.info("[Old] File Name to be deleted = " + file.getAbsolutePath());
        FileUtils.deleteQuietly(file);
      }
    });
  }

  private String getVersion() {
    return versionInfoManager.getVersionInfo().getVersion();
  }

  private void initiateSelfDestruct() {
    logger.info("Self destruct sequence initiated...");
    acquireTasks.set(false);
    upgradePending.set(false);
    upgradeNeeded.set(false);
    restartNeeded.set(false);
    selfDestruct.set(true);
  }

  private void handleEcsDelegateSpecificMessage(String message) {
    if (isEcsDelegate()) {
      int indexForToken = message.lastIndexOf("[TOKEN]");
      int indexForSeqNum = message.lastIndexOf("[SEQ]");
      String token = message.substring(indexForToken + 7, indexForSeqNum);
      String sequenceNum = message.substring(indexForSeqNum + 5);

      // Did not receive correct data, skip updating token and sequence
      if (isInvalidData(token) || isInvalidData(sequenceNum)) {
        return;
      }

      try {
        FileIo.writeWithExclusiveLockAcrossProcesses(
            new StringBuilder(128).append("[TOKEN]").append(token).append("[SEQ]").append(sequenceNum).toString(),
            DELEGATE_SEQUENCE_CONFIG_FILE, StandardOpenOption.TRUNCATE_EXISTING);
      } catch (Exception e) {
        logger.error("Failed to write registration response into delegate_sequence file");
      }
    }
  }

  private boolean isInvalidData(String value) {
    if (isBlank(value) || "null".equalsIgnoreCase(value)) {
      return true;
    }

    return false;
  }

  private void updateBuilderIfEcsDelegate(Builder builder) {
    if (!isEcsDelegate()) {
      return;
    }

    builder.withDelegateGroupName(delegateGroupName);

    try {
      if (!FileIo.checkIfFileExist(DELEGATE_SEQUENCE_CONFIG_FILE)) {
        generateEcsDelegateSequenceConfigFile();
      }

      builder.withDelegateRandomToken(getRandomTokenForEcsDelegate());
      builder.withSequenceNum(getSequenceNumForEcsDelegate());
    } catch (Exception e) {
      logger.warn("Failed while reading seqNum and delegateToken from disk file");
    }
  }

  private String getSequenceConfigData() {
    String content = null;
    try {
      FileUtils.touch(new File(DELEGATE_SEQUENCE_CONFIG_FILE));

      if (isEcsDelegate()) {
        content = FileIo.getFileContentsWithSharedLockAcrossProcesses(DELEGATE_SEQUENCE_CONFIG_FILE);
      }
    } catch (Exception e) {
      content = null;
    }

    return isBlank(content) ? null : content;
  }

  private String getSequenceNumForEcsDelegate() {
    String content = null;
    String seqNum = null;
    if (isEcsDelegate()) {
      content = getSequenceConfigData();
      if (isNotBlank(content)) {
        int indexForSeqNum = content.lastIndexOf("[SEQ]");
        seqNum = content.substring(indexForSeqNum + 5);
        if (isBlank(seqNum)) {
          seqNum = null;
        }
      }
    }
    logger.info("SeqNum being sent to manager is: " + seqNum);
    return seqNum;
  }

  private String getRandomTokenForEcsDelegate() {
    if (!isEcsDelegate()) {
      return null;
    }

    String token = null;
    token = readTokenFromFile();

    if (token == null) {
      FileUtils.deleteQuietly(new File(DELEGATE_SEQUENCE_CONFIG_FILE));
      generateEcsDelegateSequenceConfigFile();
      token = readTokenFromFile();
    }
    logger.info("Token being sent to manager is: " + token);
    return token;
  }

  private String readTokenFromFile() {
    String token = null;
    String content;
    if (isEcsDelegate()) {
      content = getSequenceConfigData();
      if (isNotBlank(content)) {
        int indexForSeqNum = content.lastIndexOf("[SEQ]");
        token = content.substring(7, indexForSeqNum);
        if (isBlank(token)) {
          token = null;
        }
      }
    }
    return token;
  }

  private void handleEcsDelegateRegistrationResponse(Delegate delegate) {
    if (isEcsDelegate()) {
      try {
        FileIo.writeWithExclusiveLockAcrossProcesses(new StringBuilder()
                                                         .append("[TOKEN]")
                                                         .append(delegate.getDelegateRandomToken())
                                                         .append("[SEQ]")
                                                         .append(delegate.getSequenceNum())
                                                         .toString(),
            DELEGATE_SEQUENCE_CONFIG_FILE, StandardOpenOption.TRUNCATE_EXISTING);
      } catch (Exception e) {
        logger.error("Failed to write registration response into delegate_sequence file");
      }
    }
  }

  private boolean isEcsDelegate() {
    return "ECS".equals(delegateType);
  }

  private void generateEcsDelegateSequenceConfigFile() {
    try {
      FileUtils.touch(new File(DELEGATE_SEQUENCE_CONFIG_FILE));
      String randomToken = UUIDGenerator.generateUuid();
      FileIo.writeWithExclusiveLockAcrossProcesses(
          new StringBuilder(128).append("[TOKEN]").append(randomToken).append("[SEQ]").toString(),
          DELEGATE_SEQUENCE_CONFIG_FILE, StandardOpenOption.TRUNCATE_EXISTING);
    } catch (IOException e) {
      logger.warn("Failed to create DelegateSequenceConfigFile");
    }
  }
}
