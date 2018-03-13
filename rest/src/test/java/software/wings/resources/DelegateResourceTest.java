package software.wings.resources;

import static java.util.Arrays.asList;
import static javax.ws.rs.client.Entity.entity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Delegate.Builder.aDelegate;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;
import static software.wings.beans.DelegateTaskResponse.Builder.aDelegateTaskResponse;
import static software.wings.dl.PageResponse.PageResponseBuilder.aPageResponse;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;

import com.google.common.collect.Lists;

import org.apache.commons.io.IOUtils;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.app.MainConfiguration;
import software.wings.beans.Delegate;
import software.wings.beans.DelegateTask;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.RestResponse;
import software.wings.common.Constants;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.exception.WingsExceptionMapper;
import software.wings.service.intfc.DelegateScopeService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.DownloadTokenService;
import software.wings.utils.ResourceTestRule;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Created by peeyushaggarwal on 11/2/16.
 */
@RunWith(Parameterized.class)
public class DelegateResourceTest {
  private static final Logger logger = LoggerFactory.getLogger(DelegateResourceTest.class);

  private static DelegateService DELEGATE_SERVICE = mock(DelegateService.class);
  private static DelegateScopeService DELEGATE_SCOPE_SERVICE = mock(DelegateScopeService.class);
  private static DownloadTokenService DOWNLOAD_TOKEN_SERVICE = mock(DownloadTokenService.class);

  private static HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);

  private static @Mock MainConfiguration mainConfiguration = mock(MainConfiguration.class);

  public @Parameter String apiUrl = null;

  @Parameters
  public static String[] data() {
    return new String[] {null, "https://testUrl", "http://testUrl"};
  }

  @ClassRule
  public static final ResourceTestRule RESOURCES =
      ResourceTestRule.builder()
          .addResource(
              new DelegateResource(DELEGATE_SERVICE, DELEGATE_SCOPE_SERVICE, DOWNLOAD_TOKEN_SERVICE, mainConfiguration))
          .addResource(new AbstractBinder() {
            @Override
            protected void configure() {
              bind(httpServletRequest).to(HttpServletRequest.class);
            }
          })
          .addProvider(WingsExceptionMapper.class)
          .build();

  @Before
  public void setUp() {
    initMocks(this);
    Mockito.when(mainConfiguration.getApiUrl()).thenReturn(apiUrl);
  }

  @Test
  public void shouldListDelegates() throws Exception {
    PageResponse<Delegate> pageResponse = new PageResponse<>();
    pageResponse.setResponse(asList(aDelegate().build()));
    pageResponse.setTotal(1l);
    when(DELEGATE_SERVICE.list(any(PageRequest.class))).thenReturn(pageResponse);
    RestResponse<PageResponse<Delegate>> restResponse =
        RESOURCES.client()
            .target("/delegates?accountId=" + ACCOUNT_ID)
            .request()
            .get(new GenericType<RestResponse<PageResponse<Delegate>>>() {});
    PageRequest<Delegate> pageRequest = new PageRequest<>();
    pageRequest.setOffset("0");
    verify(DELEGATE_SERVICE, atLeastOnce()).list(pageRequest);
    assertThat(restResponse.getResource().getResponse().size()).isEqualTo(1);
    assertThat(restResponse.getResource().getResponse().get(0)).isNotNull();
  }

  @Test
  public void shouldRegisterDelegate() throws Exception {
    when(DELEGATE_SERVICE.register(any(Delegate.class)))
        .thenAnswer(invocation -> invocation.getArgumentAt(0, Delegate.class));
    RestResponse<Delegate> restResponse =
        RESOURCES.client()
            .target("/delegates/register?accountId=" + ACCOUNT_ID)
            .request()
            .post(entity(aDelegate().withUuid(ID_KEY).build(), MediaType.APPLICATION_JSON),
                new GenericType<RestResponse<Delegate>>() {});

    ArgumentCaptor<Delegate> captor = ArgumentCaptor.forClass(Delegate.class);
    verify(DELEGATE_SERVICE, atLeastOnce()).register(captor.capture());
    Delegate captorValue = captor.getValue();
    assertThat(captorValue.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(captorValue.getUuid()).isEqualTo(ID_KEY);
    Delegate resource = restResponse.getResource();
    assertThat(resource.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(resource.getUuid()).isEqualTo(ID_KEY);
  }

  @Test
  public void shouldAddDelegate() throws Exception {
    Delegate delegate = aDelegate().build();

    when(DELEGATE_SERVICE.add(any(Delegate.class)))
        .thenAnswer(invocation -> invocation.getArgumentAt(0, Delegate.class));
    RestResponse<Delegate> restResponse =
        RESOURCES.client()
            .target("/delegates?accountId=" + ACCOUNT_ID)
            .request()
            .post(entity(delegate, MediaType.APPLICATION_JSON), new GenericType<RestResponse<Delegate>>() {});

    ArgumentCaptor<Delegate> captor = ArgumentCaptor.forClass(Delegate.class);
    verify(DELEGATE_SERVICE, atLeastOnce()).add(captor.capture());
    Delegate captorValue = captor.getValue();
    assertThat(captorValue.getAccountId()).isEqualTo(ACCOUNT_ID);
    Delegate resource = restResponse.getResource();
    assertThat(resource.getAccountId()).isEqualTo(ACCOUNT_ID);
  }

  @Test
  public void shouldUpdateDelegate() throws Exception {
    Delegate delegate = aDelegate().withUuid(ID_KEY).build();

    when(DELEGATE_SERVICE.update(any(Delegate.class)))
        .thenAnswer(invocation -> invocation.getArgumentAt(0, Delegate.class));
    RestResponse<Delegate> restResponse =
        RESOURCES.client()
            .target("/delegates/" + ID_KEY + "?accountId=" + ACCOUNT_ID)
            .request()
            .put(entity(delegate, MediaType.APPLICATION_JSON), new GenericType<RestResponse<Delegate>>() {});

    ArgumentCaptor<Delegate> captor = ArgumentCaptor.forClass(Delegate.class);
    verify(DELEGATE_SERVICE, atLeastOnce()).update(captor.capture());
    Delegate captorValue = captor.getValue();
    assertThat(captorValue.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(captorValue.getUuid()).isEqualTo(ID_KEY);
    Delegate resource = restResponse.getResource();
    assertThat(resource.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(resource.getUuid()).isEqualTo(ID_KEY);
  }

  @Test
  public void shouldDelete() throws Exception {
    Response restResponse =
        RESOURCES.client().target("/delegates/" + ID_KEY + "?accountId=" + ACCOUNT_ID).request().delete();

    verify(DELEGATE_SERVICE, atLeastOnce()).delete(ACCOUNT_ID, ID_KEY);
  }

  @Test
  public void shouldGet() throws Exception {
    Delegate delegate = aDelegate().withUuid(ID_KEY).build();

    when(DELEGATE_SERVICE.get(ACCOUNT_ID, ID_KEY)).thenReturn(delegate);
    RestResponse<Delegate> restResponse = RESOURCES.client()
                                              .target("/delegates/" + ID_KEY + "?accountId=" + ACCOUNT_ID)
                                              .request()
                                              .get(new GenericType<RestResponse<Delegate>>() {});

    verify(DELEGATE_SERVICE, atLeastOnce()).get(ACCOUNT_ID, ID_KEY);
    assertThat(restResponse.getResource()).isEqualTo(delegate);
  }

  @Test
  public void shouldGetDownloadUrl() throws Exception {
    when(httpServletRequest.getRequestURI()).thenReturn("/delegates/downloadUrl");
    when(DOWNLOAD_TOKEN_SERVICE.createDownloadToken("delegate." + ACCOUNT_ID)).thenReturn("token");
    RestResponse<Map<String, String>> restResponse = RESOURCES.client()
                                                         .target("/delegates/downloadUrl?accountId=" + ACCOUNT_ID)
                                                         .request()
                                                         .get(new GenericType<RestResponse<Map<String, String>>>() {});

    assertThat(restResponse.getResource())
        .containsKey("downloadUrl")
        .containsValue("null://null:0/delegates/download?accountId=ACCOUNT_ID&token=token");
    verify(DOWNLOAD_TOKEN_SERVICE, atLeastOnce()).createDownloadToken("delegate." + ACCOUNT_ID);
  }

  @Test
  public void shouldDownloadDelegate() throws Exception {
    File file = File.createTempFile("test", ".txt");
    try (OutputStreamWriter outputStreamWriter = new FileWriter(file)) {
      IOUtils.write("Test", outputStreamWriter);
    }
    when(DELEGATE_SERVICE.download(anyString(), anyString())).thenReturn(file);
    Response restResponse = RESOURCES.client()
                                .target("/delegates/download?accountId=" + ACCOUNT_ID + "&token=token")
                                .request()
                                .get(new GenericType<Response>() {});

    verify(DELEGATE_SERVICE, atLeastOnce()).download(anyString(), anyString());
    verify(DOWNLOAD_TOKEN_SERVICE, atLeastOnce()).validateDownloadToken("delegate." + ACCOUNT_ID, "token");

    assertThat(restResponse.getHeaderString("Content-Disposition"))
        .isEqualTo("attachment; filename=" + Constants.DELEGATE_DIR + ".zip");
    assertThat(IOUtils.readLines((InputStream) restResponse.getEntity(), Charset.defaultCharset()).get(0))
        .isEqualTo("Test");
  }

  @Test
  @Ignore
  public void shouldAcceptDelegateResponse() {
    DelegateTaskResponse response = aDelegateTaskResponse().build();

    Response response1 = RESOURCES.client()
                             .target("/delegates/" + ID_KEY + "/tasks/1?accountId=" + ACCOUNT_ID)
                             .request()
                             .post(entity(response, "application/x-kryo"), Response.class);
    logger.info(response1.toString());

    verify(DELEGATE_SERVICE, atLeastOnce()).processDelegateResponse(response);
  }

  @Test
  @Ignore
  public void shouldReturnDelegateTasks() {
    DelegateTask task = aDelegateTask().build();

    when(DELEGATE_SERVICE.getDelegateTasks(ACCOUNT_ID, ID_KEY))
        .thenReturn(aPageResponse().withTotal(1).withResponse(Lists.newArrayList(task)).build());
    RestResponse<PageResponse<DelegateTask>> restResponse =
        RESOURCES.client()
            .target("/delegates/" + ID_KEY + "/tasks?accountId=" + ACCOUNT_ID)
            .request()
            .get(new GenericType<RestResponse<PageResponse<DelegateTask>>>() {});

    verify(DELEGATE_SERVICE, atLeastOnce()).getDelegateTasks(ACCOUNT_ID, ID_KEY);
    assertThat(restResponse.getResource()).hasSize(1).containsExactly(task);
  }
}
