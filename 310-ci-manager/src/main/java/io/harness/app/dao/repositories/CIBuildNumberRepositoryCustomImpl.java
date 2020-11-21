package io.harness.app.dao.repositories;

import static org.springframework.data.mongodb.core.FindAndModifyOptions.options;

import io.harness.ci.beans.entities.BuildNumberDetails;
import io.harness.ci.beans.entities.BuildNumberDetails.BuildNumberDetailsKeys;

import com.google.inject.Inject;
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
  public BuildNumberDetails increaseBuildNumber(
      String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    Query query = new Query().addCriteria(new Criteria()
                                              .and(BuildNumberDetailsKeys.accountIdentifier)
                                              .is(accountIdentifier)
                                              .and(BuildNumberDetailsKeys.orgIdentifier)
                                              .is(orgIdentifier)
                                              .and(BuildNumberDetailsKeys.projectIdentifier)
                                              .is(projectIdentifier));

    return mongoTemplate.findAndModify(query, new Update().inc(BuildNumberDetailsKeys.buildNumber, 1),
        options().returnNew(true).upsert(true), BuildNumberDetails.class);
  }
}
