package io.harness.ng.core.environment.mappers;

import static io.harness.rule.OwnerRule.ARCHIT;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.ng.core.environment.dto.EnvironmentRequestDTO;
import io.harness.ng.core.environment.dto.EnvironmentResponseDTO;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class EnvironmentMapperTest extends CategoryTest {
  EnvironmentRequestDTO environmentRequestDTO;
  EnvironmentResponseDTO environmentResponseDTO;
  Environment requestEnvironment;
  Environment responseEnvironment;

  @Before
  public void setUp() {
    environmentRequestDTO = EnvironmentRequestDTO.builder()
                                .identifier("ENV")
                                .orgIdentifier("ORG_ID")
                                .projectIdentifier("PROJECT_ID")
                                .type(EnvironmentType.PreProduction)
                                .build();

    environmentResponseDTO = EnvironmentResponseDTO.builder()
                                 .accountId("ACCOUNT_ID")
                                 .identifier("ENV")
                                 .orgIdentifier("ORG_ID")
                                 .projectIdentifier("PROJECT_ID")
                                 .type(EnvironmentType.PreProduction)
                                 .deleted(false)
                                 .build();

    requestEnvironment = Environment.builder()
                             .accountId("ACCOUNT_ID")
                             .identifier("ENV")
                             .orgIdentifier("ORG_ID")
                             .projectIdentifier("PROJECT_ID")
                             .type(EnvironmentType.PreProduction)
                             .deleted(false)
                             .build();

    responseEnvironment = Environment.builder()
                              .accountId("ACCOUNT_ID")
                              .identifier("ENV")
                              .orgIdentifier("ORG_ID")
                              .projectIdentifier("PROJECT_ID")
                              .type(EnvironmentType.PreProduction)
                              .id("UUID")
                              .deleted(false)
                              .build();
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testToEnvironment() {
    Environment environment = EnvironmentMapper.toEnvironmentEntity("ACCOUNT_ID", environmentRequestDTO);
    assertThat(environment).isNotNull();
    assertThat(environment).isEqualTo(requestEnvironment);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testWriteDTO() {
    EnvironmentResponseDTO environmentDTO = EnvironmentMapper.writeDTO(responseEnvironment);
    assertThat(environmentDTO).isNotNull();
    assertThat(environmentDTO).isEqualTo(environmentResponseDTO);
  }
}