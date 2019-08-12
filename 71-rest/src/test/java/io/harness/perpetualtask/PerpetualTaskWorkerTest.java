package io.harness.perpetualtask;

import static io.harness.rule.OwnerRule.HANTANG;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.util.MutableHandlerRegistry;
import io.harness.category.element.E2ETests;
import io.harness.rule.OwnerRule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
@Slf4j
public class PerpetualTaskWorkerTest {
  private PerpetualTaskWorker worker;
  private ManagedChannel channel;

  private final MutableHandlerRegistry serviceRegistry = new MutableHandlerRegistry();

  @Before
  public void setUp() throws Exception {
    channel = ManagedChannelBuilder.forAddress("localhost", 9889).usePlaintext().build();
    worker = new PerpetualTaskWorker(channel);
  }

  @Test
  @Owner(emails = HANTANG)
  @Category(E2ETests.class)
  //@Ignore("This test currently depends on access to a local standalone perpetual task server.")
  public void testListTaskIds() {
    worker.updateAssignedTaskIds();
    // TODO: change this outdated test
    // logger.info(taskIdList.toString());
  }

  @Test
  @Owner(emails = HANTANG)
  @Category(E2ETests.class)
  @Ignore("This test currently depends on access to a local standalone perpetual task server.")
  public void testGetTaskContext() {
    // TODO: change this outdated test
    // PerpetualTaskId taskId = worker.updateAssignedTaskIds().get(0);
    // PerpetualTaskContext context = worker.getTaskContext(taskId);
    // logger.info(context.toString());
  }

  @Test
  @Owner(emails = HANTANG)
  @Category(E2ETests.class)
  @Ignore("This test currently depends on access to a local standalone perpetual task server.")
  public void testStartTask() throws Exception {
    // TODO: change this outdated test
    // PerpetualTaskId taskId = worker.updateAssignedTaskIds().get(0);
    // worker.startTask(taskId);
    // Thread.sleep(20000);
  }

  @Test
  @Owner(emails = HANTANG)
  @Category(E2ETests.class)
  @Ignore("This test currently depends on access to a local standalone perpetual task server.")
  public void testStartAllTasks() throws Exception {
    // TODO: change this outdated test
    // List<PerpetualTaskId> taskIdList = worker.updateAssignedTaskIds();
    // worker.startAllTasks(taskIdList);
    // Thread.sleep(8000);
  }
}
