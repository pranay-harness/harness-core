package io.harness.ng.delegate.profile;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.NICOLAS;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateProfileDetailsNg;
import io.harness.delegate.beans.ScopingRuleDetailsNg;
import io.harness.ng.core.api.DelegateProfileManagerNgService;
import io.harness.ng.core.delegate.profile.DelegateProfileNgResource;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class DelegateProfileResourceNgTest {
  private static final String TEST_ACCOUNT_ID = generateUuid();
  private static final String TEST_DELEGATE_PROFILE_ID = generateUuid();

  private DelegateProfileNgResource delegateProfileNgResource;

  @Mock private DelegateProfileManagerNgService delegateProfileManagerNgService;

  @Before
  public void setup() {
    initMocks(this);
    delegateProfileNgResource = new DelegateProfileNgResource(delegateProfileManagerNgService);
  }

  @Test
  @Owner(developers = NICOLAS)
  @Category(UnitTests.class)
  public void shouldListDelegateProfiles() {
    PageRequest<DelegateProfileDetailsNg> pageRequest = new PageRequest<>();
    pageRequest.setOffset("0");

    PageResponse<DelegateProfileDetailsNg> pageResponse = new PageResponse<>();
    pageResponse.setResponse(Collections.singletonList(DelegateProfileDetailsNg.builder().build()));
    pageResponse.setTotal(1L);

    when(delegateProfileManagerNgService.list(TEST_ACCOUNT_ID, pageRequest)).thenReturn(pageResponse);

    RestResponse<PageResponse<DelegateProfileDetailsNg>> restResponse =
        delegateProfileNgResource.list(pageRequest, TEST_ACCOUNT_ID);

    verify(delegateProfileManagerNgService, times(1)).list(TEST_ACCOUNT_ID, pageRequest);
    assertThat(restResponse.getResource().size()).isEqualTo(1);
    assertThat(restResponse.getResource().get(0)).isNotNull();
  }

  @Test
  @Owner(developers = NICOLAS)
  @Category(UnitTests.class)
  public void shouldGetDelegateProfile() {
    DelegateProfileDetailsNg delegateProfile = DelegateProfileDetailsNg.builder().build();
    when(delegateProfileManagerNgService.get(TEST_ACCOUNT_ID, TEST_DELEGATE_PROFILE_ID)).thenReturn(delegateProfile);

    RestResponse<DelegateProfileDetailsNg> restResponse =
        delegateProfileNgResource.get(TEST_DELEGATE_PROFILE_ID, TEST_ACCOUNT_ID);

    PageRequest<DelegateProfileDetailsNg> pageRequest = new PageRequest<>();
    pageRequest.setOffset("0");
    verify(delegateProfileManagerNgService, times(1)).get(TEST_ACCOUNT_ID, TEST_DELEGATE_PROFILE_ID);
    assertThat(restResponse.getResource()).isEqualTo(delegateProfile);
  }

  @Test
  @Owner(developers = NICOLAS)
  @Category(UnitTests.class)
  public void shouldUpdateDelegateProfile() {
    DelegateProfileDetailsNg toBeUpdated = DelegateProfileDetailsNg.builder()
                                               .accountId(TEST_ACCOUNT_ID)
                                               .uuid(TEST_DELEGATE_PROFILE_ID)
                                               .name("test")
                                               .build();
    when(delegateProfileManagerNgService.update(toBeUpdated)).thenReturn(toBeUpdated);

    RestResponse<DelegateProfileDetailsNg> restResponse =
        delegateProfileNgResource.update(TEST_DELEGATE_PROFILE_ID, TEST_ACCOUNT_ID, toBeUpdated);

    verify(delegateProfileManagerNgService, times(1)).update(toBeUpdated);
    assertThat(restResponse.getResource()).isEqualTo(toBeUpdated);
  }

  @Test
  @Owner(developers = NICOLAS)
  @Category(UnitTests.class)
  public void shouldUpdateDelegateProfileScopingRules() {
    Set<String> environments = new HashSet<>(Collections.singletonList("PROD"));
    ScopingRuleDetailsNg rule1 = ScopingRuleDetailsNg.builder().environmentIds(environments).build();
    ScopingRuleDetailsNg rule2 = ScopingRuleDetailsNg.builder().build();
    List<ScopingRuleDetailsNg> rules = asList(rule1, rule2);
    DelegateProfileDetailsNg result = DelegateProfileDetailsNg.builder()
                                          .accountId(TEST_ACCOUNT_ID)
                                          .uuid(TEST_DELEGATE_PROFILE_ID)
                                          .scopingRules(rules)
                                          .name("test")
                                          .build();
    when(delegateProfileManagerNgService.updateScopingRules(TEST_ACCOUNT_ID, TEST_DELEGATE_PROFILE_ID, rules))
        .thenReturn(result);

    RestResponse<DelegateProfileDetailsNg> restResponse =
        delegateProfileNgResource.updateScopingRules(TEST_DELEGATE_PROFILE_ID, TEST_ACCOUNT_ID, rules);

    verify(delegateProfileManagerNgService, times(1))
        .updateScopingRules(TEST_ACCOUNT_ID, TEST_DELEGATE_PROFILE_ID, rules);
    assertThat(restResponse.getResource()).isEqualTo(result);
  }

  @Test
  @Owner(developers = NICOLAS)
  @Category(UnitTests.class)
  public void shouldAddDelegateProfile() {
    DelegateProfileDetailsNg toBeAdded =
        DelegateProfileDetailsNg.builder().accountId(TEST_ACCOUNT_ID).name("test").build();
    DelegateProfileDetailsNg result = DelegateProfileDetailsNg.builder()
                                          .accountId(TEST_ACCOUNT_ID)
                                          .uuid(TEST_DELEGATE_PROFILE_ID)
                                          .name("test")
                                          .build();
    when(delegateProfileManagerNgService.add(toBeAdded)).thenReturn(result);

    RestResponse<DelegateProfileDetailsNg> restResponse = delegateProfileNgResource.add(TEST_ACCOUNT_ID, toBeAdded);

    verify(delegateProfileManagerNgService, times(1)).add(toBeAdded);
    assertThat(restResponse.getResource()).isEqualTo(result);
  }

  @Test
  @Owner(developers = NICOLAS)
  @Category(UnitTests.class)
  public void shouldDelete() {
    delegateProfileNgResource.delete(TEST_DELEGATE_PROFILE_ID, TEST_ACCOUNT_ID);

    verify(delegateProfileManagerNgService, times(1)).delete(TEST_ACCOUNT_ID, TEST_DELEGATE_PROFILE_ID);
  }

  @Test
  @Owner(developers = NICOLAS)
  @Category(UnitTests.class)
  public void shouldUpdateDelegateProfileSelectorsV2() {
    List<String> profileSelectors = Collections.singletonList("xxx");

    DelegateProfileDetailsNg delegateProfileDetails = DelegateProfileDetailsNg.builder()
                                                          .uuid(TEST_DELEGATE_PROFILE_ID)
                                                          .accountId(TEST_ACCOUNT_ID)
                                                          .selectors(profileSelectors)
                                                          .build();

    when(delegateProfileManagerNgService.updateSelectors(TEST_ACCOUNT_ID, TEST_DELEGATE_PROFILE_ID, profileSelectors))
        .thenReturn(delegateProfileDetails);

    RestResponse<DelegateProfileDetailsNg> restResponse =
        delegateProfileNgResource.updateSelectors(TEST_DELEGATE_PROFILE_ID, TEST_ACCOUNT_ID, profileSelectors);

    verify(delegateProfileManagerNgService, atLeastOnce())
        .updateSelectors(TEST_ACCOUNT_ID, TEST_DELEGATE_PROFILE_ID, profileSelectors);

    DelegateProfileDetailsNg resource = restResponse.getResource();
    assertThat(resource.getAccountId()).isEqualTo(TEST_ACCOUNT_ID);
    assertThat(resource.getUuid()).isEqualTo(TEST_DELEGATE_PROFILE_ID);
    assertThat(resource.getSelectors()).isEqualTo(profileSelectors);
  }
}
