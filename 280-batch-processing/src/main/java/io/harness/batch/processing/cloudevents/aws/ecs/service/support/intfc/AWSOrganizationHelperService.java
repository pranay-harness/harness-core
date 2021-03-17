package io.harness.batch.processing.cloudevents.aws.ecs.service.support.intfc;

import software.wings.beans.AwsCrossAccountAttributes;

import com.amazonaws.services.organizations.model.Account;
import java.util.List;

public interface AWSOrganizationHelperService {
  List<Account> listAwsAccounts(AwsCrossAccountAttributes awsCrossAccountAttributes);
}
