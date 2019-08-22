package io.harness.perpetualtask;

import static io.harness.rule.OwnerRule.HANTANG;

import com.google.common.io.Resources;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.validation.Validators;
import io.harness.category.element.E2ETests;
import io.harness.govern.ProviderModule;
import io.harness.mongo.MongoConfig;
import io.harness.mongo.MongoModule;
import io.harness.perpetualtask.example.SamplePerpetualTaskServiceClient;
import io.harness.rule.OwnerRule.Owner;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.parameternameprovider.ReflectionParameterNameProvider;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import software.wings.app.MainConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

@RunWith(JUnit4.class)
@Slf4j
public class PerpetualTaskServiceImplTest {
  // final Morphia morphia = new Morphia();
  // Datastore datastore = morphia.createDatastore(new MongoClient(), "harness");

  PerpetualTaskServiceImpl service;
  String clientName = SamplePerpetualTaskServiceClient.class.getSimpleName();
  String clientHandle = "testClientHandle";
  PerpetualTaskSchedule taskSchedule = PerpetualTaskSchedule.newBuilder().setInterval(1).setTimeout(1).build();

  @Before
  public void setUp() throws Exception {
    final ObjectMapper objectMapper = Jackson.newObjectMapper();
    final Validator validator = Validators.newValidator();
    final YamlConfigurationFactory<MainConfiguration> factory =
        new YamlConfigurationFactory<>(MainConfiguration.class, validator, objectMapper, "dw");
    final File yaml = new File(Resources.getResource("config.yml").toURI());
    final MainConfiguration configuration = factory.build(yaml);

    ValidatorFactory validatorFactory = Validation.byDefaultProvider()
                                            .configure()
                                            .parameterNameProvider(new ReflectionParameterNameProvider())
                                            .buildValidatorFactory();
    List<Module> modules = new ArrayList<>();
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      MongoConfig mongoConfig() {
        return configuration.getMongoConnectionFactory();
      }
    });
    MongoModule databaseModule = new MongoModule();
    modules.add(databaseModule);
    modules.add(new PerpetualTaskServiceModule());
    Injector injector = Guice.createInjector(modules);
    service = (PerpetualTaskServiceImpl) injector.getInstance(PerpetualTaskService.class);

    cleanUpData();
    loadTestData();
  }

  void cleanUpData() {
    // Query<PerpetualTaskRecord> query = datastore.createQuery(PerpetualTaskRecord.class).field("_id").exists();
    // datastore.delete(query);
  }

  void loadTestData() {
    String clientName = this.clientName;
    String clientHandle = this.clientHandle;
    service.createTask(clientName, clientHandle, taskSchedule);
  }

  @Test
  @Owner(emails = HANTANG)
  @Category(E2ETests.class)
  //@Ignore("This test currently depends on access to a local mongo db.")
  public void testCreateTask() {
    String clientName = this.clientName;
    String clientHandle = "CreateTaskTest";
    String taskId = service.createTask(clientName, clientHandle, taskSchedule);
    // verify by querying mongodb
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
  @Category(E2ETests.class)
  @Ignore("This test currently depends on access to a local mongo db.")
  public void testListTasksIds() {
    String clientName = this.clientName;
    String clientHandle = "ListTasksIdsTestHandel";
    PerpetualTaskSchedule schedule = this.taskSchedule;
    service.createTask(clientName, clientHandle, schedule);

    String delegateId = "";
    List<PerpetualTaskId> taskIdList = service.listTaskIds(delegateId);
    // logger.info(taskIdList.toString());
  }

  @Test
  @Owner(emails = HANTANG)
  @Category(E2ETests.class)
  @Ignore("This test currently depends on access to a local mongo db.")
  public void testGetTaskContext() {
    String clientName = this.clientName;
    String clientHandle = "GetTaskContextTestHandle";
    PerpetualTaskSchedule schedule = this.taskSchedule;
    String taskId = service.createTask(clientName, clientHandle, schedule);

    PerpetualTaskContext context = service.getTaskContext(taskId);
    // logger.info(context.toString());
  }

  @Test
  @Owner(emails = HANTANG)
  @Category(E2ETests.class)
  @Ignore("This test currently depends on access to a local mongo db.")
  public void testDeleteTask() {
    String clientName = this.clientName;
    String clientHandle = this.clientHandle;
    PerpetualTaskSchedule config = this.taskSchedule;
    service.createTask(clientName, clientHandle, config);
    service.deleteTask(clientName, clientHandle);
  }
}
