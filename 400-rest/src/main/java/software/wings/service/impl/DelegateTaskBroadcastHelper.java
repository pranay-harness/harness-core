package software.wings.service.impl;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.DelegateTask;
import io.harness.ff.FeatureFlagService;
import io.harness.persistence.HPersistence;

import software.wings.beans.DelegateTaskBroadcast;
import software.wings.service.intfc.AssignDelegateService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;

@Singleton
@Slf4j
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
@OwnedBy(DEL)
public class DelegateTaskBroadcastHelper {
  public static final String STREAM_DELEGATE_PATH = "/stream/delegate/";
  @Inject private AssignDelegateService assignDelegateService;
  @Inject private BroadcasterFactory broadcasterFactory;
  @Inject private HPersistence persistence;
  @Inject private ExecutorService executorService;
  @Inject private FeatureFlagService featureFlagService;

  public void broadcastNewDelegateTaskAsync(DelegateTask task) {
    executorService.submit(() -> {
      try {
        rebroadcastDelegateTask(task);
      } catch (Exception e) {
        log.error("Failed to broadcast task {} for account {}", task.getUuid(), task.getAccountId(), e);
      }
    });
  }

  public void rebroadcastDelegateTask(DelegateTask delegateTask) {
    if (delegateTask == null) {
      return;
    }

    DelegateTaskBroadcast delegateTaskBroadcast = DelegateTaskBroadcast.builder()
                                                      .version(delegateTask.getVersion())
                                                      .accountId(delegateTask.getAccountId())
                                                      .taskId(delegateTask.getUuid())
                                                      .async(delegateTask.getData().isAsync())
                                                      .broadcastToDelegatesIds(delegateTask.getBroadcastToDelegateIds())
                                                      .build();

    Broadcaster broadcaster = broadcasterFactory.lookup(STREAM_DELEGATE_PATH + delegateTask.getAccountId(), true);
    broadcaster.broadcast(delegateTaskBroadcast);
  }
}
