/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cdng.environment.helper;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureName;
import io.harness.cdng.envgroup.yaml.EnvironmentGroupYaml;
import io.harness.cdng.environment.filters.Entity;
import io.harness.cdng.environment.filters.FilterType;
import io.harness.cdng.environment.filters.FilterYaml;
import io.harness.cdng.environment.filters.TagsFilter;
import io.harness.cdng.environment.yaml.EnvironmentYamlV2;
import io.harness.cdng.environment.yaml.EnvironmentsYaml;
import io.harness.cdng.gitops.service.ClusterService;
import io.harness.cdng.infra.yaml.InfraStructureDefinitionYaml;
import io.harness.data.structure.CollectionUtils;
import io.harness.exception.InvalidRequestException;
import io.harness.gitops.models.Cluster;
import io.harness.gitops.models.ClusterQuery;
import io.harness.gitops.remote.GitopsResourceClient;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.common.beans.NGTag;
import io.harness.ng.core.environment.beans.Environment;
import io.harness.ng.core.environment.beans.Environment.EnvironmentKeys;
import io.harness.ng.core.environment.mappers.EnvironmentFilterHelper;
import io.harness.ng.core.environment.services.EnvironmentService;
import io.harness.ng.core.infrastructure.entity.InfrastructureEntity;
import io.harness.ng.core.infrastructure.services.InfrastructureEntityService;
import io.harness.ng.core.mapper.TagMapper;
import io.harness.pms.tags.TagUtils;
import io.harness.pms.yaml.ParameterField;
import io.harness.utils.NGFeatureFlagHelperService;
import io.harness.utils.RetryUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import retrofit2.Response;

@Slf4j
@OwnedBy(HarnessTeam.CDP)
@Singleton
public class EnvironmentInfraFilterHelper {
  public static final int PAGE_SIZE = 1000;
  public static final String TAGFILTER_MATCHTYPE_ALL = "all";
  public static final String TAGFILTER_MATCHTYPE_ANY = "any";
  public static final String SERVICE_TAGS = "<+service.tags>";
  @Inject private GitopsResourceClient gitopsResourceClient;
  @Inject private ClusterService clusterService;
  @Inject private EnvironmentService environmentService;
  @Inject private EnvironmentFilterHelper environmentFilterHelper;

  @Inject private InfrastructureEntityService infrastructureEntityService;
  @Inject private NGFeatureFlagHelperService featureFlagHelperService;

  private static final RetryPolicy<Object> retryPolicyForGitopsClustersFetch = RetryUtils.getRetryPolicy(
      "Error getting clusters from Harness Gitops..retrying", "Failed to fetch clusters from Harness Gitops",
      Collections.singletonList(IOException.class), Duration.ofMillis(10), 3, log);

  public boolean areAllTagFiltersMatching(List<NGTag> entityTags, List<NGTag> tagsInFilter) {
    // Safety check, if list is empty
    if (isEmpty(entityTags)) {
      return false;
    }

    int count = 0;
    for (NGTag tag : tagsInFilter) {
      if (entityTags.contains(tag)) {
        count++;
      }
    }
    return count != 0 && count == tagsInFilter.size();
  }

  public boolean areAnyTagFiltersMatching(List<NGTag> entityTags, List<NGTag> tagsInFilter) {
    if (isEmpty(entityTags)) {
      return false;
    }
    for (NGTag tag : entityTags) {
      if (tagsInFilter.contains(tag)) {
        return true;
      }
    }
    return false;
  }

  /**
   *
   * @param filterYaml - Contains the information of filters along with it's type
   * @param envs - List of environments to apply filters on
   * @return - List of filtered Environments
   */
  public Set<Environment> processTagsFilterYamlForEnvironments(FilterYaml filterYaml, Set<Environment> envs) {
    if (FilterType.all.name().equals(filterYaml.getType().name())) {
      return envs;
    }
    // filter env that match all tags
    Set<Environment> filteredEnvs = new HashSet<>();
    if (FilterType.tags.equals(filterYaml.getType())) {
      TagsFilter tagsFilter = (TagsFilter) filterYaml.getSpec();
      for (Environment environment : envs) {
        if (applyMatchAllFilter(environment.getTags(), tagsFilter)) {
          filteredEnvs.add(environment);
          continue;
        }
        if (applyMatchAnyFilter(environment.getTags(), tagsFilter)) {
          filteredEnvs.add(environment);
          continue;
        }
        if (isSupportedFilter(tagsFilter)) {
          throw new InvalidRequestException(
              String.format("TagFilter of type [%s] is not supported", tagsFilter.getMatchType().getValue()));
        }
      }
    }

    return filteredEnvs;
  }

  private boolean applyMatchAnyFilter(List<NGTag> entityTags, TagsFilter tagsFilter) {
    return tagsFilter.getMatchType().getValue().equals(TAGFILTER_MATCHTYPE_ANY)
        && areAnyTagFiltersMatching(entityTags, TagMapper.convertToList(tagsFilter.getTags().getValue()));
  }

  private boolean applyMatchAllFilter(List<NGTag> entityTags, TagsFilter tagsFilter) {
    Map<String, String> tagsMap = tagsFilter.getTags().getValue();
    // Remove UUID from tags
    TagUtils.removeUuidFromTags(tagsMap);
    String name = tagsFilter.getMatchType().getValue().toString();

    return name.equals(TAGFILTER_MATCHTYPE_ALL)
        && areAllTagFiltersMatching(entityTags, TagMapper.convertToList(tagsMap));
  }

  /**
   *
   * @param filterYaml - Contains the information of filters along with it's type
   * @param clusters - List of clusters to apply filters on
   * @param ngGitOpsClusters - Cluster Entity containing tag information for applying filtering
   * @return - List of filtered Clusters
   */
  public List<io.harness.cdng.gitops.entity.Cluster> processTagsFilterYamlForGitOpsClusters(FilterYaml filterYaml,
      Set<Cluster> clusters, Map<String, io.harness.cdng.gitops.entity.Cluster> ngGitOpsClusters) {
    if (FilterType.all.name().equals(filterYaml.getType().name())) {
      return ngGitOpsClusters.values().stream().collect(Collectors.toList());
    }

    List<io.harness.cdng.gitops.entity.Cluster> filteredClusters = new ArrayList<>();
    if (FilterType.tags.equals(filterYaml.getType())) {
      TagsFilter tagsFilter = (TagsFilter) filterYaml.getSpec();
      for (Cluster cluster : clusters) {
        if (applyMatchAllFilter(TagMapper.convertToList(cluster.getTags()), tagsFilter)) {
          filteredClusters.add(ngGitOpsClusters.get(cluster.getIdentifier()));
          continue;
        }
        if (applyMatchAnyFilter(TagMapper.convertToList(cluster.getTags()), tagsFilter)) {
          filteredClusters.add(ngGitOpsClusters.get(cluster.getIdentifier()));
          continue;
        }
        if (isSupportedFilter(tagsFilter)) {
          throw new InvalidRequestException(
              String.format("TagFilter of type [%s] is not supported", tagsFilter.getMatchType().getValue()));
        }
      }
    }
    return filteredClusters;
  }

  private static boolean isSupportedFilter(TagsFilter tagsFilter) {
    return !tagsFilter.getMatchType().getValue().equals(TAGFILTER_MATCHTYPE_ALL)
        && !tagsFilter.getMatchType().getValue().equals(TAGFILTER_MATCHTYPE_ANY);
  }

  /**
   *
   * @param environments - List of environments
   * @param filterYamls - List of FilterYamls
   * @return Applies filters on Environments Entity. Returns the same list of no filter is applied.
   * Throws exception if environments qualify after applying filters
   */
  public Set<Environment> applyFiltersOnEnvs(Set<Environment> environments, Iterable<FilterYaml> filterYamls) {
    Set<Environment> setOfFilteredEnvs = new HashSet<>();

    boolean filterOnEnvExists = false;
    for (FilterYaml filterYaml : filterYamls) {
      if (filterYaml.getEntities().contains(Entity.environments)) {
        filterOnEnvExists = true;
        setOfFilteredEnvs.addAll(processTagsFilterYamlForEnvironments(filterYaml, environments));
      }
    }

    if (!filterOnEnvExists) {
      setOfFilteredEnvs.addAll(environments);
    }

    if (isEmpty(setOfFilteredEnvs) && filterOnEnvExists) {
      log.info("No Environments are eligible for deployment due to applied filters");
    }
    return setOfFilteredEnvs;
  }

  /**
   *
   * @param filterYamls - List of FilterYamls
   * @param clsToCluster - Map of clusterRef to NG GitOps Cluster Entity
   * @param clusters - List of NG GitOpsClusters
   * @return Applies Filters on GitOpsClusters. Returns the same list of no filter is applied.
   * Throws exception if no clusters qualify after applying filters.
   */
  public Set<io.harness.cdng.gitops.entity.Cluster> applyFilteringOnClusters(Iterable<FilterYaml> filterYamls,
      Map<String, io.harness.cdng.gitops.entity.Cluster> clsToCluster, Set<io.harness.gitops.models.Cluster> clusters) {
    Set<io.harness.cdng.gitops.entity.Cluster> setOfFilteredCls = new HashSet<>();

    boolean filterOnClusterExists = false;
    for (FilterYaml filterYaml : filterYamls) {
      if (filterYaml.getEntities().contains(Entity.gitOpsClusters)) {
        setOfFilteredCls.addAll(processTagsFilterYamlForGitOpsClusters(filterYaml, clusters, clsToCluster));
        filterOnClusterExists = true;
      }
    }

    if (!filterOnClusterExists) {
      setOfFilteredCls.addAll(clsToCluster.values());
    }

    if (isEmpty(setOfFilteredCls) && filterOnClusterExists) {
      log.info("No GitOps cluster is eligible after applying filters");
    }
    return setOfFilteredCls;
  }

  /**
   * @param accountId
   * @param orgId
   * @param projectId
   * @param clsRefs - List of clusters for fetching tag information
   * @return Fetch GitOps Clusters from GitOpsService. Throw exception if unable to connect to gitOpsService or if no
   *     clusters are returned
   */
  public List<io.harness.gitops.models.Cluster> fetchClustersFromGitOps(
      String accountId, String orgId, String projectId, Set<String> clsRefs) {
    Map<String, Object> filter = ImmutableMap.of("identifier", ImmutableMap.of("$in", clsRefs));
    final ClusterQuery query = ClusterQuery.builder()
                                   .accountId(accountId)
                                   .orgIdentifier(orgId)
                                   .projectIdentifier(projectId)
                                   .pageIndex(0)
                                   .pageSize(clsRefs.size())
                                   .filter(filter)
                                   .build();
    final Response<PageResponse<Cluster>> response =
        Failsafe.with(retryPolicyForGitopsClustersFetch).get(() -> gitopsResourceClient.listClusters(query).execute());

    List<io.harness.gitops.models.Cluster> clusterList;
    if (response.isSuccessful() && response.body() != null) {
      clusterList = CollectionUtils.emptyIfNull(response.body().getContent());
    } else {
      throw new InvalidRequestException("Failed to fetch clusters from gitops-service, cannot apply filter");
    }
    return clusterList;
  }

  /**
   * @param accountIdentifier
   * @param orgIdentifier
   * @param projectIdentifier
   * @param envRefs
   * @return Fetch NGGitOps Clusters. These are clusters that are linked in Environments section. Throw Exception if no
   *     clusters are linked.
   */
  public Map<String, io.harness.cdng.gitops.entity.Cluster> getClusterRefToNGGitOpsClusterMap(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, List<String> envRefs) {
    Page<io.harness.cdng.gitops.entity.Cluster> clusters =
        clusterService.listAcrossEnv(0, PAGE_SIZE, accountIdentifier, orgIdentifier, projectIdentifier, envRefs);

    if (isEmpty(clusters.getContent())) {
      log.info("There are no gitOpsClusters linked to Environments");
    }

    Map<String, io.harness.cdng.gitops.entity.Cluster> clsToCluster = new HashMap<>();
    clusters.getContent().forEach(k -> clsToCluster.put(k.getClusterRef(), k));
    return clsToCluster;
  }

  public Set<Environment> getAllEnvironmentsInProject(
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    // Fetch All Environments
    Criteria criteria = environmentFilterHelper.createCriteriaForGetList(
        accountIdentifier, orgIdentifier, projectIdentifier, false, "");

    PageRequest pageRequest = PageRequest.of(0, PAGE_SIZE, Sort.by(Sort.Direction.DESC, EnvironmentKeys.createdAt));
    Page<Environment> allEnvsInProject = environmentService.list(criteria, pageRequest);
    if (isEmpty(allEnvsInProject.getContent())) {
      throw new InvalidRequestException(
          "Filters are applied for environments, but no enviroments exists for the project");
    }
    return new HashSet<>(allEnvsInProject.getContent());
  }

  public Set<InfrastructureEntity> getInfrastructureForEnvironmentList(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String envIdentifier) {
    List<InfrastructureEntity> infrastructureEntityList = infrastructureEntityService.getAllInfrastructureFromEnvRef(
        accountIdentifier, orgIdentifier, projectIdentifier, envIdentifier);
    return new HashSet<>(infrastructureEntityList);
  }

  public Set<InfrastructureEntity> processTagsFilterYamlForInfraStructures(
      FilterYaml filterYaml, Set<InfrastructureEntity> infras) {
    if (filterYaml.getType().name().equals(FilterType.all.name())) {
      return infras;
    }
    // filter env that match all tags
    Set<InfrastructureEntity> filteredInfras = new HashSet<>();
    if (filterYaml.getType().equals(FilterType.tags)) {
      TagsFilter tagsFilter = (TagsFilter) filterYaml.getSpec();
      for (InfrastructureEntity infra : infras) {
        if (applyMatchAllFilter(infra.getTags(), tagsFilter)) {
          filteredInfras.add(infra);
          continue;
        }
        if (applyMatchAnyFilter(infra.getTags(), tagsFilter)) {
          filteredInfras.add(infra);
          continue;
        }
        if (isSupportedFilter(tagsFilter)) {
          throw new InvalidRequestException(
              String.format("TagFilter of type [%s] is not supported", tagsFilter.getMatchType().getValue()));
        }
      }
    }

    return filteredInfras;
  }

  public Set<InfrastructureEntity> applyFilteringOnInfras(
      Iterable<FilterYaml> filterYamls, Set<InfrastructureEntity> infras) {
    Set<InfrastructureEntity> setOfFilteredInfras = new HashSet<>();

    boolean filterOnInfraExists = false;
    for (FilterYaml filterYaml : filterYamls) {
      if (filterYaml.getEntities().contains(Entity.infrastructures)) {
        filterOnInfraExists = true;
        setOfFilteredInfras.addAll(processTagsFilterYamlForInfraStructures(filterYaml, infras));
      }
    }

    if (!filterOnInfraExists) {
      setOfFilteredInfras.addAll(infras);
    }

    if (isEmpty(setOfFilteredInfras) && filterOnInfraExists) {
      log.info("No Environments are eligible for deployment due to applied filters");
    }
    return setOfFilteredInfras;
  }

  public boolean areFiltersPresent(EnvironmentsYaml environmentsYaml) {
    return environmentsYaml != null
        && ((ParameterField.isNotNull(environmentsYaml.getFilters())
                && isNotEmpty(environmentsYaml.getFilters().getValue()))
            || areFiltersSetOnIndividualEnvironments(environmentsYaml));
  }

  public boolean areFiltersSetOnIndividualEnvironments(EnvironmentsYaml environmentsYaml) {
    if (ParameterField.isNull(environmentsYaml.getValues())) {
      return false;
    }
    List<EnvironmentYamlV2> envV2YamlsWithFilters = getEnvYamlV2WithFilters(environmentsYaml.getValues());
    return isNotEmpty(envV2YamlsWithFilters);
  }

  public boolean areFiltersPresent(EnvironmentGroupYaml environmentGroupYaml) {
    return environmentGroupYaml != null
        && ((ParameterField.isNotNull(environmentGroupYaml.getFilters())
                && isNotEmpty(environmentGroupYaml.getFilters().getValue()))
            || areFiltersSetOnIndividualEnvironments(environmentGroupYaml.getEnvironments()));
  }

  public boolean areFiltersSetOnIndividualEnvironments(ParameterField<List<EnvironmentYamlV2>> environmentYamlV2s) {
    if (ParameterField.isNull(environmentYamlV2s)) {
      return false;
    }
    List<EnvironmentYamlV2> envV2YamlsWithFilters = getEnvYamlV2WithFilters(environmentYamlV2s);
    return isNotEmpty(envV2YamlsWithFilters);
  }

  public List<EnvironmentYamlV2> getEnvYamlV2WithFilters(ParameterField<List<EnvironmentYamlV2>> environmentYamlV2s) {
    return environmentYamlV2s.getValue()
        .stream()
        .filter(eg -> ParameterField.isNotNull(eg.getFilters()))
        .collect(Collectors.toList());
  }

  public boolean isServiceTagsExpressionPresent(EnvironmentsYaml environments) {
    if (environments == null) {
      return false;
    }
    ParameterField<List<FilterYaml>> filters = environments.getFilters();
    if (ParameterField.isNotNull(filters) && isServiceTagsExpressionPresent(filters)) {
      return true;
    }
    ParameterField<List<EnvironmentYamlV2>> environmentsValues = environments.getValues();
    return ParameterField.isNotNull(environmentsValues)
        && isServiceTagsExpressionPresent(environmentsValues.getValue());
  }

  private boolean isServiceTagsExpressionPresent(List<EnvironmentYamlV2> environments) {
    if (isEmpty(environments)) {
      return false;
    }
    return environments.stream()
        .map(EnvironmentYamlV2::getFilters)
        .anyMatch(filters -> ParameterField.isNotNull(filters) && isServiceTagsExpressionPresent(filters));
  }

  private boolean isServiceTagsExpressionPresent(ParameterField<List<FilterYaml>> filters) {
    if (ParameterField.isNull(filters) || isEmpty(filters.getValue())) {
      return false;
    }
    for (FilterYaml filterYaml : filters.getValue()) {
      if (FilterType.tags == filterYaml.getType()) {
        ParameterField<Map<String, String>> tags = ((TagsFilter) filterYaml.getSpec()).getTags();
        if (tags.isExpression() && tags.getExpressionValue().equals(SERVICE_TAGS)) {
          return true;
        }
      }
    }
    return false;
  }

  public boolean isServiceTagsExpressionPresent(EnvironmentGroupYaml environmentGroup) {
    if (environmentGroup == null) {
      return false;
    }
    ParameterField<List<FilterYaml>> filters = environmentGroup.getFilters();
    if (isServiceTagsExpressionPresent(filters)) {
      return true;
    }
    ParameterField<List<EnvironmentYamlV2>> environments = environmentGroup.getEnvironments();
    return ParameterField.isNotNull(environments) && isServiceTagsExpressionPresent(environments.getValue());
  }

  public void processEnvInfraFiltering(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      EnvironmentsYaml environments, EnvironmentGroupYaml environmentGroup) {
    if (featureFlagHelperService.isEnabled(accountIdentifier, FeatureName.CDS_FILTER_INFRA_CLUSTERS_ON_TAGS)) {
      if (environments != null && areFiltersPresent(environments)) {
        EnvironmentsYaml environmentsYaml = environments;
        List<EnvironmentYamlV2> finalyamlV2List = new ArrayList<>();
        if (areFiltersPresent(environmentsYaml)) {
          finalyamlV2List = processFilteringForEnvironmentsLevelFilters(
              accountIdentifier, orgIdentifier, projectIdentifier, environmentsYaml);
        }
        // Set the filtered envYamlV2 in the environments yaml so normal processing continues
        environmentsYaml.setValues(ParameterField.createValueField(finalyamlV2List));
      }

      // If deploying to environment group with filters
      if (environmentGroup != null
          && (isNotEmpty(environmentGroup.getFilters().getValue())
              || isNotEmpty(getEnvYamlV2WithFilters(environmentGroup.getEnvironments())))) {
        EnvironmentGroupYaml environmentGroupYaml = environmentGroup;

        List<EnvironmentYamlV2> finalyamlV2List = processFilteringForEnvironmentGroupLevelFilters(accountIdentifier,
            orgIdentifier, projectIdentifier, environmentGroupYaml, environmentGroup.getFilters().getValue());

        // Set the filtered envYamlV2 in the environmentGroup yaml so normal processing continues
        environmentGroupYaml.setEnvironments(ParameterField.createValueField(finalyamlV2List));
      }
    }
  }

  @NotNull
  private List<EnvironmentYamlV2> processFilteringForEnvironmentsLevelFilters(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, EnvironmentsYaml environmentsYaml) {
    List<EnvironmentYamlV2> finalyamlV2List;
    Set<EnvironmentYamlV2> envsLevelEnvironmentYamlV2 = new LinkedHashSet<>();
    if (ParameterField.isNotNull(environmentsYaml.getFilters())
        && isNotEmpty(environmentsYaml.getFilters().getValue())) {
      List<EnvironmentYamlV2> filteredEnvList = processEnvironmentInfraFilters(
          accountIdentifier, orgIdentifier, projectIdentifier, environmentsYaml.getFilters().getValue());
      envsLevelEnvironmentYamlV2.addAll(filteredEnvList);
      return new ArrayList<>(envsLevelEnvironmentYamlV2);
    }

    // Process filtering at individual Environment level
    Set<EnvironmentYamlV2> individualEnvironmentYamlV2 = new LinkedHashSet<>();
    if (areFiltersSetOnIndividualEnvironments(environmentsYaml)) {
      processFiltersOnIndividualEnvironmentsLevel(accountIdentifier, orgIdentifier, projectIdentifier,
          individualEnvironmentYamlV2, getEnvYamlV2WithFilters(environmentsYaml.getValues()));
    }

    // Merge the two lists
    List<EnvironmentYamlV2> mergedFilteredEnvs =
        getEnvOrEnvGrouplevelAndIndividualEnvFilteredEnvs(envsLevelEnvironmentYamlV2, individualEnvironmentYamlV2);

    // If there are envs in the filtered list and there are
    // specific infras specific, pick the specified infras
    finalyamlV2List = getFinalEnvsList(environmentsYaml.getValues().getValue(), mergedFilteredEnvs);

    return finalyamlV2List;
  }

  private List<EnvironmentYamlV2> processFilteringForEnvironmentGroupLevelFilters(String accountIdentifier,
      String orgIdentifier, String projectIdentifier, EnvironmentGroupYaml environmentGroupYaml,
      List<FilterYaml> filterYamls) {
    Set<EnvironmentYamlV2> envsLevelEnvironmentYamlV2 = new HashSet<>();
    if (isNotEmpty(filterYamls)) {
      List<EnvironmentYamlV2> filteredEnvList =
          processEnvironmentInfraFilters(accountIdentifier, orgIdentifier, projectIdentifier, filterYamls);
      envsLevelEnvironmentYamlV2.addAll(filteredEnvList);
    }

    Set<EnvironmentYamlV2> individualEnvironmentYamlV2 = new HashSet<>();
    if (isNotEmpty(environmentGroupYaml.getEnvironments().getValue())) {
      if (isNotEmpty(getEnvYamlV2WithFilters(environmentGroupYaml.getEnvironments()))) {
        processFiltersOnIndividualEnvironmentsLevel(accountIdentifier, orgIdentifier, projectIdentifier,
            individualEnvironmentYamlV2, getEnvYamlV2WithFilters(environmentGroupYaml.getEnvironments()));
      }
    }

    // Merge the two lists
    List<EnvironmentYamlV2> mergedFilteredEnvs =
        getEnvOrEnvGrouplevelAndIndividualEnvFilteredEnvs(envsLevelEnvironmentYamlV2, individualEnvironmentYamlV2);

    return getFinalEnvsList(environmentGroupYaml.getEnvironments().getValue(), mergedFilteredEnvs);
  }

  @NotNull
  private static List<EnvironmentYamlV2> getFinalEnvsList(
      List<EnvironmentYamlV2> envsFromYaml, List<EnvironmentYamlV2> mergedFilteredEnvs) {
    List<EnvironmentYamlV2> finalyamlV2List = new ArrayList<>();
    if (isNotEmpty(envsFromYaml)) {
      for (EnvironmentYamlV2 e : envsFromYaml) {
        List<EnvironmentYamlV2> list = mergedFilteredEnvs.stream()
                                           .filter(in -> in.getEnvironmentRef().equals(e.getEnvironmentRef()))
                                           .collect(Collectors.toList());
        if (isNotEmpty(list) || ParameterField.isNull(e.getInfrastructureDefinitions())
            || isEmpty(e.getInfrastructureDefinitions().getValue())) {
          continue;
        }
        finalyamlV2List.add(e);
      }
    }
    finalyamlV2List.addAll(mergedFilteredEnvs);
    return finalyamlV2List;
  }

  @NotNull
  private static List<EnvironmentYamlV2> getEnvOrEnvGrouplevelAndIndividualEnvFilteredEnvs(
      Set<EnvironmentYamlV2> envsLevelEnvironmentYamlV2, Set<EnvironmentYamlV2> individualEnvironmentYamlV2) {
    List<EnvironmentYamlV2> mergedFilteredEnvs = new ArrayList<>();
    for (EnvironmentYamlV2 envYamlV2 : envsLevelEnvironmentYamlV2) {
      List<EnvironmentYamlV2> eV2 = individualEnvironmentYamlV2.stream()
                                        .filter(e -> e.getEnvironmentRef().equals(envYamlV2.getEnvironmentRef()))
                                        .collect(Collectors.toList());
      if (isNotEmpty(eV2)) {
        continue;
      }
      mergedFilteredEnvs.add(envYamlV2);
    }
    mergedFilteredEnvs.addAll(individualEnvironmentYamlV2);
    return mergedFilteredEnvs;
  }

  private List<EnvironmentYamlV2> processEnvironmentInfraFilters(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, List<FilterYaml> filterYamls) {
    Set<Environment> allEnvsInProject =
        getAllEnvironmentsInProject(accountIdentifier, orgIdentifier, projectIdentifier);

    // Apply filters on environments
    Set<Environment> filteredEnvs = applyFiltersOnEnvs(allEnvsInProject, filterYamls);

    // Get All InfraDefinitions
    List<EnvironmentYamlV2> environmentYamlV2List = new ArrayList<>();
    for (Environment env : filteredEnvs) {
      Set<InfrastructureEntity> infrastructureEntitySet =
          getInfrastructureForEnvironmentList(accountIdentifier, orgIdentifier, projectIdentifier, env.getIdentifier());

      if (isNotEmpty(infrastructureEntitySet)) {
        List<EnvironmentYamlV2> temp = filterInfras(filterYamls, env.getIdentifier(), infrastructureEntitySet);
        if (isNotEmpty(temp)) {
          environmentYamlV2List.add(temp.get(0));
        }
      }
    }
    return environmentYamlV2List;
  }

  public List<EnvironmentYamlV2> filterInfras(
      List<FilterYaml> filterYamls, String env, Set<InfrastructureEntity> infrastructureEntitySet) {
    List<EnvironmentYamlV2> environmentYamlV2List = new ArrayList<>();
    Set<InfrastructureEntity> filteredInfras = applyFilteringOnInfras(filterYamls, infrastructureEntitySet);

    if (isNotEmpty(filteredInfras)) {
      List<InfraStructureDefinitionYaml> infraDefYamlList = new ArrayList<>();

      for (InfrastructureEntity in : filteredInfras) {
        infraDefYamlList.add(createInfraDefinitionYaml(in));
      }

      EnvironmentYamlV2 environmentYamlV2 =
          EnvironmentYamlV2.builder()
              .environmentRef(ParameterField.createValueField(env))
              .infrastructureDefinitions(ParameterField.createValueField(infraDefYamlList))
              .build();

      environmentYamlV2List.add(environmentYamlV2);
    }
    return environmentYamlV2List;
  }
  @VisibleForTesting
  protected InfraStructureDefinitionYaml createInfraDefinitionYaml(InfrastructureEntity infrastructureEntity) {
    return InfraStructureDefinitionYaml.builder()
        .identifier(ParameterField.createValueField(infrastructureEntity.getIdentifier()))
        .build();
  }

  private List<EnvironmentYamlV2> processFiltersOnIndividualEnvironmentsLevel(String accountIdentifier,
      String orgIdentifier, String projectIdentifier, Set<EnvironmentYamlV2> individualEnvironmentYamlV2,
      List<EnvironmentYamlV2> envV2YamlsWithFilters) {
    List<EnvironmentYamlV2> filteredInfraList = new ArrayList<>();
    for (EnvironmentYamlV2 envYamlV2 : envV2YamlsWithFilters) {
      Set<InfrastructureEntity> infrastructureEntitySet = getInfrastructureForEnvironmentList(
          accountIdentifier, orgIdentifier, projectIdentifier, envYamlV2.getEnvironmentRef().getValue());

      filteredInfraList = filterInfras(
          envYamlV2.getFilters().getValue(), envYamlV2.getEnvironmentRef().getValue(), infrastructureEntitySet);
      individualEnvironmentYamlV2.addAll(filteredInfraList);
    }
    return filteredInfraList;
  }
}
