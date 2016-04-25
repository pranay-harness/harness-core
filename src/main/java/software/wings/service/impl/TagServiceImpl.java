package software.wings.service.impl;

import static software.wings.service.intfc.FileService.FileBucket.CONFIGS;

import software.wings.beans.ConfigFile;
import software.wings.beans.Tag;
import software.wings.beans.TagType;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.TagService;

import java.io.InputStream;
import javax.inject.Inject;

/**
 * Created by anubhaw on 4/25/16.
 */
public class TagServiceImpl implements TagService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private FileService fileService;

  @Override
  public Tag createTag(Tag tag) {
    return wingsPersistence.saveAndGet(Tag.class, tag);
  }

  @Override
  public TagType createTagType(TagType tagType) {
    return wingsPersistence.saveAndGet(TagType.class, tagType);
  }

  @Override
  public String saveFile(String tagId, ConfigFile configFile, InputStream inputStream) {
    fileService.saveFile(configFile, inputStream, CONFIGS);
    String configFileId = wingsPersistence.save(configFile);
    wingsPersistence.addToList(Tag.class, tagId, "configFiles", configFile);
    return configFileId;
  }
}
