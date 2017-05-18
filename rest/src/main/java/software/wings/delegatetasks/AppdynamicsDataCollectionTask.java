package software.wings.delegatetasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AppDynamicsConfig;
import software.wings.beans.DelegateTask;
import software.wings.service.impl.appdynamics.AppdynamicsDataCollectionTaskResult;

import static software.wings.service.impl.appdynamics.AppdynamicsDataCollectionTaskResult.Builder.aAppdynamicsDataCollectionTaskResult;

import software.wings.service.impl.appdynamics.AppdynamicsDataCollectionTaskResult.AppdynamicsDataCollectionTaskStatus;
import software.wings.service.intfc.appdynamics.AppdynamicsDelegateService;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.inject.Inject;

/**
 * Created by rsingh on 5/18/17.
 */
public class AppdynamicsDataCollectionTask extends AbstractDelegateRunnableTask<AppdynamicsDataCollectionTaskResult> {
  private static final Logger logger = LoggerFactory.getLogger(AppdynamicsDataCollectionTask.class);

  @Inject private AppdynamicsDelegateService appdynamicsDelegateService;

  public AppdynamicsDataCollectionTask(String delegateId, DelegateTask delegateTask,
      Consumer<AppdynamicsDataCollectionTaskResult> consumer, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public AppdynamicsDataCollectionTaskResult run(Object[] parameters) {
    final Map<AppDynamicsConfig, Map<Long, Long>> appTierMap = (Map<AppDynamicsConfig, Map<Long, Long>>) parameters[0];
    logger.info("going to collect appdynamics data for " + appTierMap);
    return aAppdynamicsDataCollectionTaskResult().withStatus(AppdynamicsDataCollectionTaskStatus.SUCCESS).build();
  }
}
