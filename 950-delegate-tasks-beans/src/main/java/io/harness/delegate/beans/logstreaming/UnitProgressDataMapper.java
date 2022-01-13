/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.logstreaming;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.UnitProgress;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@OwnedBy(HarnessTeam.CDP)
public class UnitProgressDataMapper {
  public static UnitProgressData toUnitProgressData(CommandUnitsProgress commandUnitsProgress) {
    if (commandUnitsProgress == null || commandUnitsProgress.getCommandUnitProgressMap() == null) {
      return null;
    }
    LinkedHashMap<String, CommandUnitProgress> commandUnitProgressMap =
        commandUnitsProgress.getCommandUnitProgressMap();

    List<UnitProgress> unitProgressList = new ArrayList<>();

    for (Map.Entry<String, CommandUnitProgress> commandUnitEntry : commandUnitProgressMap.entrySet()) {
      String unitName = commandUnitEntry.getKey();
      CommandUnitProgress commandUnitProgress = commandUnitEntry.getValue();

      UnitProgress unitProgress = UnitProgress.newBuilder()
                                      .setUnitName(unitName)
                                      .setStatus(commandUnitProgress.getStatus().getUnitStatus())
                                      .setStartTime(commandUnitProgress.getStartTime())
                                      .setEndTime(commandUnitProgress.getEndTime())
                                      .build();
      unitProgressList.add(unitProgress);
    }

    return UnitProgressData.builder().unitProgresses(unitProgressList).build();
  }

  public static CommandUnitsProgress toCommandUnitsProgress(UnitProgressData unitProgressData) {
    if (unitProgressData == null || isEmpty(unitProgressData.getUnitProgresses())) {
      return null;
    }

    List<UnitProgress> unitProgressList = unitProgressData.getUnitProgresses();
    LinkedHashMap<String, CommandUnitProgress> commandUnitProgressMap = new LinkedHashMap<>(unitProgressList.size());
    for (UnitProgress unitProgress : unitProgressList) {
      CommandUnitProgress commandUnitProgress =
          CommandUnitProgress.builder()
              .status(CommandExecutionStatus.getCommandExecutionStatus(unitProgress.getStatus()))
              .startTime(unitProgress.getStartTime())
              .endTime(unitProgress.getEndTime())
              .build();
      commandUnitProgressMap.put(unitProgress.getUnitName(), commandUnitProgress);
    }

    return CommandUnitsProgress.builder().commandUnitProgressMap(commandUnitProgressMap).build();
  }
}
