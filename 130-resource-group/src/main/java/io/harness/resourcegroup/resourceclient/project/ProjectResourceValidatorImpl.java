package io.harness.resourcegroup.resourceclient.project;

import static io.harness.remote.client.NGRestUtils.getResponse;

import static java.util.stream.Collectors.toList;

import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.dto.ProjectResponse;
import io.harness.projectmanagerclient.remote.ProjectManagerClient;
import io.harness.resourcegroup.model.Scope;
import io.harness.resourcegroup.resourceclient.api.ResourceValidator;

import com.google.inject.Inject;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor(access = AccessLevel.PUBLIC, onConstructor = @__({ @Inject }))
public class ProjectResourceValidatorImpl implements ResourceValidator {
  ProjectManagerClient projectManagerClient;

  @Override
  public List<Boolean> validate(
      List<String> resourceIds, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    PageResponse<ProjectResponse> projects =
        getResponse(projectManagerClient.listProjects(accountIdentifier, orgIdentifier, resourceIds));
    Set<String> validResourcIds =
        projects.getContent().stream().map(e -> e.getProject().getIdentifier()).collect(Collectors.toSet());
    return resourceIds.stream().map(validResourcIds::contains).collect(toList());
  }

  @Override
  public String getResourceType() {
    return "PROJECT";
  }

  @Override
  public Set<Scope> getScopes() {
    return EnumSet.of(Scope.ORGANIZATION);
  }
}
