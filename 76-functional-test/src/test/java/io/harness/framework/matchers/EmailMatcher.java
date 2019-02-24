package io.harness.framework.matchers;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

import io.harness.framework.email.EmailMetaData;
import io.harness.framework.email.GuerillaEmailDetails;

import java.util.List;

public class EmailMatcher<T> implements Matcher {
  @Override
  public boolean matches(Object expected, Object actual) {
    String subject = (String) expected;
    if (actual instanceof GuerillaEmailDetails) {
      GuerillaEmailDetails gmailDetails = (GuerillaEmailDetails) actual;
      assertNotNull(gmailDetails);
      List<EmailMetaData> metaDataList = gmailDetails.getList();
      assertTrue(metaDataList != null && metaDataList.size() > 0);
      for (EmailMetaData metaData : metaDataList) {
        if (metaData.getMailSubject().equals(subject)) {
          return true;
        }
      }
    }
    return false;
  }
}
