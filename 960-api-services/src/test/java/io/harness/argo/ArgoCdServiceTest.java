package io.harness.argo;

import static io.harness.rule.OwnerRule.SRINIVAS;

import io.harness.ApiServiceTestBase;
import io.harness.argo.beans.ArgoConfigInternal;
import io.harness.argo.beans.ClusterResourceTreeDTO;
import io.harness.argo.service.ArgoCdServiceImpl;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.inject.Inject;

public class ArgoCdServiceTest extends ApiServiceTestBase {

  @Inject private ArgoCdServiceImpl argoCdService;

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldReturnApp() throws Exception {
    ClusterResourceTreeDTO app = argoCdService.fetchResourceTree(ArgoConfigInternal.builder()
                                                                     .argoServerUrl("https://34.136.244.4/")
                                                                     .username("admin")
                                                                     .password("abcd")
                                                                     .build(),
        "guestbook");

    System.out.println("appName = " + app);

  }
}
