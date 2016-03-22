/**
 *
 */
package software.wings.sm;

import java.util.ArrayList;
import java.util.List;

import software.wings.common.UUIDGenerator;
import software.wings.common.thread.ThreadPool;
import software.wings.waitNotify.WaitNotifyEngine;

/**
 * @author Rishi
 *
 */
public class StateAsynch extends State {
  private String name;
  private int duration;

  public StateAsynch(String name, int duration) {
    super(name, StateType.HTTP);
    this.duration = duration;
  }

  /* (non-Javadoc)
   * @see software.wings.sm.State#execute(software.wings.sm.ExecutionContext)
   */
  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    String uuid = UUIDGenerator.getUUID();

    System.out.println("Executing ..." + StateAsynch.class.getName() + "..duration=" + duration + ", uuid=" + uuid);
    ExecutionResponse response = new ExecutionResponse();
    response.setAsynch(true);
    List<String> correlationIds = new ArrayList<>();
    correlationIds.add(uuid);
    response.setCorrelationIds(correlationIds);
    ThreadPool.execute(new Notifier(uuid, duration));
    return response;
  }
}

class Notifier implements Runnable {
  private String uuid;
  private int duration;

  /**
   * @param uuid
   * @param duration
   */
  public Notifier(String uuid, int duration) {
    this.uuid = uuid;
    this.duration = duration;
  }

  /* (non-Javadoc)
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    System.out.println("duration = " + duration);
    try {
      Thread.sleep(duration);
    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    WaitNotifyEngine.getInstance().notify(uuid, "SUCCESS");
  }
}
