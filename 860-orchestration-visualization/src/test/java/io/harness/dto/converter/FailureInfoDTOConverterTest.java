/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.dto.converter;

import static io.harness.rule.OwnerRule.ALEXEI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.OrchestrationVisualizationTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.dto.FailureInfoDTO;
import io.harness.eraro.ErrorCode;
import io.harness.eraro.Level;
import io.harness.eraro.ResponseMessage;
import io.harness.pms.contracts.execution.failure.FailureData;
import io.harness.pms.contracts.execution.failure.FailureInfo;
import io.harness.pms.contracts.execution.failure.FailureType;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableList;
import java.util.EnumSet;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class FailureInfoDTOConverterTest extends OrchestrationVisualizationTestBase {
  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void toFailureInfoDTOWhenInfoIsNUll() {
    assertThat(FailureInfoDTOConverter.toFailureInfoDTO(null)).isNull();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void toFailureInfoDTO() {
    String errorMessage = "This is error message";
    String errorCode = "code";
    FailureInfo failureInfo = FailureInfo.newBuilder()
                                  .setErrorMessage(errorMessage)
                                  .addFailureTypes(FailureType.APPLICATION_FAILURE)
                                  .addFailureData(FailureData.newBuilder()
                                                      .setCode(ErrorCode.ACCESS_DENIED.name())
                                                      .setLevel(Level.ERROR.name())
                                                      .setMessage("message")
                                                      .addFailureTypes(FailureType.CONNECTIVITY_FAILURE)
                                                      .build())
                                  .build();

    FailureInfoDTO failureInfoDTO = FailureInfoDTO.builder()
                                        .message(errorMessage)
                                        .failureTypeList(EnumSet.of(io.harness.exception.FailureType.APPLICATION_ERROR))
                                        .responseMessages(ImmutableList.of(
                                            ResponseMessage.builder()
                                                .code(ErrorCode.ACCESS_DENIED)
                                                .level(Level.ERROR)
                                                .failureTypes(EnumSet.of(io.harness.exception.FailureType.CONNECTIVITY))
                                                .message("message")
                                                .build()))
                                        .build();

    assertThat(FailureInfoDTOConverter.toFailureInfoDTO(failureInfo)).isEqualTo(failureInfoDTO);
  }
}
