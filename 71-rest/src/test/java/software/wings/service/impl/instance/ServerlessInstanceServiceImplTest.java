package software.wings.service.impl.instance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.google.inject.Inject;

import io.harness.CategoryTest;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.exception.NoResultFoundException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.mongodb.morphia.query.UpdateResults;
import software.wings.beans.infrastructure.instance.ServerlessInstance;
import software.wings.beans.infrastructure.instance.SyncStatus;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.instance.ServerlessTestHelper.Mocks;
import software.wings.service.intfc.AppService;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

public class ServerlessInstanceServiceImplTest extends CategoryTest {
  @Mock private WingsPersistence wingsPersistence;
  @Mock private AppService appService;

  @InjectMocks @Inject @Spy ServerlessInstanceServiceImpl serverlessInstanceService;

  public static final String ACCOUNTID = "accountid";
  public static final String APPID_1 = "appid1";
  public static final String SERVICEID = "serviceid";

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }
  private ServerlessInstance getServerlessInstance() {
    return ServerlessInstance.builder()
        .uuid("instanceid")
        .appId(APPID_1)
        .createdAt(Instant.now().toEpochMilli())
        .build();
  }

  @Test
  @Category(UnitTests.class)
  public void test_save() {
    setup_AggregationPipeline();
    doReturn(true).when(appService).exist(anyString());
    final Mocks mocks = setup_wingspersistence();
    mocks.serverlessInstance.setDeleted(false);
    doReturn(null).when(serverlessInstanceService).get(anyString());
    final ServerlessInstance serverlessInstance = getServerlessInstance();
    final ServerlessInstance save = serverlessInstanceService.save(serverlessInstance);
    assertThat(serverlessInstance).isEqualTo(save);
  }

  @Test(expected = NoResultFoundException.class)
  @Category(UnitTests.class)
  public void test_save_error() {
    setup_AggregationPipeline();
    doReturn(false).when(appService).exist(anyString());
    final Mocks mocks = setup_wingspersistence();
    mocks.serverlessInstance.setDeleted(false);
    final ServerlessInstance serverlessInstance = getServerlessInstance();
    final ServerlessInstance save = serverlessInstanceService.save(serverlessInstance);
    assertThat(serverlessInstance).isEqualTo(save);
  }
  private Mocks setup_wingspersistence() {
    final ServerlessInstance serverlessInstance = getServerlessInstance();
    doReturn(serverlessInstance).when(wingsPersistence).get(eq(ServerlessInstance.class), anyString());
    doReturn("instanceid").when(wingsPersistence).save(any(ServerlessInstance.class));
    doReturn(getServerlessInstance())
        .when(wingsPersistence)
        .getWithAppId(eq(ServerlessInstance.class), anyString(), anyString());
    Query queryMock = mock(Query.class);
    doReturn(queryMock).when(wingsPersistence).createQuery(ServerlessInstance.class);
    final UpdateOperations updateOperationsMock = mock(UpdateOperations.class);
    doReturn(updateOperationsMock).when(wingsPersistence).createUpdateOperations(ServerlessInstance.class);
    doReturn(updateOperationsMock).when(updateOperationsMock).set(anyString(), any());
    doReturn(updateOperationsMock).when(updateOperationsMock).unset(anyString());

    doReturn(mock(UpdateResults.class)).when(wingsPersistence).update(any(Query.class), any(UpdateOperations.class));
    doReturn(serverlessInstance).when(wingsPersistence).getWithAppId(any(Class.class), anyString(), anyString());
    doReturn(mock(PageResponse.class)).when(wingsPersistence).query(any(Class.class), any(PageRequest.class));

    return Mocks.builder()
        .serverlessInstance(serverlessInstance)
        .queryMock(queryMock)
        .updateOperationsMock(updateOperationsMock)
        .build();
  }

  @Test
  @Category(UnitTests.class)
  public void test_get() {
    setup_AggregationPipeline();
    setup_wingspersistence();
    final ServerlessInstance serverlessInstance = serverlessInstanceService.get("instanceid");
    assertThat(serverlessInstance.getUuid()).isEqualTo("instanceid");
  }

  @Test
  @Category(UnitTests.class)
  public void test_delete() {
    setup_wingspersistence();
    setup_AggregationPipeline();
    final boolean result = serverlessInstanceService.delete(Arrays.asList("id"));
    assertThat(result).isTrue();
  }

  @Test
  @Category(UnitTests.class)
  public void test_update() {
    setup_wingspersistence();
    setup_AggregationPipeline();
    doReturn(true).when(appService).exist(anyString());
    final ServerlessInstance saved = getServerlessInstance();
    final ServerlessInstance update = serverlessInstanceService.update(saved);
    assertThat(update).isEqualTo(saved);
  }

  @Test(expected = NoResultFoundException.class)
  @Category(UnitTests.class)
  public void test_update_error() {
    setup_wingspersistence();
    setup_AggregationPipeline();
    doReturn(false).when(appService).exist(anyString());
    final ServerlessInstance saved = getServerlessInstance();
    final ServerlessInstance update = serverlessInstanceService.update(saved);
    assertThat(update).isEqualTo(saved);
  }

  @Test
  @Category(UnitTests.class)
  public void test_getSyncStatus() {
    setup_wingspersistence();
    final List<SyncStatus> syncStatuses = serverlessInstanceService.getSyncStatus(APPID_1, SERVICEID, "envid");
    assertThat(syncStatuses).isEmpty();
  }

  private Mocks setup_AggregationPipeline() {
    return ServerlessTestHelper.setup_AggregationPipeline(wingsPersistence);
  }
}