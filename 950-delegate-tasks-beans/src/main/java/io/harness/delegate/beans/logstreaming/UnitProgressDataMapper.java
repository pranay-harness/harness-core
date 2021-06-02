package io.harness.delegate.beans.logstreaming;

import io.harness.logging.UnitProgress;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
}
