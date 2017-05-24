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
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.RandomUtils;
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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.inject.Inject;
import javax.inject.Named;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;

/**
 * Created by peeyushaggarwal on 11/29/16.
 */
@Singleton
public class DelegateServiceImpl implements DelegateService {
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
                                     .withSupportedTaskTypes(Lists.newArrayList(TaskType.values()));

      if (upgrade) {
        logger.info("Received Bot upgrade request");
        LineIterator it = IOUtils.lineIterator(System.in, "utf-8");
        String line = "";
        while (it.hasNext() && !StringUtils.startsWith(line, "goahead")) {
          line = it.nextLine();
        }
      }

      URI uri = new URI(delegateConfiguration.getManagerUrl());

      String delegateId = registerDelegate(accountId, builder);

      SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("SSL");
      sslContext.init(null, TRUST_ALL_CERTS, new java.security.SecureRandom());

      Client client = ClientFactory.getDefault().newClient();

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
                                  .reconnectAttempts(Integer.MAX_VALUE)
                                  .pauseBeforeReconnectInMilliseconds(RandomUtils.nextInt(1000, 10000))
                                  .build();
      socket = client.create(clientOptions);
      socket
          .on(Event.MESSAGE,
              new Function<String>() { // Do not change this wasync doesn't like lambda's
                @Override
                public void on(String message) {
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
                      logger.error("Exception while decoding task: ", e);
                    }
                  }
                }
              })
          .on(Event.ERROR,
              new Function<Exception>() { // Do not change this wasync doesn't like lambda's
                @Override
                public void on(Exception e) {
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
                      logger.error("Unable to open socket: ", e);
                    }
                  } else {
                    logger.error("Exception: ", e);
                    try {
                      socket.close();
                    } catch (Exception ex) {
                      // Ignore
                    }
                  }
                }
              })
          .on(Event.REOPENED, new Function<Object>() { // Do not change this wasync doesn't like lambda's
            @Override
            public void on(Object o) {
              try {
                socket.fire(builder.but()
                                .withLastHeartBeat(System.currentTimeMillis())
                                .withStatus(Status.ENABLED)
                                .withConnected(true)
                                .build());
              } catch (IOException e) {
                e.printStackTrace();
              }
            }
          });

      socket.open(request.build());

      startHeartbeat(builder, socket);

      //      startUpgradeCheck(accountId, delegateId, getVersion()); // don't auto-upgrade for now

      if (upgrade) {
        logger.info("Bot upgraded.");
      } else {
        logger.info("Bot started.");
      }

      synchronized (waiter) {
        waiter.wait();
      }

    } catch (Exception e) {
      logger.error("Exception while starting/running delegate ", e);
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
      logger.error("Failed to resume.");
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
    try {
      return await().with().pollInterval(Duration.FIVE_HUNDRED_MILLISECONDS).until(() -> {
        RestResponse<Delegate> delegateResponse;
        try {
          delegateResponse = execute(managerClient.registerDelegate(accountId,
              builder.but().withLastHeartBeat(System.currentTimeMillis()).withStatus(Status.ENABLED).build()));
        } catch (Exception e) {
          String msg = "Unknown error occurred while registering Bot [" + accountId + "] with manager";
          logger.error(msg, e);
          throw new WingsException(msg, e);
        }
        if (delegateResponse == null) {
          String msg = "Error occurred while registering Bot [" + accountId
              + "] with manager. Please see the manager log for more information.";
          logger.error(msg);
          throw new WingsException(msg);
        }
        builder.withUuid(delegateResponse.getResource().getUuid())
            .withStatus(delegateResponse.getResource().getStatus());
        logger.info("Delegate registered with id {} and status {} ", delegateResponse.getResource().getUuid(),
            delegateResponse.getResource().getStatus());
        return delegateResponse.getResource().getUuid();
      }, notNullValue());
    } catch (ConditionTimeoutException e) {
      String msg = "Timeout occurred while registering Bot [" + accountId + "] with manager";
      logger.error(msg, e);
      throw new WingsException(msg, e);
    }
  }

  private void startUpgradeCheck(String accountId, String delegateId, String version) {
    logger.info("Starting upgrade check at interval {} ms", delegateConfiguration.getHeartbeatIntervalMs());
    upgradeExecutor.scheduleWithFixedDelay(() -> {
      logger.info("checking for upgrade");
      try {
        RestResponse<Delegate> restResponse = execute(managerClient.checkForUpgrade(version, delegateId, accountId));
        if (restResponse.getResource().isDoUpgrade()) {
          logger.info("Upgrading delegate...");
          upgradeService.doUpgrade(restResponse.getResource(), getVersion());
        } else {
          logger.info("delegate uptodate...");
        }
      } catch (IOException | InterruptedException | TimeoutException e) {
        logger.error("Exception while checking for upgrade ", e);
      }
    }, 0, delegateConfiguration.getHeartbeatIntervalMs(), TimeUnit.MILLISECONDS);
  }

  private void startHeartbeat(Builder builder, Socket socket) {
    logger.info("Starting heartbeat at interval {} ms", delegateConfiguration.getHeartbeatIntervalMs());
    Delegate delegate = builder.but().withLastHeartBeat(System.currentTimeMillis()).build();
    heartbeatExecutor.scheduleAtFixedRate(() -> {
      logger.debug("sending heartbeat..");
      try {
        if (socket.status() == STATUS.OPEN || socket.status() == STATUS.REOPENED) {
          socket.fire(JsonUtils.asJson(
              builder.but()
                  .withLastHeartBeat(System.currentTimeMillis())
                  .withConnected(true)
                  .withCurrentlyExecutingDelegateTasks(Lists.newArrayList(currentlyExecutingTasks.values()))
                  .build()));
        }
      } catch (IOException e) {
        logger.error("Exception while sending heartbeat ", e);
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
    Response<DelegateTask> acquireResponse = null;

    try {
      DelegateTask delegateTask =
          execute(managerClient.acquireTask(delegateId, delegateTaskEvent.getDelegateTaskId(), accountId));
      if (delegateTask != null) {
        logger.info("DelegateTask acquired - uuid: {}, accountId: {}, taskType: {}", delegateTask.getUuid(),
            delegateTask.getAccountId(), delegateTask.getTaskType());
        DelegateTask finalDelegateTask = delegateTask;
        DelegateRunnableTask delegateRunnableTask =
            delegateTask.getTaskType().getDelegateRunnableTask(delegateId, delegateTask,
                notifyResponseData
                -> {
                  Response<ResponseBody> response = null;
                  try {
                    response = managerClient
                                   .sendTaskStatus(delegateId, finalDelegateTask.getUuid(), accountId,
                                       aDelegateTaskResponse()
                                           .withTask(finalDelegateTask)
                                           .withAccountId(accountId)
                                           .withResponse(notifyResponseData)
                                           .build())
                                   .execute();
                  } catch (IOException e) {
                    logger.error("Unable to send response to manager ", e);
                  } finally {
                    currentlyExecutingTasks.remove(finalDelegateTask.getUuid());
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
                    logger.error("Unable to update task status on manager.", e);
                    return false;
                  }
                });
        injector.injectMembers(delegateRunnableTask);
        currentlyExecutingFutures.putIfAbsent(delegateTask.getUuid(), executorService.submit(delegateRunnableTask));
      } else {
        logger.info("DelegateTask already executing - uuid: {}, accountId: {}", delegateTaskEvent.getDelegateTaskId(),
            delegateTaskEvent.getAccountId());
        logger.info("Currently executing tasks: {}", currentlyExecutingTasks.keys());
      }
    } catch (IOException e) {
      logger.error("Unable to acquire task ", e);
    }
  }

  private String getVersion() {
    return System.getProperty("version", "1.0.0-DEV");
  }
}
