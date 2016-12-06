package software.wings.delegate.service;

import static software.wings.beans.Delegate.Builder.aDelegate;

import retrofit2.Call;
import software.wings.beans.Delegate;
import software.wings.beans.Delegate.Status;
import software.wings.delegate.app.DelegateConfiguration;
import software.wings.managerclient.ManagerClient;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 11/29/16.
 */
public class DelegateServiceImpl implements DelegateService {
  @Inject private DelegateConfiguration delegateConfiguration;

  @Inject private ManagerClient managerClient;

  @Inject private ScheduledExecutorService scheduledExecutorService;

  @Override
  public void run() {
    try {
      String ip = InetAddress.getLocalHost().getHostAddress();
      String hostName = InetAddress.getLocalHost().getHostName();
      String accountId = delegateConfiguration.getAccountId();
      Delegate.Builder builder = aDelegate().withIp(ip).withAccountId(accountId).withHostName(hostName);

      System.out.println("Register delegate...");
      Delegate delegate =
          managerClient
              .registerDelegate(accountId,
                  builder.but().withLastHeartBeat(System.currentTimeMillis()).withStatus(Status.ENABLED).build())
              .execute()
              .body();

      builder.withUuid(delegate.getUuid()).withStatus(delegate.getStatus());

      System.out.println("Start heartbeat...");
      scheduledExecutorService.scheduleAtFixedRate(() -> {
        System.out.println("sending heartbeat...");
        try {
          Call<Delegate> call = managerClient.sendHeartbeat(
              delegate.getUuid(), accountId, builder.but().withLastHeartBeat(System.currentTimeMillis()).build());
          System.out.println(call);
          System.out.println(call.execute().code());
        } catch (IOException e) {
          e.printStackTrace();
        }
      }, 0, delegateConfiguration.getHeartbeatInterval(), TimeUnit.MILLISECONDS);

      while (true) {
        // Loop for tasks.
        Thread.sleep(1000);
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
