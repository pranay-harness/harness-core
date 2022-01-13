/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.delegateservice;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.MARKO;
import static io.harness.rule.OwnerRule.SANJA;
import static io.harness.rule.OwnerRule.VUK;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageResponse;
import io.harness.category.element.FunctionalTests;
import io.harness.delegate.beans.Delegate.DelegateKeys;
import io.harness.delegate.beans.DelegateProfile;
import io.harness.delegate.beans.DelegateProfileDetails;
import io.harness.delegate.beans.ScopingRuleDetails;
import io.harness.delegate.beans.ScopingRules;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.testframework.framework.Setup;

import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import javax.ws.rs.core.GenericType;
import lombok.extern.slf4j.Slf4j;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
@OwnedBy(HarnessTeam.DEL)
public class DelegateProfileApiFunctionalTest extends AbstractFunctionalTest {
  private static final String TEST_DELEGATE_PROFILE_IDENTIFIER = generateUuid();

  @Inject private WingsPersistence wingsPersistence;

  @Test
  @Owner(developers = MARKO)
  @Category(FunctionalTests.class)
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void testUpdateProfileScopingRules() {
    String delegateProfileId = wingsPersistence.save(DelegateProfile.builder()
                                                         .accountId(getAccount().getUuid())
                                                         .name(generateUuid())
                                                         .identifier(generateUuid())
                                                         .build());

    // Update scoping rules
    RestResponse<DelegateProfileDetails> updateScopingRulesRestResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .pathParam(DelegateKeys.delegateProfileId, delegateProfileId)
            .queryParam(DelegateKeys.accountId, getAccount().getUuid())
            .body(ScopingRules.builder()
                      .scopingRuleDetails(
                          Collections.singletonList(ScopingRuleDetails.builder()
                                                        .description("")
                                                        .applicationId("appId")
                                                        .environmentTypeId("envType1")
                                                        .environmentIds(new HashSet<>(Arrays.asList("env1", "env2")))
                                                        .serviceIds(new HashSet<>(Arrays.asList("srv1", "srv2")))
                                                        .build()))
                      .build())
            .put("/delegate-profiles/v2/{delegateProfileId}/scoping-rules")
            .as(new GenericType<RestResponse<DelegateProfileDetails>>() {}.getType());

    assertThat(updateScopingRulesRestResponse.getResource()).isNotNull();
    assertThat(updateScopingRulesRestResponse.getResource().getUuid()).isEqualTo(delegateProfileId);
    assertThat(updateScopingRulesRestResponse.getResource().getAccountId()).isEqualTo(getAccount().getUuid());
    assertThat(updateScopingRulesRestResponse.getResource().getScopingRules()).isNotEmpty();
    assertThat(updateScopingRulesRestResponse.getResource().getScopingRules().get(0)).isNotNull();
    assertThat(updateScopingRulesRestResponse.getResource().getScopingRules().get(0).getDescription())
        .isEqualTo("Application: appId; Service: srv1,srv2; Environment: env2,env1; ");
    assertThat(updateScopingRulesRestResponse.getResource().getScopingRules().get(0).getApplicationId())
        .isEqualTo("appId");
    assertThat(updateScopingRulesRestResponse.getResource().getScopingRules().get(0).getEnvironmentTypeId())
        .isEqualTo("envType1");
    assertThat(updateScopingRulesRestResponse.getResource().getScopingRules().get(0).getEnvironmentIds()).isNotEmpty();
    assertThat(updateScopingRulesRestResponse.getResource().getScopingRules().get(0).getEnvironmentIds())
        .containsExactlyInAnyOrder("env1", "env2");
    assertThat(updateScopingRulesRestResponse.getResource().getScopingRules().get(0).getServiceIds())
        .containsExactlyInAnyOrder("srv1", "srv2");
  }

  @Test
  @Owner(developers = VUK)
  @Category(FunctionalTests.class)
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void testGetProfile() {
    String delegateProfileId = wingsPersistence.save(DelegateProfile.builder()
                                                         .accountId(getAccount().getUuid())
                                                         .name(generateUuid())
                                                         .identifier(generateUuid())
                                                         .build());

    // Get profile
    RestResponse<DelegateProfileDetails> getProfileRestResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .pathParam(DelegateKeys.delegateProfileId, delegateProfileId)
            .queryParam(DelegateKeys.accountId, getAccount().getUuid())
            .get("/delegate-profiles/v2/{delegateProfileId}")
            .as(new GenericType<RestResponse<DelegateProfileDetails>>() {}.getType());

    assertThat(getProfileRestResponse.getResource()).isNotNull();
    assertThat(getProfileRestResponse.getResource().getUuid()).isEqualTo(delegateProfileId);
    assertThat(getProfileRestResponse.getResource().getAccountId()).isEqualTo(getAccount().getUuid());
  }

  @Test
  @Owner(developers = VUK)
  @Category(FunctionalTests.class)
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void testUpdateDelegateProfile() {
    String delegateProfileId = wingsPersistence.save(DelegateProfile.builder()
                                                         .accountId(getAccount().getUuid())
                                                         .name(generateUuid())
                                                         .identifier(generateUuid())
                                                         .build());

    // Update profile
    DelegateProfileDetails delegateProfileDetails =
        DelegateProfileDetails.builder().accountId(getAccount().getUuid()).name(generateUuid()).build();
    delegateProfileDetails.setScopingRules(
        Collections.singletonList(ScopingRuleDetails.builder()
                                      .description("")
                                      .applicationId("appId")
                                      .environmentTypeId("envTypeId")
                                      .environmentIds(new HashSet<>(Arrays.asList("env1", "env2")))
                                      .serviceIds(new HashSet<>(Arrays.asList("srv1", "srv2")))
                                      .build()));
    delegateProfileDetails.setSelectors(Arrays.asList("selector1", "selector2"));

    RestResponse<DelegateProfileDetails> updateProfileRestResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .pathParam(DelegateKeys.delegateProfileId, delegateProfileId)
            .queryParam(DelegateKeys.accountId, getAccount().getUuid())
            .body(delegateProfileDetails)
            .put("/delegate-profiles/v2/{delegateProfileId}")
            .as(new GenericType<RestResponse<DelegateProfileDetails>>() {}.getType());

    assertThat(updateProfileRestResponse.getResource()).isNotNull();
    assertThat(updateProfileRestResponse.getResource().getUuid()).isEqualTo(delegateProfileId);
    assertThat(updateProfileRestResponse.getResource().getAccountId()).isEqualTo(getAccount().getUuid());
    assertThat(updateProfileRestResponse.getResource().getScopingRules()).isNotEmpty();
    assertThat(updateProfileRestResponse.getResource().getScopingRules().get(0)).isNotNull();
    assertThat(updateProfileRestResponse.getResource().getScopingRules().get(0).getDescription())
        .isEqualTo("Application: appId; Service: srv1,srv2; Environment: env2,env1; ");
    assertThat(updateProfileRestResponse.getResource().getScopingRules().get(0).getApplicationId()).isEqualTo("appId");
    assertThat(updateProfileRestResponse.getResource().getScopingRules().get(0).getEnvironmentTypeId())
        .isEqualTo("envTypeId");
    assertThat(updateProfileRestResponse.getResource().getScopingRules().get(0).getEnvironmentIds()).isNotEmpty();
    assertThat(updateProfileRestResponse.getResource().getScopingRules().get(0).getEnvironmentIds())
        .containsExactlyInAnyOrder("env1", "env2");
    assertThat(updateProfileRestResponse.getResource().getScopingRules().get(0).getServiceIds())
        .containsExactlyInAnyOrder("srv1", "srv2");
    assertThat(updateProfileRestResponse.getResource().getSelectors())
        .containsExactlyInAnyOrder("selector1", "selector2");
  }

  @Test
  @Owner(developers = SANJA)
  @Category(FunctionalTests.class)
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void testAddDelegateProfile() {
    DelegateProfileDetails delegateProfileDetails = DelegateProfileDetails.builder()
                                                        .uuid(generateUuid())
                                                        .accountId(getAccount().getUuid())
                                                        .name(generateUuid())
                                                        .identifier(TEST_DELEGATE_PROFILE_IDENTIFIER)
                                                        .build();
    delegateProfileDetails.setScopingRules(
        Collections.singletonList(ScopingRuleDetails.builder()
                                      .description("")
                                      .applicationId("appId")
                                      .environmentTypeId("envTypeId")
                                      .environmentIds(new HashSet<>(Arrays.asList("env1", "env2")))
                                      .serviceIds(new HashSet<>(Arrays.asList("srv1", "srv2")))
                                      .build()));
    delegateProfileDetails.setSelectors(Arrays.asList("selector1", "selector2"));

    RestResponse<DelegateProfileDetails> addProfileRestResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam(DelegateKeys.accountId, getAccount().getUuid())
            .body(delegateProfileDetails)
            .post("/delegate-profiles/v2")
            .as(new GenericType<RestResponse<DelegateProfileDetails>>() {}.getType());

    assertThat(addProfileRestResponse.getResource()).isNotNull();
    assertThat(addProfileRestResponse.getResource().getUuid()).isNotBlank();
    assertThat(addProfileRestResponse.getResource().getAccountId()).isEqualTo(getAccount().getUuid());
    assertThat(addProfileRestResponse.getResource().getScopingRules()).isNotEmpty();
    assertThat(addProfileRestResponse.getResource().getScopingRules().get(0)).isNotNull();
    assertThat(addProfileRestResponse.getResource().getScopingRules().get(0).getDescription())
        .isEqualTo("Application: appId; Service: srv1,srv2; Environment: env2,env1; ");
    assertThat(addProfileRestResponse.getResource().getScopingRules().get(0).getApplicationId()).isEqualTo("appId");
    assertThat(addProfileRestResponse.getResource().getScopingRules().get(0).getEnvironmentTypeId())
        .isEqualTo("envTypeId");
    assertThat(addProfileRestResponse.getResource().getScopingRules().get(0).getEnvironmentIds()).isNotEmpty();
    assertThat(addProfileRestResponse.getResource().getScopingRules().get(0).getEnvironmentIds())
        .containsExactlyInAnyOrder("env1", "env2");
    assertThat(addProfileRestResponse.getResource().getScopingRules().get(0).getServiceIds())
        .containsExactlyInAnyOrder("srv1", "srv2");
    assertThat(addProfileRestResponse.getResource().getSelectors()).containsExactlyInAnyOrder("selector1", "selector2");
    assertThat(addProfileRestResponse.getResource().getIdentifier()).isEqualTo(TEST_DELEGATE_PROFILE_IDENTIFIER);
  }

  @Test
  @Owner(developers = VUK)
  @Category(FunctionalTests.class)
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void testUpdateProfileSelectors() {
    String delegateProfileId = wingsPersistence.save(DelegateProfile.builder()
                                                         .accountId(getAccount().getUuid())
                                                         .name(generateUuid())
                                                         .identifier(generateUuid())
                                                         .build());

    // Update Selectors
    RestResponse<DelegateProfileDetails> updateSelectorsRestResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .pathParam(DelegateKeys.delegateProfileId, delegateProfileId)
            .queryParam(DelegateKeys.accountId, getAccount().getUuid())
            .body(Arrays.asList("selector1", "selector2"))
            .put("/delegate-profiles/v2/{delegateProfileId}/selectors")
            .as(new GenericType<RestResponse<DelegateProfileDetails>>() {}.getType());

    assertThat(updateSelectorsRestResponse.getResource()).isNotNull();
    assertThat(updateSelectorsRestResponse.getResource().getUuid()).isEqualTo(delegateProfileId);
    assertThat(updateSelectorsRestResponse.getResource().getAccountId()).isEqualTo(getAccount().getUuid());
    assertThat(updateSelectorsRestResponse.getResource().getSelectors()).isNotEmpty();
    assertThat(updateSelectorsRestResponse.getResource().getSelectors().get(0)).isNotNull();
    assertThat(updateSelectorsRestResponse.getResource().getSelectors().get(0)).isEqualTo("selector1");
    assertThat(updateSelectorsRestResponse.getResource().getSelectors()).containsExactly("selector1", "selector2");
  }
  @Test
  @Owner(developers = SANJA)
  @Category(FunctionalTests.class)
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void testDeleteProfile() {
    String delegateProfileId = wingsPersistence.save(DelegateProfile.builder()
                                                         .accountId(getAccount().getUuid())
                                                         .name(generateUuid())
                                                         .identifier(generateUuid())
                                                         .build());

    Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .pathParam(DelegateKeys.delegateProfileId, delegateProfileId)
        .queryParam(DelegateKeys.accountId, getAccount().getUuid())
        .delete("/delegate-profiles/v2/{delegateProfileId}")
        .as(new GenericType<RestResponse<Void>>() {}.getType());

    DelegateProfile profile = wingsPersistence.get(DelegateProfile.class, delegateProfileId);
    assertThat(profile).isNull();
  }

  @Test
  @Owner(developers = SANJA)
  @Category(FunctionalTests.class)
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void testListDelegateProfiles() {
    wingsPersistence.save(DelegateProfile.builder()
                              .accountId(getAccount().getUuid())
                              .name(generateUuid())
                              .identifier(generateUuid())
                              .description("test1")
                              .build());
    wingsPersistence.save(DelegateProfile.builder()
                              .accountId(getAccount().getUuid())
                              .name(generateUuid())
                              .identifier(generateUuid())
                              .description("test2")
                              .build());
    wingsPersistence.save(DelegateProfile.builder()
                              .accountId(getAccount().getUuid())
                              .name(generateUuid())
                              .identifier(generateUuid())
                              .description("test3")
                              .build());
    // Get profile list
    RestResponse<PageResponse<DelegateProfileDetails>> listRestResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .queryParam(DelegateKeys.accountId, getAccount().getUuid())
            .queryParam("fieldsIncluded", new String[] {"description", "accountId"})
            .queryParam("limit", "2")
            .get("/delegate-profiles/v2")
            .as(new GenericType<RestResponse<PageResponse<DelegateProfileDetails>>>() {}.getType());

    assertThat(listRestResponse.getResource()).isNotNull();
    assertThat(listRestResponse.getResource().getLimit()).isEqualTo("2");
    assertThat(listRestResponse.getResource().getFieldsIncluded()).isEqualTo(Arrays.asList("description", "accountId"));
    assertThat(listRestResponse.getResource().getResponse().size()).isEqualTo(2);
    assertThat(listRestResponse.getResource().getResponse().get(0).getDescription()).isEqualTo("test3");
    assertThat(listRestResponse.getResource().getResponse().get(0).getName()).isEmpty();
    assertThat(listRestResponse.getResource().getResponse().get(1).getDescription()).isEqualTo("test2");
    assertThat(listRestResponse.getResource().getResponse().get(1).getName()).isEmpty();

    // Get profile list with accountId only
    listRestResponse = Setup.portal()
                           .auth()
                           .oauth2(bearerToken)
                           .queryParam(DelegateKeys.accountId, getAccount().getUuid())
                           .get("/delegate-profiles/v2")
                           .as(new GenericType<RestResponse<PageResponse<DelegateProfileDetails>>>() {}.getType());

    assertThat(listRestResponse.getResource()).isNotNull();
    assertThat(listRestResponse.getResource().getLimit()).isBlank();
    assertThat(listRestResponse.getResource().getFieldsIncluded()).isNullOrEmpty();
    assertThat(listRestResponse.getResource().getResponse().size()).isGreaterThanOrEqualTo(3);
    assertThat(listRestResponse.getResource().getResponse().get(0).getUuid()).isNotBlank();
    assertThat(listRestResponse.getResource().getResponse().get(0).getAccountId()).isNotBlank();
    assertThat(listRestResponse.getResource().getResponse().get(0).getDescription()).isNotBlank();
    assertThat(listRestResponse.getResource().getResponse().get(0).getName()).isNotBlank();
    assertThat(listRestResponse.getResource().getResponse().get(1).getUuid()).isNotBlank();
    assertThat(listRestResponse.getResource().getResponse().get(1).getAccountId()).isNotBlank();
    assertThat(listRestResponse.getResource().getResponse().get(1).getDescription()).isNotBlank();
    assertThat(listRestResponse.getResource().getResponse().get(1).getName()).isNotBlank();
    assertThat(listRestResponse.getResource().getResponse().get(2).getUuid()).isNotBlank();
    assertThat(listRestResponse.getResource().getResponse().get(2).getAccountId()).isNotBlank();
    assertThat(listRestResponse.getResource().getResponse().get(2).getDescription()).isNotBlank();
    assertThat(listRestResponse.getResource().getResponse().get(2).getName()).isNotBlank();
  }
}
