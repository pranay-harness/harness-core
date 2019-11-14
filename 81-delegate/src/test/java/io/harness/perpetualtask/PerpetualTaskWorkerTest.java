package io.harness.perpetualtask;

import static io.harness.rule.OwnerRule.HANTANG;

import com.google.inject.Guice;
import com.google.inject.Injector;

import io.harness.CategoryTest;
import io.harness.category.element.IntegrationTests;
import io.harness.rule.OwnerRule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import software.wings.delegatetasks.k8s.client.KubernetesClientFactoryModule;

@RunWith(JUnit4.class)
@Slf4j
public class PerpetualTaskWorkerTest extends CategoryTest {
  private PerpetualTaskWorker worker;

  @Before
  public void setUp() throws Exception {
    // TODO: test the map of factory
    Injector injector = Guice.createInjector(new PerpetualTaskWorkerModule(), new KubernetesClientFactoryModule());
    worker = injector.getInstance(PerpetualTaskWorker.class);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(IntegrationTests.class)
  @Ignore("This test currently depends on access to the gRPC port for Perpetual Task Service.")
  public void testUpdateTaskIds() {
    worker.updateAssignedTaskIds();
    logger.info(worker.getAssignedTaskIds().toString());
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(IntegrationTests.class)
  @Ignore("This test currently depends on access to the gRPC port for Perpetual Task Service.")
  public void testGetTaskContext() {
    worker.updateAssignedTaskIds();
    PerpetualTaskId taskId = worker.getAssignedTaskIds().iterator().next();
    //    PerpetualTaskContext context = worker.getTaskContext(taskId);
    //    logger.info(context.toString());
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(IntegrationTests.class)
  @Ignore("This test currently depends on access to a local perpetual task server.")
  public void testStartTask() throws Exception {
    // TODO: verify this test
    worker.updateAssignedTaskIds();
    PerpetualTaskId taskId = worker.getAssignedTaskIds().iterator().next();
    worker.startTask(taskId);
    Thread.sleep(30000);
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(IntegrationTests.class)
  @Ignore("This test currently depends on access to a local perpetual task server.")
  public void testStartAllTasks() throws Exception {
    // TODO: change this outdated test
    // List<PerpetualTaskId> taskIdList = worker.updateAssignedTaskIds();
    // worker.startAllTasks(taskIdList);
    // Thread.sleep(8000);
  }
}
