package io.harness.cvng.beans.change;
import io.harness.cvng.beans.activity.ActivityType;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.collections4.MapUtils;

@AllArgsConstructor
public enum ChangeSourceType {
  @JsonProperty("HarnessCDNextGen") HARNESS_CD(ChangeCategory.DEPLOYMENT, ActivityType.HARNESS_CD),
  @JsonProperty("PagerDuty") PAGER_DUTY(ChangeCategory.ALERTS, ActivityType.PAGER_DUTY),
  @JsonProperty("K8sCluster") KUBERNETES(ChangeCategory.INFRASTRUCTURE, ActivityType.KUBERNETES),
  @JsonProperty("HarnessCD") HARNESS_CD_CURRENT_GEN(ChangeCategory.DEPLOYMENT, ActivityType.HARNESS_CD_CURRENT_GEN);

  private static Map<ActivityType, ChangeSourceType> ACTIVITY_TO_CHANGE_SOURCE_TYPE_MAP;
  private static Map<ChangeCategory, List<ChangeSourceType>> CATEGORY_TO_CHANGE_SOURCE_TYPES_MAP;

  @Getter private ChangeCategory changeCategory;
  @Getter private ActivityType activityType;

  public static List<ChangeSourceType> getForCategory(ChangeCategory changeCategory) {
    if (MapUtils.isEmpty(CATEGORY_TO_CHANGE_SOURCE_TYPES_MAP)) {
      CATEGORY_TO_CHANGE_SOURCE_TYPES_MAP =
          Arrays.stream(ChangeSourceType.values()).collect(Collectors.groupingBy(ChangeSourceType::getChangeCategory));
    }
    return CATEGORY_TO_CHANGE_SOURCE_TYPES_MAP.getOrDefault(changeCategory, Collections.emptyList());
  }

  public static ChangeSourceType ofActivityType(ActivityType activityType) {
    if (MapUtils.isEmpty(ACTIVITY_TO_CHANGE_SOURCE_TYPE_MAP)) {
      ACTIVITY_TO_CHANGE_SOURCE_TYPE_MAP =
          Arrays.stream(ChangeSourceType.values())
              .collect(Collectors.toMap(ChangeSourceType::getActivityType, Function.identity()));
    }
    if (!ACTIVITY_TO_CHANGE_SOURCE_TYPE_MAP.containsKey(activityType)) {
      throw new IllegalStateException("Activity type:" + activityType + " not mapped to ChangeSourceType");
    }
    return ACTIVITY_TO_CHANGE_SOURCE_TYPE_MAP.get(activityType);
  }
}
