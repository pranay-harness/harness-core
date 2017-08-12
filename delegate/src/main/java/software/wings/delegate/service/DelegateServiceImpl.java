package software.wings.delegate.service;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.notNullValue;
import static software.wings.beans.Delegate.Builder.aDelegate;
import static software.wings.beans.DelegateTaskResponse.Builder.aDelegateTaskResponse;
import static software.wings.managerclient.ManagerClientFactory.TRUST_ALL_CERTS;
import static software.wings.managerclient.SafeHttpCall.execute;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import com.ning.http.client.AsyncHttpClient;
import okhttp3.ResponseBody;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
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
import software.wings.exception.WingsException;
import software.wings.http.ExponentialBackOff;
import software.wings.managerclient.ManagerClient;
import software.wings.managerclient.TokenGenerator;
import software.wings.utils.JsonUtils;
import software.wings.utils.Misc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
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
  private static final int MAX_CONNECT_ATTEMPTS = 100;
  private static final int CONNECT_INTERVAL_SECONDS = 10;
  private final Logger logger = LoggerFactory.getLogger(DelegateServiceImpl.class);
  Object waiter = new Object();
  @Inject private DelegateConfiguration delegateConfiguration;
  @Inject private ManagerClient managerClient;
  @Inject @Named("heartbeatExecutor") private ScheduledExecutorService heartbeatExecutor;
  @Inject @Named("upgradeExecutor") private ScheduledExecutorService upgradeExecutor;
  @Inject private ExecutorService executorService;
  @Inject private UpgradeService upgradeService;
  @Inject private Injector injector;
  @Inject private TokenGenerator tokenGenerator;
  @Inject private AsyncHttpClient asyncHttpClient;
  private ConcurrentHashMap<String, DelegateTask> currentlyExecutingTasks = new ConcurrentHashMap<>();
  private ConcurrentHashMap<String, Future<?>> currentlyExecutingFutures = new ConcurrentHashMap<>();

  private Socket socket;
  private RequestBuilder request;

  @Override
  public void run(boolean upgrade) {
    try {
      String ip = InetAddress.getLocalHost().getHostAddress();
      String hostName = InetAddress.getLocalHost().getHostName();
      String accountId = delegateConfiguration.getAccountId();
      Delegate.Builder builder = aDelegate()
                                     .withIp(ip)
                                     .withAccountId(accountId)
                                     .withHostName(hostName)
                                     .withVersion(getVersion())
                                     .withSupportedTaskTypes(Lists.newArrayList(TaskType.values()))
                                     .withIncludeScopes(new ArrayList<>())
                                     .withExcludeScopes(new ArrayList<>());

      if (upgrade) {
        System.out.println("botstarted"); // Don't remove this. It is used as message in upgrade flow.
        logger.info("Received Delegate upgrade request");
        LineIterator it = IOUtils.lineIterator(System.in, "utf-8");
        String line = "";
        while (it.hasNext() && !StringUtils.startsWith(line, "goahead")) {
          logger.info("Message received [{}]", line);
          line = it.nextLine();
        }
      }

      URI uri = new URI(delegateConfiguration.getManagerUrl());

      long start = System.currentTimeMillis();
      String delegateId = registerDelegate(accountId, builder);
      logger.info("Delegate registered in {} ms", (System.currentTimeMillis() - start));

      SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("SSL");
      sslContext.init(null, TRUST_ALL_CERTS, new java.security.SecureRandom());

      Client client = ClientFactory.getDefault().newClient();
      ExecutorService fixedThreadPool = Executors.newWorkStealingPool(5);

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
                  handleMessageSubmit(message, fixedThreadPool, delegateId, accountId);
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

      socket.open(request.build());

      startHeartbeat(builder, socket);

      startUpgradeCheck(accountId, delegateId, getVersion());

      if (upgrade) {
        logger.info("Delegate upgraded.");
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

  private void handleMessageSubmit(
      String message, ExecutorService fixedThreadPool, String delegateId, String accountId) {
    fixedThreadPool.submit(() -> { handleMessage(message, delegateId, accountId); });
  }

  private void handleMessage(String message, String delegateId, String accountId) {
    logger.info("Executing: Event:{}, message:[{}]", Event.MESSAGE.name(), message);
    if (!StringUtils.equals(message, "X")) { // Ignore heartbeats
      try {
        DelegateTaskEvent delegateTaskEvent = JsonUtils.asObject(message, DelegateTaskEvent.class);
        if (delegateTaskEvent instanceof DelegateTaskAbortEvent) {
          abortDelegateTask((DelegateTaskAbortEvent) delegateTaskEvent);
        } else {
          dispatchDelegateTask(delegateTaskEvent, delegateId, accountId);
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

  private String registerDelegate(String accountId, Builder builder) throws IOException {
    logger.info("Registering delegate....");
    writeRestartScript();
    try {
      return await().with().timeout(Duration.FOREVER).pollInterval(Duration.FIVE_SECONDS).until(() -> {
        RestResponse<Delegate> delegateResponse;
        try {
          delegateResponse = execute(managerClient.registerDelegate(accountId,
              builder.but().withLastHeartBeat(System.currentTimeMillis()).withStatus(Status.ENABLED).build()));
        } catch (Exception e) {
          String msg = "Unknown error occurred while registering Delegate [" + accountId + "] with manager";
          logger.error(msg, e);
          Thread.sleep(55000);
          return null;
        }
        if (delegateResponse == null) {
          String msg = "Error occurred while registering Delegate [" + accountId
              + "] with manager. Please see the manager log for more information.";
          logger.error(msg);
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

  private void writeRestartScript() {
    String filename = "restart.sh";
    String script = "#!/bin/bash -e\n\nif ./stop.sh; then ./run.sh; fi\n";

    try {
      Files.deleteIfExists(Paths.get(filename));
      File scriptFile = new File(filename);
      try (BufferedWriter writer = Files.newBufferedWriter(scriptFile.toPath())) {
        writer.write(script, 0, script.length());
        writer.flush();
      }
      logger.info("Done replacing file [{}]. Set User and Group permission", scriptFile);
      Files.setPosixFilePermissions(scriptFile.toPath(),
          Sets.newHashSet(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_EXECUTE,
              PosixFilePermission.OWNER_WRITE, PosixFilePermission.GROUP_READ, PosixFilePermission.OTHERS_READ));
      logger.info("Done setting file permissions");
    } catch (IOException e) {
      logger.error("Couldn't write restart script.", e);
    }
  }

  private void restartDelegate() {
    try {
      logger.info("Restarting delegate");
      new ProcessExecutor()
          .timeout(1, TimeUnit.MINUTES)
          .command("./restart.sh")
          .redirectError(Slf4jStream.of("RestartScript").asError())
          .redirectOutput(Slf4jStream.of("RestartScript").asInfo())
          .redirectOutputAlsoTo(new PipedOutputStream(new PipedInputStream()))
          .readOutput(true)
          .setMessageLogger((log, format, arguments) -> log.info(format, arguments))
          .start();
    } catch (Exception ex) {
      ex.printStackTrace();
      logger.error("Exception while restarting", ex);
    }
  }

  private void startUpgradeCheck(String accountId, String delegateId, String version) {
    if (!delegateConfiguration.isDoUpgrade()) {
      logger.info("Auto upgrade is disabled in configuration.");
      logger.info("Delegate stays on version: [{}]", version);
      return;
    }

    logger.info("Starting upgrade check at interval {} ms", delegateConfiguration.getHeartbeatIntervalMs());
    upgradeExecutor.scheduleWithFixedDelay(() -> {
      logger.info("Checking for upgrade");
      try {
        RestResponse<DelegateScripts> restResponse =
            execute(managerClient.checkForUpgrade(version, delegateId, accountId));
        if (restResponse.getResource().isDoUpgrade()) {
          logger.info("Upgrading delegate...");
          upgradeService.doUpgrade(restResponse.getResource(), getVersion());
        } else {
          logger.info("Delegate up to date");
        }
      } catch (Exception e) {
        logger.error("Exception while checking for upgrade", e);
      }
    }, 0, delegateConfiguration.getHeartbeatIntervalMs(), TimeUnit.MILLISECONDS);
  }

  private void startHeartbeat(Builder builder, Socket socket) {
    logger.info("Starting heartbeat at interval {} ms", delegateConfiguration.getHeartbeatIntervalMs());
    heartbeatExecutor.scheduleAtFixedRate(() -> {
      logger.debug("sending heartbeat...");
      try {
        if (socket.status() == STATUS.OPEN || socket.status() == STATUS.REOPENED) {
          socket.fire(JsonUtils.asJson(
              builder.but()
                  .withLastHeartBeat(System.currentTimeMillis())
                  .withConnected(true)
                  .withCurrentlyExecutingDelegateTasks(Lists.newArrayList(currentlyExecutingTasks.values()))
                  .build()));
        }
      } catch (Exception ex) {
        logger.error("Exception while sending heartbeat", ex);
      }
    }, 0, delegateConfiguration.getHeartbeatIntervalMs(), TimeUnit.MILLISECONDS);
  }

  private void abortDelegateTask(DelegateTaskAbortEvent delegateTaskEvent) {
    System.out.println("Aborting task " + delegateTaskEvent);
    Optional.ofNullable(currentlyExecutingFutures.get(delegateTaskEvent.getDelegateTaskId()))
        .ifPresent(future -> future.cancel(true));
  }

  private void dispatchDelegateTask(DelegateTaskEvent delegateTaskEvent, String delegateId, String accountId) {
    logger.info("DelegateTaskEvent received - {}", delegateTaskEvent);
    if (delegateTaskEvent.getDelegateTaskId() != null
        && currentlyExecutingTasks.containsKey(delegateTaskEvent.getDelegateTaskId())) {
      logger.info("Task [DelegateTaskEvent: {}] already acquired. Don't acquire again", delegateTaskEvent);
      return;
    }

    try {
      // TODO(brett): Check whether to acquire based on task attributes
      logger.info("DelegateTask trying to acquire - uuid: {}, accountId: {}", delegateTaskEvent.getDelegateTaskId(),
          delegateTaskEvent.getAccountId());
      DelegateTask delegateTask =
          execute(managerClient.acquireTask(delegateId, delegateTaskEvent.getDelegateTaskId(), accountId));
      if (delegateTask != null) {
        logger.info("DelegateTask acquired - uuid: {}, accountId: {}, taskType: {}", delegateTask.getUuid(),
            delegateTask.getAccountId(), delegateTask.getTaskType());
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

  private String getVersion() {
    return System.getProperty("version", "1.0.0-DEV");
  }
}
