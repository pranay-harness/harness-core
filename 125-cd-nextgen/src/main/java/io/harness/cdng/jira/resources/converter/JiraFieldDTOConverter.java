package io.harness.cdng.jira.resources.converter;

import io.harness.cdng.jira.resources.response.dto.JiraFieldDTO;
import io.harness.jira.JiraField;
import lombok.experimental.UtilityClass;

import java.util.function.Function;

@UtilityClass
public class JiraFieldDTOConverter {
  public Function<JiraField, JiraFieldDTO> toJiraFieldDTO = jiraField
      -> JiraFieldDTO.builder()
             .key(jiraField.getKey())
             .name(jiraField.getName())
             .required(jiraField.isRequired())
             .isCustom(jiraField.isCustom())
             .allowedValues(jiraField.getAllowedValues())
             .schema(jiraField.getSchema())
             .build();
}
