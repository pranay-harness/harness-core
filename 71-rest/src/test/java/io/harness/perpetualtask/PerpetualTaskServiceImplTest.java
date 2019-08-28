package io.harness.perpetualtask;

import static io.harness.rule.OwnerRule.HANTANG;

import com.google.inject.Inject;
import com.google.protobuf.util.Durations;

import io.harness.category.element.IntegrationTests;
import io.harness.perpetualtask.example.SamplePerpetualTaskServiceClient;
import io.harness.rule.OwnerRule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mongodb.morphia.query.Query;
import software.wings.dl.WingsPersistence;
import software.wings.integration.BaseIntegrationTest;

import java.util.List;

@RunWith(JUnit4.class)
@Slf4j
public class PerpetualTaskServiceImplTest extends BaseIntegrationTest {
  @Inject PerpetualTaskServiceImpl service;
  @Inject private WingsPersistence datastore;

  String clientName = SamplePerpetualTaskServiceClient.class.getSimpleName();
  String clientHandle = "testClientHandle";
  PerpetualTaskSchedule taskSchedule = PerpetualTaskSchedule.newBuilder()
                                           .setInterval(Durations.fromSeconds(1))
                                           .setTimeout(Durations.fromMillis(1))
                                           .build();

  @Override
  @Before
  public void setUp() throws Exception {
    cleanUpData();
  }

  private void cleanUpData() {
    Query<PerpetualTaskRecord> query = datastore.createQuery(PerpetualTaskRecord.class).field("_id").exists();
    datastore.delete(query);
  }

  @Test
  @Owner(emails = HANTANG)
  @Category(IntegrationTests.class)
  @Ignore("This test has no clearly defined assertion.")
  public void testCreateTask() {
    String clientName = this.clientName;
    String clientHandle = "CreateTaskTest";
    service.createTask(clientName, clientHandle, taskSchedule);
    // TODO: verify by querying mongodb
    /*Query<PerpetualTaskRecord> query = datastore.createQuery(PerpetualTaskRecord.class)
                                           .field("clientName")
                                           .equal(clientName)
                                           .field("clientHandle")
                                           .equal(clientHandle);
    List<PerpetualTaskRecord> records = query.asList();
    Assert.assertThat( records).hasSize(1);*/
  }

  @Test
  @Owner(emails = HANTANG)
  @Category(IntegrationTests.class)
  @Ignore("This test currently depends on access to a local mongo db.")
  public void testListTasksIds() {
    String clientName = this.clientName;
    String clientHandle = "ListTasksIdsTestHandel";
    PerpetualTaskSchedule schedule = this.taskSchedule;
    service.createTask(clientName, clientHandle, schedule);

    String delegateId = "";
    List<PerpetualTaskId> taskIdList = service.listTaskIds(delegateId);
    logger.info(taskIdList.toString());
  }

  @Test
  @Owner(emails = HANTANG)
  @Category(IntegrationTests.class)
  @Ignore("This test currently depends on access to a local mongo db.")
  public void testGetTaskContext() {
    String clientName = this.clientName;
    String clientHandle = "GetTaskContextTestHandle";
    PerpetualTaskSchedule schedule = this.taskSchedule;
    service.createTask(clientName, clientHandle, schedule);
    PerpetualTaskId taskId = service.listTaskIds("").iterator().next();

    PerpetualTaskContext context = service.getTaskContext(taskId.getId());
    logger.info(context.toString());
  }

  @Test
  @Owner(emails = HANTANG)
  @Category(IntegrationTests.class)
  @Ignore("This test currently depends on access to a local mongo db.")
  public void testDeleteTask() {
    String clientName = this.clientName;
    String clientHandle = this.clientHandle;
    PerpetualTaskSchedule config = this.taskSchedule;
    service.createTask(clientName, clientHandle, config);
    service.deleteTask(clientName, clientHandle);
  }
}
