package io.harness.gitopsprovider.mappers;

import static io.harness.annotations.dev.HarnessTeam.GITOPS;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.gitops.GitOpsProviderDTO;
import io.harness.gitopsprovider.entity.GitOpsProvider;
import io.harness.ng.core.mapper.TagMapper;

@OwnedBy(GITOPS)
public abstract class AbstractGitOpsProviderEntityMapper implements GitOpsProviderEntityMapper {
  protected void setDtoFields(GitOpsProvider src, GitOpsProviderDTO target) {
    target.setName(src.getName());
    target.setIdentifier(src.getIdentifier());
    target.setTags(TagMapper.convertToMap(src.getTags()));
    target.setOrgIdentifier(src.getOrgIdentifier());
    target.setProjectIdentifier(src.getProjectIdentifier());
    target.setDescription(src.getDescription());
  }

  protected void setEntityFields(GitOpsProviderDTO src, GitOpsProvider target, String accountIdentifier) {
    target.setName(src.getName());
    target.setIdentifier(src.getIdentifier());
    target.setTags(TagMapper.convertToList(src.getTags()));
    target.setOrgIdentifier(src.getOrgIdentifier());
    target.setProjectIdentifier(src.getProjectIdentifier());
    target.setDescription(src.getDescription());
    target.setAccountIdentifier(accountIdentifier);
  }
}
