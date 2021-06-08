package io.harness.argo;

import static io.harness.rule.OwnerRule.SRINIVAS;

import io.harness.ApiServiceTestBase;
import io.harness.argo.beans.ArgoConfigInternal;
import io.harness.argo.beans.ClusterResourceTreeDTO;
import io.harness.argo.beans.ManifestDiff;
import io.harness.argo.service.ArgoCdServiceImpl;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import java.util.List;
import javax.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ArgoCdServiceTest extends ApiServiceTestBase {
  @Inject private ArgoCdServiceImpl argoCdService;

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldReturnApp() throws Exception {
    List<ManifestDiff> app = argoCdService.fetchManifestDiff(ArgoConfigInternal.builder()
                                                                 .argoServerUrl("https://34.136.244.4/")
                                                                 .username("admin")
                                                                 .password("abkabfk")
                                                                 .build(),
        "guestbook");

    System.out.println("appName = " + app);
  }
}
