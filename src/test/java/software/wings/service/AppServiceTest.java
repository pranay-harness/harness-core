/**
 *
 */

package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SortOrder;
import software.wings.beans.SortOrder.OrderType;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.AppService;

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
  @Inject AppService appService;

  /**
   * Should save and get.
   */
  @Test
  public void shouldSaveAndGet() {
    Application app = Application.Builder.anApplication().withName("AppA").withDescription("Description1").build();

    appService.save(app);
    assertThat(app).isNotNull();
    assertThat(app.getUuid()).isNotNull();

    Application app2 = appService.findByUuid(app.getUuid());
    assertThat(app2).isNotNull();
    assertThat(app2.getUuid()).isNotNull();
    assertThat(app2).isEqualToComparingOnlyGivenFields(app, "uuid", "name", "description");
  }

  /**
   * Should list.
   */
  @Test
  public void shouldList() {
    Application app1 = Application.Builder.anApplication().withName("App1").withDescription("Description1").build();
    appService.save(app1);
    Application app2 = Application.Builder.anApplication().withName("App2").withDescription("Description1").build();
    appService.save(app2);
    Application app3 = Application.Builder.anApplication().withName("App3").withDescription("Description1").build();
    appService.save(app3);
    Application app4 = Application.Builder.anApplication().withName("App4").withDescription("Description1").build();
    appService.save(app4);
    Application app5 = Application.Builder.anApplication().withName("App5").withDescription("Description1").build();
    appService.save(app5);

    PageRequest<Application> req =
        PageRequest.Builder.aPageRequest()
            .withLimit("2")
            .withOffset("1")
            .addFilter(SearchFilter.Builder.aSearchFilter()
                           .withField("name", Operator.IN, "App1", "App2", "App3", "App4")
                           .build())
            .addOrder(SortOrder.Builder.aSortOrder().withField("name", OrderType.DESC).build())
            .build();
    PageResponse<Application> list = appService.list(req);

    assertThat(list).isNotNull();
    assertThat(list.size()).isEqualTo(2);
    assertThat(list.getTotal()).isEqualTo(4);
    assertThat(list.getStart()).isEqualTo(1);
    assertThat(list.getResponse())
        .extracting(Application::getUuid)
        .doesNotContainNull()
        .containsExactly(app3.getUuid(), app2.getUuid());
  }

  /**
   * Should update.
   */
  @Test
  public void shouldUpdate() {
    Application app1 = Application.Builder.anApplication().withName("App1").withDescription("Description1").build();
    appService.save(app1);
    assertThat(app1).isNotNull();
    assertThat(app1.getUuid()).isNotNull();
    app1.setName("App2");
    app1.setDescription("Description2");
    Application app2 = appService.update(app1);

    assertThat(app2).isNotNull();
    assertThat(app2.getUuid()).isNotNull();
    assertThat(app2).isEqualToComparingOnlyGivenFields(app1, "uuid", "name", "description");
  }

  /**
   * Should delete.
   */
  @Test
  public void shouldDelete() {
    Application app1 = Application.Builder.anApplication().withName("App1").withDescription("Description1").build();
    appService.save(app1);
    Application app2 = Application.Builder.anApplication().withName("App2").withDescription("Description1").build();
    appService.save(app2);

    PageRequest<Application> req = PageRequest.Builder.aPageRequest().build();
    PageResponse<Application> list = appService.list(req);
    assertThat(list).isNotNull();
    assertThat(list.size()).isEqualTo(2);
    assertThat(list.getResponse())
        .extracting(Application::getUuid)
        .doesNotContainNull()
        .containsExactly(app2.getUuid(), app1.getUuid());

    appService.deleteApp(app1.getUuid());
    list = appService.list(req);
    assertThat(list).isNotNull();
    assertThat(list.size()).isEqualTo(1);
    assertThat(list.getResponse())
        .extracting(Application::getUuid)
        .doesNotContainNull()
        .containsExactly(app2.getUuid());
  }
}
