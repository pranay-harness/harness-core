/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.execution.export.metadata;

import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;

import software.wings.beans.command.CommandUnitDetails;
import software.wings.beans.command.CommandUnitDetails.CommandUnitType;

import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ActivityCommandUnitMetadataTest extends CategoryTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testFromCommandUnitDetailsList() {
    List<ActivityCommandUnitMetadata> activityCommandUnitMetadataList =
        ActivityCommandUnitMetadata.fromCommandUnitDetailsList(
            Collections.singletonList(CommandUnitDetails.builder()
                                          .name("n")
                                          .commandUnitType(CommandUnitType.COMMAND)
                                          .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                                          .build()));
    assertThat(activityCommandUnitMetadataList).isNotNull();
    assertThat(activityCommandUnitMetadataList.size()).isEqualTo(1);
    validateActivityCommandUnitMetadata(activityCommandUnitMetadataList.get(0));
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testFromCommandUnitDetails() {
    assertThat(ActivityCommandUnitMetadata.fromCommandUnitDetails(null)).isNull();
    ActivityCommandUnitMetadata activityCommandUnitMetadata =
        ActivityCommandUnitMetadata.fromCommandUnitDetails(CommandUnitDetails.builder()
                                                               .name("n")
                                                               .commandUnitType(CommandUnitType.COMMAND)
                                                               .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
                                                               .build());
    validateActivityCommandUnitMetadata(activityCommandUnitMetadata);
  }

  private void validateActivityCommandUnitMetadata(ActivityCommandUnitMetadata activityCommandUnitMetadata) {
    assertThat(activityCommandUnitMetadata).isNotNull();
    assertThat(activityCommandUnitMetadata.getName()).isEqualTo("n");
    assertThat(activityCommandUnitMetadata.getType()).isEqualTo(CommandUnitType.COMMAND);
    assertThat(activityCommandUnitMetadata.getStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
  }
}
