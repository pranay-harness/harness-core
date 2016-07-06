package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Infra.InfraBuilder.anInfra;
import static software.wings.beans.Infra.InfraType.STATIC;
import static software.wings.utils.WingsTestConstants.APP_ID;
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
import software.wings.beans.Infra;
import software.wings.beans.Infra.InfraBuilder;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.InfraServiceImpl;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfraService;

import javax.inject.Inject;

/**
 * Created by anubhaw on 6/29/16.
 */
public class InfraServiceTest extends WingsBaseTest {
  /**
   * The Query.
   */
  @Mock Query<Infra> query;
  /**
   * The End.
   */
  @Mock FieldEnd end;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private HostService hostService;
  @Inject @InjectMocks private InfraService infraService;
  @Spy @InjectMocks private InfraService spyInfraService = new InfraServiceImpl();

  /**
   * Sets up.
   *
   * @throws Exception the exception
   */
  @Before
  public void setUp() throws Exception {
    when(wingsPersistence.createQuery(Infra.class)).thenReturn(query);
    when(query.field(any())).thenReturn(end);
    when(end.equal(any())).thenReturn(query);
  }

  /**
   * Should list.
   */
  @Test
  public void shouldList() {
    Infra infra = Infra.InfraBuilder.anInfra().build();
    PageResponse<Infra> pageResponse = new PageResponse<>();
    PageRequest<Infra> pageRequest = new PageRequest<>();
    pageResponse.setResponse(asList(infra));
    when(wingsPersistence.query(Infra.class, pageRequest)).thenReturn(pageResponse);
    PageResponse<Infra> infras = infraService.list(pageRequest);
    assertThat(infras).containsAll(asList(infra));
  }

  /**
   * Should save.
   */
  @Test
  public void shouldSave() {
    Infra infra = InfraBuilder.anInfra().withAppId(APP_ID).withEnvId(ENV_ID).build();
    Infra savedInfra = InfraBuilder.anInfra().withAppId(APP_ID).withEnvId(ENV_ID).withUuid(INFRA_ID).build();
    when(wingsPersistence.saveAndGet(Infra.class, infra)).thenReturn(savedInfra);
    savedInfra = infraService.save(infra);
    verify(wingsPersistence).saveAndGet(Infra.class, infra);
    Assertions.assertThat(savedInfra).isInstanceOf(Infra.class);
  }

  /**
   * Should create default infra for environment.
   */
  @Test
  public void shouldCreateDefaultInfraForEnvironment() {
    infraService.createDefaultInfraForEnvironment(APP_ID, ENV_ID);
    verify(wingsPersistence)
        .saveAndGet(Infra.class, anInfra().withAppId(APP_ID).withEnvId(ENV_ID).withInfraType(STATIC).build());
  }

  /**
   * Should get infra id by env id.
   */
  @Test
  public void shouldGetInfraIdByEnvId() {
    when(query.get()).thenReturn(anInfra().withUuid(INFRA_ID).withAppId(APP_ID).withEnvId(ENV_ID).build());
    String infraId = infraService.getInfraIdByEnvId(APP_ID, ENV_ID);
    verify(query).field("appId");
    verify(end).equal(APP_ID);
    verify(query).field("envId");
    verify(end).equal(ENV_ID);
    verify(query).get();
    assertThat(infraId).isEqualTo(INFRA_ID);
  }

  /**
   * Should delete.
   */
  @Test
  public void shouldDelete() {
    when(wingsPersistence.delete(any(Query.class))).thenReturn(true);
    infraService.delete(APP_ID, ENV_ID, INFRA_ID);
    verify(hostService).deleteByInfra(APP_ID, INFRA_ID);
    verify(query).field("appId");
    verify(end).equal(APP_ID);
    verify(query).field("envId");
    verify(end).equal(ENV_ID);
    verify(query).field(ID_KEY);
    verify(end).equal(INFRA_ID);
  }

  /**
   * Should delete by env.
   */
  @Test
  public void shouldDeleteByEnv() {
    when(query.asList()).thenReturn(asList(anInfra().withUuid(INFRA_ID).withAppId(APP_ID).withEnvId(ENV_ID).build()));
    doNothing().when(spyInfraService).delete(APP_ID, ENV_ID, INFRA_ID);
    spyInfraService.deleteByEnv(APP_ID, ENV_ID);
    verify(spyInfraService).delete(APP_ID, ENV_ID, INFRA_ID);
    verify(query).field("appId");
    verify(end).equal(APP_ID);
    verify(query).field("envId");
    verify(end).equal(ENV_ID);
    verify(query).asList();
  }
}
