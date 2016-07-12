/**
 *
 */

package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.utils.WingsTestConstants.APP_ID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppContainerService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowService;

import javax.inject.Inject;

/**
 * The type App service test.
 *
 * @author Rishi
 */
public class AppServiceTest extends WingsBaseTest {
  /**
   * The App service.
   */
  @Inject @InjectMocks AppService appService;
  /**
   * The Query.
   */
  @Mock Query<Application> query;

  /**
   * The End.
   */
  @Mock FieldEnd end;
  /**
   * The Update operations.
   */
  @Mock UpdateOperations<Application> updateOperations;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private SettingsService settingsService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private EnvironmentService environmentService;
  @Mock private AppContainerService appContainerService;
  @Mock private WorkflowService workflowService;

  /**
   * Sets up.
   *
   * @throws Exception the exception
   */
  @Before
  public void setUp() throws Exception {
    when(wingsPersistence.createQuery(Application.class)).thenReturn(query);
    when(wingsPersistence.createUpdateOperations(Application.class)).thenReturn(updateOperations);
    when(query.field(any())).thenReturn(end);
    when(end.equal(any())).thenReturn(query);
    when(updateOperations.set(any(), any())).thenReturn(updateOperations);
  }

  /**
   * Should save application.
   */
  @Test
  public void shouldSaveApplication() {
    Application app = anApplication().withName("AppA").withDescription("Description1").build();
    Application savedApp = anApplication().withUuid(APP_ID).withName("AppA").withDescription("Description1").build();
    when(wingsPersistence.saveAndGet(eq(Application.class), any(Application.class))).thenReturn(savedApp);
    appService.save(app);
    verify(wingsPersistence).saveAndGet(Application.class, app);
    verify(settingsService).createDefaultSettings(APP_ID);
  }

  /**
   * Should list.
   */
  @Test
  public void shouldListApplicationWithSummary() {
    Application application = anApplication().build();
    PageResponse<Application> pageResponse = new PageResponse<>();
    PageRequest<Application> pageRequest = new PageRequest<>();
    pageResponse.setResponse(asList(application));
    when(wingsPersistence.query(Application.class, pageRequest)).thenReturn(pageResponse);
    when(workflowService.listExecutions(any(PageRequest.class), eq(false))).thenReturn(new PageResponse<>());
    PageResponse<Application> applications = appService.list(pageRequest, true, 5);
    assertThat(applications).containsAll(asList(application));
    assertThat(application.getLast5Executions()).isNotNull();
  }

  /**
   * Should list.
   */
  @Test
  public void shouldListApplication() {
    Application application = anApplication().build();
    PageResponse<Application> pageResponse = new PageResponse<>();
    PageRequest<Application> pageRequest = new PageRequest<>();
    pageResponse.setResponse(asList(application));
    when(wingsPersistence.query(Application.class, pageRequest)).thenReturn(pageResponse);
    PageResponse<Application> applications = appService.list(pageRequest, false, 5);
    assertThat(applications).containsAll(asList(application));
  }

  /**
   * Should get application.
   */
  @Test
  public void shouldGetApplication() {
    when(wingsPersistence.get(Application.class, APP_ID)).thenReturn(anApplication().withUuid(APP_ID).build());
    appService.get(APP_ID);
    verify(wingsPersistence).get(Application.class, APP_ID);
  }

  /**
   * Should update.
   */
  @Test
  public void shouldUpdateApplication() {
    appService.update(anApplication().withUuid(APP_ID).withName("App_Name").withDescription("Description").build());
    verify(query).field(ID_KEY);
    verify(end).equal(APP_ID);
    verify(updateOperations).set("name", "App_Name");
    verify(updateOperations).set("description", "Description");
    verify(wingsPersistence).update(query, updateOperations);
    verify(wingsPersistence).get(Application.class, APP_ID);
  }

  /**
   * Should delete.
   */
  @Test
  public void shouldDeleteApplication() {
    when(wingsPersistence.delete(any(), any())).thenReturn(true);
    appService.delete(APP_ID);
    InOrder inOrder = inOrder(wingsPersistence, serviceResourceService, environmentService, appContainerService);
    inOrder.verify(wingsPersistence).delete(Application.class, APP_ID);
    inOrder.verify(serviceResourceService).deleteByAppId(APP_ID);
    inOrder.verify(environmentService).deleteByApp(APP_ID);
    inOrder.verify(appContainerService).deleteByAppId(APP_ID);
  }
}
