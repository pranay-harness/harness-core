package io.harness.cvng.servicelevelobjective.resources;

import static io.harness.rule.OwnerRule.KAMAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.rule.Owner;
import io.harness.rule.ResourceTestRule;

import com.google.inject.Inject;
import com.google.inject.Injector;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class SLODashboardResourceTest extends CvNextGenTestBase {
  @Inject private Injector injector;

  private BuilderFactory builderFactory;
  private static SLODashboardResource sloDashboardResource = new SLODashboardResource();

  @ClassRule
  public static final ResourceTestRule RESOURCES = ResourceTestRule.builder().addResource(sloDashboardResource).build();

  @Before
  public void setup() {
    injector.injectMembers(sloDashboardResource);
    builderFactory = BuilderFactory.getDefault();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetSLODashboardWidgets_emptyResponse() {
    Response response = RESOURCES.client()
                            .target("http://localhost:9998/slo-dashboard/widgets")
                            .queryParam("accountId", builderFactory.getContext().getAccountId())
                            .request(MediaType.APPLICATION_JSON_TYPE)
                            .get();
    assertThat(response.getStatus()).isEqualTo(200);
  }
}
