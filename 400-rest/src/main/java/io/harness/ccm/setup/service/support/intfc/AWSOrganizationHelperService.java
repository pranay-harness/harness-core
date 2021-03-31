package io.harness.ccm.setup.service.support.intfc;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;

import software.wings.beans.AwsCrossAccountAttributes;

import com.amazonaws.services.organizations.model.Account;
import java.util.List;

@OwnedBy(CE)
public interface AWSOrganizationHelperService {
  List<Account> listAwsAccounts(AwsCrossAccountAttributes awsCrossAccountAttributes);
}
