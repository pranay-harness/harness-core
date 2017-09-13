package software.wings.delegatetasks;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.sm.StateType;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by sriram_parthasarathy on 9/12/17.
 */
public class SumoDataCollectionTask extends AbstractDelegateDataCollectionTask {
  private static final Logger logger = LoggerFactory.getLogger(SumoDataCollectionTask.class);

  public SumoDataCollectionTask(String delegateId, DelegateTask delegateTask,
      Consumer<DataCollectionTaskResult> consumer, Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  protected StateType getStateType() {
    return StateType.SUMO;
  }

  @Override
  protected DataCollectionTaskResult initDataCollection(Object[] parameters) {
    return null;
  }

  @Override
  protected Logger getLogger() {
    return null;
  }

  @Override
  protected Runnable getDataCollector(DataCollectionTaskResult taskResult) throws IOException {
    return null;
  }
}
