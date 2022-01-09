/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.connector.mappers.kubernetesMapper;

import static io.harness.encryption.Scope.ACCOUNT;
import static io.harness.encryption.SecretRefData.SECRET_DELIMINITER;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.kubernetescluster.K8sUserNamePassword;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesClusterDetails;
import io.harness.connector.entities.embedded.kubernetescluster.KubernetesDelegateDetails;
import io.harness.delegate.beans.connector.k8Connector.KubernetesAuthType;
import io.harness.exception.UnexpectedException;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import java.util.Collections;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class KubernetesConfigCastHelperTest extends CategoryTest {
  @InjectMocks KubernetesConfigCastHelper kubernetesConfigCastHelper;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void castToKubernetesDelegateCredentialTest() {
    String delegateName = "testDeleagete";
    KubernetesDelegateDetails delegateCredential =
        KubernetesDelegateDetails.builder().delegateSelectors(Collections.singleton(delegateName)).build();
    try {
      KubernetesDelegateDetails delegateDetails =
          kubernetesConfigCastHelper.castToKubernetesDelegateCredential(delegateCredential);
    } catch (ClassCastException ex) {
      assertThat(false).isTrue();
    }
  }

  @Test(expected = UnexpectedException.class)
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void castManualCredentialToKubernetesDelegateCredentialTest() {
    String masterURL = "masterURL";
    String userName = "userName";
    String passwordIdentifier = "passwordIdentifier";
    String passwordRef = "acc" + SECRET_DELIMINITER + passwordIdentifier;
    K8sUserNamePassword k8sUserNamePassword =
        K8sUserNamePassword.builder().userName(userName).passwordRef(passwordRef).build();
    KubernetesClusterDetails kubernetesClusterDetails = KubernetesClusterDetails.builder()
                                                            .masterUrl(masterURL)
                                                            .authType(KubernetesAuthType.USER_PASSWORD)
                                                            .auth(k8sUserNamePassword)
                                                            .build();
    KubernetesDelegateDetails delegateDetails =
        kubernetesConfigCastHelper.castToKubernetesDelegateCredential(kubernetesClusterDetails);
  }

  @Test
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void castToManualKubernetesCredentialsTest() {
    String masterURL = "masterURL";
    String userName = "userName";
    String passwordIdentifier = "passwordIdentifier";
    String passwordRef = ACCOUNT.getYamlRepresentation() + SECRET_DELIMINITER + passwordIdentifier;
    K8sUserNamePassword k8sUserNamePassword =
        K8sUserNamePassword.builder().userName(userName).passwordRef(passwordRef).build();
    KubernetesClusterDetails kubernetesClusterDetails = KubernetesClusterDetails.builder()
                                                            .masterUrl(masterURL)
                                                            .authType(KubernetesAuthType.USER_PASSWORD)
                                                            .auth(k8sUserNamePassword)
                                                            .build();
    try {
      KubernetesClusterDetails kubernetesClusterConfig =
          kubernetesConfigCastHelper.castToManualKubernetesCredentials(kubernetesClusterDetails);
    } catch (ClassCastException ex) {
      assertThat(false).isTrue();
    }
  }

  @Test(expected = UnexpectedException.class)
  @Owner(developers = OwnerRule.DEEPAK)
  @Category(UnitTests.class)
  public void castDelegateCredsToManualKubernetesCredentialsTest() {
    String delegateName = "testDeleagete";
    KubernetesDelegateDetails delegateCredential =
        KubernetesDelegateDetails.builder().delegateSelectors(Collections.singleton(delegateName)).build();
    KubernetesClusterDetails kubernetesClusterConfig =
        kubernetesConfigCastHelper.castToManualKubernetesCredentials(delegateCredential);
  }
}
