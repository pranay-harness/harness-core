package io.harness.batch.processing;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import io.harness.batch.processing.ccm.BatchJobType;
import io.harness.batch.processing.config.K8sBatchConfiguration;
import io.harness.batch.processing.schedule.BatchJobRunner;
import io.harness.category.element.IntegrationTests;
import io.harness.event.grpc.PublishedMessage;
import io.harness.persistence.HPersistence;
import lombok.val;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.batch.core.Job;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import software.wings.integration.BaseIntegrationTest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.time.temporal.ChronoUnit;
import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = K8sBatchConfiguration.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class K8sJobTest extends BaseIntegrationTest {
  @Autowired private BatchJobRunner batchJobRunner;
  @Autowired private HPersistence hPersistence;
  @Qualifier("k8sJob") @Autowired private Job k8sJob;

  @Before
  public void loadData() throws Exception {
    val datastore = hPersistence.getDatastore(PublishedMessage.class);
    datastore.delete(datastore.createQuery(PublishedMessage.class));

    final Gson gson = new Gson();
    File file = new File(getClass().getClassLoader().getResource("./pod_message.json").getFile());
    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      Type type = new TypeToken<List<PublishedMessage>>() {}.getType();
      List<PublishedMessage> publishedMessages = gson.fromJson(br, type);
      hPersistence.save(publishedMessages);
    }
  }

  @Test
  @Category(IntegrationTests.class)
  public void testK8sJob() throws Exception {
    batchJobRunner.runJob(k8sJob, BatchJobType.ECS_EVENT, 1, ChronoUnit.DAYS);
  }
}
