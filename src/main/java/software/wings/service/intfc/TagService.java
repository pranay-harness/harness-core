package software.wings.service.intfc;

import software.wings.beans.ConfigFile;
import software.wings.beans.Tag;
import software.wings.beans.TagType;

import java.io.InputStream;

/**
 * Created by anubhaw on 4/25/16.
 */
public interface TagService {
  Tag createTag(Tag tag);
  TagType createTagType(TagType tagType);
  String saveFile(String tagId, ConfigFile configFile, InputStream inputStream);
}
