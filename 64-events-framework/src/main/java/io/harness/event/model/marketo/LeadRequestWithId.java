package io.harness.event.model.marketo;

import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;

import java.util.List;

/**
 * @author rktummala on 11/20/2018
 */
@Data
@Builder
public class LeadRequestWithId {
  //  {
  //    "action":"createOrUpdate",
  //      "lookupField":"email",
  //      "input":[
  //    {
  //      "email": "rama@harness.io",
  //        "firstName": "Rama",
  //        "lastName": "Tummala",
  //        "company": "Test account"
  //    }   ]
  //  }

  /**
   * Valid values are createOnly, updateOnly and createOrUpdate
   */
  @Default private String action = "createOrUpdate";
  @Default private String lookupField = "id";
  private List<Lead> input;
}
