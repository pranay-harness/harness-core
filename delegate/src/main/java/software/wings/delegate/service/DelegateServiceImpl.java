package software.wings.delegate.service;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.Delegate.Builder.aDelegate;
import static software.wings.beans.DelegateTaskResponse.Builder.aDelegateTaskResponse;
import static software.wings.managerclient.ManagerClientFactory.TRUST_ALL_CERTS;

import com.google.inject.Injector;
import com.google.inject.Singleton;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;
import okhttp3.ResponseBody;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.atmosphere.wasync.Client;
import org.atmosphere.wasync.ClientFactory;
import org.atmosphere.wasync.Encoder;
import org.atmosphere.wasync.Event;
import org.atmosphere.wasync.Options;
import org.atmosphere.wasync.Request;
import org.atmosphere.wasync.Request.METHOD;
import org.atmosphere.wasync.RequestBuilder;
import org.atmosphere.wasync.Socket;
import org.atmosphere.wasync.Socket.STATUS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Response;
import software.wings.beans.Delegate;
import software.wings.beans.Delegate.Builder;
import software.wings.beans.Delegate.Status;
import software.wings.beans.DelegateTask;
import software.wings.beans.RestResponse;
import software.wings.delegate.app.DelegateConfiguration;
import software.wings.delegatetasks.DelegateRunnableTask;
import software.wings.managerclient.ManagerClient;
import software.wings.managerclient.TokenGenerator;
import software.wings.utils.JsonUtils;
import software.wings.utils.KryoUtils;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.inject.Inject;
import javax.inject.Named;
import javax.net.ssl.SSLContext;

/**
 * Created by peeyushaggarwal on 11/29/16.
 */
@Singleton
public class DelegateServiceImpl implements DelegateService {
  private final Logger logger = LoggerFactory.getLogger(DelegateServiceImpl.class);

  @Inject private DelegateConfiguration delegateConfiguration;

  @Inject private ManagerClient managerClient;

  @Inject @Named("heartbeatExecutor") private ScheduledExecutorService heartbeatExecutor;

  @Inject @Named("upgradeExecutor") private ScheduledExecutorService upgradeExecutor;

  @Inject private ExecutorService executorService;

  @Inject private UpgradeService upgradeService;

  @Inject private Injector injector;

  @Inject private SignalService signalService;

  @Inject private TokenGenerator tokenGenerator;

  @Override
  public void run(boolean upgrade) {
    try {
      String ip = InetAddress.getLocalHost().getHostAddress();
      String hostName = InetAddress.getLocalHost().getHostName();
      String accountId = delegateConfiguration.getAccountId();
      Delegate.Builder builder =
          aDelegate().withIp(ip).withAccountId(accountId).withHostName(hostName).withVersion(getVersion());

      if (upgrade) {
        System.out.println("Delegate started.");
        LineIterator it = IOUtils.lineIterator(System.in, "utf-8");
        String line = "";
        while (it.hasNext() && !StringUtils.startsWith(line, "StartTasks")) {
          line = it.nextLine();
        }
      }

      String delegateId = registerDelegate(accountId, builder);

      SSLContext sslContext = javax.net.ssl.SSLContext.getInstance("SSL");
      sslContext.init(null, TRUST_ALL_CERTS, new java.security.SecureRandom());

      AsyncHttpClient asyncHttpClient =
          new AsyncHttpClient(new AsyncHttpClientConfig.Builder().setAcceptAnyCertificate(true).build());
      Client client = ClientFactory.getDefault().newClient();

      URI uri = new URI(delegateConfiguration.getManagerUrl());
      // Stream the request body
      RequestBuilder request =
          client.newRequestBuilder()
              .method(METHOD.GET)
              .uri(uri.getScheme() + "://" + uri.getHost() + ":" + uri.getPort() + "/stream/delegate/" + accountId)
              .queryString("delegateId", delegateId)
              .queryString("token", tokenGenerator.getToken("https", "localhost", 9090))
              .header("Version", getVersion())
              .encoder(new Encoder<Delegate, Reader>() { // Stream the request body
                @Override
                public Reader encode(Delegate s) {
                  return new StringReader(JsonUtils.asJson(s));
                }
              })
              .transport(Request.TRANSPORT.WEBSOCKET);

      Options clientOptions = client.newOptionsBuilder()
                                  .runtime(asyncHttpClient, true)
                                  .reconnect(true)
                                  .reconnectAttempts(Integer.MAX_VALUE)
                                  .pauseBeforeReconnectInMilliseconds(RandomUtils.nextInt(1000, 10000))
                                  .build();
      final Socket socket = client.create(clientOptions);
      socket
          .on(Event.MESSAGE,
              (String message) -> {
                if (!StringUtils.equals(message, "X")) {
                  try {
                    DelegateTask delegateTask = (DelegateTask) KryoUtils.asObject(message);
                    dispatchDelegateTask(delegateTask, delegateId, accountId);
                  } catch (Exception e) {
                    System.out.println(message);
                    logger.error("Exception while decoding task: ", e);
                  }
                }
              })
          .on(Event.ERROR,
              (Exception ioe) -> {
                ioe.printStackTrace();
                logger.error("Exception: ", ioe);
                // Some IOException occurred
              })
          .on(Event.REOPENED, o -> {
            // register again
            try {
              socket.fire(builder.but()
                              .withLastHeartBeat(System.currentTimeMillis())
                              .withStatus(Status.ENABLED)
                              .withConnected(true)
                              .build());
            } catch (IOException e) {
              e.printStackTrace();
            }
          });

      startHeartbeat(builder, socket);

      startUpgradeCheck(accountId, delegateId, getVersion());
      // startHeartbeat(accountId, builder, delegateId);

      logger.info("Delegate started.");

      socket.open(request.build());

      /*while (!signalService.shouldStop()) {
        runTaskLoop(accountId, delegateId);
        signalService.paused();
      }*/
      Object waiter = new Object();
      synchronized (waiter) {
        waiter.wait();
      }

    } catch (Exception e) {
      logger.error("Exception while starting/running delegate ", e);
    }
  }

  private String registerDelegate(String accountId, Builder builder) throws IOException {
    logger.info("Registering delegate....");
    RestResponse<Delegate> delegateResponse =
        managerClient
            .registerDelegate(accountId,
                builder.but().withLastHeartBeat(System.currentTimeMillis()).withStatus(Status.ENABLED).build())
            .execute()
            .body();

    builder.withUuid(delegateResponse.getResource().getUuid()).withStatus(delegateResponse.getResource().getStatus());
    logger.info("Delegate registered with id " + delegateResponse.getResource().getUuid());

    return delegateResponse.getResource().getUuid();
  }

  private void startUpgradeCheck(String accountId, String delegateId, String version) {
    logger.info("Starting upgrade check at interval {} ms", delegateConfiguration.getHeartbeatIntervalMs());
    upgradeExecutor.scheduleWithFixedDelay(() -> {
      logger.info("checking for upgrade");
      try {
        RestResponse<Delegate> restResponse =
            managerClient.checkForUpgrade(version, delegateId, accountId).execute().body();
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
              builder.but().withLastHeartBeat(System.currentTimeMillis()).withConnected(true).build()));
        }
      } catch (IOException e) {
        logger.error("Exception while sending heartbeat ", e);
      }
    }, 0, delegateConfiguration.getHeartbeatIntervalMs(), TimeUnit.MILLISECONDS);
  }

  private void dispatchDelegateTask(DelegateTask delegateTask, String delegateId, String accountId) {
    logger.info("DelegateTask received - uuid: {}, accountId: {}, taskType: {}", delegateTask.getUuid(),
        delegateTask.getAccountId(), delegateTask.getTaskType());
    Response<Boolean> acquireResponse = null;

    try {
      boolean taskAcquired = false;
      if (isNotBlank(delegateTask.getWaitId())) {
        acquireResponse = managerClient.acquireTask(delegateId, delegateTask.getUuid(), accountId).execute();
        taskAcquired = acquireResponse.body();
      } else {
        taskAcquired = true;
      }
      if (taskAcquired) {
        logger.info("DelegateTask acquired - uuid: {}, accountId: {}, taskType: {}", delegateTask.getUuid(),
            delegateTask.getAccountId(), delegateTask.getTaskType());
        DelegateRunnableTask delegateRunnableTask =
            delegateTask.getTaskType().getDelegateRunnableTask(delegateId, delegateTask, notifyResponseData -> {
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
              } catch (IOException e) {
                logger.error("Unable to send response to manager ", e);
              } finally {
                if (response != null && !response.isSuccessful()) {
                  response.errorBody().close();
                }
              }
            });
        injector.injectMembers(delegateRunnableTask);
        executorService.submit(delegateRunnableTask);
      } else {
        logger.info("DelegateTask excecuting on some other delegate - uuid: {}, accountId: {}, taskType: {}",
            delegateTask.getUuid(), delegateTask.getAccountId(), delegateTask.getTaskType());
      }
    } catch (IOException e) {
      logger.error("Unable to acquire task ", e);
    } finally {
      if (acquireResponse != null && !acquireResponse.isSuccessful()) {
        acquireResponse.errorBody().close();
      }
    }
  }

  private String getVersion() {
    return System.getProperty("version", "1.0.0-DEV");
  }
}
