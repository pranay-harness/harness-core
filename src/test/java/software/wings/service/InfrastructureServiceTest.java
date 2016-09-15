package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.infrastructure.StaticInfrastructure.Builder.aStaticInfrastructure;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.INFRA_ID;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.beans.infrastructure.Infrastructure;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.InfrastructureServiceImpl;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureService;

import javax.inject.Inject;

/**
 * Created by anubhaw on 6/29/16.
 */
public class InfrastructureServiceTest extends WingsBaseTest {
  /**
   * The Query.
   */
  @Mock Query<Infrastructure> query;
  /**
   * The End.
   */
  @Mock FieldEnd end;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private HostService hostService;
  @Inject @InjectMocks private InfrastructureService infrastructureService;
  @Spy @InjectMocks private InfrastructureService spyInfrastructureService = new InfrastructureServiceImpl();

  /**
   * Sets up.
   *
   * @throws Exception the exception
   */
  @Before
  public void setUp() throws Exception {
    when(wingsPersistence.createQuery(Infrastructure.class)).thenReturn(query);
    when(query.field(any())).thenReturn(end);
    when(end.equal(any())).thenReturn(query);
  }

  /**
   * Should list.
   */
  @Test
  public void shouldList() {
    Infrastructure infrastructure = aStaticInfrastructure().build();
    PageResponse<Infrastructure> pageResponse = new PageResponse<>();
    PageRequest<Infrastructure> pageRequest = new PageRequest<>();
    pageResponse.setResponse(asList(infrastructure));
    when(wingsPersistence.query(Infrastructure.class, pageRequest)).thenReturn(pageResponse);
    PageResponse<Infrastructure> infrastructures = infrastructureService.list(pageRequest);
    assertThat(infrastructures).containsAll(asList(infrastructure));
  }

  /**
   * Should save.
   */
  @Test
  public void shouldSave() {
    Infrastructure infrastructure = aStaticInfrastructure().withAppId(GLOBAL_APP_ID).build();
    Infrastructure savedInfrastructure = aStaticInfrastructure().withAppId(GLOBAL_APP_ID).withUuid(INFRA_ID).build();
    when(wingsPersistence.saveAndGet(Infrastructure.class, infrastructure)).thenReturn(savedInfrastructure);
    savedInfrastructure = infrastructureService.save(infrastructure);
    verify(wingsPersistence).saveAndGet(Infrastructure.class, infrastructure);
    Assertions.assertThat(savedInfrastructure).isInstanceOf(Infrastructure.class);
  }

  /**
   * Should get infra id by env id.
   */
  @Test
  public void shouldGetInfraIdByEnvId() {
    when(query.get()).thenReturn(aStaticInfrastructure().withAppId(GLOBAL_APP_ID).withUuid(INFRA_ID).build());
    String infraId = infrastructureService.getInfraByEnvId(ENV_ID).getUuid();
    verify(query).field("appId");
    verify(end).equal(GLOBAL_APP_ID);
    verify(query).get();
    assertThat(infraId).isEqualTo(INFRA_ID);
  }

  /**
   * Should delete.
   */
  @Test
  public void shouldDelete() {
    when(wingsPersistence.delete(any(Query.class))).thenReturn(true);
    infrastructureService.delete(INFRA_ID);
    verify(hostService).deleteByInfra(INFRA_ID);
    verify(query).field("appId");
    verify(end).equal(GLOBAL_APP_ID);
    verify(query).field(ID_KEY);
    verify(end).equal(INFRA_ID);
  }
}
