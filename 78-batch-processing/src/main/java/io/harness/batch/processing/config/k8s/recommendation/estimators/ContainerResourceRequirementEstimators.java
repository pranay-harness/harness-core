package io.harness.batch.processing.config.k8s.recommendation.estimators;

import static io.harness.batch.processing.config.k8s.recommendation.estimators.ResourceAmountUtils.convertToReadableForm;
import static software.wings.graphql.datafetcher.ce.recommendation.entity.ResourceRequirement.CPU;
import static software.wings.graphql.datafetcher.ce.recommendation.entity.ResourceRequirement.MEMORY;

import com.google.common.collect.ImmutableMap;

import lombok.experimental.UtilityClass;
import software.wings.graphql.datafetcher.ce.recommendation.entity.ResourceRequirement;

import java.util.Map;

@UtilityClass
public class ContainerResourceRequirementEstimators {
  // 50th, 90th & 95th percentiles of cpu usage as lower, target, and upper cpu recommendations
  private static final double TARGET_CPU_PERCENTILE = 0.9;
  private static final double LOWER_BOUND_CPU_PERCENTILE = 0.5;
  private static final double UPPER_BOUND_CPU_PERCENTILE = 0.95;

  // 50th, 90th & 95th percentiles of memory usage peaks as lower, target, and upper memory recommendations
  private static final double TARGET_MEMORY_PERCENTILE = 0.9;
  private static final double LOWER_BOUND_MEMORY_PERCENTILE = 0.5;
  private static final double UPPER_BOUND_MEMORY_PERCENTILE = 0.95;

  private static final double SAFETY_MARGIN_FRACTION = 0.15; // bump all recommendations by 15%
  private static final long MIN_CPU_MILLICORES = 25; // 25m
  private static final long MIN_MEMORY_BYTES = 262144000; // 250Mi

  private static final Map<String, Long> MIN_RESOURCES =
      ImmutableMap.<String, Long>builder().put(CPU, MIN_CPU_MILLICORES).put(MEMORY, MIN_MEMORY_BYTES).build();

  private static final ResourceEstimator TARGET_ESTIMATOR =
      PercentileEstimator.of(TARGET_CPU_PERCENTILE, TARGET_MEMORY_PERCENTILE)
          .withMargin(SAFETY_MARGIN_FRACTION)
          .withMinResources(MIN_RESOURCES);

  private static final ResourceEstimator UPPER_BOUND_ESTIMATOR =
      PercentileEstimator.of(UPPER_BOUND_CPU_PERCENTILE, UPPER_BOUND_MEMORY_PERCENTILE)
          .withMargin(SAFETY_MARGIN_FRACTION)
          .withConfidenceMultiplier(1.0, 1.0)
          .withMinResources(MIN_RESOURCES);

  static final ResourceEstimator LOWER_BOUND_ESTIMATOR =
      PercentileEstimator.of(LOWER_BOUND_CPU_PERCENTILE, LOWER_BOUND_MEMORY_PERCENTILE)
          .withMargin(SAFETY_MARGIN_FRACTION)
          .withConfidenceMultiplier(0.001, -2.0)
          .withMinResources(MIN_RESOURCES);

  public static ContainerResourceRequirementEstimator burstableRecommender() {
    return cs
        -> ResourceRequirement.builder()
               .requests(convertToReadableForm(LOWER_BOUND_ESTIMATOR.getResourceEstimation(cs)))
               .limits(convertToReadableForm(UPPER_BOUND_ESTIMATOR.getResourceEstimation(cs)))
               .build();
  }

  public static ContainerResourceRequirementEstimator guaranteedRecommender() {
    return cs -> {
      Map<String, String> resources = convertToReadableForm(TARGET_ESTIMATOR.getResourceEstimation(cs));
      return ResourceRequirement.builder().requests(resources).limits(resources).build();
    };
  }
}
