package io.harness.testframework.framework.matchers;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.testframework.framework.email.EmailMetaData;
import io.harness.testframework.framework.email.GuerillaEmailDetails;

import java.util.ArrayList;
import java.util.List;

public class EmailMatcher<T> implements Matcher {
  @Override
  public boolean matches(Object expected, Object actual) {
    String subject = (String) expected;
    if (actual instanceof GuerillaEmailDetails) {
      GuerillaEmailDetails gmailDetails = (GuerillaEmailDetails) actual;
      assertThat(gmailDetails).isNotNull();
      List<EmailMetaData> metaDataList = new ArrayList<>();
      assertThat(metaDataList != null && metaDataList.size() > 0).isTrue();
      for (EmailMetaData metaData : metaDataList) {
        if (metaData.getMailSubject().equals(subject)) {
          return true;
        }
      }
    }
    return false;
  }
}
