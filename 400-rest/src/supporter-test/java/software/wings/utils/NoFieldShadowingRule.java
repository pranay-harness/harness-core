/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.utils;

import com.google.common.collect.Lists;
import com.openpojo.log.utils.MessageFormatter;
import com.openpojo.reflection.PojoClass;
import com.openpojo.reflection.PojoField;
import com.openpojo.validation.affirm.Affirm;
import com.openpojo.validation.rule.Rule;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by peeyushaggarwal on 5/19/16.
 */
public class NoFieldShadowingRule implements Rule {
  private static final List<String> ignoredFields = Lists.newArrayList("logger", "serialVersionUID");

  /**
   * {@inheritDoc}
   */
  @Override
  public void evaluate(final PojoClass pojoClass) {
    final List<PojoField> parentPojoFields = new LinkedList<>();
    PojoClass parentPojoClass = pojoClass.getSuperClass();
    while (parentPojoClass != null) {
      parentPojoFields.addAll(parentPojoClass.getPojoFields());
      parentPojoClass = parentPojoClass.getSuperClass();
    }
    final List<PojoField> childPojoFields = pojoClass.getPojoFields();
    for (final PojoField childPojoField : childPojoFields) {
      if (contains(childPojoField.getName(), parentPojoFields) && !ignoredFields.contains(childPojoField.getName())) {
        Affirm.fail(MessageFormatter.format(
            "Field=[{0}] shadows field with the same name in parent class=[{1}]", childPojoField, parentPojoFields));
      }
    }
  }

  private boolean contains(final String fieldName, final List<PojoField> pojoFields) {
    for (final PojoField pojoField : pojoFields) {
      if (pojoField.getName().equals(fieldName)) {
        return true;
      }
    }
    return false;
  }
}
