package io.harness.cvng.core.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Getter;

public enum ChangeSourceType {
  @JsonProperty("HarnessCD") HARNESS_CD(ChangeCategory.DEPLOYMENT),
  @JsonProperty("PagerDuty") PAGER_DUTY(ChangeCategory.ALERTS);

  @Getter private ChangeCategory changeCategory;

  ChangeSourceType(ChangeCategory changeCategory) {
    this.changeCategory = changeCategory;
  }

  public static List<ChangeSourceType> getForCategory(ChangeCategory changeCategory) {
    return Arrays.stream(ChangeSourceType.values())
        .filter(changeSourceType -> changeSourceType.getChangeCategory().equals(changeCategory))
        .collect(Collectors.toList());
  }
}
