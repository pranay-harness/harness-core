package software.wings.helpers.ext.pcf;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.apache.commons.lang3.StringUtils;
import org.cloudfoundry.operations.applications.ApplicationSummary;
import org.cloudfoundry.operations.organizations.OrganizationSummary;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;

import java.util.Arrays;
import java.util.List;
public class PivotalDeploymentManagerImplTest extends WingsBaseTest {
  @Mock PcfClientImpl client;
  @InjectMocks @Inject PcfDeploymentManagerImpl deploymentManager;

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testGetOrganizations() throws Exception {
    OrganizationSummary summary1 = OrganizationSummary.builder().id("1").name("org1").build();
    OrganizationSummary summary2 = OrganizationSummary.builder().id("2").name("org2").build();

    when(client.getOrganizations(any())).thenReturn(Arrays.asList(summary1, summary2));
    List<String> orgs = deploymentManager.getOrganizations(null);
    assertThat(orgs).isNotNull();
    assertThat(orgs).containsExactly("org1", "org2");
  }

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void getAppPrefixByRemovingNumber() {
    assertThat(StringUtils.EMPTY).isEqualTo(deploymentManager.getAppPrefixByRemovingNumber(null));
    assertThat("a_b_c").isEqualTo(deploymentManager.getAppPrefixByRemovingNumber("a_b_c__4"));
  }

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void getMatchesPrefix() {
    ApplicationSummary applicationSummary = ApplicationSummary.builder()
                                                .id("id1")
                                                .name("a__b__c__1")
                                                .diskQuota(1)
                                                .instances(1)
                                                .memoryLimit(1)
                                                .requestedState("RUNNING")
                                                .runningInstances(0)
                                                .build();

    assertThat(deploymentManager.matchesPrefix("a__b__c", applicationSummary)).isTrue();

    applicationSummary = ApplicationSummary.builder()
                             .id("id1")
                             .name("a__b__c__2")
                             .diskQuota(1)
                             .instances(1)
                             .memoryLimit(1)
                             .requestedState("RUNNING")
                             .runningInstances(0)
                             .build();
    assertThat(deploymentManager.matchesPrefix("a__b__c", applicationSummary)).isTrue();

    applicationSummary = ApplicationSummary.builder()
                             .id("id1")
                             .name("a__b__c__d__2")
                             .diskQuota(1)
                             .instances(1)
                             .memoryLimit(1)
                             .requestedState("RUNNING")
                             .runningInstances(0)
                             .build();
    assertThat(deploymentManager.matchesPrefix("a__b__c", applicationSummary)).isFalse();

    applicationSummary = ApplicationSummary.builder()
                             .id("id1")
                             .name("a__b__2")
                             .diskQuota(1)
                             .instances(1)
                             .memoryLimit(1)
                             .requestedState("RUNNING")
                             .runningInstances(0)
                             .build();
    assertThat(deploymentManager.matchesPrefix("a__b__c", applicationSummary)).isFalse();

    applicationSummary = ApplicationSummary.builder()
                             .id("id1")
                             .name("BG__1_vars.yml")
                             .diskQuota(1)
                             .instances(1)
                             .memoryLimit(1)
                             .requestedState("RUNNING")
                             .runningInstances(0)
                             .build();
    assertThat(deploymentManager.matchesPrefix("BG", applicationSummary)).isFalse();
  }
}
