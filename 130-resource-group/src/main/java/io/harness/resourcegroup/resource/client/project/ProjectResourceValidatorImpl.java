package io.harness.resourcegroup.resource.client.project;

import static io.harness.remote.client.NGRestUtils.getResponse;

import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ProjectResponse;
import io.harness.projectmanagerclient.remote.ProjectManagerClient;
import io.harness.resourcegroup.resource.validator.ResourceValidator;

import com.google.inject.Inject;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor(access = AccessLevel.PUBLIC, onConstructor = @__({ @Inject }))
public class ProjectResourceValidatorImpl implements ResourceValidator {
  ProjectManagerClient projectManagerClient;

  @Override
  public boolean validate(
      List<String> resourceIds, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    PageResponse<ProjectResponse> projects =
        getResponse(projectManagerClient.listProjects(accountIdentifier, orgIdentifier, resourceIds));
    return projects.getContent().size() < resourceIds.size();
  }
}
