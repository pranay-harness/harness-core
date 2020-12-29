package io.harness.cvng.core.jobs;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.VerificationApplication;
import io.harness.cvng.beans.CVMonitoringCategory;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.SplunkCVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.models.VerificationType;
import io.harness.cvng.verificationjob.beans.Sensitivity;
import io.harness.cvng.verificationjob.beans.TestVerificationJobDTO;
import io.harness.cvng.verificationjob.beans.VerificationJobDTO;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.eventsframework.entity_crud.project.ProjectEntityChangeDTO;
import io.harness.persistence.PersistentEntity;
import io.harness.rule.Owner;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.reflections.Reflections;

public class ProjectChangeEventMessageProcessorTest extends CvNextGenTest {
  @Inject private ProjectChangeEventMessageProcessor projectChangeEventMessageProcessor;
  @Inject private CVConfigService cvConfigService;
  @Inject private VerificationJobService verificationJobService;
  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testProcessDeleteAction() {
    String accountId = generateUuid();
    String orgIdentifier = generateUuid();
    CVConfig cvConfig1 = createCVConfig(accountId, orgIdentifier, "project1");
    CVConfig cvConfig2 = createCVConfig(accountId, orgIdentifier, "project2");
    cvConfigService.save(cvConfig1);
    cvConfigService.save(cvConfig2);
    VerificationJobDTO verificationJobDTO1 = createVerificationJobDTO(orgIdentifier, "project1");
    VerificationJobDTO verificationJobDTO2 = createVerificationJobDTO(orgIdentifier, "project2");
    verificationJobService.upsert(accountId, verificationJobDTO1);
    verificationJobService.upsert(accountId, verificationJobDTO2);
    projectChangeEventMessageProcessor.processDeleteAction(ProjectEntityChangeDTO.newBuilder()
                                                               .setAccountIdentifier(accountId)
                                                               .setOrgIdentifier(orgIdentifier)
                                                               .setIdentifier("project1")
                                                               .build());
    assertThat(verificationJobService.getVerificationJobDTO(accountId, verificationJobDTO1.getIdentifier())).isNull();
    assertThat(verificationJobService.getVerificationJobDTO(accountId, verificationJobDTO2.getIdentifier()))
        .isNotNull();
    assertThat(cvConfigService.get(cvConfig1.getUuid())).isNull();
    assertThat(cvConfigService.get(cvConfig2.getUuid())).isNotNull();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testProcessDeleteAction_entitiesList() {
    Set<Class<? extends PersistentEntity>> entitiesWithVerificationTaskId = new HashSet<>();
    entitiesWithVerificationTaskId.addAll(ProjectChangeEventMessageProcessor.ENTITIES_MAP.keySet());
    Reflections reflections = new Reflections(VerificationApplication.class.getPackage().getName());
    Set<Class<? extends PersistentEntity>> withProjectIdentifier = new HashSet<>();
    reflections.getSubTypesOf(PersistentEntity.class).forEach(entity -> {
      if (doesClassContainField(entity, "accountId") && doesClassContainField(entity, "orgIdentifier")
          && doesClassContainField(entity, "projectIdentifier")) {
        withProjectIdentifier.add(entity);
      }
    });
    assertThat(entitiesWithVerificationTaskId)
        .isEqualTo(withProjectIdentifier)
        .withFailMessage(
            "Entities with projectIdentifier found which is not added to ENTITIES_TO_DELETE_BY_PROJECT_IDENTIFIER or ENTITIES_DELETE_BLACKLIST_BY_PROJECT_IDENTIFIER");
  }

  private boolean doesClassContainField(Class<?> clazz, String fieldName) {
    return Arrays.stream(clazz.getDeclaredFields()).anyMatch(f -> f.getName().equals(fieldName));
  }

  private CVConfig createCVConfig(String accountId, String orgIdentifier, String projectIdentifier) {
    SplunkCVConfig cvConfig = new SplunkCVConfig();
    fillCommon(cvConfig, accountId, orgIdentifier, projectIdentifier);
    cvConfig.setQuery("exception");
    cvConfig.setServiceInstanceIdentifier(generateUuid());
    return cvConfig;
  }

  private void fillCommon(CVConfig cvConfig, String accountId, String orgIdentifier, String projectIdentifier) {
    cvConfig.setVerificationType(VerificationType.LOG);
    cvConfig.setAccountId(accountId);
    cvConfig.setConnectorIdentifier(generateUuid());
    cvConfig.setServiceIdentifier("service");
    cvConfig.setEnvIdentifier("env");
    cvConfig.setOrgIdentifier(orgIdentifier);
    cvConfig.setProjectIdentifier(projectIdentifier);
    cvConfig.setCategory(CVMonitoringCategory.PERFORMANCE);
    cvConfig.setProductName(generateUuid());
    cvConfig.setIdentifier(generateUuid());
    cvConfig.setMonitoringSourceName(generateUuid());
  }

  private VerificationJobDTO createVerificationJobDTO(String orgIdentifier, String projectIdentifier) {
    TestVerificationJobDTO testVerificationJobDTO = new TestVerificationJobDTO();
    testVerificationJobDTO.setIdentifier(generateUuid());
    testVerificationJobDTO.setJobName(generateUuid());
    testVerificationJobDTO.setDataSources(Lists.newArrayList(DataSourceType.APP_DYNAMICS));
    testVerificationJobDTO.setBaselineVerificationJobInstanceId(null);
    testVerificationJobDTO.setSensitivity(Sensitivity.MEDIUM.name());
    testVerificationJobDTO.setServiceIdentifier(generateUuid());
    testVerificationJobDTO.setEnvIdentifier(generateUuid());
    testVerificationJobDTO.setBaselineVerificationJobInstanceId(generateUuid());
    testVerificationJobDTO.setDuration("15m");
    testVerificationJobDTO.setProjectIdentifier(projectIdentifier);
    testVerificationJobDTO.setOrgIdentifier(orgIdentifier);
    return testVerificationJobDTO;
  }
}