package io.harness.functional.delegateservice;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.MARKO;
import static io.harness.rule.OwnerRule.SANJA;
import static io.harness.rule.OwnerRule.VUK;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.FunctionalTests;
import io.harness.delegate.beans.DelegateProfile;
import io.harness.delegate.beans.DelegateProfileDetails;
import io.harness.delegate.beans.ScopingRuleDetails;
import io.harness.delegate.beans.ScopingRules;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.testframework.framework.Setup;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Delegate.DelegateKeys;
import software.wings.dl.WingsPersistence;

import java.util.Arrays;
import java.util.HashSet;
import javax.ws.rs.core.GenericType;

@Slf4j
public class DelegateProfileApiFunctionalTest extends AbstractFunctionalTest {
  @Inject private WingsPersistence wingsPersistence;

  @Test
  @Owner(developers = MARKO)
  @Category(FunctionalTests.class)
  public void testUpdateProfileScopingRules() {
    String delegateProfileId =
        wingsPersistence.save(DelegateProfile.builder().accountId(getAccount().getUuid()).name(generateUuid()).build());

    // Update scoping rules
    RestResponse<DelegateProfileDetails> updateScopingRulesRestResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .pathParam(DelegateKeys.delegateProfileId, delegateProfileId)
            .queryParam(DelegateKeys.accountId, getAccount().getUuid())
            .body(
                ScopingRules.builder()
                    .scopingRuleDetails(Arrays.asList(ScopingRuleDetails.builder()
                                                          .description("desc")
                                                          .applicationId("appId")
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
        .isEqualTo("desc");
    assertThat(updateScopingRulesRestResponse.getResource().getScopingRules().get(0).getApplicationId())
        .isEqualTo("appId");
    assertThat(updateScopingRulesRestResponse.getResource().getScopingRules().get(0).getEnvironmentIds()).isNotEmpty();
    assertThat(updateScopingRulesRestResponse.getResource().getScopingRules().get(0).getEnvironmentIds())
        .containsExactlyInAnyOrder("env1", "env2");
    assertThat(updateScopingRulesRestResponse.getResource().getScopingRules().get(0).getServiceIds())
        .containsExactlyInAnyOrder("srv1", "srv2");
  }

  @Test
  @Owner(developers = VUK)
  @Category(FunctionalTests.class)
  public void testGetProfile() {
    String delegateProfileId =
        wingsPersistence.save(DelegateProfile.builder().accountId(getAccount().getUuid()).name(generateUuid()).build());

    // Get profile
    RestResponse<DelegateProfileDetails> updateScopingRulesRestResponse =
        Setup.portal()
            .auth()
            .oauth2(bearerToken)
            .pathParam(DelegateKeys.delegateProfileId, delegateProfileId)
            .queryParam(DelegateKeys.accountId, getAccount().getUuid())
            .get("/delegate-profiles/v2/{delegateProfileId}")
            .as(new GenericType<RestResponse<DelegateProfileDetails>>() {}.getType());

    assertThat(updateScopingRulesRestResponse.getResource()).isNotNull();
    assertThat(updateScopingRulesRestResponse.getResource().getUuid()).isEqualTo(delegateProfileId);
    assertThat(updateScopingRulesRestResponse.getResource().getAccountId()).isEqualTo(getAccount().getUuid());
  }

  @Test
  @Owner(developers = SANJA)
  @Category(FunctionalTests.class)
  public void testAddDelegateProfile() {
    DelegateProfileDetails delegateProfileDetails =
        DelegateProfileDetails.builder().accountId(getAccount().getUuid()).name(generateUuid()).build();
    delegateProfileDetails.setScopingRules(
        Arrays.asList(ScopingRuleDetails.builder()
                          .description("desc")
                          .applicationId("appId")
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
    assertThat(addProfileRestResponse.getResource().getUuid()).isNotNull();
    assertThat(addProfileRestResponse.getResource().getAccountId()).isEqualTo(getAccount().getUuid());
    assertThat(addProfileRestResponse.getResource().getScopingRules()).isNotEmpty();
    assertThat(addProfileRestResponse.getResource().getScopingRules().get(0)).isNotNull();
    assertThat(addProfileRestResponse.getResource().getScopingRules().get(0).getDescription()).isEqualTo("desc");
    assertThat(addProfileRestResponse.getResource().getScopingRules().get(0).getApplicationId()).isEqualTo("appId");
    assertThat(addProfileRestResponse.getResource().getScopingRules().get(0).getEnvironmentIds()).isNotEmpty();
    assertThat(addProfileRestResponse.getResource().getScopingRules().get(0).getEnvironmentIds())
        .containsExactlyInAnyOrder("env1", "env2");
    assertThat(addProfileRestResponse.getResource().getScopingRules().get(0).getServiceIds())
        .containsExactlyInAnyOrder("srv1", "srv2");
    assertThat(addProfileRestResponse.getResource().getSelectors()).containsExactlyInAnyOrder("selector1", "selector2");
  }

  @Test
  @Owner(developers = VUK)
  @Category(FunctionalTests.class)
  public void testUpdateProfileSelectors() {
    String delegateProfileId =
        wingsPersistence.save(DelegateProfile.builder().accountId(getAccount().getUuid()).name(generateUuid()).build());

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
}
