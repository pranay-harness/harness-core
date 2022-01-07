#!/usr/bin/python3
# Copyright 2020 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

import json


map = {}  # id --> node
relation = {}
test_set =set()
source_set = set()
def mergeRelations(filename):
    with open(filename, mode='r') as json_file:
        for line in json_file:
            jsonobj = json.loads(line)
            test = jsonobj['test']
            test['type'] = 'test'
            test_id = test['id']
            map[test_id] = test
            test_set.add(test_id)  #just for counting purpose

            source = jsonobj['source']
            source['type'] = 'source'
            source_id = source['id']
            map[source_id] = source
            source_set.add(source_id) #just for counting purpose

            if source_id not in relation:
                relation[source_id] = set()
            relation[source_id].add(test_id)

# mergeRelations('./test.json')
mergeRelations('./portal1.json')
mergeRelations('./portal2.json')
mergeRelations('./portal3.json')

merge = {}
with open('relations.json', 'w') as relationsfile:
    for key, value in relation.items():
        tests = []
        # merge['source'] = map[key]
        merge['source'] = key
        for v in value:
            #tests.append(map[v])
            tests.append(v)
        merge['tests'] = tests
        # print(merge)
        json.dump(merge, relationsfile)
        relationsfile.write('\n')

with open('nodes.json', 'w') as nodesfile:
    # json.dump(map, nodesfile)
    for key,value in map.items():
        node = {}
        node[key] = value
        json.dump(node, nodesfile)
        nodesfile.write('\n')




print(len(test_set))
print(len(source_set))
