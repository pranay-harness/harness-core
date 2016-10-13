package software.wings.service.impl;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static java.util.stream.StreamSupport.stream;
import static org.eclipse.jetty.util.LazyList.isEmpty;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.CountsByStatuses.Builder.aCountsByStatuses;
import static software.wings.beans.ErrorCodes.DUPLICATE_ARTIFACTSOURCE_NAMES;
import static software.wings.beans.Release.BreakdownByEnvironments.Builder.aBreakdownByEnvironments;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.dl.MongoHelper.setUnset;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.common.collect.Lists;
import com.google.inject.Singleton;

import com.mongodb.BasicDBObject;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.Artifact;
import software.wings.beans.ArtifactSource;
import software.wings.beans.InstanceCountByEnv;
import software.wings.beans.JenkinsArtifactSource;
import software.wings.beans.Release;
import software.wings.beans.Release.BreakdownByEnvironments;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SortOrder.OrderType;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.ReleaseService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.utils.validation.Create;
import software.wings.utils.validation.Update;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.validation.executable.ValidateOnExecution;
import javax.ws.rs.NotFoundException;

/**
 * The Class ReleaseServiceImpl.
 */
@Singleton
@ValidateOnExecution
public class ReleaseServiceImpl implements ReleaseService {
  @Inject private WingsPersistence wingsPersistence;

  @Inject private SettingsService settingsService;

  @Inject private ServiceInstanceService serviceInstanceService;

  @Inject private ServiceTemplateService serviceTemplateService;

  @Inject private EnvironmentService environmentService;

  @Inject private ArtifactService artifactService;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ReleaseService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<Release> list(PageRequest<Release> req) {
    PageResponse<Release> releases = wingsPersistence.query(Release.class, req);
    releases.forEach(release -> populateJenkinsSettingName(release.getArtifactSources()));
    releases.forEach(release
        -> release.getArtifactSources().parallelStream().forEach(artifactSource
            -> artifactSource.setLastArtifact(
                (Artifact) artifactService
                    .list(aPageRequest()
                              .withLimit("1")
                              .addOrder("lastUpdatedAt", OrderType.DESC)
                              .addFilter(aSearchFilter().withField("appId", EQ, release.getAppId()).build())
                              .addFilter(aSearchFilter()
                                             .withField("release", EQ, wingsPersistence.getDatastore().getKey(release))
                                             .build())
                              .addFilter(aSearchFilter()
                                             .withField("artifactSourceName", EQ, artifactSource.getSourceName())
                                             .build())
                              .<Artifact>build())
                    .getResponse()
                    .stream()
                    .findFirst()
                    .orElse(null))));
    return releases;
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ReleaseService#get(java.lang.String, java.lang.String)
   */
  @Override
  public Release get(String id, String appId) {
    Release release = wingsPersistence.get(Release.class, appId, id);
    if (release != null) {
      populateJenkinsSettingName(release.getArtifactSources());
    }
    return release;
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ReleaseService#create(software.wings.beans.Release)
   */
  @Override
  @ValidationGroups(Create.class)
  public Release create(Release release) {
    List<BreakdownByEnvironments> breakdownByEnvironments = getReleaseBreakdownByEnvironments(release);

    release.setBreakdownByEnvironments(breakdownByEnvironments);

    String id = wingsPersistence.save(release);
    return get(id, release.getAppId());
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ReleaseService#update(software.wings.beans.Release)
   */
  @Override
  @ValidationGroups(Update.class)
  public Release update(Release release) {
    Query<Release> query = wingsPersistence.createQuery(Release.class)
                               .field(ID_KEY)
                               .equal(release.getUuid())
                               .field("appId")
                               .equal(release.getAppId());

    UpdateOperations<Release> updateOperations = wingsPersistence.createUpdateOperations(Release.class);

    setUnset(updateOperations, "releaseName", release.getReleaseName());
    setUnset(updateOperations, "description", release.getDescription());
    setUnset(updateOperations, "targetDate", release.getTargetDate());
    setUnset(updateOperations, "services", release.getServices());

    List<BreakdownByEnvironments> breakdownByEnvironments = getReleaseBreakdownByEnvironments(release);

    release.setBreakdownByEnvironments(breakdownByEnvironments);

    Iterable<InstanceCountByEnv> instanceCountByEnvs = serviceInstanceService.getCountsByEnvReleaseAndTemplate(
        release.getAppId(), release, getServiceTemplatesForRelease(release));

    stream(instanceCountByEnvs.spliterator(), false).forEach(instanceCountByEnv -> {
      breakdownByEnvironments.stream()
          .filter(breakdownByEnvironment
              -> StringUtils.equals(breakdownByEnvironment.getEnvId(), instanceCountByEnv.getEnvId()))
          .forEach(breakdownByEnvironment -> {
            breakdownByEnvironment.getBreakdown().setSuccess(instanceCountByEnv.getCount());
          });
    });

    setUnset(updateOperations, "breakdownByEnvironments", release.getBreakdownByEnvironments());

    wingsPersistence.update(query, updateOperations);
    return get(release.getUuid(), release.getAppId());
  }

  @Override
  public void addSuccessCount(String appId, String releaseId, String envId, int count) {
    wingsPersistence.update(wingsPersistence.createQuery(Release.class)
                                .field(ID_KEY)
                                .equal(releaseId)
                                .field("appId")
                                .equal(appId)
                                .field("breakdownByEnvironments.envId")
                                .equal(envId),
        wingsPersistence.createUpdateOperations(Release.class)
            .inc("breakdownByEnvironments.$.breakdown.success", count));
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ReleaseService#addArtifactSource(java.lang.String, java.lang.String,
   * software.wings.beans.ArtifactSource)
   */
  @Override
  public Release addArtifactSource(String id, String appId, ArtifactSource artifactSource) {
    Release release = wingsPersistence.get(Release.class, appId, id);
    if (release == null) {
      throw new NotFoundException("Release with id " + id + " not found");
    }

    if (!wingsPersistence.addToList(Release.class, appId, id,
            wingsPersistence.createQuery(Release.class)
                .field("artifactSources.sourceName")
                .notEqual(artifactSource.getSourceName()),
            "artifactSources", artifactSource)) {
      throw new WingsException(DUPLICATE_ARTIFACTSOURCE_NAMES, "artifactSourceName", artifactSource.getSourceName());
    }
    return get(id, appId);
  }

  @Override
  public Release updateArtifactSource(String id, String appId, ArtifactSource artifactSource) {
    Release release = wingsPersistence.get(Release.class, appId, id);
    if (release == null) {
      throw new NotFoundException("Release with id " + id + " not found");
    }

    wingsPersistence.update(wingsPersistence.createQuery(Release.class)
                                .field(ID_KEY)
                                .equal(id)
                                .field("appId")
                                .equal(appId)
                                .field("artifactSources.sourceName")
                                .equal(artifactSource.getSourceName()),
        wingsPersistence.createUpdateOperations(Release.class).set("artifactSources.$", artifactSource));

    return get(id, appId);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ReleaseService#deleteArtifactSource(java.lang.String, java.lang.String,
   * java.lang.String)
   */
  @Override
  public <T extends ArtifactSource> Release deleteArtifactSource(String id, String appId, String artifactSourceName) {
    Release release = wingsPersistence.get(Release.class, appId, id);
    if (release == null) {
      throw new NotFoundException("Release with id " + id + " not found");
    }

    wingsPersistence.update(
        wingsPersistence.createQuery(Release.class).field(ID_KEY).equal(id).field("appId").equal(appId),
        wingsPersistence.createUpdateOperations(Release.class)
            .removeAll("artifactSources", new BasicDBObject("sourceName", artifactSourceName)));

    return get(id, appId);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ReleaseService#delete(java.lang.String, java.lang.String)
   */
  @Override
  public boolean delete(String id, String appId) {
    return wingsPersistence.delete(
        wingsPersistence.createQuery(Release.class).field(ID_KEY).equal(id).field("appId").equal(appId));
  }

  @Override
  public void deleteByApplication(String appId) {
    wingsPersistence.createQuery(Release.class)
        .field("appId")
        .equal(appId)
        .asList()
        .forEach(release -> delete(release.getUuid(), appId));
  }

  private void populateJenkinsSettingName(List<ArtifactSource> artifactSources) {
    if (!isEmpty(artifactSources)) {
      for (ArtifactSource artifactSource : artifactSources) {
        if (artifactSource instanceof JenkinsArtifactSource) {
          try {
            SettingAttribute attribute =
                settingsService.get(((JenkinsArtifactSource) artifactSource).getJenkinsSettingId());
            if (attribute != null) {
              ((JenkinsArtifactSource) artifactSource).setName(attribute.getName());
            }
          } catch (Exception e) {
            // Ignore
          }
        }
      }
    }
  }

  private Set<ServiceTemplate> getServiceTemplatesForRelease(Release release) {
    Map<String, List<ServiceTemplate>> serviceTemplatesByEnv =
        release.getServices()
            .parallelStream()
            .flatMap(service
                -> (Stream<ServiceTemplate>) serviceTemplateService
                       .list(aPageRequest()
                                 .addFilter(aSearchFilter().withField("appId", EQ, release.getAppId()).build())
                                 .aPageRequest()
                                 .addFilter(aSearchFilter().withField("serviceId", EQ, service.getUuid()).build())
                                 .addFieldsIncluded("name", "envId")
                                 .<ServiceTemplate>build(),
                           false)
                       .getResponse()
                       .stream())
            .collect(groupingBy(ServiceTemplate::getEnvId));

    return serviceTemplatesByEnv.values()
        .stream()
        .flatMap(serviceTemplates1 -> serviceTemplates1.stream())
        .collect(toSet());
  }

  private List<BreakdownByEnvironments> getReleaseBreakdownByEnvironments(Release release) {
    if (isEmpty(release.getServices())) {
      return Lists.newArrayList();
    } else {
      Map<String, List<ServiceTemplate>> serviceTemplatesByEnv =
          release.getServices()
              .parallelStream()
              .flatMap(service
                  -> (Stream<ServiceTemplate>) serviceTemplateService
                         .list(aPageRequest()
                                   .addFilter(aSearchFilter().withField("appId", EQ, release.getAppId()).build())
                                   .aPageRequest()
                                   .addFilter(aSearchFilter().withField("serviceId", EQ, service.getUuid()).build())
                                   .addFieldsIncluded("name", "envId")
                                   .<ServiceTemplate>build(),
                             false)
                         .getResponse()
                         .stream())
              .collect(groupingBy(ServiceTemplate::getEnvId));

      Set<ServiceTemplate> serviceTemplates = serviceTemplatesByEnv.values()
                                                  .stream()
                                                  .flatMap(serviceTemplates1 -> serviceTemplates1.stream())
                                                  .collect(toSet());

      Iterable<InstanceCountByEnv> instanceCountByEnv =
          serviceInstanceService.getCountsByEnv(release.getAppId(), serviceTemplates);

      Map<String, Integer> countsByEnv =
          stream(instanceCountByEnv.spliterator(), false)
              .collect(toMap(InstanceCountByEnv::getEnvId, InstanceCountByEnv::getCount));

      return serviceTemplatesByEnv.entrySet()
          .parallelStream()
          .map(entry
              -> aBreakdownByEnvironments()
                     .withEnvId(entry.getKey())
                     .withEnvName(environmentService.get(release.getAppId(), entry.getKey(), false).getName())
                     .withTotal(countsByEnv.getOrDefault(entry.getKey(), 0))
                     .withBreakdown(aCountsByStatuses().build())
                     .build())
          .collect(toList());
    }
  }
}
