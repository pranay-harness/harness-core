package software.wings.graphql.datafetcher.tag;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Inject;

import io.harness.persistence.HPersistence;
import software.wings.beans.HarnessTagLink;
import software.wings.beans.HarnessTagLink.HarnessTagLinkKeys;
import software.wings.graphql.datafetcher.AbstractArrayDataFetcher;
import software.wings.graphql.schema.query.QLTagsQueryParameters;
import software.wings.graphql.schema.type.QLTag;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TagsDataFetcher extends AbstractArrayDataFetcher<QLTag, QLTagsQueryParameters> {
  @Inject HPersistence persistence;

  protected QLTag unusedReturnTypePassingDummyMethod() {
    return null;
  }

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  public List<QLTag> fetch(QLTagsQueryParameters qlQuery, String accountId) {
    List<HarnessTagLink> harnessTagLinks = new ArrayList<>();

    if (isNotBlank(qlQuery.getServiceId())) {
      harnessTagLinks = persistence.createQuery(HarnessTagLink.class)
                            .filter(HarnessTagLinkKeys.accountId, accountId)
                            .filter(HarnessTagLinkKeys.entityId, qlQuery.getServiceId())
                            .order(HarnessTagLinkKeys.key)
                            .asList();
    }

    return harnessTagLinks.stream()
        .map(harnessTagLink -> QLTag.builder().name(harnessTagLink.getKey()).value(harnessTagLink.getValue()).build())
        .collect(Collectors.toList());
  }
}
