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
func (cg *CallGraph) Parse(file string) (CGphDTO, error) {
	f, err := os.Open(file)
	if err != nil {
		// cg.log.ErrorW(fmt.Sprintf("failed to open callgraph file at path %s", file), zap.Error(err))
		return CGphDTO{}, err
	}
	r := bufio.NewReader(f)
	cgStr, err := readF(r)
	return parseInt(cgStr)
}

func parseInt(cgStr []string) (CGphDTO, error) {
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
			return CGphDTO{}, err
		}
		inp = append(inp, *tinp)
	}
	rels, dets := process(inp)
	cgphDTO := CGphDTO{
		Nodes:     dets,
		Relations: rels,
	}
	return cgphDTO, nil
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
func (cg *CGphDTO) ToStringMap() (map[string]interface{}, error) {
	var nodes, relations []interface{}
	for _, v := range (*cg).Nodes {
		datumIn := map[string]interface{}{
			"package": string(v.Pkg),
			"method":  string(v.Method),
		}
		nodes = append(nodes, datumIn)
	}
	for _, v := range (*cg).Relations {
		fmt.Println("source", int(v.Source))
		datumIn := map[string]interface{}{
			"source": int(v.Source),
			"tests":  v.Tests,
		}
		relations = append(relations, datumIn)
	}

	datum := map[string]interface{}{
		"nodes":     nodes,
		"relations": relations,
	}
	return datum, nil
}

//FromStringMap converts CallgraphDto to string for avro encoding
func (cg *CGphDTO) FromStringMap(data map[string]interface{}) (*CGphDTO, error) {
	var fNodes []Node
	var fRelations []Relation
	for k, v := range data {
		switch k {
		case "nodes":
			if nodes, ok := v.([]interface{}); ok {
				for _, t := range nodes {
					fields := t.(map[string]interface{})
					var node Node
					for f, v := range fields {
						switch f {
						case "method":
							node.Method = v.(string)
						case "package":
							node.Pkg = v.(string)
						}
					}
					fNodes = append(fNodes, node)
				}
			}
		case "relations":
			if relations, ok := v.([]interface{}); ok {
				for _, reln := range relations {
					var relation Relation
					fields := reln.(map[string]interface{})
					for k, v := range fields {
						switch k {
						case "source":
							relation.Source = int(v.(int32))
						case "tests":
							var testsN []int
							for _, v := range v.([]interface{}) {
								testsN = append(testsN, int(v.(int32)))
							}
							relation.Tests = testsN
						}
					}
					fRelations = append(fRelations, relation)
				}
			}
		}
	}
	return &CGphDTO{
		Relations: fRelations,
		Nodes:     fNodes,
	}, nil
}
