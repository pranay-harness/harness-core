package io.harness.projectsummary;

import static io.harness.rule.OwnerRule.DEEPAK;

import static org.powermock.api.mockito.PowerMockito.mockStatic;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.maintenance.MaintenanceController;
import io.harness.ng.projectsummary.ProjectSummaryServiceImpl;
import io.harness.rule.Owner;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(MaintenanceController.class)
@OwnedBy(HarnessTeam.CE)
@Slf4j
@PowerMockIgnore({"javax.security.*", "javax.net.*"})
public class ProjectSummaryTest extends CategoryTest {
  DSLContext dslContext = DSL.using("jdbc:postgresql://34.83.25.129:5432/harnessdev", "harnessappdev", "harnessappdev");
  @InjectMocks ProjectSummaryServiceImpl projectSummaryService;
  String accountIdentifier = "accountIdentifier";
  String orgIdentifier = "orgIdentifier";

  @Before
  public void setup() throws Exception {
    mockStatic(MaintenanceController.class);
    FieldUtils.writeField(projectSummaryService, "dslContext", dslContext, true);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testCreate() throws Exception {
    String projectIdentifier = "projectIdentifier4";
    projectSummaryService.createProjectSummary(accountIdentifier, orgIdentifier, projectIdentifier, "Next Gen Project");
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testUpdate() throws Exception {
    String projectIdentifier = "projectIdentifier";
    projectSummaryService.updateDeploymentCount(accountIdentifier, orgIdentifier, projectIdentifier, 110, 201, 216);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void testAggregation() throws Exception {
    projectSummaryService.aggregateTheProjectData(orgIdentifier);
  }
}
