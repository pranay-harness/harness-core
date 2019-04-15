package migrations.all;

import static software.wings.beans.security.UserGroup.DEFAULT_USER_GROUPS;

import com.google.inject.Singleton;

import com.mongodb.DBObject;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.security.UserGroup;

@Slf4j
@Singleton
public class AddIsDefaultFlagToUserGroup extends AddFieldMigration {
  @Override
  protected org.slf4j.Logger getLogger() {
    return logger;
  }

  @Override
  protected String getCollectionName() {
    return "userGroups";
  }

  @Override
  protected Class getCollectionClass() {
    return UserGroup.class;
  }

  @Override
  protected String getFieldName() {
    return "isDefault";
  }

  @Override
  protected Object getFieldValue(DBObject existingRecord) {
    String userGroupName = (String) existingRecord.get(UserGroup.NAME_KEY);

    return DEFAULT_USER_GROUPS.contains(userGroupName);
  }
}
