package io.harness.cdng.jira.resources.response;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.cdng.jira.resources.response.dto.JiraApprovalDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
@ApiModel(value = "JiraApprovalResponse")
public class JiraApprovalResponseDTO {
  JiraApprovalDTO jiraApproval;
}
