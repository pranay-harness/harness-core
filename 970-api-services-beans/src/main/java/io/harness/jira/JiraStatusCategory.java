/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.jira;

import lombok.Data;
import lombok.experimental.FieldNameConstants;
import net.sf.json.JSONObject;

@Data
@FieldNameConstants(innerTypeName = "JiraStatusCategoryKeys")
public class JiraStatusCategory {
  private String id;
  private String key;
  private String name;

  public JiraStatusCategory(JSONObject jsonObject) {
    this.id = jsonObject.getString(JiraStatusCategoryKeys.id);
    this.key = jsonObject.getString(JiraStatusCategoryKeys.key);
    this.name = jsonObject.getString(JiraStatusCategoryKeys.name);
  }
}
