package software.wings.delegate.service;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.notNullValue;
import static software.wings.beans.Delegate.Builder.aDelegate;
import static software.wings.beans.DelegateTaskResponse.Builder.aDelegateTaskResponse;
import static software.wings.managerclient.ManagerClientFactory.TRUST_ALL_CERTS;
import static software.wings.managerclient.SafeHttpCall.execute;

import com.google.common.collect.Lists;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import com.ning.http.client.AsyncHttpClient;
import okhttp3.ResponseBody;
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
import org.awaitility.Duration;
import org.awaitility.core.ConditionTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import software.wings.exception.WingsException;
import software.wings.http.ExponentialBackOff;
import software.wings.managerclient.ManagerClient;
import software.wings.managerclient.TokenGenerator;
import software.wings.utils.JsonUtils;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

/**
 * Created by peeyushaggarwal on 11/29/16.
 */
@Singleton
public class DelegateServiceImpl implements DelegateService {
  static final int MAX_UPGRADE_WAIT_SECS = 2 * 60 * 60; // 2 hours max
  private static final int MAX_CONNECT_ATTEMPTS = 100;
  private static final int CONNECT_INTERVAL_SECONDS = 10;
  private static final long MAX_HB_TIMEOUT = TimeUnit.MINUTES.toMillis(15);
  private final Logger logger = LoggerFactory.getLogger(DelegateServiceImpl.class);
  private final Object waiter = new Object();
  @Inject private DelegateConfiguration delegateConfiguration;
  @Inject private ManagerClient managerClient;
  @Inject @Named("heartbeatExecutor") private ScheduledExecutorService heartbeatExecutor;
  @Inject @Named("upgradeExecutor") private ScheduledExecutorService upgradeExecutor;
  @Inject private ExecutorService executorService;
  @Inject private UpgradeService upgradeService;
  @Inject private Injector injector;
  @Inject private TokenGenerator tokenGenerator;
  @Inject private AsyncHttpClient asyncHttpClient;
  private final ConcurrentHashMap<String, DelegateTask> currentlyExecutingTasks = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Future<?>> currentlyExecutingFutures = new ConcurrentHashMap<>();
  private static long lastHeartbeatSentAt = System.currentTimeMillis();
  private static long lastHeartbeatReceivedAt = System.currentTimeMillis();

  private Socket socket;
  private RequestBuilder request;
  private final List<Boolean> upgradePending = new ArrayList<>();
  private String delegateId;
  private String accountId;

  @Override
  public void run(boolean upgrade, boolean restart) {
    try {
      String ip = InetAddress.getLocalHost().getHostAddress();
      String hostName = InetAddress.getLocalHost().getHostName();
      accountId = delegateConfiguration.getAccountId();

      if (upgrade) {
        logger.info("[New] Upgraded delegate process started. Sending confirmation.");
        System.out.println("botstarted"); // Don't remove this. It is used as message in upgrade flow.

        logger.info("[New] Waiting for go ahead from old delegate.");
        int secs = 0;
        File goaheadFile = new File("goahead");
        while (!goaheadFile.exists() && secs++ < MAX_UPGRADE_WAIT_SECS) {
          logger.info("[New] Waiting for go ahead... ({} seconds elapsed)", secs);
          Thread.sleep(1000);
        }

        if (secs < MAX_UPGRADE_WAIT_SECS) {
          logger.info("[New] Go ahead received from old delegate. Sending confirmation.");
        } else {
          logger.info("[New] Timed out waiting for go ahead. Proceeding anyway.");
        }
        System.out.println("proceeding"); // Don't remove this. It is used as message in upgrade flow.
      } else if (restart) {
        logger.info("[New] Restarted delegate process started");
      } else {
        logger.info("Delegate process started");
      }

      URI uri = new URI(delegateConfiguration.getManagerUrl());

      long start = System.currentTimeMillis();
      Delegate.Builder builder = aDelegate()
                                     .withIp(ip)
                                     .withAccountId(accountId)
                                     .withHostName(hostName)
                                     .withVersion(getVersion())
                                     .withSupportedTaskTypes(Lists.newArrayList(TaskType.values()))
                                     .withIncludeScopes(new ArrayList<>())
                                     .withExcludeScopes(new ArrayList<>());

      delegateId = registerDelegate(builder);
      logger.info("[New] Delegate registered in {} ms", (System.currentTimeMillis() - start));

      SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("SSL");
      sslContext.init(null, TRUST_ALL_CERTS, new java.security.SecureRandom());

      Client client = ClientFactory.getDefault().newClient();
      ExecutorService fixedThreadPool = Executors.newFixedThreadPool(5);

      // Stream the request body
      request =
          client.newRequestBuilder()
              .method(METHOD.GET)
              .uri(uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort() + "/stream/delegate/" + accountId)
              .queryString("delegateId", delegateId)
              .queryString("token", tokenGenerator.getToken("https", "localhost", 9090))
              .header("Version", getVersion())
              .encoder(new Encoder<Delegate, Reader>() { // Do not change this wasync doesn't like lambda's
                @Override
                public Reader encode(Delegate s) {
                  return new StringReader(JsonUtils.asJson(s));
                }
              })
              .transport(TRANSPORT.WEBSOCKET);

      Options clientOptions = client.newOptionsBuilder()
                                  .runtime(asyncHttpClient, true)
                                  .reconnect(true)
                                  .reconnectAttempts(MAX_CONNECT_ATTEMPTS)
                                  .pauseBeforeReconnectInSeconds(CONNECT_INTERVAL_SECONDS)
                                  .build();
      socket = client.create(clientOptions);
      socket
          .on(Event.MESSAGE,
              new Function<String>() { // Do not change this wasync doesn't like lambda's
                @Override
                public void on(String message) {
                  handleMessageSubmit(message, fixedThreadPool);
                }
              })
          .on(Event.ERROR,
              new Function<Exception>() { // Do not change this wasync doesn't like lambda's
                @Override
                public void on(Exception e) {
                  handleError(e);
                }
              })
          .on(Event.REOPENED,
              new Function<Object>() { // Do not change this wasync doesn't like lambda's
                @Override
                public void on(Object o) {
                  handleReopened(o, builder);
                }
              })
          .on(Event.CLOSE, new Function<Object>() { // Do not change this wasync doesn't like lambda's
            @Override
            public void on(Object o) {
              handleClose(o);
            }
          });

      logger.info("[New] Setting delegate {} upgrade pending: {}", delegateId, false);
      upgradePending.add(false);
      execute(managerClient.setUpgradePending(delegateId, accountId, false));
      socket.open(request.build());

      startHeartbeat(builder, socket);

      startUpgradeCheck(getVersion());

      if (upgrade) {
        logger.info("[New] Delegate upgraded.");
      } else if (restart) {
        logger.info("[New] Delegate restarted.");
      } else {
        logger.info("Delegate started.");
      }

      synchronized (waiter) {
        waiter.wait();
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
      socket.fire(builder.but()
                      .withLastHeartBeat(System.currentTimeMillis())
                      .withStatus(Status.ENABLED)
                      .withConnected(true)
                      .build());
    } catch (IOException e) {
      logger.error("Error connecting", e);
      e.printStackTrace();
    }
  }

  private void handleError(Exception e) {
    logger.info("Event:{}, message:[{}]", Event.ERROR.name(), e.getMessage());
    if (e instanceof SSLException) {
      logger.info("Reopening connection to manager.");
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
      logger.warn("Failed to connect after {} attempts. Restarting delegate.", MAX_CONNECT_ATTEMPTS);
      restartDelegate();
    } else {
      logger.error("Exception: " + e.getMessage(), e);
      try {
        socket.close();
      } catch (Exception ex) {
        // Ignore
      }
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
        lastHeartbeatReceivedAt = System.currentTimeMillis();
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
    socket.close();
  }

  @Override
  public void resume() {
    try {
      ExponentialBackOff.executeForEver(() -> socket.open(request.build()));
      logger.info("[Old] Setting delegate {} upgrade pending: {}", delegateId, false);
      upgradePending.set(0, false);
      execute(managerClient.setUpgradePending(accountId, delegateId, false));
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
    try {
      List<Integer> attempts = new ArrayList<>();
      attempts.add(0);
      return await().with().timeout(Duration.FOREVER).pollInterval(Duration.FIVE_SECONDS).until(() -> {
        RestResponse<Delegate> delegateResponse;
        try {
          attempts.set(0, attempts.get(0) + 1);
          String attemptString = attempts.get(0) > 1 ? " (Attempt " + attempts.get(0) + ")" : "";
          logger.info("Registering delegate." + attemptString);
          delegateResponse = execute(managerClient.registerDelegate(accountId,
              builder.but().withLastHeartBeat(System.currentTimeMillis()).withStatus(Status.ENABLED).build()));
        } catch (Exception e) {
          String msg = "Unknown error occurred while registering Delegate [" + accountId + "] with manager";
          logger.error(msg, e);
          Thread.sleep(55000);
          return null;
        }
        if (delegateResponse == null || delegateResponse.getResource() == null) {
          logger.error(
              "Error occurred while registering elegate with manager for account {}. Please see the manager log for more information.",
              accountId);
          Thread.sleep(55000);
          return null;
        }
        builder.withUuid(delegateResponse.getResource().getUuid())
            .withStatus(delegateResponse.getResource().getStatus());
        logger.info("Delegate registered with id {} and status {} ", delegateResponse.getResource().getUuid(),
            delegateResponse.getResource().getStatus());
        return delegateResponse.getResource().getUuid();
      }, notNullValue());
    } catch (ConditionTimeoutException e) {
      String msg = "Timeout occurred while registering Delegate [" + accountId + "] with manager";
      logger.error(msg, e);
      throw new WingsException(msg, e);
    }
  }

  private void restartDelegate() {
    try {
      logger.info("Restarting delegate");
      upgradeService.doRestart();
    } catch (Exception ex) {
      logger.error("Exception while restarting", ex);
    } finally {
      // Reset timeout so that next attempt is made after 15 minutes
      lastHeartbeatSentAt = System.currentTimeMillis();
      lastHeartbeatReceivedAt = System.currentTimeMillis();
    }
  }

  private void startUpgradeCheck(String version) {
    if (!delegateConfiguration.isDoUpgrade()) {
      logger.info("Auto upgrade is disabled in configuration.");
      logger.info("Delegate stays on version: [{}]", version);
      return;
    }

    logger.info("Starting upgrade check at interval {} ms", delegateConfiguration.getHeartbeatIntervalMs());
    upgradeExecutor.scheduleWithFixedDelay(() -> {
      if (upgradePending.get(0)) {
        logger.info("[Old] Upgrade is pending...");
      } else {
        logger.info("Checking for upgrade");
        try {
          RestResponse<DelegateScripts> restResponse =
              execute(managerClient.checkForUpgrade(version, delegateId, accountId));
          if (restResponse.getResource().isDoUpgrade()) {
            logger.info("[Old] Setting delegate {} upgrade pending: {}", delegateId, true);
            upgradePending.set(0, true);
            execute(managerClient.setUpgradePending(delegateId, accountId, true));
            logger.info("[Old] Upgrading delegate...");
            upgradeService.doUpgrade(restResponse.getResource(), getVersion());
          } else {
            logger.info("Delegate up to date");
          }
        } catch (Exception e) {
          try {
            logger.info("[Old] Setting delegate {} upgrade pending: {}", delegateId, false);
            upgradePending.set(0, false);
            execute(managerClient.setUpgradePending(delegateId, accountId, false));
          } catch (IOException e1) {
            logger.error("[Old] Exception while setting upgrade pending", e1);
          }
          logger.error("[Old] Exception while checking for upgrade", e);
        }
      }
    }, 0, delegateConfiguration.getHeartbeatIntervalMs(), TimeUnit.MILLISECONDS);
  }

  public long getRunningTaskCount() {
    return currentlyExecutingTasks.size();
  }

  private void startHeartbeat(Builder builder, Socket socket) {
    logger.info("Starting heartbeat at interval {} ms", delegateConfiguration.getHeartbeatIntervalMs());
    heartbeatExecutor.scheduleAtFixedRate(()
                                              -> executorService.submit(() -> {
      try {
        if (doRestartDelegate()) {
          restartDelegate();
        }
        sendHeartbeat(builder, socket);
      } catch (Exception ex) {
        logger.error("Exception while sending heartbeat", ex);
      }
    }),
        0, delegateConfiguration.getHeartbeatIntervalMs(), TimeUnit.MILLISECONDS);
  }

  private boolean doRestartDelegate() {
    long now = System.currentTimeMillis();
    return (now - lastHeartbeatSentAt) > MAX_HB_TIMEOUT || (now - lastHeartbeatReceivedAt) > MAX_HB_TIMEOUT;
  }

  private void sendHeartbeat(Builder builder, Socket socket) throws IOException {
    logger.debug("sending heartbeat...");
    if (socket.status() == STATUS.OPEN || socket.status() == STATUS.REOPENED) {
      socket.fire(JsonUtils.asJson(
          builder.but()
              .withLastHeartBeat(System.currentTimeMillis())
              .withConnected(true)
              .withCurrentlyExecutingDelegateTasks(Lists.newArrayList(currentlyExecutingTasks.values()))
              .build()));
      lastHeartbeatSentAt = System.currentTimeMillis();
    }
  }

  private void abortDelegateTask(DelegateTaskAbortEvent delegateTaskEvent) {
    logger.info("Aborting task {}", delegateTaskEvent);
    Optional.ofNullable(currentlyExecutingFutures.get(delegateTaskEvent.getDelegateTaskId()))
        .ifPresent(future -> future.cancel(true));
  }

  private void dispatchDelegateTask(DelegateTaskEvent delegateTaskEvent) {
    logger.info("DelegateTaskEvent received - {}", delegateTaskEvent);
    if (delegateTaskEvent.getDelegateTaskId() != null
        && currentlyExecutingTasks.containsKey(delegateTaskEvent.getDelegateTaskId())) {
      logger.info("Task [DelegateTaskEvent: {}] already acquired. Don't acquire again", delegateTaskEvent);
      return;
    }

    try {
      logger.info(
          "DelegateTask trying to acquire - uuid: {}, accountId: {}", delegateTaskEvent.getDelegateTaskId(), accountId);
      DelegateTask delegateTask =
          execute(managerClient.acquireTask(delegateId, delegateTaskEvent.getDelegateTaskId(), accountId));
      if (delegateTask != null) {
        logger.info("DelegateTask acquired - uuid: {}, accountId: {}, taskType: {}", delegateTask.getUuid(), accountId,
            delegateTask.getTaskType());
        DelegateRunnableTask delegateRunnableTask =
            delegateTask.getTaskType().getDelegateRunnableTask(delegateId, delegateTask,
                notifyResponseData
                -> {
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
                    if (response != null && !response.isSuccessful()) {
                      response.errorBody().close();
                    }
                    if (response != null && response.isSuccessful()) {
                      response.body().close();
                    }
                  }
                },
                () -> {
                  try {
                    DelegateTask delegateTask1 =
                        execute(managerClient.startTask(delegateId, delegateTaskEvent.getDelegateTaskId(), accountId));
                    boolean taskAcquired = delegateTask1 != null;
                    if (taskAcquired) {
                      if (currentlyExecutingTasks.containsKey(delegateTask.getUuid())) {
                        logger.error(
                            "Delegate task {} already in executing tasks for this delegate.", delegateTask.getUuid());
                      }
                      currentlyExecutingTasks.put(delegateTask.getUuid(), delegateTask1);
                    }
                    return taskAcquired;
                  } catch (IOException e) {
                    logger.error("Unable to update task status on manager", e);
                    return false;
                  }
                });
        injector.injectMembers(delegateRunnableTask);
        currentlyExecutingFutures.putIfAbsent(delegateTask.getUuid(), executorService.submit(delegateRunnableTask));
        executorService.submit(() -> enforceDelegateTaskTimeout(delegateTask));
        logger.info("Task [{}] submitted for execution", delegateTask.getUuid());
      } else {
        logger.info("DelegateTask already executing - uuid: {}, accountId: {}", delegateTaskEvent.getDelegateTaskId(),
            delegateTaskEvent.getAccountId());
        logger.info("Currently executing tasks: {}", currentlyExecutingTasks.keys());
      }
    } catch (IOException e) {
      logger.error("Unable to acquire task", e);
    }
  }

  private void enforceDelegateTaskTimeout(DelegateTask delegateTask) {
    long startTime = System.currentTimeMillis();
    boolean stillRunning = true;
    while (stillRunning && System.currentTimeMillis() - startTime < delegateTask.getTimeout()) {
      try {
        Thread.sleep(5000);
      } catch (InterruptedException e) {
        logger.warn("Time limiter thread interrupted.", e);
      }

      Future taskFuture = currentlyExecutingFutures.get(delegateTask.getUuid());
      stillRunning = taskFuture != null && !taskFuture.isDone() && !taskFuture.isCancelled();
    }
    if (stillRunning) {
      logger.info("Task timed out: {}", delegateTask.getUuid());
      Optional.ofNullable(currentlyExecutingFutures.get(delegateTask.getUuid()))
          .ifPresent(future -> future.cancel(true));
    }
  }

  private String getVersion() {
    return System.getProperty("version", "1.0.0-DEV");
  }
}
