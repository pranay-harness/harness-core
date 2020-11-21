package io.harness.cdng.jira.resources.converter;

import io.harness.cdng.jira.resources.response.dto.JiraStatusCategoryDTO;
import io.harness.jira.JiraStatusCategory;

import java.util.function.Function;
import lombok.experimental.UtilityClass;

@UtilityClass
public class JiraStatusCategoryDTOConverter {
  public Function<JiraStatusCategory, JiraStatusCategoryDTO> toCategoryDTO = jiraStatusCategory
      -> JiraStatusCategoryDTO.builder()
             .id(jiraStatusCategory.getId())
             .key(jiraStatusCategory.getKey())
             .name(jiraStatusCategory.getName())
             .build();
}
