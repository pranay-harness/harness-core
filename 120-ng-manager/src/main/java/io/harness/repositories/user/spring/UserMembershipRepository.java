package io.harness.repositories.user.spring;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.entities.Project;
import io.harness.ng.core.user.entities.UserMembership;
import io.harness.repositories.user.custom.UserMembershipRepositoryCustom;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@OwnedBy(PL)
public interface UserMembershipRepository
    extends PagingAndSortingRepository<UserMembership, String>, UserMembershipRepositoryCustom {
  Optional<UserMembership> findDistinctByUserId(String userId);

  Long deleteUserMembershipByUserId(String userId);

  @Aggregation(
      pipeline = {"{ $match : { userId : ?0 } }", "{ $unwind : { path : '$scopes' } }",
          "{ $match: {'scopes.projectIdentifier' : {$exists: true}}}",
          "{ $lookup: { "
              + " 'from': 'projects', "
              + " 'let': {  "
              + "    accountId: '$scopes.accountIdentifier', "
              + "    orgId:     '$scopes.orgIdentifier', "
              + "    projectId: '$scopes.projectIdentifier' "
              + " }, "
              + " 'pipeline': [ "
              + " { '$match': { '$expr': { "
              + "   $and: [ "
              + "        { $eq: ['$accountIdentifier', '$$accountId'] }, "
              + "        { $eq: ['$orgIdentifier', '$$orgId'] }, "
              + "        { $eq: ['$identifier', '$$projectId'] }, "
              + "   ] "
              + " }}} "
              + " ], "
              + " 'as':'projectDetails' "
              + " }}",
          "{$project: {'contents': {$arrayElemAt:[ '$projectDetails',0]}}},", "{$replaceRoot: {newRoot:'$contents'}}"})
  List<Project>
  findProjectList(String userId, Pageable pageable);
}
