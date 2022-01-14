rs.initiate( {
   _id : "replicaset_name",
   members: [
      { _id: 0, host: "host1:mongodb_port" },
      { _id: 1, host: "host2:mongodb_port" },
      { _id: 2, host: "host3:mongodb_port" }
   ]
})
