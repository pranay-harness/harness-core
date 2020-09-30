package io.harness.cvng.verificationjob.services.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.PRAVEEN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.verificationjob.beans.Sensitivity;
import io.harness.cvng.verificationjob.beans.TestVerificationJobDTO;
import io.harness.cvng.verificationjob.beans.VerificationJobDTO;
import io.harness.cvng.verificationjob.services.api.VerificationJobService;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;

public class VerificationJobServiceImplTest extends CvNextGenTest {
  @Inject private VerificationJobService verificationJobService;
  private String identifier;
  private String accountId;
  @Before
  public void setup() {
    identifier = "test-verification-harness";
    accountId = generateUuid();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testUpsert_newJobCreation() {
    VerificationJobDTO verificationJobDTO = createDTO();
    verificationJobService.upsert(accountId, verificationJobDTO);
    VerificationJobDTO inserted =
        verificationJobService.getVerificationJobDTO(accountId, verificationJobDTO.getIdentifier());
    assertThat(inserted).isEqualTo(verificationJobDTO);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testUpsert_invalidJob() {
    VerificationJobDTO verificationJobDTO = createDTO();
    verificationJobDTO.setEnvIdentifier(null);
    assertThatThrownBy(() -> verificationJobService.upsert(accountId, verificationJobDTO))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("envIdentifier should not be null");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testUpsert_updateExisting() {
    VerificationJobDTO verificationJobDTO = createDTO();
    verificationJobService.upsert(accountId, verificationJobDTO);
    VerificationJobDTO inserted =
        verificationJobService.getVerificationJobDTO(accountId, verificationJobDTO.getIdentifier());
    assertThat(inserted).isEqualTo(verificationJobDTO);
    verificationJobDTO.setEnvIdentifier("updated_env");
    verificationJobService.upsert(accountId, verificationJobDTO);
    VerificationJobDTO updated =
        verificationJobService.getVerificationJobDTO(accountId, verificationJobDTO.getIdentifier());
    assertThat(updated).isNotEqualTo(inserted);
    assertThat(updated).isEqualTo(verificationJobDTO);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetVerificationJobDTO_invalidIdentifier() {
    VerificationJobDTO updated = verificationJobService.getVerificationJobDTO(accountId, "invalid");
    assertThat(updated).isEqualTo(null);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetVerificationJobDTO_validIdentifier() {
    VerificationJobDTO verificationJobDTO = createDTO();
    verificationJobService.upsert(accountId, verificationJobDTO);
    VerificationJobDTO updated =
        verificationJobService.getVerificationJobDTO(accountId, verificationJobDTO.getIdentifier());
    assertThat(updated).isEqualTo(verificationJobDTO);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testDelete_validIdentifier() {
    VerificationJobDTO verificationJobDTO = createDTO();
    verificationJobService.upsert(accountId, verificationJobDTO);
    verificationJobService.delete(accountId, verificationJobDTO.getIdentifier());
    assertThat(verificationJobService.getVerificationJobDTO(accountId, verificationJobDTO.getIdentifier()))
        .isEqualTo(null);
  }

  @Test
  @Owner(developers = PRAVEEN)
  @Category(UnitTests.class)
  public void testUpsert_newJobCreationWithRuntimeParams() {
    VerificationJobDTO verificationJobDTO = createDTOWithRuntimeParams();
    verificationJobService.upsert(accountId, verificationJobDTO);
    VerificationJobDTO inserted =
        verificationJobService.getVerificationJobDTO(accountId, verificationJobDTO.getIdentifier());
    assertThat(inserted).isEqualTo(verificationJobDTO);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testList_empty() {
    assertThat(verificationJobService.list(accountId, generateUuid(), generateUuid())).isEmpty();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testList_notEmpty() {
    VerificationJobDTO verificationJobDTO = createDTOWithRuntimeParams();
    verificationJobService.upsert(accountId, verificationJobDTO);
    List<VerificationJobDTO> verificationJobDTOList = verificationJobService.list(
        accountId, verificationJobDTO.getProjectIdentifier(), verificationJobDTO.getOrgIdentifier());
    assertThat(verificationJobDTOList).hasSize(1);
    assertThat(verificationJobDTOList.get(0)).isEqualTo(verificationJobDTO);
  }

  private VerificationJobDTO createDTO() {
    TestVerificationJobDTO testVerificationJobDTO = new TestVerificationJobDTO();
    testVerificationJobDTO.setIdentifier(identifier);
    testVerificationJobDTO.setJobName(generateUuid());
    testVerificationJobDTO.setDataSources(Lists.newArrayList(DataSourceType.APP_DYNAMICS));
    testVerificationJobDTO.setBaselineVerificationTaskIdentifier(null);
    testVerificationJobDTO.setSensitivity(Sensitivity.MEDIUM);
    testVerificationJobDTO.setServiceIdentifier(generateUuid());
    testVerificationJobDTO.setEnvIdentifier(generateUuid());
    testVerificationJobDTO.setBaselineVerificationTaskIdentifier(generateUuid());
    testVerificationJobDTO.setDuration("15m");
    testVerificationJobDTO.setProjectIdentifier(generateUuid());
    testVerificationJobDTO.setOrgIdentifier(generateUuid());
    return testVerificationJobDTO;
  }

  private VerificationJobDTO createDTOWithRuntimeParams() {
    TestVerificationJobDTO testVerificationJobDTO = (TestVerificationJobDTO) createDTO();
    testVerificationJobDTO.setEnvIdentifier("${envIdentifier}");
    testVerificationJobDTO.setServiceIdentifier("${serviceIdentifier}");
    return testVerificationJobDTO;
  }
}