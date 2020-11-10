package io.harness.cdng.jira.resources.converter;

import io.harness.cdng.jira.resources.response.dto.JiraStatusCategoryDTO;
import io.harness.jira.JiraStatusCategory;
import lombok.experimental.UtilityClass;

import java.util.function.Function;

@UtilityClass
public class JiraStatusCategoryDTOConverter {
  public Function<JiraStatusCategory, JiraStatusCategoryDTO> toCategoryDTO = jiraStatusCategory
      -> JiraStatusCategoryDTO.builder()
             .id(jiraStatusCategory.getId())
             .key(jiraStatusCategory.getKey())
             .name(jiraStatusCategory.getName())
             .build();
}
