package io.harness.cdng.jira.resources.response;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.cdng.jira.resources.response.dto.JiraIssueTypeDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModel;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldDefaults;

@Value
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
@ApiModel(value = "JiraProjectStatusesResponse")
public class JiraProjectStatusesResponseDTO {
  List<JiraIssueTypeDTO> jiraIssueTypeList;
}
