package software.wings.resources;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.ServiceVariable.Builder.aServiceVariable;
import static software.wings.beans.ServiceVariable.DEFAULT_TEMPLATE_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.TAG_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;

import org.junit.ClassRule;
import org.junit.Test;
import software.wings.beans.EntityType;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.Type;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.exception.WingsExceptionMapper;
import software.wings.service.intfc.ServiceVariableService;
import software.wings.utils.ResourceTestRule;
import software.wings.utils.WingsTestConstants;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

/**
 * Created by peeyushaggarwal on 9/27/16.
 */
public class ServiceVariableResourceTest {
  private static final ServiceVariableService VARIABLE_SERVICE = mock(ServiceVariableService.class);

  @ClassRule
  public static final ResourceTestRule RESOURCES = ResourceTestRule.builder()
                                                       .addResource(new ServiceVariableResource(VARIABLE_SERVICE))
                                                       .addProvider(WingsExceptionMapper.class)
                                                       .build();

  private static final ServiceVariable SERVICE_VARIABLE = aServiceVariable()
                                                              .withAppId(APP_ID)
                                                              .withEnvId(ENV_ID)
                                                              .withUuid(WingsTestConstants.SERVICE_VARIABLE_ID)
                                                              .withEntityType(EntityType.ENVIRONMENT)
                                                              .withEntityId(TAG_ID)
                                                              .withTemplateId(TEMPLATE_ID)
                                                              .withType(Type.TEXT)
                                                              .withValue("8080")
                                                              .build();

  @Test
  public void shouldListVariables() throws Exception {
    PageResponse<ServiceVariable> pageResponse = new PageResponse<>();
    pageResponse.setResponse(asList(SERVICE_VARIABLE));
    pageResponse.setTotal(1);
    when(VARIABLE_SERVICE.list(any(PageRequest.class))).thenReturn(pageResponse);
    RestResponse<PageResponse<ServiceVariable>> restResponse =
        RESOURCES.client()
            .target("/service-variables/?appId=" + APP_ID)
            .request()
            .get(new GenericType<RestResponse<PageResponse<ServiceVariable>>>() {});
    PageRequest<ServiceVariable> pageRequest = new PageRequest<>();
    pageRequest.setOffset("0");
    pageRequest.setLimit("50");
    verify(VARIABLE_SERVICE).list(pageRequest);
    assertThat(restResponse.getResource().getResponse().size()).isEqualTo(1);
    assertThat(restResponse.getResource().getResponse().get(0)).isNotNull();
  }

  @Test
  public void shouldSaveServiceVariable() throws Exception {
    when(VARIABLE_SERVICE.save(any(ServiceVariable.class))).thenReturn(SERVICE_VARIABLE);
    RestResponse<ServiceVariable> restResponse = RESOURCES.client()
                                                     .target(format("/service-variables/?appId=%s", APP_ID))
                                                     .request()
                                                     .post(Entity.entity(SERVICE_VARIABLE, APPLICATION_JSON),
                                                         new GenericType<RestResponse<ServiceVariable>>() {});
    assertThat(restResponse.getResource()).isInstanceOf(ServiceVariable.class);
    verify(VARIABLE_SERVICE).save(SERVICE_VARIABLE);
  }

  @Test
  public void shouldGetVariable() throws Exception {
    when(VARIABLE_SERVICE.get(APP_ID, WingsTestConstants.SERVICE_VARIABLE_ID)).thenReturn(SERVICE_VARIABLE);
    RestResponse<Service> restResponse =
        RESOURCES.client()
            .target(format("/service-variables/%s?appId=%s", WingsTestConstants.SERVICE_VARIABLE_ID, APP_ID))
            .request()
            .get(new GenericType<RestResponse<Service>>() {});
    assertThat(restResponse.getResource()).isInstanceOf(Service.class);
    verify(VARIABLE_SERVICE).get(APP_ID, WingsTestConstants.SERVICE_VARIABLE_ID);
  }

  @Test
  public void shouldUpdateServiceVariable() throws Exception {
    when(VARIABLE_SERVICE.update(any(ServiceVariable.class))).thenReturn(SERVICE_VARIABLE);
    RestResponse<ServiceVariable> restResponse =
        RESOURCES.client()
            .target(format("/service-variables/%s?appId=%s", WingsTestConstants.SERVICE_VARIABLE_ID, APP_ID))
            .request()
            .put(
                Entity.entity(SERVICE_VARIABLE, APPLICATION_JSON), new GenericType<RestResponse<ServiceVariable>>() {});
    assertThat(restResponse.getResource()).isInstanceOf(ServiceVariable.class);
    verify(VARIABLE_SERVICE).update(SERVICE_VARIABLE);
  }

  @Test
  public void shouldDeleteServiceVariable() throws Exception {
    Response restResponse =
        RESOURCES.client()
            .target(format("/service-variables/%s?appId=%s", WingsTestConstants.SERVICE_VARIABLE_ID, APP_ID))
            .request()
            .delete();
    assertThat(restResponse.getStatus()).isEqualTo(200);
    verify(VARIABLE_SERVICE).delete(APP_ID, WingsTestConstants.SERVICE_VARIABLE_ID);
  }

  @Test
  public void shoudlDeleteByEntity() throws Exception {
    Response restResponse =
        RESOURCES.client().target(format("/service-variables/entity/%s?appId=%s", TAG_ID, APP_ID)).request().delete();
    assertThat(restResponse.getStatus()).isEqualTo(200);
    verify(VARIABLE_SERVICE).deleteByEntityId(APP_ID, DEFAULT_TEMPLATE_ID, TAG_ID);
  }
}
