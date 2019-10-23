package io.harness.config;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Data
@Slf4j
public class WorkersConfiguration {
  @JsonProperty("active") Map<String, Boolean> active;

  public boolean confirmWorkerIsActive(Class cls) {
    boolean flag = true;
    if (isEmpty(active)) {
      return flag;
    }

    final String name = cls.getName();
    final Boolean classFlag = active.get(name);
    if (classFlag != null) {
      return classFlag.booleanValue();
    }

    int index = name.indexOf('.');
    while (index != -1) {
      final Boolean packageFlag = active.get(name.substring(0, index));
      if (packageFlag != null) {
        flag = packageFlag.booleanValue();
      }
      index = name.indexOf('.', index + 1);
    }

    return flag;
  }
}
