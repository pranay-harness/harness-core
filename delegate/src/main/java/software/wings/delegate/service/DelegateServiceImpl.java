package software.wings.delegate.service;

import static io.harness.data.network.NetworkUtil.getLocalHostAddress;
import static io.harness.data.network.NetworkUtil.getLocalHostName;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.threading.Morpheus.sleep;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.apache.commons.io.filefilter.FileFilterUtils.falseFileFilter;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.Delegate.Builder.aDelegate;
import static software.wings.beans.DelegateTaskResponse.Builder.aDelegateTaskResponse;
import static software.wings.delegate.app.DelegateApplication.getProcessId;
import static software.wings.managerclient.ManagerClientFactory.TRUST_ALL_CERTS;
import static software.wings.managerclient.SafeHttpCall.execute;
import static software.wings.utils.message.MessageConstants.DELEGATE_DASH;
import static software.wings.utils.message.MessageConstants.DELEGATE_GO_AHEAD;
import static software.wings.utils.message.MessageConstants.DELEGATE_HEARTBEAT;
import static software.wings.utils.message.MessageConstants.DELEGATE_IS_NEW;
import static software.wings.utils.message.MessageConstants.DELEGATE_RESTART_NEEDED;
import static software.wings.utils.message.MessageConstants.DELEGATE_RESUME;
import static software.wings.utils.message.MessageConstants.DELEGATE_SHUTDOWN_PENDING;
import static software.wings.utils.message.MessageConstants.DELEGATE_SHUTDOWN_STARTED;
import static software.wings.utils.message.MessageConstants.DELEGATE_STARTED;
import static software.wings.utils.message.MessageConstants.DELEGATE_STOP_ACQUIRING;
import static software.wings.utils.message.MessageConstants.DELEGATE_UPGRADE_NEEDED;
import static software.wings.utils.message.MessageConstants.DELEGATE_UPGRADE_PENDING;
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
import okhttp3.ResponseBody;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.lang3.StringUtils;
import org.atmosphere.wasync.Client;
import org.atmosphere.wasync.ClientFactory;
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
import org.zeroturnaround.exec.stream.slf4j.Slf4jStream;
import retrofit2.Response;
import software.wings.beans.Delegate;
import software.wings.beans.Delegate.Builder;
import software.wings.beans.Delegate.Status;
import software.wings.beans.DelegateScripts;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskAbortEvent;
import software.wings.beans.DelegateTaskEvent;
import software.wings.beans.RestResponse;
import software.wings.beans.TaskType;
import software.wings.delegate.app.DelegateConfiguration;
import software.wings.delegatetasks.DelegateRunnableTask;
import software.wings.delegatetasks.validation.DelegateConnectionResult;
import software.wings.delegatetasks.validation.DelegateValidateTask;
import software.wings.http.ExponentialBackOff;
import software.wings.managerclient.ManagerClient;
import software.wings.security.TokenGenerator;
import software.wings.utils.JsonUtils;
import software.wings.utils.message.Message;
import software.wings.utils.message.MessageService;
import software.wings.waitnotify.NotifyResponseData;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.ConnectException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.validation.constraints.NotNull;

/**
 * Created by peeyushaggarwal on 11/29/16
 */
@Singleton
public class DelegateServiceImpl implements DelegateService {
  private static final int MAX_CONNECT_ATTEMPTS = 50;
  private static final int RECONNECT_INTERVAL_SECONDS = 10;
  private static final int POLL_INTERVAL_SECONDS = 3;
  private static final long UPGRADE_TIMEOUT = TimeUnit.HOURS.toMillis(2);
  private static final long HEARTBEAT_TIMEOUT = TimeUnit.MINUTES.toMillis(15);
  private static final long WATCHER_HEARTBEAT_TIMEOUT = TimeUnit.MINUTES.toMillis(10);
  private static final long WATCHER_VERSION_MATCH_TIMEOUT = TimeUnit.MINUTES.toMillis(2);

  private static String hostName;

  @Inject private DelegateConfiguration delegateConfiguration;
  @Inject private ManagerClient managerClient;
  @Inject @Named("heartbeatExecutor") private ScheduledExecutorService heartbeatExecutor;
  @Inject @Named("localHeartbeatExecutor") private ScheduledExecutorService localHeartbeatExecutor;
  @Inject @Named("upgradeExecutor") private ScheduledExecutorService upgradeExecutor;
  @Inject @Named("inputExecutor") private ScheduledExecutorService inputExecutor;
  @Inject @Named("taskPollExecutor") private ScheduledExecutorService taskPollExecutor;
  @Inject private ExecutorService executorService;
  @Inject private SignalService signalService;
  @Inject private MessageService messageService;
  @Inject private Injector injector;
  @Inject private TokenGenerator tokenGenerator;
  @Inject private AsyncHttpClient asyncHttpClient;
  @Inject private Clock clock;
  @Inject private TimeLimiter timeLimiter;

  private static final Logger logger = LoggerFactory.getLogger(DelegateServiceImpl.class);
  private final Object waiter = new Object();

  private final Map<String, DelegateTask> currentlyValidatingTasks = new ConcurrentHashMap<>();
  private final Map<String, DelegateTask> currentlyExecutingTasks = new ConcurrentHashMap<>();
  private final Map<String, Future<?>> currentlyExecutingFutures = new ConcurrentHashMap<>();

  private final AtomicLong lastHeartbeatSentAt = new AtomicLong(System.currentTimeMillis());
  private final AtomicLong lastHeartbeatReceivedAt = new AtomicLong(System.currentTimeMillis());
  private final AtomicBoolean upgradePending = new AtomicBoolean(false);
  private final AtomicBoolean upgradeNeeded = new AtomicBoolean(false);
  private final AtomicBoolean restartNeeded = new AtomicBoolean(false);
  private final AtomicBoolean acquireTasks = new AtomicBoolean(true);

  private Socket socket;
  private RequestBuilder request;
  private String upgradeVersion;
  private long stoppedAcquiringAt;
  private String delegateId;
  private String accountId;
  private long watcherVersionMatchedAt = System.currentTimeMillis();

  public static String getHostName() {
    return hostName;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void run(boolean watched) {
    try {
      hostName = getLocalHostName();

      accountId = delegateConfiguration.getAccountId();

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

      String proxyHost = System.getProperty("https.proxyHost");

      if (isNotBlank(proxyHost)) {
        logger.info("Using {} proxy {}:{}", System.getProperty("proxyScheme"), proxyHost,
            System.getProperty("https.proxyPort"));
      } else {
        logger.info("No proxy settings. Configure in proxy.config if needed");
      }

      long start = clock.millis();
      Delegate.Builder builder = aDelegate()
                                     .withIp(getLocalHostAddress())
                                     .withAccountId(accountId)
                                     .withHostName(hostName)
                                     .withVersion(getVersion())
                                     .withSupportedTaskTypes(Lists.newArrayList(TaskType.values()))
                                     .withIncludeScopes(new ArrayList<>())
                                     .withExcludeScopes(new ArrayList<>());

      delegateId = registerDelegate(builder);
      logger.info("[New] Delegate registered in {} ms", clock.millis() - start);

      SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("SSL");
      sslContext.init(null, TRUST_ALL_CERTS, new java.security.SecureRandom());

      if (delegateConfiguration.isPollForTasks()) {
        startTaskPolling();
        startHeartbeat();
      } else {
        Client client = ClientFactory.getDefault().newClient();
        ExecutorService fixedThreadPool = Executors.newFixedThreadPool(5);

        URI uri = new URI(delegateConfiguration.getManagerUrl());
        // Stream the request body
        RequestBuilder reqBuilder =
            client.newRequestBuilder()
                .method(METHOD.GET)
                .uri(uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort() + "/stream/delegate/" + accountId)
                .queryString("delegateId", delegateId)
                .queryString("token", tokenGenerator.getToken("https", "localhost", 9090, hostName))
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
                .reconnectAttempts(new File("delegate.sh").exists() ? MAX_CONNECT_ATTEMPTS : Integer.MAX_VALUE)
                .pauseBeforeReconnectInSeconds(RECONNECT_INTERVAL_SECONDS)
                .build();
        socket = client.create(clientOptions);
        socket
            .on(Event.MESSAGE,
                new Function<String>() { // Do not change this, wasync doesn't like lambdas
                  @Override
                  public void on(String message) {
                    handleMessageSubmit(message, fixedThreadPool);
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
      }

      startUpgradeCheck(getVersion());

      logger.info("Delegate started");

      synchronized (waiter) {
        waiter.wait();
      }

      messageService.closeData(DELEGATE_DASH + getProcessId());
      messageService.closeChannel(DELEGATE, getProcessId());

      if (upgradePending.get()) {
        removeDelegateVersionFromCapsule();
        cleanupOldDelegateVersionFromBackup();
      }

    } catch (Exception e) {
      logger.error("Exception while starting/running delegate", e);
    }
  }

  private void handleClose(Object o) {
    logger.info("Event:{}, message:[{}]", Event.CLOSE.name(), o.toString());
  }

  private void handleReopened(Object o, Builder builder) {
    logger.info("Event:{}, message:[{}]", Event.REOPENED.name(), o.toString());
    try {
      socket.fire(
          builder.but().withLastHeartBeat(clock.millis()).withStatus(Status.ENABLED).withConnected(true).build());
    } catch (IOException e) {
      logger.error("Error connecting", e);
      e.printStackTrace();
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
        ExponentialBackOff.executeForEver(() -> socket.open(request.build()));
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

  private void handleMessageSubmit(String message, ExecutorService fixedThreadPool) {
    fixedThreadPool.submit(() -> handleMessage(message));
  }

  private void handleMessage(String message) {
    if (StringUtils.startsWith(message, "[X]")) {
      String receivedId = message.substring(3); // Remove the "[X]"
      if (delegateId.equals(receivedId)) {
        logger.info("Delegate {} received heartbeat response", receivedId);
        lastHeartbeatReceivedAt.set(clock.millis());
      } else {
        logger.info("Delegate {} received heartbeat response for another delegate, {}", delegateId, receivedId);
      }
    } else if (!StringUtils.equals(message, "X")) {
      logger.info("Executing: Event:{}, message:[{}]", Event.MESSAGE.name(), message);
      try {
        DelegateTaskEvent delegateTaskEvent = JsonUtils.asObject(message, DelegateTaskEvent.class);
        if (delegateTaskEvent instanceof DelegateTaskAbortEvent) {
          abortDelegateTask((DelegateTaskAbortEvent) delegateTaskEvent);
        } else {
          dispatchDelegateTask(delegateTaskEvent);
        }
      } catch (Exception e) {
        System.out.println(message);
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
        ExponentialBackOff.executeForEver(() -> socket.open(request.build()));
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

  private String registerDelegate(Builder builder) throws IOException {
    AtomicInteger attempts = new AtomicInteger(0);
    while (true) {
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
      String delegateId = delegateResponse.getResource().getUuid();
      builder.withUuid(delegateId).withStatus(delegateResponse.getResource().getStatus());
      logger.info(
          "Delegate registered with id {} and status {}", delegateId, delegateResponse.getResource().getStatus());
      return delegateId;
    }
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

      executorService.submit(() -> {
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
          RestResponse<DelegateScripts> restResponse =
              execute(managerClient.checkForUpgrade(version, delegateId, accountId));
          DelegateScripts delegateScripts = restResponse.getResource();
          if (delegateScripts.isDoUpgrade()) {
            upgradePending.set(true);
            logger.info("[Old] Replace run scripts");
            replaceRunScripts(delegateScripts);
            logger.info("[Old] Run scripts downloaded. Upgrading delegate. Stop acquiring async tasks");
            upgradeVersion = delegateScripts.getVersion();
            upgradeNeeded.set(true);
          } else {
            logger.info("Delegate up to date");
          }
        } catch (Exception e) {
          upgradePending.set(false);
          upgradeNeeded.set(false);
          acquireTasks.set(true);
          logger.error("[Old] Exception while checking for upgrade", e);
        }
      }
    }, 0, delegateConfiguration.getHeartbeatIntervalMs(), TimeUnit.MILLISECONDS);
  }

  private void startTaskPolling() {
    taskPollExecutor.scheduleAtFixedRate(this ::pollForTask, 0, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
  }

  private void pollForTask() {
    try {
      List<DelegateTaskEvent> taskEvents = timeLimiter.callWithTimeout(
          () -> execute(managerClient.pollTaskEvents(delegateId, accountId)), 15L, TimeUnit.SECONDS, true);
      if (isNotEmpty(taskEvents)) {
        logger.info("Processing DelegateTaskEvents {}", taskEvents);
        for (DelegateTaskEvent taskEvent : taskEvents) {
          if (taskEvent instanceof DelegateTaskAbortEvent) {
            abortDelegateTask((DelegateTaskAbortEvent) taskEvent);
          } else {
            dispatchDelegateTask(taskEvent);
          }
        }
      }
    } catch (UncheckedTimeoutException tex) {
      logger.warn("Timed out fetching delegate task events");
    } catch (Exception e) {
      logger.error("Exception while decoding task", e);
    }
  }

  private void startHeartbeat(Builder builder, Socket socket) {
    logger.info("Starting heartbeat at interval {} ms", delegateConfiguration.getHeartbeatIntervalMs());
    heartbeatExecutor.scheduleAtFixedRate(()
                                              -> executorService.submit(() -> {
      try {
        sendHeartbeat(builder, socket);
      } catch (Exception ex) {
        logger.error("Exception while sending heartbeat", ex);
      }
    }),
        0, delegateConfiguration.getHeartbeatIntervalMs(), TimeUnit.MILLISECONDS);
  }

  private void startHeartbeat() {
    logger.info("Starting heartbeat at interval {} ms", delegateConfiguration.getHeartbeatIntervalMs());
    heartbeatExecutor.scheduleAtFixedRate(()
                                              -> executorService.submit(() -> {
      try {
        sendHeartbeat();
      } catch (Exception ex) {
        logger.error("Exception while sending heartbeat", ex);
      }
    }),
        0, delegateConfiguration.getHeartbeatIntervalMs(), TimeUnit.MILLISECONDS);
  }

  private void startLocalHeartbeat() {
    localHeartbeatExecutor.scheduleAtFixedRate(()
                                                   -> executorService.submit(() -> {
      Map<String, Object> statusData = new HashMap<>();
      statusData.put(DELEGATE_HEARTBEAT, clock.millis());
      statusData.put(DELEGATE_VERSION, getVersion());
      statusData.put(DELEGATE_IS_NEW, false);
      statusData.put(DELEGATE_RESTART_NEEDED, doRestartDelegate());
      statusData.put(DELEGATE_UPGRADE_NEEDED, upgradeNeeded.get());
      statusData.put(DELEGATE_UPGRADE_PENDING, upgradePending.get());
      statusData.put(DELEGATE_SHUTDOWN_PENDING, !acquireTasks.get());
      if (!acquireTasks.get()) {
        statusData.put(DELEGATE_SHUTDOWN_STARTED, stoppedAcquiringAt);
      }
      messageService.putAllData(DELEGATE_DASH + getProcessId(), statusData);
      watchWatcher();
    }),
        0, 10, TimeUnit.SECONDS);
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

    if (heartbeatTimedOut || versionMatchTimedOut) {
      String watcherProcess = messageService.getData(WATCHER_DATA, WATCHER_PROCESS, String.class);
      logger.warn("Watcher process {} needs restart", watcherProcess);

      executorService.submit(() -> {
        try {
          new ProcessExecutor().timeout(5, TimeUnit.SECONDS).command("kill", "-9", watcherProcess).start();
          messageService.closeChannel(WATCHER, watcherProcess);
          Thread.sleep(TimeUnit.SECONDS.toMillis(2));
          // Prevent a second restart attempt right away at next heartbeat by writing the watcher heartbeat and
          // resetting version matched timestamp
          messageService.putData(WATCHER_DATA, WATCHER_HEARTBEAT, clock.millis());
          watcherVersionMatchedAt = clock.millis();
          new ProcessExecutor()
              .timeout(1, TimeUnit.MINUTES)
              .command("nohup", "./start.sh")
              .redirectError(Slf4jStream.of("RestartWatcherScript").asError())
              .redirectOutput(Slf4jStream.of("RestartWatcherScript").asInfo())
              .readOutput(true)
              .setMessageLogger((log, format, arguments) -> log.info(format, arguments))
              .start();
        } catch (Exception e) {
          logger.error("Error restarting watcher {}", watcherProcess, e);
        }
      });
    }
  }

  private String findExpectedWatcherVersion() {
    String watcherVersionPrefix = "REMOTE_WATCHER_VERSION=";
    try {
      return FileUtils.readLines(new File("start.sh"), UTF_8)
          .stream()
          .filter(line -> StringUtils.startsWith(line, watcherVersionPrefix))
          .map(line -> line.substring(watcherVersionPrefix.length()))
          .findFirst()
          .orElse(null);
    } catch (Exception e) {
      logger.error("Error reading start script.", e);
      return null;
    }
  }

  private boolean doRestartDelegate() {
    long now = clock.millis();
    return new File("delegate.sh").exists()
        && (restartNeeded.get() || now - lastHeartbeatSentAt.get() > HEARTBEAT_TIMEOUT
               || now - lastHeartbeatReceivedAt.get() > HEARTBEAT_TIMEOUT);
  }

  private void sendHeartbeat(Builder builder, Socket socket) throws IOException {
    logger.debug("sending heartbeat...");
    if (socket.status() == STATUS.OPEN || socket.status() == STATUS.REOPENED) {
      socket.fire(JsonUtils.asJson(
          builder.but()
              .withLastHeartBeat(clock.millis())
              .withConnected(true)
              .withCurrentlyExecutingDelegateTasks(Lists.newArrayList(currentlyExecutingTasks.values()))
              .build()));
      lastHeartbeatSentAt.set(clock.millis());
    }
  }

  private void sendHeartbeat() throws IOException {
    logger.debug("sending heartbeat...");
    Delegate response = execute(managerClient.delegateHeartbeat(delegateId, accountId));
    if (delegateId.equals(response.getUuid())) {
      lastHeartbeatSentAt.set(clock.millis());
      lastHeartbeatReceivedAt.set(clock.millis());
    }
  }

  private void abortDelegateTask(DelegateTaskAbortEvent delegateTaskEvent) {
    logger.info("Aborting task {}", delegateTaskEvent);
    Optional.ofNullable(currentlyExecutingFutures.get(delegateTaskEvent.getDelegateTaskId()))
        .ifPresent(future -> future.cancel(true));
    currentlyExecutingTasks.remove(delegateTaskEvent.getDelegateTaskId());
    currentlyExecutingFutures.remove(delegateTaskEvent.getDelegateTaskId());
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
      logger.info("Validating DelegateTask - uuid: {}, accountId: {}", delegateTaskId, accountId);

      DelegateTask delegateTask = execute(managerClient.acquireTask(delegateId, delegateTaskId, accountId));

      if (delegateTask == null) {
        logger.info("DelegateTask not available for validation - uuid: {}, accountId: {}", delegateTaskId,
            delegateTaskEvent.getAccountId());
        logger.info("Currently validating tasks: {}", currentlyValidatingTasks.keySet());
        logger.info("Currently executing tasks: {}", currentlyExecutingTasks.keySet());
        logger.info("Currently executing futures: {}", currentlyExecutingFutures.keySet());
        return;
      }

      if (isEmpty(delegateTask.getDelegateId())) {
        // Not whitelisted. Perform validation.
        DelegateValidateTask delegateValidateTask = delegateTask.getTaskType().getDelegateValidateTask(
            delegateId, delegateTask, getPostValidationFunction(delegateTaskEvent, delegateTask));
        injector.injectMembers(delegateValidateTask);
        currentlyValidatingTasks.put(delegateTask.getUuid(), delegateTask);
        currentlyExecutingFutures.put(delegateTask.getUuid(), executorService.submit(delegateValidateTask));
        logger.info("Task [{}] submitted for validation", delegateTask.getUuid());
      } else if (delegateId.equals(delegateTask.getDelegateId())) {
        // Whitelisted. Proceed immediately.
        logger.info("Delegate {} whitelisted for task {}, accountId: {}", delegateId, delegateTaskId, accountId);
        executeTask(delegateTaskEvent, delegateTask);
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
      currentlyExecutingFutures.remove(taskId);
      List<DelegateConnectionResult> results = Optional.ofNullable(delegateConnectionResults).orElse(emptyList());
      boolean validated = results.stream().anyMatch(DelegateConnectionResult::isValidated);
      logger.info("Validation {} for task {}", validated ? "succeeded" : "failed", taskId);
      try {
        DelegateTask delegateTask1 = execute(managerClient.reportConnectionResults(
            delegateId, delegateTaskEvent.getDelegateTaskId(), accountId, results));
        if (delegateTask1 != null && delegateId.equals(delegateTask1.getDelegateId())) {
          logger.info("Got the go-ahead to proceed for task {}.", taskId);
          executeTask(delegateTaskEvent, delegateTask1);
        } else {
          logger.info("Did not get the go-ahead to proceed for task {}", taskId);
          if (validated) {
            logger.info("Task {} validated but was not assigned", taskId);
          } else {
            int delay = POLL_INTERVAL_SECONDS + 2;
            logger.info("Waiting {} seconds to give other delegates a chance to register as validators for task {}",
                delay, taskId);
            sleep(ofSeconds(delay));
            try {
              logger.info("Checking whether all delegates failed for task {}", taskId);
              DelegateTask delegateTask2 = execute(
                  managerClient.shouldProceedAnyway(delegateId, delegateTaskEvent.getDelegateTaskId(), accountId));
              if (delegateTask2 != null && delegateId.equals(delegateTask2.getDelegateId())) {
                logger.info("All delegates failed. Proceeding anyway to get proper failure for task {}", taskId);
                executeTask(delegateTaskEvent, delegateTask2);
              } else {
                logger.info("Did not get go-ahead for task {}, giving up", taskId);
              }
            } catch (IOException e) {
              logger.error("Unable to check whether to proceed. Task {}", taskId, e);
            }
          }
        }
      } catch (IOException e) {
        logger.error("Unable to report validation results. Task {}", taskId, e);
      }
    };
  }

  private void executeTask(DelegateTaskEvent delegateTaskEvent, @NotNull DelegateTask delegateTask) {
    logger.info("DelegateTask acquired - uuid: {}, accountId: {}, taskType: {}", delegateTask.getUuid(), accountId,
        delegateTask.getTaskType());
    DelegateRunnableTask delegateRunnableTask = delegateTask.getTaskType().getDelegateRunnableTask(delegateId,
        delegateTask, getPostExecutionFunction(delegateTask), getPreExecutionFunction(delegateTaskEvent, delegateTask));
    injector.injectMembers(delegateRunnableTask);
    currentlyExecutingFutures.put(delegateTask.getUuid(), executorService.submit(delegateRunnableTask));
    executorService.submit(() -> enforceDelegateTaskTimeout(delegateTask));
    logger.info("Task [{}] submitted for execution", delegateTask.getUuid());
  }

  private Supplier<Boolean> getPreExecutionFunction(
      DelegateTaskEvent delegateTaskEvent, @NotNull DelegateTask delegateTask) {
    return () -> {
      try {
        DelegateTask delegateTask1 =
            execute(managerClient.startTask(delegateId, delegateTaskEvent.getDelegateTaskId(), accountId));
        boolean taskAcquired = delegateTask1 != null;
        if (taskAcquired) {
          if (currentlyExecutingTasks.containsKey(delegateTask.getUuid())) {
            logger.error("Delegate task {} already in executing tasks for this delegate", delegateTask.getUuid());
            return false;
          }
          currentlyExecutingTasks.put(delegateTask.getUuid(), delegateTask1);
        }
        return taskAcquired;
      } catch (IOException e) {
        logger.error("Unable to update task status on manager", e);
        return false;
      }
    };
  }

  private Consumer<NotifyResponseData> getPostExecutionFunction(@NotNull DelegateTask delegateTask) {
    return notifyResponseData -> {
      Response<ResponseBody> response = null;
      try {
        response = managerClient
                       .sendTaskStatus(delegateId, delegateTask.getUuid(), accountId,
                           aDelegateTaskResponse()
                               .withTask(delegateTask)
                               .withAccountId(accountId)
                               .withResponse(notifyResponseData)
                               .build())
                       .execute();
        logger.info("Task [{}] response sent to manager", delegateTask.getUuid());
      } catch (IOException e) {
        logger.error("Unable to send response to manager", e);
      } finally {
        currentlyExecutingTasks.remove(delegateTask.getUuid());
        currentlyExecutingFutures.remove(delegateTask.getUuid());
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
    while (stillRunning && clock.millis() - startTime < timeout) {
      sleep(ofSeconds(5));
      Future taskFuture = currentlyExecutingFutures.get(delegateTask.getUuid());
      stillRunning = taskFuture != null && !taskFuture.isDone() && !taskFuture.isCancelled();
    }
    if (stillRunning) {
      logger.error("Task {} timed out after {} milliseconds", delegateTask.getUuid(), timeout);
      Optional.ofNullable(currentlyExecutingFutures.get(delegateTask.getUuid()))
          .ifPresent(future -> future.cancel(true));
    }
    currentlyExecutingTasks.remove(delegateTask.getUuid());
    currentlyExecutingFutures.remove(delegateTask.getUuid());
  }

  private void replaceRunScripts(DelegateScripts delegateScripts) throws IOException {
    for (String fileName : asList("start.sh", "stop.sh", "delegate.sh")) {
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
      logger.error(String.format("Failed to clean delegate version [%s] from Backup", upgradeVersion), ex);
    }
  }

  private void removeDelegateVersionFromCapsule() {
    try {
      cleanup(new File(System.getProperty("capsule.dir")).getParentFile(), getVersion(), upgradeVersion, "delegate-");
    } catch (Exception ex) {
      logger.error(String.format("Failed to clean delegate version [%s] from Capsule", upgradeVersion), ex);
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
    return System.getProperty("version", "1.0.0-DEV");
  }
}
