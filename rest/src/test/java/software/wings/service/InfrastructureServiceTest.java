package software.wings.service;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.beans.infrastructure.Infrastructure;
import software.wings.dl.WingsPersistence;
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
  @Ignore
  public void shouldList() {
    //    Infrastructure infrastructure =
    //    Infrastructure.Builder.anInfrastructure().withType(InfrastructureType.STATIC).withUuid(INFRA_ID).build();
    //    PageResponse<Infrastructure> pageResponse = new PageResponse<>();
    //    PageRequest<Infrastructure> pageRequest = new PageRequest<>();
    //    pageResponse.setResponse(asList(infrastructure));
    //    when(wingsPersistence.query(Infrastructure.class, pageRequest)).thenReturn(pageResponse);
    //    PageResponse<Infrastructure> infrastructures = infrastructureService.list(pageRequest, true);
    //    assertThat(infrastructures).containsAll(asList(infrastructure));
  }
}
