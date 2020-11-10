package io.harness.cdng.jira.resources.converter;

import io.harness.cdng.jira.resources.response.dto.JiraStatusDTO;
import io.harness.jira.JiraStatus;
import lombok.experimental.UtilityClass;

import java.util.function.Function;

@UtilityClass
public class JiraStatusDTOConverter {
  public Function<JiraStatus, JiraStatusDTO> toJiraStatusDTO = jiraStatus
      -> JiraStatusDTO.builder()
             .id(jiraStatus.getId())
             .name(jiraStatus.getName())
             .untranslatedName(jiraStatus.getUntranslatedName())
             .description(jiraStatus.getDescription())
             .statusCategory(JiraStatusCategoryDTOConverter.toCategoryDTO.apply(jiraStatus.getStatusCategory()))
             .build();
}
