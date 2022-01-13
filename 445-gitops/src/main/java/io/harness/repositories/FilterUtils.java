/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.repositories;

import static io.harness.annotations.dev.HarnessTeam.GITOPS;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.connector.GitOpsProviderType;
import io.harness.gitopsprovider.SearchTerm;
import io.harness.gitopsprovider.entity.GitOpsProvider;
import io.harness.gitopsprovider.entity.GitOpsProvider.GitOpsProviderKeys;
import io.harness.ng.core.common.beans.NGTag;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.reflections.Reflections;
import org.springframework.data.mongodb.core.query.Criteria;

@UtilityClass
@OwnedBy(GITOPS)
public class FilterUtils {
  static Reflections providerReflection = new Reflections(GitOpsProvider.class.getPackage().getName());

  static void applySearchFilter(Criteria criteria, String searchTerm) {
    if (isNotBlank(searchTerm)) {
      Criteria criteriaWithSearchTerm = getSearchTermFilter(searchTerm);
      criteria.andOperator(new Criteria[] {criteriaWithSearchTerm});
    }
  }

  static void applySearchFilterForType(Criteria criteria, GitOpsProviderType type) {
    if (type != null) {
      criteria.and(GitOpsProviderKeys.type).is(type);
    }
  }
  static Criteria getSearchTermFilter(String searchTerm) {
    if (isNotBlank(searchTerm)) {
      Criteria tagCriteria = createCriteriaForSearchingTag(searchTerm);
      List<Criteria> criterias = new ArrayList<>();
      criterias.add(Criteria.where(GitOpsProviderKeys.name).regex(searchTerm, "i"));
      criterias.add(Criteria.where(GitOpsProviderKeys.identifier).regex(searchTerm, "i"));
      criterias.add(Criteria.where(GitOpsProviderKeys.description).regex(searchTerm, "i"));
      criterias.add(tagCriteria);
      final List<Criteria> customFieldsCriteria = createCriteriaForSearchingCustomFields(searchTerm);
      customFieldsCriteria.forEach(criterias::add);
      return (new Criteria()).orOperator(criterias.stream().toArray(Criteria[] ::new));
    } else {
      return null;
    }
  }

  private static List<Criteria> createCriteriaForSearchingCustomFields(String searchTerm) {
    final Set<Class<? extends GitOpsProvider>> subTypes = providerReflection.getSubTypesOf(GitOpsProvider.class);
    if (EmptyPredicate.isNotEmpty(subTypes)) {
      final List<String> fieldsToSearchFor = subTypes.stream()
                                                 .map(type -> {
                                                   Field[] fields = type.getDeclaredFields();
                                                   List<String> annotatedFields = new ArrayList<>();
                                                   for (Field field : fields) {
                                                     if (field.isAnnotationPresent(SearchTerm.class)) {
                                                       annotatedFields.add(field.getName());
                                                     }
                                                   }
                                                   return annotatedFields;
                                                 })
                                                 .flatMap(Collection::stream)
                                                 .collect(Collectors.toList());
      return fieldsToSearchFor.stream().map(f -> Criteria.where(f).regex(searchTerm, "i")).collect(Collectors.toList());
    }
    return Collections.emptyList();
  }

  static Criteria createCriteriaForSearchingTag(String searchTerm) {
    String keyToBeSearched = searchTerm;
    String valueToBeSearched = "";
    if (searchTerm.contains(":")) {
      String[] split = searchTerm.split(":");
      keyToBeSearched = split[0];
      valueToBeSearched = split.length >= 2 ? split[1] : "";
    }

    return Criteria.where(GitOpsProviderKeys.tags)
        .is(NGTag.builder().key(keyToBeSearched).value(valueToBeSearched).build());
  }

  static Criteria getCriteria(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    return getCriteria(accountIdentifier, orgIdentifier, projectIdentifier)
        .and(GitOpsProviderKeys.identifier)
        .is(identifier);
  }

  static Criteria getCriteria(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return new Criteria()
        .and(GitOpsProviderKeys.accountIdentifier)
        .is(accountIdentifier)
        .and(GitOpsProviderKeys.orgIdentifier)
        .is(orgIdentifier)
        .and(GitOpsProviderKeys.projectIdentifier)
        .is(projectIdentifier);
  }
}
