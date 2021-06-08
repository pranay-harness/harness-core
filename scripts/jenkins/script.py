import csv
from queue import PriorityQueue

MAX_NODES=5
THRESHOLD = float(0.2)


def getTime(num_nodes, testsWithTime):
    q = PriorityQueue()
    tests_per_node={}
    q.put((0,0))
    tests_per_node[0]=[]
    for i in range(1,num_nodes):
        q.put((2,i))
        tests_per_node[i]=[]
    for testWithTime in testsWithTime:
        top = q.get()
        tests_per_node[top[1]].append(testWithTime[1])
        q.put((top[0]+testWithTime[0],top[1]))
    maxTime=0
    while not q.empty():
        maxTime=max(maxTime,q.get()[0])
    return maxTime, tests_per_node


def checkThreshold(previous_time, current_time):
    return ((previous_time-current_time)/previous_time) > THRESHOLD 


with open ("data.txt", "r") as myfile:
    data=myfile.readlines()

tests=[]

for i in data:
    cur=i
    test_name=cur.partition("NO STATUS")[0]
    tests.append(test_name.strip())

reader = csv.reader(open('TestClassReport.csv', 'r'))
test_times= {}
totalTime=float(0)
count=0
for k,z,v in reader:
    if (v=="Time"):
        continue
    totalTime+= float(v)
    count+=1
    for test in tests:
        if k.strip() in test:
            test_times[test] = float(v)
averageTime= totalTime/count

for test in tests:
    if test not in test_times:
        test_times[test]=averageTime

tests_with_time=[]
for test in tests:
    tests_with_time.append((test_times[test],test))
tests_with_time.sort(reverse=1)

ideal_time = 0
ideal_test_per_node = {}
ideal_test_per_node[0]=[]
for test in tests:
    ideal_time+=test_times[test]
    ideal_test_per_node[0].append(test)

for num_nodes in range(2,MAX_NODES+1):
    current_time, current_tests_per_node = getTime(num_nodes, tests_with_time)
    if (not checkThreshold(float(ideal_time),float(current_time))):
        break
    ideal_time, ideal_test_per_node = current_time, current_tests_per_node

print(ideal_test_per_node)

for i in range(5):
    f = open(str(i)+".txt", "w")

for node in ideal_test_per_node:
    toWrite=""
    for test in ideal_test_per_node[node]:
        toWrite+=test+"/... "
    f = open(str(node)+".txt", "w")
    f.write(toWrite)
