package testintelligence

import (
	"bufio"
	"encoding/json"
	"fmt"
	"log"
	"os"

	"go.uber.org/zap"
)

//Parser reads callgraph file, processes it to extract
//extracted from callgraph
type Parser interface {
	// Parse and read the file from
	Parse(file string) ([]Relation, []Node, error)
}

// Callgraph object
type CallGraph struct {
	log *zap.SugaredLogger // logger
}

// NewCallGraph returns a new CallGraph client
func NewCallGraph(log *zap.SugaredLogger) *CallGraph {
	return &CallGraph{
		log: log,
	}
}

// Parse callgraph and return nodes and relations
func (cg *CallGraph) Parse(file string) ([]Relation, []Node, error) {
	f, err := os.Open(file)
	if err != nil {
		// cg.log.ErrorW(fmt.Sprintf("failed to open callgraph file at path %s", file), zap.Error(err))
		return nil, nil, err
	}
	r := bufio.NewReader(f)
	cgStr, err := readF(r)
	return parseInt(cgStr)
}

func parseInt(cgStr []string) ([]Relation, []Node, error) {
	var (
		err error
		inp []Input
	)
	fmt.Println("size of cgstr:", len(cgStr))
	for _, line := range cgStr {
		tinp := &Input{}
		err = json.Unmarshal([]byte(line), tinp)
		if err != nil {
			fmt.Printf("failed to parse json")
			log.Fatal(err)
			return nil, nil, err
		}
		inp = append(inp, *tinp)
	}
	rels, dets := process(inp)
	return rels, dets, nil
}

func process(inps []Input) ([]Relation, []Node) {
	var rel []Relation
	var node []Node

	relMap, nodeMap := convCallgph(inps)
	// Updating the Relations map
	for k, v := range relMap {
		tRel := Relation{
			Source: k,
			Tests:  v,
		}
		rel = append(rel, tRel)
	}

	// Updating thr nodes map
	for _, v := range nodeMap {
		node = append(node, v)
	}
	fmt.Println(rel)
	fmt.Println(node)
	return rel, node
}

func convCallgph(inps []Input) (map[int][]int, map[int]Node) {
	relMap := make(map[int][]int)
	nodeMap := make(map[int]Node)

	for _, inp := range inps {
		test := inp.Test
		test.Type = "test"
		testID := test.ID
		nodeMap[testID] = test
		source := inp.Source
		source.Type = "source"
		sourceID := source.ID
		nodeMap[sourceID] = source
		relMap[sourceID] = append(relMap[sourceID], testID)
	}
	return relMap, nodeMap
}

func readln(r *bufio.Reader) (string, error) {
	var (
		isPrefix bool  = true
		err      error = nil
		line, ln []byte
	)
	y := 1
	for isPrefix && err == nil {
		fmt.Println(y, "y")
		line, isPrefix, err = r.ReadLine()
		ln = append(ln, line...)
	}
	return string(ln), err
}

func readF(r *bufio.Reader) ([]string, error) {
	var ret []string
	s, e := readln(r)
	for e == nil {
		ret = append(ret, s)
		s, e = readln(r)
	}
	return ret, nil
}

//Node type represent details of callgraph
type Node struct {
	Pkg    string `json:"package"`
	Method string
	ID     int
	Params string
	Class  string
	Type   string
}

// Input corresponds to each entry in callgraph
type Input struct {
	Test   Node
	Source Node
}

//Relation b/w source and test
type Relation struct {
	Source int   `json:"source"`
	Tests  []int `json:"tests"`
}

type CGphDTO struct {
	Nodes     []Node
	Relations []Relation
}

//ToStringMap converts CallgraphDto to string for avro encoding
func (cg *CGphDTO) ToStringMap(map[string]interface{}, error) {
	var nodes, relations []interface{}
	for _, v := range *cg.Nodes {
		datumIn := map[string]interface{}{
			"package": string(u.Pkg),
			"method":  string(u.Method),
		}
		names = append(names, datumIn)
	}
	for _, v = range *cg.Relations {
		relations = append(relations, v)
	}

	datum := map[string]interface{}{
		"nodes":     nodes,
		"relations": relations,
	}
	return datum
}
