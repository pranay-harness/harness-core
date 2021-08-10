package io.harness.feature;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.feature.configs.FeatureInfo;
import io.harness.feature.configs.FeaturesConfig;
import io.harness.feature.services.FeatureService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.google.common.io.Resources;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

@OwnedBy(HarnessTeam.GTM)
@Slf4j
@Singleton
public class FeaturesManagementJob {
  private static final String FEATURES_YAML_PATH = "classpath:io/harness/feature/*.yml";

  private final FeatureService featureService;
  private final List<FeaturesConfig> featuresConfigs;

  @Inject
  public FeaturesManagementJob(FeatureService featureService) throws IOException {
    ObjectMapper om = new ObjectMapper(new YAMLFactory());
    ClassLoader cl = this.getClass().getClassLoader();
    ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(cl);
    Resource[] resources = resolver.getResources(FEATURES_YAML_PATH);

    featuresConfigs = new ArrayList<>();
    for (Resource resource : resources) {
      byte[] bytes = Resources.toByteArray(resource.getURL());
      FeaturesConfig featuresConfig = om.readValue(bytes, FeaturesConfig.class);
      featuresConfigs.add(featuresConfig);
    }
    this.featureService = featureService;
  }

  public void run() {
    // Scan and register all feature instances user implemented for FeatureLimit and RateLimit, by
    // featureService.registerFeature()

    // According to FeatureConfig, instantiate FeatureEnable or inject info to FeatureLimit
    // validateFeatureInfo()
  }

  private void validateFeatureInfo(FeatureInfo featureInfo) {
    if (featureInfo.getName() == null || featureInfo.getModuleType() == null || featureInfo.getType() == null) {
      throw new IllegalArgumentException("Invalid FeatureInfo");
    }
  }
}
