package software.wings.resources;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.PRAVEEN;
import static io.harness.rule.OwnerRule.ROHITKARELIA;
import static java.util.Arrays.asList;
import static javax.ws.rs.client.Entity.entity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.DELEGATE_GROUP_NAME;
import static software.wings.utils.WingsTestConstants.DELEGATE_ID;
import static software.wings.utils.WingsTestConstants.DELEGATE_NAME;
import static software.wings.utils.WingsTestConstants.DELEGATE_PROFILE_ID;
import static software.wings.utils.WingsTestConstants.HOST_NAME;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import org.apache.commons.io.IOUtils;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.ArgumentCaptor;
import software.wings.beans.Delegate;
import software.wings.beans.DelegateStatus;
import software.wings.exception.WingsExceptionMapper;
import software.wings.helpers.ext.url.SubdomainUrlHelperIntfc;
import software.wings.resources.DelegateSetupResource.DelegateScopes;
import software.wings.resources.DelegateSetupResource.DelegateTags;
import software.wings.service.impl.DelegateServiceImpl;
import software.wings.service.intfc.DelegateScopeService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.DownloadTokenService;
import software.wings.utils.ResourceTestRule;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class DelegateSetupResourceTest {
  private static DelegateService delegateService = mock(DelegateService.class);
  private static HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
  private static DelegateScopeService delegateScopeService = mock(DelegateScopeService.class);
  private static DownloadTokenService downloadTokenService = mock(DownloadTokenService.class);
  private static SubdomainUrlHelperIntfc subdomainUrlHelper = mock(SubdomainUrlHelperIntfc.class);

  @Parameter public String apiUrl;

  @Parameters
  public static String[] data() {
    return new String[] {null, "https://testUrl"};
  }

  @ClassRule
  public static final ResourceTestRule RESOURCES =

      ResourceTestRule.builder()
          .addResource(new DelegateSetupResource(
              delegateService, delegateScopeService, downloadTokenService, subdomainUrlHelper))
          .addResource(new AbstractBinder() {
            @Override
            protected void configure() {
              bind(httpServletRequest).to(HttpServletRequest.class);
            }
          })
          .addProvider(WingsExceptionMapper.class)
          .build();

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void shouldListDelegates() {
    PageResponse<Delegate> pageResponse = new PageResponse<>();
    pageResponse.setResponse(asList(Delegate.builder().build()));
    pageResponse.setTotal(1l);
    when(delegateService.list(any(PageRequest.class))).thenReturn(pageResponse);
    RestResponse<PageResponse<Delegate>> restResponse =
        RESOURCES.client()
            .target("/setup/delegates?accountId=" + ACCOUNT_ID)
            .request()
            .get(new GenericType<RestResponse<PageResponse<Delegate>>>() {});
    PageRequest<Delegate> pageRequest = new PageRequest<>();
    pageRequest.setOffset("0");
    verify(delegateService, atLeastOnce()).list(pageRequest);
    assertThat(restResponse.getResource().getResponse().size()).isEqualTo(1);
    assertThat(restResponse.getResource().getResponse().get(0)).isNotNull();
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void shouldGetDelegateStatus() {
    when(delegateService.getDelegateStatus(any()))
        .thenReturn(DelegateStatus.builder().publishedVersions(asList("1.0.0")).build());
    RestResponse<DelegateStatus> restResponse = RESOURCES.client()
                                                    .target("/setup/delegates/status?accountId=" + ACCOUNT_ID)
                                                    .request()
                                                    .get(new GenericType<RestResponse<DelegateStatus>>() {});
    verify(delegateService, atLeastOnce()).getDelegateStatus(ACCOUNT_ID);
    assertThat(restResponse.getResource().getPublishedVersions().get(0)).isEqualTo("1.0.0");
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void shouldGet() {
    Delegate delegate = Delegate.builder().uuid(ID_KEY).build();

    when(delegateService.get(ACCOUNT_ID, ID_KEY, true)).thenReturn(delegate);
    RestResponse<Delegate> restResponse = RESOURCES.client()
                                              .target("/setup/delegates/" + ID_KEY + "?accountId=" + ACCOUNT_ID)
                                              .request()
                                              .get(new GenericType<RestResponse<Delegate>>() {});

    verify(delegateService, atLeastOnce()).get(ACCOUNT_ID, ID_KEY, true);
    assertThat(restResponse.getResource()).isEqualTo(delegate);
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void shouldGetLatestVersion() {
    Delegate delegate = Delegate.builder().uuid(ID_KEY).build();

    when(delegateService.getLatestDelegateVersion(ACCOUNT_ID)).thenReturn("1.0.0");
    RestResponse<String> restResponse = RESOURCES.client()
                                            .target("/setup/delegates/latest?accountId=" + ACCOUNT_ID)
                                            .request()
                                            .get(new GenericType<RestResponse<String>>() {});

    verify(delegateService, atLeastOnce()).getLatestDelegateVersion(ACCOUNT_ID);
    assertThat(restResponse.getResource().isEmpty()).isFalse();
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void shouldUpdateDelegate() {
    Delegate delegate = Delegate.builder().uuid(ID_KEY).build();

    when(delegateService.update(any(Delegate.class)))
        .thenAnswer(invocation -> invocation.getArgumentAt(0, Delegate.class));
    RestResponse<Delegate> restResponse =
        RESOURCES.client()
            .target("/setup/delegates/" + ID_KEY + "?accountId=" + ACCOUNT_ID)
            .request()
            .put(entity(delegate, MediaType.APPLICATION_JSON), new GenericType<RestResponse<Delegate>>() {});

    ArgumentCaptor<Delegate> captor = ArgumentCaptor.forClass(Delegate.class);
    verify(delegateService, atLeastOnce()).update(captor.capture());
    Delegate captorValue = captor.getValue();
    assertThat(captorValue.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(captorValue.getUuid()).isEqualTo(ID_KEY);
    Delegate resource = restResponse.getResource();
    assertThat(resource.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(resource.getUuid()).isEqualTo(ID_KEY);
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void shouldGetProfileResult() {
    when(delegateService.getProfileResult(ACCOUNT_ID, DELEGATE_ID)).thenReturn("ProfileResult");
    RestResponse<String> restResponse = RESOURCES.client()
                                            .target("/setup/delegates/" + DELEGATE_ID + "/profile-result?delegateId="
                                                + DELEGATE_ID + "&accountId=" + ACCOUNT_ID)
                                            .request()
                                            .get(new GenericType<RestResponse<String>>() {});
    verify(delegateService, atLeastOnce()).getProfileResult(ACCOUNT_ID, DELEGATE_ID);
    assertThat(restResponse).isNotNull();
    assertThat(restResponse.getResource().isEmpty()).isFalse();
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void shouldUpdateDescription() {
    Delegate delegate = Delegate.builder().uuid(ID_KEY).build();

    when(delegateService.updateDescription(anyString(), anyString(), anyString())).thenReturn(delegate);
    RestResponse<Delegate> restResponse =
        RESOURCES.client()
            .target("/setup/delegates/" + ID_KEY + "/description?delegateId=" + ID_KEY + "&accountId=" + ACCOUNT_ID)
            .request()
            .put(entity(delegate, MediaType.APPLICATION_JSON), new GenericType<RestResponse<Delegate>>() {});

    verify(delegateService, atLeastOnce()).updateDescription(anyString(), anyString(), anyString());
    assertThat(restResponse.getResource()).isNotNull();
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void shouldUpdateTags() {
    Delegate delegate = Delegate.builder().accountId(ACCOUNT_ID).uuid(ID_KEY).build();

    DelegateTags delegateTags = new DelegateTags();
    delegateTags.setTags(asList("tag"));

    when(delegateService.get(anyString(), anyString(), anyBoolean())).thenReturn(delegate);

    RestResponse<Delegate> restResponse =
        RESOURCES.client()
            .target("/setup/delegates/" + ID_KEY + "/tags?accountId=" + ACCOUNT_ID)
            .request()
            .put(entity(delegateTags, MediaType.APPLICATION_JSON), new GenericType<RestResponse<Delegate>>() {});

    verify(delegateService, atLeastOnce()).updateTags(delegate);
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void shouldUpdateScopes() {
    Delegate delegate = Delegate.builder().accountId(ACCOUNT_ID).uuid(ID_KEY).build();

    DelegateScopes delegateScopes = new DelegateScopes();
    delegateScopes.setIncludeScopeIds(asList("Scope1", "Scope2"));
    delegateScopes.setExcludeScopeIds(asList("Scope3", "Scope4"));

    when(delegateService.get(anyString(), anyString(), anyBoolean())).thenReturn(delegate);

    RestResponse<Delegate> restResponse =
        RESOURCES.client()
            .target("/setup/delegates/" + ID_KEY + "/scopes?accountId=" + ACCOUNT_ID)
            .request()
            .put(entity(delegateScopes, MediaType.APPLICATION_JSON), new GenericType<RestResponse<Delegate>>() {});

    verify(delegateService, atLeastOnce()).updateScopes(delegate);
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void shouldGetDelegateTags() {
    RestResponse<Set<String>> restResponse = RESOURCES.client()
                                                 .target("/setup/delegates/delegate-tags?accountId=" + ACCOUNT_ID)
                                                 .request()
                                                 .get(new GenericType<RestResponse<Set<String>>>() {});

    verify(delegateService, atLeastOnce()).getAllDelegateTags(ACCOUNT_ID);
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void shouldGetAvailableDelegateVersions() {
    RestResponse<List<String>> restResponse =
        RESOURCES.client()
            .target("/setup/delegates/available-versions-for-verification?accountId=" + ACCOUNT_ID)
            .request()
            .get(new GenericType<RestResponse<List<String>>>() {});
    verify(delegateService, atLeastOnce()).getAvailableVersions(ACCOUNT_ID);
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void shouldGetkubernetesDelegateNames() {
    RestResponse<List<String>> restResponse =
        RESOURCES.client()
            .target("/setup/delegates/kubernetes-delegates?accountId=" + ACCOUNT_ID)
            .request()
            .get(new GenericType<RestResponse<List<String>>>() {});
    verify(delegateService, atLeastOnce()).getKubernetesDelegateNames(ACCOUNT_ID);
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void shouldDelete() {
    Response restResponse =
        RESOURCES.client().target("/setup/delegates/" + ID_KEY + "?accountId=" + ACCOUNT_ID).request().delete();

    verify(delegateService, atLeastOnce()).delete(ACCOUNT_ID, ID_KEY);
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void shoulddeleteAllExcept() {
    Response restResponse =
        RESOURCES.client().target("/setup/delegates/delete-all-except?accountId=" + ACCOUNT_ID).request().delete();

    verify(delegateService, atLeastOnce()).retainOnlySelectedDelegatesAndDeleteRest(anyString(), anyList());
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void shouldGetDownloadUrl() {
    when(httpServletRequest.getRequestURI()).thenReturn("/setup/delegates/downloadUrl");
    String accountId = generateUuid();
    String tokenId = generateUuid();
    when(downloadTokenService.createDownloadToken("delegate." + accountId)).thenReturn(tokenId);
    when(subdomainUrlHelper.getManagerUrl(any(), any())).thenReturn(apiUrl + "://" + apiUrl + ":0");
    RestResponse<Map<String, String>> restResponse = RESOURCES.client()
                                                         .target("/setup/delegates/downloadUrl?accountId=" + accountId)
                                                         .request()
                                                         .get(new GenericType<RestResponse<Map<String, String>>>() {});

    assertThat(restResponse.getResource())
        .containsKey("downloadUrl")
        .containsValue(apiUrl == null
                ? apiUrl + "://" + apiUrl + ":0/setup/delegates/download?accountId=" + accountId + "&token=" + tokenId
                : apiUrl + "/setup/delegates/download?accountId=" + accountId + "&token=" + tokenId);
    verify(downloadTokenService, atLeastOnce()).createDownloadToken("delegate." + accountId);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void shouldDownloadDelegate() throws Exception {
    File file = File.createTempFile("test", ".txt");
    try (OutputStreamWriter outputStreamWriter = new FileWriter(file)) {
      IOUtils.write("Test", outputStreamWriter);
    }
    when(delegateService.downloadScripts(anyString(), anyString(), anyString())).thenReturn(file);
    Response restResponse = RESOURCES.client()
                                .target("/setup/delegates/download?accountId=" + ACCOUNT_ID + "&token=token")
                                .request()
                                .get(new GenericType<Response>() {});

    verify(delegateService, atLeastOnce()).downloadScripts(anyString(), anyString(), anyString());
    verify(downloadTokenService, atLeastOnce()).validateDownloadToken("delegate." + ACCOUNT_ID, "token");

    assertThat(restResponse.getHeaderString("Content-Disposition"))
        .isEqualTo("attachment; filename=" + DelegateServiceImpl.DELEGATE_DIR + ".tar.gz");
    assertThat(IOUtils.readLines((InputStream) restResponse.getEntity(), Charset.defaultCharset()).get(0))
        .isEqualTo("Test");
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void shouldDownloadDocker() throws Exception {
    File file = File.createTempFile("test", ".txt");
    try (OutputStreamWriter outputStreamWriter = new FileWriter(file)) {
      IOUtils.write("Test", outputStreamWriter);
    }
    when(delegateService.downloadDocker(anyString(), anyString(), anyString())).thenReturn(file);
    Response restResponse = RESOURCES.client()
                                .target("/setup/delegates/docker?accountId=" + ACCOUNT_ID + "&token=token")
                                .request()
                                .get(new GenericType<Response>() {});

    verify(delegateService, atLeastOnce()).downloadDocker(anyString(), anyString(), anyString());
    verify(downloadTokenService, atLeastOnce()).validateDownloadToken("delegate." + ACCOUNT_ID, "token");

    assertThat(restResponse.getHeaderString("Content-Disposition"))
        .isEqualTo("attachment; filename=" + DelegateServiceImpl.DOCKER_DELEGATE + ".tar.gz");
    assertThat(IOUtils.readLines((InputStream) restResponse.getEntity(), Charset.defaultCharset()).get(0))
        .isEqualTo("Test");
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void shoulddownloadKubernetes() throws Exception {
    File file = File.createTempFile("test", ".txt");
    try (OutputStreamWriter outputStreamWriter = new FileWriter(file)) {
      IOUtils.write("Test", outputStreamWriter);
    }
    when(delegateService.downloadKubernetes(anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(file);
    Response restResponse = RESOURCES.client()
                                .target("/setup/delegates/kubernetes?accountId=" + ACCOUNT_ID + "&delegateName="
                                    + DELEGATE_NAME + "&delegateProfileId=" + DELEGATE_PROFILE_ID + "&token=token")
                                .request()
                                .get(new GenericType<Response>() {});

    verify(delegateService, atLeastOnce())
        .downloadKubernetes(anyString(), anyString(), anyString(), anyString(), anyString());
    verify(downloadTokenService, atLeastOnce()).validateDownloadToken("delegate." + ACCOUNT_ID, "token");

    assertThat(restResponse.getHeaderString("Content-Disposition"))
        .isEqualTo("attachment; filename=" + DelegateServiceImpl.KUBERNETES_DELEGATE + ".tar.gz");
    assertThat(IOUtils.readLines((InputStream) restResponse.getEntity(), Charset.defaultCharset()).get(0))
        .isEqualTo("Test");
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void shouldDownloadECSDelegate() throws Exception {
    File file = File.createTempFile("test", ".txt");
    try (OutputStreamWriter outputStreamWriter = new FileWriter(file)) {
      IOUtils.write("Test", outputStreamWriter);
    }
    when(delegateService.downloadECSDelegate(
             anyString(), anyString(), anyString(), anyBoolean(), anyString(), anyString(), anyString()))
        .thenReturn(file);

    Response restResponse =
        RESOURCES.client()
            .target("/setup/delegates/ecs?accountId=" + ACCOUNT_ID + "&delegateGroupName=" + DELEGATE_GROUP_NAME
                + "&awsVpcMode=true"
                + "&hostname=" + HOST_NAME + "&delegateProfileId=" + DELEGATE_PROFILE_ID + "&token=token")
            .request()
            .get(new GenericType<Response>() {});

    verify(delegateService, atLeastOnce())
        .downloadECSDelegate(
            anyString(), anyString(), anyString(), anyBoolean(), anyString(), anyString(), anyString());
    verify(downloadTokenService, atLeastOnce()).validateDownloadToken("delegate." + ACCOUNT_ID, "token");

    assertThat(restResponse.getHeaderString("Content-Disposition"))
        .isEqualTo("attachment; filename=" + DelegateServiceImpl.ECS_DELEGATE + ".tar.gz");
    assertThat(IOUtils.readLines((InputStream) restResponse.getEntity(), Charset.defaultCharset()).get(0))
        .isEqualTo("Test");
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void downloadDelegateValuesYaml() throws Exception {
    File file = File.createTempFile("test", ".txt");
    try (OutputStreamWriter outputStreamWriter = new FileWriter(file)) {
      IOUtils.write("Test", outputStreamWriter);
    }
    when(
        delegateService.downloadDelegateValuesYamlFile(anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(file);

    Response restResponse =
        RESOURCES.client()
            .target("/setup/delegates/delegate-helm-values-yaml?accountId=" + ACCOUNT_ID
                + "&delegateName=" + DELEGATE_NAME + "&delegateProfileId=" + DELEGATE_PROFILE_ID + "&token=token")
            .request()
            .get(new GenericType<Response>() {});

    verify(delegateService, atLeastOnce())
        .downloadDelegateValuesYamlFile(anyString(), anyString(), anyString(), anyString(), anyString());
    verify(downloadTokenService, atLeastOnce()).validateDownloadToken("delegate." + ACCOUNT_ID, "token");

    assertThat(restResponse.getHeaderString("Content-Disposition"))
        .isEqualTo("attachment; filename=" + DelegateServiceImpl.HARNESS_DELEGATE_VALUES_YAML + ".yaml");
    assertThat(IOUtils.readLines((InputStream) restResponse.getEntity(), Charset.defaultCharset()).get(0))
        .isEqualTo("Test");
  }
}