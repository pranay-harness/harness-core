package io.harness.migrations.all;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static software.wings.beans.security.UserGroup.DEFAULT_USER_GROUPS;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.security.UserGroup;

import com.google.inject.Singleton;
import com.mongodb.DBObject;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@Slf4j
@Singleton
public class AddIsDefaultFlagToUserGroup extends AddFieldMigration {
  @Override
  protected org.slf4j.Logger getLogger() {
    return log;
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
