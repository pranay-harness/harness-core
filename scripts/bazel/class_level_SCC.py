# Copyright 2020 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

import subprocess
import os
import sys
class SCC:
    MAXN = 10000
    BUNDLE = 30
    comp=0
    graph=dict()
    reverse_graph=dict()
    visited = dict()
    comp_id = dict()
    order  = []
    nodes = []
    all_classes = []
    compressed_DAG = dict()
    full_deps = dict()
    scc_list = dict()

    def init(self,nodes):
        self.all_classes = nodes
        for node in nodes:
            if "$" not in node:
                self.nodes.append(node)
        for node in self.nodes:
            self.graph[node]=[]
            self.reverse_graph[node]=[]
            # self.compressed_DAG[node]=set()
        # print(self.nodes)
    def add_edge(self,u,v):
        self.graph[u].append(v)
        self.reverse_graph[v].append(u)

    def dfs1(self,node):
        self.visited[node]=1
        for v in self.graph[node]:
            if not self.visited.get(v):
                self.dfs1(v)
        self.order.append(node)

    def dfs2(self,node,id):
        self.comp_id[node]=id
        for v in self.reverse_graph[node]:
            if v not in self.comp_id:
                self.dfs2(v,id)

    def get_SCC(self):
        for node in self.nodes:
            if not self.visited.get(node):
                self.dfs1(node)
        for node in self.nodes:
            if node not in self.order:
                self.order.append(node)
        while len(self.order)>0:
            node = self.order[-1]
            self.order.pop()
            if not self.comp_id.get(node):
                self.dfs2(node,self.comp)
                self.compressed_DAG[self.comp]=set()
                self.comp+=1

        for node in self.nodes:
            for v in self.graph[node]:
                if self.comp_id[node] != self.comp_id[v]:
                    self.compressed_DAG[self.comp_id[node]].add(self.comp_id[v])

    def dfs3(self,id):
        if self.visited.get(id):
            return 0
        self.visited[id]=1
        self.full_deps[id].update(self.compressed_DAG[id])
        for dep_id in self.compressed_DAG[id]:
            self.dfs3(dep_id)
            # self.full_deps[id].update(self.compressed_DAG[dep_id])

    def populate_scc_list(self):
        for node in self.comp_id.keys():
            if self.comp_id[node] not in self.scc_list:
                self.scc_list[self.comp_id[node]]=set()
            self.scc_list[self.comp_id[node]].add(node)

    def calculate_full_deps(self):
        self.visited.clear()
        for id in self.compressed_DAG.keys():
            self.full_deps[id]=set()
        for id in self.compressed_DAG.keys():
            self.dfs3(id)
        self.populate_scc_list()

    def add_module_target(self, n):
        target_list = []
        for i in range(1001, 1001+n):
            target_list.append("target"+str(i))
        f3.write("java_library(\n\tname=\"full\",\n")
        f3.write("\texports="+str(target_list)+",\n")
        f3.write("\tvisibility = [\"//visibility:public\"],\n)\n")

    def add_targets(self):
        targets = dict()
        files = dict()

        target_index = 1001
        targets[target_index] = set()
        for id in self.compressed_DAG.keys():
            if len(targets[target_index]) != 0 and \
               (len(targets[target_index]) + len(self.compressed_DAG[id]) >= self.BUNDLE):
              target_index += 1
              targets[target_index] = set()

            targets[target_index].add(id)
            files[id] = target_index

        for targetId in targets.keys():
            srcs = set()
            target_set = set()
            for id in targets[targetId]:
                srcs.update(self.scc_list[id])
                for depId in self.compressed_DAG[id]:
                    depTargetId = files[depId]
                    if depTargetId != targetId:
                        target_set.add("target"+str(depTargetId))

            srcs1 = []
            for src in srcs:
                if "/generated/" in src:
                    srcs1.append("src/generated/java/"+src+"$$java")
                    continue
                srcs1.append("src/main/java/"+src+"$$java")
            srcs1 =str(srcs1)
            srcs1 = srcs1.replace(".","/")
            srcs1 = srcs1.replace("$$",".")
            f3.write("java_library(\n\tname=\"target"+str(targetId)+"\",\n")
            f3.write("\tsrcs="+srcs1+",\n")

            target_list = list(target_set)

            f3.write("\texports="+str(target_list)+",\n")

            target_list.append("maven_deps")
            target_list.append("//:lombok")
            f3.write("\tdeps="+str(target_list)+",\n)\n")
        self.add_module_target(len(targets))

    def store_graph(self):
        temp_set = set()
        f4 = open("resources/bazel/graph.txt","w")
        for u in self.nodes:
            for v in self.graph[u]:
                f4.write(u+" "+v+"\n")
                temp_set.add(u)
                temp_set.add(v)
        for node in self.nodes:
            if node not in temp_set:
                f4.write(node+" "+node+"\n")

    def print_SCC(self):
        print(self.graph)
        print("\n\n\n\n")
        print(self.comp_id)
        print("\n\n\n\n")
        print(self.compressed_DAG)

dir_name = "experiment"
os.system('find resources/bazel/'+dir_name+' -name "*.class" -type f > resources/bazel/sources-list.txt')
os.system('find 400-rest/src/main/java -name "*.java" -type f > resources/bazel/raw-files.txt')
f1 = open("resources/bazel/sources-list.txt","r")
f2 = open("resources/bazel/raw-files.txt","r")
f3 = open("resources/bazel/targets-list.txt","w")
count=0
temp_lines = f1.readlines()
java_files_list = f2.readlines()
scc = SCC()
class_files_list = []
for line in temp_lines:
    index = line.rfind(".")
    line = line[:index]
    line=line.replace(dir_name+"/","")
    class_files_list.append(line)
class_files_list=list(set(class_files_list))

for i in range(len(java_files_list)):
    temp = java_files_list[i]
    temp = "/".join(temp.split("/")[4:])
    index = temp.rfind(".")
    temp = temp[:index]
    java_files_list[i] = temp
java_files_list = set(java_files_list)
visited=dict()
def analyse_class(f):
    l=[]
    l.append(f)
    # need to check on this. sometimes independent packages doesn't appear in targets list
    while(len(l)>0):
        f = l[0]
        fro = f
        l=l[1:]
        if visited.get(f):
            continue
        visited[f]=1
        # print(f)
        f1=f[:]
        f1 = f1.replace("$","\$")
        output = subprocess.check_output('jdeps -v -cp '+dir_name+" "+dir_name+'/'+f1+".class", shell=True)
        output = output.decode("utf-8")
        output = output.split('\n')
        for i in range(len(output)):
            # print(output[i])
            line = output[i].split()
            if len(line)<2:
                continue
            to = line[2]
            if "->" in to:
                to = line[1]
            to = to.replace(".","/")
            fro1 = fro[:]
            to1 = to[:]
            if "$" in fro1:
                fro1 = fro[:fro.find('$')]
            if "$" in to1:
                to1 = to[:to.find('$')]
            # print(to)
            # don't add in graph if package belongs to another module
            if to not in scc.nodes:
                continue
            # print(output[i])
            l.append(to)
            print(fro1,to1)
            scc.add_edge(*(fro1,to1))
        print(len(visited))
def add_dymmy_cycles():
    scc.add_edge(*("A","B"))
    scc.add_edge(*("B","C"))
    scc.add_edge(*("C","D"))
    scc.add_edge(*("D","A"))
    scc.add_edge(*("C","E"))
    scc.add_edge(*("E","F"))
    scc.add_edge(*("F","C"))
    scc.add_edge(*("E","Q"))
    scc.add_edge(*("P","Q"))
    scc.add_edge(*("Q","P"))
    scc.add_edge(*("S","R"))

def read_graph_from_file():
    f = open("resources/bazel/400-rest-graph-1.txt","r")
    edges = f.readlines()
    nodes = set()
    for edge in edges:
        edge = edge.split()
        if edge[0] not in java_files_list and "/generated/" not in edge[0] or edge[1] not in java_files_list and "/generated/" not in edge[1] :
            continue
        nodes.add(edge[0])
        nodes.add(edge[1])
    scc.init(list(nodes))
    for edge in edges:
        edge = edge.split()
        if edge[0] not in java_files_list and "/generated/" not in edge[0] or edge[1] not in java_files_list and "/generated/" not in edge[1] :
            continue
        scc.add_edge(*(edge[0],edge[1]))
    add_compile_time_deps()

def add_compile_time_deps():
    f = open("resources/bazel/compile_time_deps.txt","r")
    edges = f.readlines()
    nodes = set()
    for edge in edges:
        edge = edge.split()
        nodes.add(edge[0])
        nodes.add(edge[1])
    for node in nodes:
        if node not in scc.graph:
            scc.graph[node]=[]
        if node not in scc.reverse_graph:
            scc.reverse_graph[node]=[]
    scc.nodes+=list(nodes)
    for edge in edges:
        edge = edge.split()
        scc.add_edge(*(edge[0],edge[1]))

def create_graph_from_scratch():
    scc.init(class_files_list)
    add_compile_time_deps()
    for i in range(len(class_files_list)):
        analyse_class(class_files_list[i])
def main(args):
    if len(args)>1 and args[1]=="create":
        create_graph_from_scratch()
    else:
        read_graph_from_file()
    scc.get_SCC()
    print("\n\n\n\n\n\n")
    scc.calculate_full_deps()
    scc.add_targets()
    if len(args)>2 and args[2]=="store":
        scc.store_graph()
if __name__ == "__main__":
    main(sys.argv)
