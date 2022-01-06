/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.infra;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;

import software.wings.annotation.CustomFieldMapKey;
import software.wings.annotation.IncludeFieldMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

@OwnedBy(CDP)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public interface FieldKeyValMapProvider {
  @JsonIgnore
  default Map<String, Object> getFieldMapForClass() {
    Class cls = this.getClass();
    Map<String, Object> queryMap = new HashMap<>();
    Field[] fields = cls.getDeclaredFields();
    for (Field field : fields) {
      String fieldKey;
      if (!field.isAnnotationPresent(IncludeFieldMap.class)) {
        continue;
      }

      if (field.isAnnotationPresent(CustomFieldMapKey.class)) {
        CustomFieldMapKey customFieldMapKey = field.getAnnotation(CustomFieldMapKey.class);
        fieldKey = customFieldMapKey.value();
      } else {
        fieldKey = field.getName();
      }

      try {
        field.setAccessible(true);
        queryMap.put(fieldKey, field.get(this));
      } catch (IllegalAccessException e) {
        throw new WingsException(ErrorCode.UNKNOWN_ERROR, e);
      }
    }
    return queryMap;
  }
}
