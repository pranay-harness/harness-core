//mongeez formatted javascript
//changeset srinivas:application-index-migration



if(db.applications.getIndexes().map(function(index) {  return index.name; }).indexOf("name_1") > -1) {
    //Dropping indexes
    db.applications.dropIndex('name_1');
}

