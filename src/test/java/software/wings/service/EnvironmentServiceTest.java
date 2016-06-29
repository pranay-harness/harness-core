package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Environment.EnvironmentBuilder.anEnvironment;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_DESCRIPTION;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;

import com.google.common.collect.ImmutableMap;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.beans.Environment;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.EnvironmentServiceImpl;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfraService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.TagService;

import javax.inject.Inject;

/**
 * Created by anubhaw on 6/28/16.
 */
public class EnvironmentServiceTest extends WingsBaseTest {
  @Mock private WingsPersistence wingsPersistence;
  @Mock private AppService appService;
  @Mock private InfraService infraService;
  @Mock private ServiceTemplateService serviceTemplateService;
  @Mock private TagService tagService;

  @Inject @InjectMocks private EnvironmentService environmentService;

  @Spy @InjectMocks private EnvironmentService spyEnvService = new EnvironmentServiceImpl();

  @Mock Query<Environment> query;
  @Mock FieldEnd end;

  @Before
  public void setUp() throws Exception {
    when(wingsPersistence.createQuery(Environment.class)).thenReturn(query);
    when(query.field(any())).thenReturn(end);
    when(end.equal(any())).thenReturn(query);
  }

  @Test
  public void shouldListEnvironments() {
    Environment environment = anEnvironment().build();
    PageResponse<Environment> pageResponse = new PageResponse<>();
    PageRequest<Environment> pageRequest = new PageRequest<>();
    pageResponse.setResponse(asList(environment));
    when(wingsPersistence.query(Environment.class, pageRequest)).thenReturn(pageResponse);
    PageResponse<Environment> applications = environmentService.list(pageRequest);
    assertThat(applications).containsAll(asList(environment));
  }

  @Test
  public void shouldGetEnvironment() {
    environmentService.get(APP_ID, ENV_ID);
    verify(wingsPersistence).get(Environment.class, APP_ID, ENV_ID);
  }

  @Test
  public void shouldSaveEnvironment() {
    Environment environment =
        anEnvironment().withAppId(APP_ID).withName(ENV_NAME).withDescription(ENV_DESCRIPTION).build();
    Environment savedEnv =
        anEnvironment().withAppId(APP_ID).withUuid(ENV_ID).withName(ENV_NAME).withDescription(ENV_DESCRIPTION).build();
    when(wingsPersistence.saveAndGet(Environment.class, environment)).thenReturn(savedEnv);

    environmentService.save(environment);
    verify(wingsPersistence).saveAndGet(Environment.class, environment);
    verify(appService).addEnvironment(savedEnv);
    verify(infraService).createDefaultInfraForEnvironment(APP_ID, ENV_ID);
    verify(tagService).createDefaultRootTagForEnvironment(savedEnv);
  }

  @Test
  public void shouldUpdateEnvironment() {
    Environment environment =
        anEnvironment().withAppId(APP_ID).withUuid(ENV_ID).withName(ENV_NAME).withDescription(ENV_DESCRIPTION).build();
    environmentService.update(environment);
    verify(wingsPersistence)
        .updateFields(Environment.class, ENV_ID, ImmutableMap.of("name", ENV_NAME, "description", ENV_DESCRIPTION));
    verify(wingsPersistence).get(Environment.class, APP_ID, ENV_ID);
  }

  @Test
  public void shouldDeleteEnvironment() {
    environmentService.delete(APP_ID, ENV_ID);
    InOrder inOrder = inOrder(wingsPersistence, serviceTemplateService, tagService, infraService);
    inOrder.verify(wingsPersistence).delete(any(Query.class));
    inOrder.verify(serviceTemplateService).deleteByEnv(APP_ID, ENV_ID);
    inOrder.verify(tagService).deleteByEnv(APP_ID, ENV_ID);
    inOrder.verify(infraService).deleteByEnv(APP_ID, ENV_ID);
  }

  @Test
  public void shouldDeleteByApp() {
    when(query.asList()).thenReturn(asList(anEnvironment().withAppId(APP_ID).withUuid(ENV_ID).build()));
    doNothing().when(spyEnvService).delete(APP_ID, ENV_ID);
    spyEnvService.deleteByApp(APP_ID);
    verify(query).field("appId");
    verify(end).equal(APP_ID);
    verify(spyEnvService).delete(APP_ID, ENV_ID);
  }
}
