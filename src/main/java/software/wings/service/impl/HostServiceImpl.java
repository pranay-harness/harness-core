package software.wings.service.impl;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Host.Builder.aHost;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.collect.ImmutableMap;

import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Base;
import software.wings.beans.ErrorCodes;
import software.wings.beans.Host;
import software.wings.beans.Infra;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Tag;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfraService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.TagService;
import software.wings.utils.BoundedInputStream;
import software.wings.utils.HostCsvFileHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 5/9/16.
 */
@ValidateOnExecution
@Singleton
public class HostServiceImpl implements HostService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private HostCsvFileHelper csvFileHelper;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private InfraService infraService;
  @Inject private SettingsService settingsService;
  @Inject private TagService tagService;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.HostService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<Host> list(PageRequest<Host> req) {
    return wingsPersistence.query(Host.class, req);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.HostService#get(java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public Host get(String appId, String infraId, String hostId) {
    return wingsPersistence.createQuery(Host.class)
        .field(ID_KEY)
        .equal(hostId)
        .field("infraId")
        .equal(infraId)
        .field("appId")
        .equal(appId)
        .get();
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.HostService#save(software.wings.beans.Host)
   */
  private Host save(Host host) {
    host.setHostName(host.getHostName().trim());
    return wingsPersistence.saveAndGet(Host.class, host);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.HostService#update(software.wings.beans.Host)
   */
  @Override
  public Host update(Host host) {
    ImmutableMap.Builder builder = ImmutableMap.<String, Object>builder()
                                       .put("hostName", host.getHostName())
                                       .put("hostConnAttr", host.getHostConnAttr());
    if (host.getBastionConnAttr() != null) {
      builder.put("bastionConnAttr", host.getBastionConnAttr());
    }
    if (host.getTags() != null) {
      builder.put("tags", host.getTags());
    }
    wingsPersistence.updateFields(Host.class, host.getUuid(), builder.build());
    return wingsPersistence.saveAndGet(Host.class, host);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.HostService#importHosts(java.lang.String, java.lang.String,
   * software.wings.utils.BoundedInputStream)
   */
  @Override
  public int importHosts(String appId, String infraId, BoundedInputStream inputStream) {
    Infra infra = wingsPersistence.get(Infra.class, infraId);
    notNullCheck("infra", infra);
    List<Host> hosts = csvFileHelper.parseHosts(infra, inputStream);
    List<String> Ids = wingsPersistence.save(hosts);
    return Ids.size();
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.HostService#getHostsByHostIds(java.lang.String, java.util.List)
   */
  @Override
  public List<Host> getHostsByHostIds(String appId, String infraId, List<String> hostUuids) {
    return wingsPersistence.createQuery(Host.class)
        .field("appId")
        .equal(appId)
        .field("infraId")
        .equal(infraId)
        .field(ID_KEY)
        .hasAnyOf(hostUuids)
        .asList();
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.HostService#getHostsByTags(java.lang.String, java.util.List)
   */
  @Override
  public List<Host> getHostsByTags(String appId, String envId, List<Tag> tags) {
    String infraId = infraService.getInfraIdByEnvId(appId, envId);
    return wingsPersistence.createQuery(Host.class)
        .field("appId")
        .equal(appId)
        .field("infraId")
        .equal(infraId)
        .field("tags")
        .hasAnyOf(tags)
        .asList();
  }

  @Override
  public void setTags(Host host, List<Tag> tags) {
    UpdateOperations<Host> updateOp = wingsPersistence.createUpdateOperations(Host.class).set("tags", tags);
    wingsPersistence.update(host, updateOp);
  }

  @Override
  public void removeTagFromHost(Host host, Tag tag) {
    UpdateOperations<Host> updateOp = wingsPersistence.createUpdateOperations(Host.class).removeAll("tags", tag);
    wingsPersistence.update(host, updateOp);
  }

  @Override
  public List<Host> getHostsByEnv(String appId, String envId) {
    String infraId = infraService.getInfraIdByEnvId(appId, envId);
    return wingsPersistence.createQuery(Host.class)
        .field("appId")
        .equal(appId)
        .field("infraId")
        .equal(infraId)
        .asList();
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.HostService#exportHosts(java.lang.String, java.lang.String)
   */
  @Override
  public File exportHosts(String appId, String infraId) {
    List<Host> hosts = wingsPersistence.createQuery(Host.class).field("infraId").equal(infraId).asList();
    return csvFileHelper.createHostsFile(hosts);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.HostService#delete(java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public void delete(String appId, String infraId, String hostId) {
    Host host = get(appId, infraId, hostId);
    delete(host);
  }

  private void delete(Host host) {
    if (host != null) {
      wingsPersistence.delete(host);
      serviceTemplateService.deleteHostFromTemplates(host);
    }
  }

  @Override
  public void deleteByInfra(String appId, String infraId) {
    wingsPersistence.createQuery(Host.class)
        .field("appId")
        .equal(appId)
        .field("infraId")
        .equal(infraId)
        .asList()
        .forEach(this ::delete);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.HostService#bulkSave(software.wings.beans.Host, java.util.List)
   */
  @Override
  public void bulkSave(String envId, Host baseHost) {
    List<String> hostNames = baseHost.getHostNames()
                                 .stream()
                                 .filter(hostName -> !isNullOrEmpty(hostName))
                                 .map(String::trim)
                                 .collect(Collectors.toList());
    List<ServiceTemplate> serviceTemplates =
        validateAndFetchServiceTemplate(baseHost.getAppId(), envId, baseHost.getServiceTemplates());

    hostNames.forEach(hostName -> {
      Host host = aHost()
                      .withHostName(hostName)
                      .withAppId(baseHost.getAppId())
                      .withInfraId(baseHost.getInfraId())
                      .withHostConnAttr(baseHost.getHostConnAttr())
                      .build();
      SettingAttribute bastionConnSttr =
          validateAndFetchBastionHostConnectionReference(baseHost.getAppId(), envId, baseHost.getBastionConnAttr());
      if (bastionConnSttr != null) {
        host.setBastionConnAttr(bastionConnSttr);
      }
      List<Tag> tags = validateAndFetchTags(baseHost.getAppId(), envId, baseHost.getTags());
      host.setTags(tags);
      save(host); // dedup
    });
    serviceTemplates.forEach(serviceTemplate
        -> serviceTemplateService.updateHosts(baseHost.getAppId(), envId, serviceTemplate.getUuid(), hostNames));
  }

  private List<ServiceTemplate> validateAndFetchServiceTemplate(
      String appId, String envId, List<ServiceTemplate> serviceTemplates) {
    List<ServiceTemplate> fetchedServiceTemplate = new ArrayList<>();
    if (serviceTemplates != null) {
      serviceTemplates.stream()
          .filter(this ::isValidDbReference)
          .map(serviceTemplate -> serviceTemplateService.get(appId, envId, serviceTemplate.getUuid()))
          .forEach(serviceTemplate -> {
            if (serviceTemplate == null) {
              throw new WingsException(ErrorCodes.INVALID_ARGUMENT, "args", "service mapping");
            }
            fetchedServiceTemplate.add(serviceTemplate);
          });
    }
    return fetchedServiceTemplate;
  }

  private List<Tag> validateAndFetchTags(String appId, String envId, List<Tag> tags) {
    List<Tag> fetchedTags = new ArrayList<>();
    if (tags != null) {
      tags.forEach(tag -> {
        if (isValidDbReference(tag)) {
          Tag fetchedTag = tagService.get(appId, envId, tag.getUuid());
          if (fetchedTag == null) {
            throw new WingsException(ErrorCodes.INVALID_ARGUMENT, "args", "tags");
          }
          fetchedTags.add(fetchedTag);
        }
      });
    }
    return fetchedTags;
  }

  private SettingAttribute validateAndFetchBastionHostConnectionReference(
      String appId, String envId, SettingAttribute settingAttribute) {
    if (!isValidDbReference(settingAttribute)) {
      return null;
    }
    SettingAttribute fetchedAttribute = settingsService.get(appId, envId, settingAttribute.getUuid());
    if (fetchedAttribute == null) {
      throw new WingsException(ErrorCodes.INVALID_ARGUMENT, "args", "bastionConnAttr");
    }
    return fetchedAttribute;
  }

  private boolean isValidDbReference(Base base) {
    return base != null && !isNullOrEmpty(base.getUuid());
  }
}
