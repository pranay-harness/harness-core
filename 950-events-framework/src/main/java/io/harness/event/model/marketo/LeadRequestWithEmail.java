/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.event.model.marketo;

import java.util.List;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;

/**
 * @author rktummala on 11/20/2018
 */
@Data
@Builder
public class LeadRequestWithEmail {
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
  @Default private String lookupField = "email";
  private List<Lead> input;
}
