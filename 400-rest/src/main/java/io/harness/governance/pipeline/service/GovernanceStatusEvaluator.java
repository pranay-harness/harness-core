/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.governance.pipeline.service;

import io.harness.governance.pipeline.enforce.GovernanceRuleStatus;
import io.harness.governance.pipeline.service.model.PipelineGovernanceRule;
import io.harness.governance.pipeline.service.model.Tag;

import software.wings.beans.HarnessTagLink;
import software.wings.beans.entityinterface.TagAware;

import java.util.List;
import org.apache.commons.lang3.StringUtils;

/**
 * Evaluates how a tagged entity scores in respect to a governance standard.
 * @param <T>
 */
public interface GovernanceStatusEvaluator<T extends TagAware> {
  enum EntityType { PIPELINE, WORKFLOW }
  GovernanceRuleStatus status(String accountId, T entity, PipelineGovernanceRule rule);

  static boolean containsAll(List<HarnessTagLink> links, List<Tag> tagsToLookFor) {
    // all tags should have a `match` in the links
    return tagsToLookFor.stream().allMatch(tag -> links.stream().anyMatch(link -> matches(link, tag)));
  }

  static boolean containsAny(List<HarnessTagLink> links, List<Tag> tagsToLookFor) {
    // any tag should have a `match` in the links
    return tagsToLookFor.stream().anyMatch(tag -> links.stream().anyMatch(link -> matches(link, tag)));
  }

  static boolean matches(HarnessTagLink tagLink, Tag tagToLookFor) {
    if (StringUtils.isEmpty(tagToLookFor.getValue())) {
      return StringUtils.equals(tagToLookFor.getKey(), tagLink.getKey());
    } else {
      return StringUtils.equals(tagToLookFor.getKey(), tagLink.getKey())
          && StringUtils.equals(tagToLookFor.getValue(), tagLink.getValue());
    }
  }
}
