/*
 * Copyright 2018 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

rs.initiate( {
   _id : "replicaset_name",
   members: [
      { _id: 0, host: "host1:mongodb_port" },
      { _id: 1, host: "host2:mongodb_port" },
      { _id: 2, host: "host3:mongodb_port" }
   ]
})
