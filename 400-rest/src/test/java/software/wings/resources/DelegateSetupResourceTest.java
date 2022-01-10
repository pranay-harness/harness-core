/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.beans.DelegateType.DOCKER;
import static io.harness.rule.OwnerRule.BOJAN;
import static io.harness.rule.OwnerRule.HANTANG;
import static io.harness.rule.OwnerRule.MARKO;
import static io.harness.rule.OwnerRule.PRAVEEN;
import static io.harness.rule.OwnerRule.ROHITKARELIA;
import static io.harness.rule.OwnerRule.SANJA;
import static io.harness.rule.OwnerRule.UTSAV;

import static software.wings.resources.DelegateSetupResource.YAML;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.DELEGATE_GROUP_NAME;
import static software.wings.utils.WingsTestConstants.DELEGATE_ID;
import static software.wings.utils.WingsTestConstants.DELEGATE_NAME;
import static software.wings.utils.WingsTestConstants.DELEGATE_PROFILE_ID;
import static software.wings.utils.WingsTestConstants.HOST_NAME;

import static java.util.Arrays.asList;
import static javax.ws.rs.client.Entity.entity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import io.harness.CategoryTest;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.annotations.dev.BreakDependencyOn;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.DelegateApproval;
import io.harness.delegate.beans.DelegateGroupDetails;
import io.harness.delegate.beans.DelegateGroupListing;
import io.harness.delegate.beans.DelegateSetupDetails;
import io.harness.delegate.beans.DelegateSize;
import io.harness.delegate.beans.DelegateSizeDetails;
import io.harness.delegate.beans.DelegateType;
import io.harness.delegate.filter.DelegateFilterPropertiesDTO;
import io.harness.delegate.resources.DelegateSetupResourceV2;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.service.intfc.DelegateCache;
import io.harness.service.intfc.DelegateSetupService;

import software.wings.beans.CEDelegateStatus;
import software.wings.beans.DelegateScalingGroup;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.assertj.core.util.Lists;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.joda.time.DateTime;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.ArgumentCaptor;

@OwnedBy(HarnessTeam.DEL)
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
@BreakDependencyOn("software.wings.exception.WingsExceptionMapper")
@BreakDependencyOn("software.wings.service.intfc.DownloadTokenService")
@BreakDependencyOn("software.wings.utils.ResourceTestRule")
public class DelegateSetupResourceTest extends CategoryTest {
  private static String accountId = "ACCOUNT_ID";
  private static DelegateService delegateService = mock(DelegateService.class);
  private static DelegateSetupService delegateSetupService = mock(DelegateSetupService.class);
  private static HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
  private static DelegateScopeService delegateScopeService = mock(DelegateScopeService.class);
  private static DownloadTokenService downloadTokenService = mock(DownloadTokenService.class);
  private static SubdomainUrlHelperIntfc subdomainUrlHelper = mock(SubdomainUrlHelperIntfc.class);
  private static DelegateCache delegateCache = mock(DelegateCache.class);
  private static AccessControlClient accessControlClient = mock(AccessControlClient.class);

  @Parameter public String apiUrl;

  @Parameters
  public static String[] data() {
    return new String[] {null, "https://testUrl"};
  }

  @ClassRule
  public static final ResourceTestRule RESOURCES =
      ResourceTestRule.builder()
          .instance(new DelegateSetupResource(delegateService, delegateScopeService, downloadTokenService,
              subdomainUrlHelper, delegateCache, accessControlClient))
          .instance(new DelegateSetupResourceV3(delegateService, delegateScopeService, downloadTokenService,
              subdomainUrlHelper, delegateCache, accessControlClient, delegateSetupService))
          .instance(new AbstractBinder() {
            @Override
            protected void configure() {
              bind(httpServletRequest).to(HttpServletRequest.class);
            }
          })
          .instance(new DelegateSetupResourceV2(delegateSetupService, accessControlClient))
          .type(WingsExceptionMapper.class)
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
  @Owner(developers = BOJAN)
  @Category(UnitTests.class)
  public void listV2ShouldReturnDelegates() {
    DelegateGroupDetails delegateGroupDetails = DelegateGroupDetails.builder().groupName("group name").build();
    when(delegateSetupService.listDelegateGroupDetailsV2(
             eq(ACCOUNT_ID), eq("orgId"), eq("projectId"), any(), any(), any(DelegateFilterPropertiesDTO.class)))
        .thenReturn(DelegateGroupListing.builder().delegateGroupDetails(Lists.list(delegateGroupDetails)).build());
    RestResponse<DelegateGroupListing> restResponse =
        RESOURCES.client()
            .target("/setup/delegates/ng/v2?accountId=" + ACCOUNT_ID + "&orgId=orgId&projectId=projectId")
            .request()
            .post(entity(DelegateFilterPropertiesDTO.builder().build(), MediaType.APPLICATION_JSON),
                new GenericType<RestResponse<DelegateGroupListing>>() {});

    verify(delegateSetupService, atLeastOnce())
        .listDelegateGroupDetailsV2(
            ACCOUNT_ID, "orgId", "projectId", null, null, DelegateFilterPropertiesDTO.builder().build());
    assertThat(restResponse.getResource().getDelegateGroupDetails()).isNotEmpty();
    assertThat(restResponse.getResource().getDelegateGroupDetails().get(0).getGroupName()).isEqualTo("group name");
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
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldGetDelegateStatusWithScalingGroupsVersion() {
    when(delegateService.getDelegateStatusWithScalingGroups(any()))
        .thenReturn(DelegateStatus.builder().publishedVersions(asList("1.0.0")).build());
    RestResponse<DelegateStatus> restResponse = RESOURCES.client()
                                                    .target("/setup/delegates/status2?accountId=" + ACCOUNT_ID)
                                                    .request()
                                                    .get(new GenericType<RestResponse<DelegateStatus>>() {});
    verify(delegateService, atLeastOnce()).getDelegateStatusWithScalingGroups(ACCOUNT_ID);
    assertThat(restResponse.getResource().getPublishedVersions().get(0)).isEqualTo("1.0.0");
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldGetDelegateStatusWithScalingGroupsDelegates() {
    List<DelegateStatus.DelegateInner> delegates =
        Arrays.asList(DelegateStatus.DelegateInner.builder().delegateName("test1").build());
    when(delegateService.getDelegateStatusWithScalingGroups(any()))
        .thenReturn(DelegateStatus.builder().delegates(delegates).build());
    RestResponse<DelegateStatus> restResponse = RESOURCES.client()
                                                    .target("/setup/delegates/status2?accountId=" + ACCOUNT_ID)
                                                    .request()
                                                    .get(new GenericType<RestResponse<DelegateStatus>>() {});
    verify(delegateService, atLeastOnce()).getDelegateStatusWithScalingGroups(ACCOUNT_ID);
    assertThat(restResponse.getResource().getDelegates()).isEqualTo(delegates);
  }

  @Test
  @Owner(developers = SANJA)
  @Category(UnitTests.class)
  public void shouldGetDelegateStatusWithScalingGroupsScalingGroups() {
    List<DelegateStatus.DelegateInner> delegatesInScalingGroup = Arrays.asList(
        DelegateStatus.DelegateInner.builder().delegateName("test2").delegateGroupName("scaling1").build());
    List<DelegateScalingGroup> scalingGroups =
        Arrays.asList(DelegateScalingGroup.builder().delegates(delegatesInScalingGroup).groupName("scaling1").build());
    when(delegateService.getDelegateStatusWithScalingGroups(any()))
        .thenReturn(DelegateStatus.builder().scalingGroups(scalingGroups).build());
    RestResponse<DelegateStatus> restResponse = RESOURCES.client()
                                                    .target("/setup/delegates/status2?accountId=" + ACCOUNT_ID)
                                                    .request()
                                                    .get(new GenericType<RestResponse<DelegateStatus>>() {});
    verify(delegateService, atLeastOnce()).getDelegateStatusWithScalingGroups(ACCOUNT_ID);
    assertThat(restResponse.getResource().getScalingGroups()).isEqualTo(scalingGroups);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldFetchDelegateSizes() {
    List<DelegateSizeDetails> delegateSizes = Collections.singletonList(
        DelegateSizeDetails.builder().size(DelegateSize.LAPTOP).label("Laptop").replicas(1).cpu(0.5).ram(2048).build());
    when(delegateService.fetchAvailableSizes()).thenReturn(delegateSizes);
    RestResponse<List<DelegateSizeDetails>> restResponse =
        RESOURCES.client()
            .target("/setup/delegates/delegate-sizes?accountId=" + ACCOUNT_ID)
            .request()
            .get(new GenericType<RestResponse<List<DelegateSizeDetails>>>() {});
    verify(delegateService, atLeastOnce()).fetchAvailableSizes();
    assertThat(restResponse.getResource().size()).isEqualTo(1);
    assertThat(restResponse.getResource()).isEqualTo(delegateSizes);
  }

  @Test
  @Owner(developers = UTSAV)
  @Category(UnitTests.class)
  public void shouldValidateCEDelegate() {
    long lastHeartBeart = DateTime.now().getMillis();
    when(delegateService.validateCEDelegate(any(), any()))
        .thenReturn(
            CEDelegateStatus.builder().found(true).delegateName(DELEGATE_NAME).lastHeartBeat(lastHeartBeart).build());

    RestResponse<CEDelegateStatus> restResponse =
        RESOURCES.client()
            .target("/setup/delegates/validate-ce-delegate?accountId=" + ACCOUNT_ID + "&delegateName=" + DELEGATE_NAME)
            .request()
            .get(new GenericType<RestResponse<CEDelegateStatus>>() {});

    verify(delegateService, atLeastOnce()).validateCEDelegate(eq(ACCOUNT_ID), eq(DELEGATE_NAME));
    assertThat(restResponse.getResource().getFound()).isTrue();
    assertThat(restResponse.getResource().getDelegateName()).isEqualTo(DELEGATE_NAME);
    assertThat(restResponse.getResource().getLastHeartBeat()).isEqualTo(lastHeartBeart);
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void shouldGet() {
    Delegate delegate = Delegate.builder().uuid(ID_KEY).build();

    when(delegateCache.get(ACCOUNT_ID, ID_KEY, true)).thenReturn(delegate);
    RestResponse<Delegate> restResponse = RESOURCES.client()
                                              .target("/setup/delegates/" + ID_KEY + "?accountId=" + ACCOUNT_ID)
                                              .request()
                                              .get(new GenericType<RestResponse<Delegate>>() {});

    verify(delegateCache, atLeastOnce()).get(ACCOUNT_ID, ID_KEY, true);
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
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldValidateKubernetesYaml() {
    String accountId = generateUuid();
    DelegateSetupDetails setupDetails = DelegateSetupDetails.builder()
                                            .name("name")
                                            .description("desc")
                                            .size(DelegateSize.LARGE)
                                            .delegateConfigurationId("delConfigId")
                                            .delegateType(DelegateType.KUBERNETES)
                                            .build();

    when(delegateService.validateKubernetesYaml(accountId, setupDetails)).thenReturn(setupDetails);
    RestResponse<DelegateSetupDetails> restResponse =
        RESOURCES.client()
            .target("/setup/delegates/validate-kubernetes-yaml?accountId=" + accountId)
            .request()
            .post(entity(setupDetails, MediaType.APPLICATION_JSON),
                new GenericType<RestResponse<DelegateSetupDetails>>() {});

    DelegateSetupDetails resource = restResponse.getResource();
    assertThat(resource).isEqualTo(setupDetails);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldGenerateKubernetesYaml() throws Exception {
    String accountId = generateUuid();

    File file = File.createTempFile("test", ".txt");
    try (OutputStreamWriter outputStreamWriter = new FileWriter(file)) {
      IOUtils.write("Test", outputStreamWriter);
    }

    DelegateSetupDetails setupDetails = DelegateSetupDetails.builder()
                                            .name("name")
                                            .description("desc")
                                            .size(DelegateSize.LARGE)
                                            .delegateConfigurationId("delConfigId")
                                            .delegateType(DelegateType.KUBERNETES)
                                            .build();

    when(delegateService.generateKubernetesYaml(
             eq(accountId), eq(setupDetails), anyString(), anyString(), any(MediaType.class)))
        .thenReturn(file);
    Response restResponse = RESOURCES.client()
                                .target("/setup/delegates/generate-kubernetes-yaml?accountId=" + accountId)
                                .request()
                                .post(entity(setupDetails, MediaType.APPLICATION_JSON), new GenericType<Response>() {});

    verify(delegateService, atLeastOnce())
        .generateKubernetesYaml(eq(accountId), eq(setupDetails), anyString(), anyString(), any(MediaType.class));

    assertThat(restResponse.getHeaderString("Content-Disposition"))
        .isEqualTo("attachment; filename=" + DelegateServiceImpl.KUBERNETES_DELEGATE + ".tar.gz");
    assertThat(IOUtils.readLines((InputStream) restResponse.getEntity(), Charset.defaultCharset()).get(0))
        .isEqualTo("Test");
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
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldUpdateApprovalStatus() {
    String delegateId = generateUuid();
    Delegate delegate = Delegate.builder().uuid(delegateId).build();

    when(delegateService.updateApprovalStatus(ACCOUNT_ID, delegateId, DelegateApproval.ACTIVATE)).thenReturn(delegate);
    RestResponse<Delegate> restResponse =
        RESOURCES.client()
            .target("/setup/delegates/" + delegateId + "/approval?delegateId=" + delegateId + "&accountId=" + ACCOUNT_ID
                + "&action=ACTIVATE")
            .request()
            .put(entity(delegate, MediaType.APPLICATION_JSON), new GenericType<RestResponse<Delegate>>() {});

    verify(delegateService).updateApprovalStatus(ACCOUNT_ID, delegateId, DelegateApproval.ACTIVATE);
    assertThat(restResponse.getResource()).isEqualTo(delegate);
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void shouldUpdateTags() {
    Delegate delegate = Delegate.builder().accountId(ACCOUNT_ID).uuid(ID_KEY).build();

    DelegateTags delegateTags = new DelegateTags();
    delegateTags.setTags(asList("tag"));

    when(delegateCache.get(anyString(), anyString(), anyBoolean())).thenReturn(delegate);

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

    when(delegateCache.get(anyString(), anyString(), anyBoolean())).thenReturn(delegate);

    RestResponse<Delegate> restResponse =
        RESOURCES.client()
            .target("/setup/delegates/" + ID_KEY + "/scopes?accountId=" + ACCOUNT_ID)
            .request()
            .put(entity(delegateScopes, MediaType.APPLICATION_JSON), new GenericType<RestResponse<Delegate>>() {});

    verify(delegateService, atLeastOnce()).updateScopes(delegate);
  }

  /*
    Duplicated Test method in case of Rollback, soon as "delegate-selectors" is verified and tested from UI,
    below method "shouldGetDelegateTags" will be removed.
  */

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void shouldGetDelegateSelectors() {
    RestResponse<Set<String>> restResponse = RESOURCES.client()
                                                 .target("/setup/delegates/delegate-selectors?accountId=" + ACCOUNT_ID)
                                                 .request()
                                                 .get(new GenericType<RestResponse<Set<String>>>() {});

    verify(delegateService, atLeastOnce()).getAllDelegateSelectors(ACCOUNT_ID);
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void shouldGetDelegateTags() {
    RestResponse<Set<String>> restResponse = RESOURCES.client()
                                                 .target("/setup/delegates/delegate-tags?accountId=" + ACCOUNT_ID)
                                                 .request()
                                                 .get(new GenericType<RestResponse<Set<String>>>() {});

    verify(delegateService, atLeastOnce()).getAllDelegateSelectors(ACCOUNT_ID);
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
    when(delegateService.downloadScripts(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(file);
    Response restResponse = RESOURCES.client()
                                .target("/setup/delegates/download?accountId=" + ACCOUNT_ID
                                    + "&token=token&delegateName=delegateName&delegateProfileId=delegateProfileId")
                                .request()
                                .get(new GenericType<Response>() {});

    verify(delegateService, atLeastOnce())
        .downloadScripts(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    verify(downloadTokenService, atLeastOnce()).validateDownloadToken("delegate." + ACCOUNT_ID, "token");

    assertThat(restResponse.getHeaderString("Content-Disposition"))
        .isEqualTo("attachment; filename=" + DelegateServiceImpl.DELEGATE_DIR + ".tar.gz");
    assertThat(IOUtils.readLines((InputStream) restResponse.getEntity(), Charset.defaultCharset()).get(0))
        .isEqualTo("Test");
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldDownloadDelegateWithoutNameAndProfile() throws Exception {
    File file = File.createTempFile("test", ".txt");
    try (OutputStreamWriter outputStreamWriter = new FileWriter(file)) {
      IOUtils.write("Test", outputStreamWriter);
    }
    when(delegateService.downloadScripts(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(file);
    Response restResponse = RESOURCES.client()
                                .target("/setup/delegates/download?accountId=" + ACCOUNT_ID + "&token=token")
                                .request()
                                .get(new GenericType<Response>() {});

    verify(delegateService, atLeastOnce())
        .downloadScripts(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
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
    when(delegateService.downloadDocker(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(file);
    Response restResponse = RESOURCES.client()
                                .target("/setup/delegates/docker?accountId=" + ACCOUNT_ID
                                    + "&token=token&delegateName=delegateName&delegateProfileId=delegateProfileId")
                                .request()
                                .get(new GenericType<Response>() {});

    verify(delegateService, atLeastOnce())
        .downloadDocker(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    verify(downloadTokenService, atLeastOnce()).validateDownloadToken("delegate." + ACCOUNT_ID, "token");

    assertThat(restResponse.getHeaderString("Content-Disposition"))
        .isEqualTo("attachment; filename=" + DelegateServiceImpl.DOCKER_DELEGATE + ".tar.gz");
    assertThat(IOUtils.readLines((InputStream) restResponse.getEntity(), Charset.defaultCharset()).get(0))
        .isEqualTo("Test");
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void shouldDownloadDockerWithoutNameAndProfile() throws Exception {
    File file = File.createTempFile("test", ".txt");
    try (OutputStreamWriter outputStreamWriter = new FileWriter(file)) {
      IOUtils.write("Test", outputStreamWriter);
    }
    when(delegateService.downloadDocker(anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(file);
    Response restResponse = RESOURCES.client()
                                .target("/setup/delegates/docker?accountId=" + ACCOUNT_ID + "&token=token")
                                .request()
                                .get(new GenericType<Response>() {});

    verify(delegateService, atLeastOnce())
        .downloadDocker(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    verify(downloadTokenService, atLeastOnce()).validateDownloadToken("delegate." + ACCOUNT_ID, "token");

    assertThat(restResponse.getHeaderString("Content-Disposition"))
        .isEqualTo("attachment; filename=" + DelegateServiceImpl.DOCKER_DELEGATE + ".tar.gz");
    assertThat(IOUtils.readLines((InputStream) restResponse.getEntity(), Charset.defaultCharset()).get(0))
        .isEqualTo("Test");
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void shouldDownloadKubernetes() throws Exception {
    File file = File.createTempFile("test", ".txt");
    try (OutputStreamWriter outputStreamWriter = new FileWriter(file)) {
      IOUtils.write("Test", outputStreamWriter);
    }
    when(delegateService.downloadKubernetes(
             anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(file);
    Response restResponse = RESOURCES.client()
                                .target("/setup/delegates/kubernetes?accountId=" + ACCOUNT_ID + "&delegateName="
                                    + DELEGATE_NAME + "&delegateProfileId=" + DELEGATE_PROFILE_ID + "&token=token"
                                    + "&isCeEnabled=false")
                                .request()
                                .get(new GenericType<Response>() {});

    verify(downloadTokenService, atLeastOnce()).validateDownloadToken("delegate." + ACCOUNT_ID, "token");
    verify(delegateService, atLeastOnce())
        .downloadKubernetes(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());

    assertThat(restResponse.getHeaderString("Content-Disposition"))
        .isEqualTo("attachment; filename=" + DelegateServiceImpl.KUBERNETES_DELEGATE + ".tar.gz");
    assertThat(IOUtils.readLines((InputStream) restResponse.getEntity(), Charset.defaultCharset()).get(0))
        .isEqualTo("Test");
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldDownloadCeKubernetesYaml() throws Exception {
    File file = File.createTempFile("test", ".txt");
    try (OutputStreamWriter outputStreamWriter = new FileWriter(file)) {
      IOUtils.write("Test", outputStreamWriter);
    }
    when(delegateService.downloadCeKubernetesYaml(
             anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(file);
    Response restResponse = RESOURCES.client()
                                .target("/setup/delegates/kubernetes?accountId=" + ACCOUNT_ID + "&delegateName="
                                    + DELEGATE_NAME + "&delegateProfileId=" + DELEGATE_PROFILE_ID + "&token=token"
                                    + "&isCeEnabled=true")
                                .request()
                                .get(new GenericType<Response>() {});

    verify(downloadTokenService, atLeastOnce()).validateDownloadToken("delegate." + ACCOUNT_ID, "token");
    verify(delegateService, atLeastOnce())
        .downloadCeKubernetesYaml(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());

    assertThat(restResponse.getHeaderString("Content-Disposition"))
        .isEqualTo("attachment; filename=" + DelegateServiceImpl.KUBERNETES_DELEGATE + YAML);
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
             anyString(), anyString(), anyString(), anyBoolean(), anyString(), anyString(), anyString(), anyString()))
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
            anyString(), anyString(), anyString(), anyBoolean(), anyString(), anyString(), anyString(), anyString());
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
    when(delegateService.downloadDelegateValuesYamlFile(
             anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(file);

    Response restResponse =
        RESOURCES.client()
            .target("/setup/delegates/delegate-helm-values-yaml?accountId=" + ACCOUNT_ID
                + "&delegateName=" + DELEGATE_NAME + "&delegateProfileId=" + DELEGATE_PROFILE_ID + "&token=token")
            .request()
            .get(new GenericType<Response>() {});

    verify(delegateService, atLeastOnce())
        .downloadDelegateValuesYamlFile(anyString(), anyString(), anyString(), anyString(), anyString(), anyString());
    verify(downloadTokenService, atLeastOnce()).validateDownloadToken("delegate." + ACCOUNT_ID, "token");

    assertThat(restResponse.getHeaderString("Content-Disposition"))
        .isEqualTo("attachment; filename=" + DelegateServiceImpl.HARNESS_DELEGATE_VALUES_YAML + ".yaml");
    assertThat(IOUtils.readLines((InputStream) restResponse.getEntity(), Charset.defaultCharset()).get(0))
        .isEqualTo("Test");
  }

  @Test
  @Owner(developers = BOJAN)
  @Category(UnitTests.class)
  public void shouldDownloadNgDockerCompose() throws Exception {
    File file = File.createTempFile("test", ".yaml");
    try (OutputStreamWriter outputStreamWriter = new FileWriter(file)) {
      IOUtils.write("Test docker compose", outputStreamWriter);
    }

    DelegateSetupDetails setupDetails = DelegateSetupDetails.builder()
                                            .name("name")
                                            .description("desc")
                                            .size(DelegateSize.LARGE)
                                            .delegateType(DelegateType.DOCKER)
                                            .build();

    when(delegateService.downloadNgDocker(anyString(), anyString(), anyString(), eq(setupDetails))).thenReturn(file);
    Response restResponse = RESOURCES.client()
                                .target("/setup/delegates/v3/ng/docker?accountId=" + ACCOUNT_ID + "&fileFormat=yaml")
                                .request()
                                .post(entity(setupDetails, MediaType.APPLICATION_JSON), new GenericType<Response>() {});

    verify(delegateService, atLeastOnce()).downloadNgDocker(anyString(), anyString(), anyString(), eq(setupDetails));

    assertThat(restResponse.getHeaderString("Content-Disposition"))
        .isEqualTo("attachment; filename=docker-compose.yaml");
    assertThat(IOUtils.readLines((InputStream) restResponse.getEntity(), Charset.defaultCharset()).get(0))
        .isEqualTo("Test docker compose");
  }

  @Test
  @Owner(developers = BOJAN)
  @Category(UnitTests.class)
  public void shouldValidateDockerDelegateDetails() {
    Response restResponse = RESOURCES.client()
                                .target("/setup/delegates/v3/ng/validate-docker-delegate-details?accountId="
                                    + ACCOUNT_ID + "&delegateName=name1")
                                .request()
                                .get();
    DelegateSetupDetails details = DelegateSetupDetails.builder().delegateType(DOCKER).name("name1").build();
    verify(delegateService, atLeastOnce())
        .validateDockerSetupDetailsNg(anyString(), eq(details), eq(DelegateType.DOCKER));

    assertThat(restResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
  }

  @Test
  @Owner(developers = BOJAN)
  @Category(UnitTests.class)
  public void shouldCreateDelegateGroup() throws Exception {
    DelegateSetupDetails setupDetails =
        DelegateSetupDetails.builder().name("name").description("desc").delegateType(DelegateType.DOCKER).build();

    Response restResponse = RESOURCES.client()
                                .target("/setup/delegates/v3/ng/delegate-group?accountId=" + ACCOUNT_ID)
                                .request()
                                .post(entity(setupDetails, MediaType.APPLICATION_JSON), new GenericType<Response>() {});

    verify(delegateService, atLeastOnce()).createDelegateGroup(anyString(), eq(setupDetails));

    assertThat(restResponse.getStatus()).isEqualTo(Response.Status.OK.getStatusCode());
  }
}
