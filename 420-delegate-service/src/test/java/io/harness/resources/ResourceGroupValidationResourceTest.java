/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.resources;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.MARKO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.DelegateServiceTestBase;
import io.harness.accesscontrol.clients.AccessControlClient;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.resources.ResourceGroupValidationResource;
import io.harness.ng.core.delegate.DelegateConfigResourceValidationResponse;
import io.harness.ng.core.delegate.DelegateResourceValidationResponse;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.service.intfc.DelegateSetupService;

import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

@OwnedBy(HarnessTeam.DEL)
public class ResourceGroupValidationResourceTest extends DelegateServiceTestBase {
  private static final String ACCOUNT_ID = generateUuid();
  private static final String ORG_ID = generateUuid();
  private static final String PROJECT_ID = generateUuid();

  @Mock private AccessControlClient accessControlClient;
  @Mock private DelegateSetupService delegateSetupService;
  @InjectMocks private ResourceGroupValidationResource resourceGroupValidationResource;

  @Before
  public void setUp() {
    initMocks(this);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldValidateDelegateGroups() {
    List<String> delegateGroupIds = Arrays.asList("id1", "id2");
    List<Boolean> delegateGroupValidationData = Arrays.asList(Boolean.FALSE, Boolean.TRUE);

    Mockito.when(delegateSetupService.validateDelegateGroups(ACCOUNT_ID, ORG_ID, PROJECT_ID, delegateGroupIds))
        .thenReturn(delegateGroupValidationData);
    RestResponse<DelegateResourceValidationResponse> response =
        resourceGroupValidationResource.validateDelegateGroups(ACCOUNT_ID, ORG_ID, PROJECT_ID, delegateGroupIds);

    assertThat(response).isNotNull();
    assertThat(response.getResource()).isNotNull();
    assertThat(response.getResource().getDelegateValidityData()).isEqualTo(delegateGroupValidationData);

    Mockito.verify(delegateSetupService).validateDelegateGroups(ACCOUNT_ID, ORG_ID, PROJECT_ID, delegateGroupIds);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldValidateDelegateConfigs() {
    List<String> delegateConfigIds = Arrays.asList("id1", "id2");
    List<Boolean> delegateConfigValidationData = Arrays.asList(Boolean.FALSE, Boolean.TRUE);

    Mockito.when(delegateSetupService.validateDelegateConfigurations(ACCOUNT_ID, ORG_ID, PROJECT_ID, delegateConfigIds))
        .thenReturn(delegateConfigValidationData);
    RestResponse<DelegateConfigResourceValidationResponse> response =
        resourceGroupValidationResource.validateDelegateConfigurations(
            ACCOUNT_ID, ORG_ID, PROJECT_ID, delegateConfigIds);

    assertThat(response).isNotNull();
    assertThat(response.getResource()).isNotNull();
    assertThat(response.getResource().getDelegateConfigValidityData()).isEqualTo(delegateConfigValidationData);

    Mockito.verify(delegateSetupService)
        .validateDelegateConfigurations(ACCOUNT_ID, ORG_ID, PROJECT_ID, delegateConfigIds);
  }
}
