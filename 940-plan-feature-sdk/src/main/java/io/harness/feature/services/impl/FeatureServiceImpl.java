package io.harness.feature.services.impl;

import io.harness.feature.bases.FeatureEnable;
import io.harness.feature.configs.FeatureInfo;
import io.harness.feature.constants.FeatureType;
import io.harness.feature.interfaces.Feature;
import io.harness.feature.services.FeatureService;
import io.harness.reflection.ReflectionUtils;

import com.google.inject.Singleton;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class FeatureServiceImpl implements FeatureService {
  private Map<String, Feature> featureMap;
  @Override
  public void registerFeature(String featureName, Feature feature) {
    featureMap.put(featureName, feature);
  }

  @Override
  public void initializeOrUpdateFeature(FeatureInfo featureInfo) throws NoSuchFieldException, IllegalAccessException {
    if (FeatureType.FEATURE_ENABLE.equals(featureInfo.getType())) {
      featureMap.put(featureInfo.getName(), instantiateFeatureEnable(featureInfo));
    } else if (FeatureType.FEATURE_LIMIT.equals(featureInfo.getType())) {
      updateFeatureLimit(featureInfo);
    }
  }

  private FeatureEnable instantiateFeatureEnable(FeatureInfo featureInfo)
      throws NoSuchFieldException, IllegalAccessException {
    FeatureEnable featureEnable = new FeatureEnable();
    setFieldForFeature(featureEnable, featureInfo);
    return featureEnable;
  }

  private void updateFeatureLimit(FeatureInfo featureInfo) throws NoSuchFieldException, IllegalAccessException {
    if (!featureMap.containsKey(featureInfo.getName())) {
      throw new IllegalStateException(String.format(
          "Cannot find implementation class for FeatureLimit type with name [%s]", featureInfo.getName()));
    }
    Feature feature = featureMap.get(featureInfo.getName());
    setFieldForFeature(feature, featureInfo);
  }

  private void setFieldForFeature(Object object, FeatureInfo featureInfo)
      throws NoSuchFieldException, IllegalAccessException {
    List<Field> infoFields = ReflectionUtils.getAllDeclaredAndInheritedFields(featureInfo.getClass());
    for (Field infoField : infoFields) {
      Object value = ReflectionUtils.getFieldValue(featureInfo, infoField);

      if (value == null) {
        value = initiateValue(infoField);
      }
      ReflectionUtils.setObjectField(object.getClass().getField(infoField.getName()), object, value);
    }
  }

  private Object initiateValue(Field infoField) {
    Class<?> type = infoField.getType();
    if (type.isAssignableFrom(Map.class)) {
      return new HashMap();
    } else if (type.isAssignableFrom(List.class)) {
      return new ArrayList();
    } else if (type.isAssignableFrom(Set.class)) {
      return new HashSet<>();
    }
    return null;
  }
}
