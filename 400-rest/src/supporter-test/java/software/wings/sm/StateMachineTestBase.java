/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import io.harness.beans.ExecutionStatus;
import io.harness.tasks.ResponseData;
import io.harness.threading.ThreadPool;
import io.harness.waiter.StringNotifyResponseData;
import io.harness.waiter.WaitNotifyEngine;

import software.wings.WingsBaseTest;
import software.wings.app.GeneralNotifyEventListener;
import software.wings.rules.Listeners;
import software.wings.service.StaticMap;
import software.wings.sm.ExecutionResponse.ExecutionResponseBuilder;

import com.google.inject.Inject;
import com.google.inject.Injector;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;

/**
 * The Class StateMachineTest.
 */
@Listeners(GeneralNotifyEventListener.class)
@Slf4j
public class StateMachineTestBase extends WingsBaseTest {
  /**
   * The Class Notifier.
   */
  static class Notifier implements Runnable {
    @Inject private WaitNotifyEngine waitNotifyEngine;
    private boolean shouldFail;
    private String name;
    private int duration;
    private String uuid;

    /**
     * Creates a new Notifier object.
     *
     * @param name     name of notifier.
     * @param uuid     the uuid
     * @param duration duration to sleep for.
     */
    Notifier(String name, String uuid, int duration) {
      this(name, uuid, duration, false);
    }

    /**
     * Instantiates a new notifier.
     *
     * @param name       the name
     * @param uuid       the uuid
     * @param duration   the duration
     * @param shouldFail the should fail
     */
    Notifier(String name, String uuid, int duration, boolean shouldFail) {
      this.name = name;
      this.uuid = uuid;
      this.duration = duration;
      this.shouldFail = shouldFail;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
      log.info("duration = " + duration);
      try {
        Thread.sleep(duration);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        log.error("", e);
      }
      StaticMap.putValue(name, System.currentTimeMillis());
      if (shouldFail) {
        waitNotifyEngine.doneWith(uuid, StringNotifyResponseData.builder().data("FAILURE").build());
      } else {
        waitNotifyEngine.doneWith(uuid, StringNotifyResponseData.builder().data("SUCCESS").build());
      }
    }
  }

  /**
   * The Class StateSync.
   *
   * @author Rishi
   */
  public static class StateSync extends State {
    private boolean shouldFail;

    /**
     * Instantiates a new state synch.
     *
     * @param name the name
     */
    public StateSync(String name) {
      this(name, false);
    }

    /**
     * Instantiates a new state synch.
     *
     * @param name       the name
     * @param shouldFail the should fail
     */
    public StateSync(String name, boolean shouldFail) {
      super(name, StateType.HTTP.name());
      this.shouldFail = shouldFail;
    }

    /*
     * (non-Javadoc)
     *
     * @see software.wings.sm.State#execute(software.wings.sm.ExecutionContext)
     */
    @Override
    public ExecutionResponse execute(ExecutionContext context) {
      log.info("Executing ..." + getClass());
      ExecutionResponseBuilder executionResponseBuilder = ExecutionResponse.builder();
      StateExecutionData stateExecutionData = new TestStateExecutionData(getName(), System.currentTimeMillis() + "");
      executionResponseBuilder.stateExecutionData(stateExecutionData);
      StaticMap.putValue(getName(), System.currentTimeMillis());
      log.info("stateExecutionData:" + stateExecutionData);
      if (shouldFail) {
        executionResponseBuilder.executionStatus(ExecutionStatus.FAILED);
      }
      return executionResponseBuilder.build();
    }

    /**
     * Handle abort event.
     *
     * @param context the context
     */
    @Override
    public void handleAbortEvent(ExecutionContext context) {}

    @Override
    public int hashCode() {
      return Objects.hash(shouldFail);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      final StateSync other = (StateSync) obj;
      return Objects.equals(this.shouldFail, other.shouldFail);
    }
  }

  /**
   * The Class StateAsync.
   *
   * @author Rishi
   */
  public static class StateAsync extends State {
    private boolean shouldFail;
    private boolean shouldThrowException;
    private int duration;

    @Inject private Injector injector;

    /**
     * Instantiates a new state asynch.
     *
     * @param name     the name
     * @param duration the duration
     */
    public StateAsync(String name, int duration) {
      this(name, duration, false);
    }

    /**
     * Instantiates a new state asynch.
     *
     * @param name       the name
     * @param duration   the duration
     * @param shouldFail the should fail
     */
    public StateAsync(String name, int duration, boolean shouldFail) {
      this(name, duration, shouldFail, false);
    }

    /**
     * Instantiates a new State async.
     *
     * @param name                 the name
     * @param duration             the duration
     * @param shouldFail           the should fail
     * @param shouldThrowException the should throw exception
     */
    public StateAsync(String name, int duration, boolean shouldFail, boolean shouldThrowException) {
      super(name, StateType.HTTP.name());
      this.duration = duration;
      this.shouldFail = shouldFail;
      this.shouldThrowException = shouldThrowException;
    }

    /*
     * (non-Javadoc)
     *
     * @see software.wings.sm.State#execute(software.wings.sm.ExecutionContext)
     */
    @Override
    public ExecutionResponse execute(ExecutionContext context) {
      String uuid = generateUuid();

      log.info("Executing ..." + StateAsync.class.getName() + "..duration=" + duration + ", uuid=" + uuid);
      ExecutionResponseBuilder executionResponseBuilder = ExecutionResponse.builder();
      executionResponseBuilder.async(true);
      List<String> correlationIds = new ArrayList<>();
      correlationIds.add(uuid);
      executionResponseBuilder.correlationIds(correlationIds);
      if (shouldThrowException) {
        throw new RuntimeException("Exception for test");
      }
      Notifier notifier = new Notifier(getName(), uuid, duration, shouldFail);
      injector.injectMembers(notifier);
      ThreadPool.execute(notifier);
      return executionResponseBuilder.build();
    }

    /**
     * Handle abort event.
     *
     * @param context the context
     */
    @Override
    public void handleAbortEvent(ExecutionContext context) {}

    /* (non-Javadoc)
     * @see software.wings.sm.State#handleAsyncResponse(software.wings.sm.ExecutionContextImpl, java.util.Map)
     */
    @Override
    public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> responseMap) {
      ExecutionResponseBuilder executionResponseBuilder = ExecutionResponse.builder();
      for (Object response : responseMap.values()) {
        if (!"SUCCESS".equals(((StringNotifyResponseData) response).getData())) {
          executionResponseBuilder.executionStatus(ExecutionStatus.FAILED);
        }
      }
      return executionResponseBuilder.build();
    }

    @Override
    public int hashCode() {
      return Objects.hash(shouldFail, shouldThrowException, duration, injector);
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      final StateAsync other = (StateAsync) obj;
      return Objects.equals(this.shouldFail, other.shouldFail)
          && Objects.equals(this.shouldThrowException, other.shouldThrowException)
          && Objects.equals(this.duration, other.duration) && Objects.equals(this.injector, other.injector);
    }
  }
}
