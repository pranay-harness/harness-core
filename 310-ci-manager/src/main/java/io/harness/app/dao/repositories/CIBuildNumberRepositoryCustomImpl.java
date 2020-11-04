package io.harness.app.dao.repositories;

import static org.springframework.data.mongodb.core.FindAndModifyOptions.options;

import com.google.inject.Inject;

import io.harness.ci.beans.entities.BuildNumber;
import io.harness.ci.beans.entities.BuildNumber.BuildNumberKeys;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class CIBuildNumberRepositoryCustomImpl implements CIBuildNumberRepositoryCustom {
  private final MongoTemplate mongoTemplate;

  @Override
  public BuildNumber increaseBuildNumber(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    Query query = new Query().addCriteria(new Criteria()
                                              .and(BuildNumberKeys.accountIdentifier)
                                              .is(accountIdentifier)
                                              .and(BuildNumberKeys.orgIdentifier)
                                              .is(orgIdentifier)
                                              .and(BuildNumberKeys.projectIdentifier)
                                              .is(projectIdentifier));

    return mongoTemplate.findAndModify(query, new Update().inc(BuildNumberKeys.buildNumber, 1),
        options().returnNew(true).upsert(true), BuildNumber.class);
  }
}
