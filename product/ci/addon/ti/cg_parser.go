package ti

import (
	"bufio"
	"encoding/json"
	"fmt"
	"os"

	"github.com/pkg/errors"

	"go.uber.org/zap"
)

//Parser reads callgraph file, processes it to extract
//extracted from callgraph
type Parser interface {
	// Parse and read the file from
	Parse(file string) ([]Relation, []Node, error)
}

// CallGraphParser object
type CallGraphParser struct {
	log *zap.SugaredLogger // logger
}

// NewCallGraphParser returns a new CallGraph client
func NewCallGraphParser(log *zap.SugaredLogger) *CallGraphParser {
	return &CallGraphParser{
		log: log,
	}
}

// Parse callgraph and return nodes and relations
func (cg *CallGraphParser) Parse(file string) (Callgraph, error) {
	f, err := os.Open(file)
	if err != nil {
		return Callgraph{}, errors.Wrap(err, fmt.Sprintf("failed to open file %s", file))
	}
	r := bufio.NewReader(f)
	cgStr, err := rFile(r)
	if err != nil {
		return Callgraph{}, errors.Wrap(err, fmt.Sprintf("failed to read file %s", file))
	}
	return parseInt(cgStr)
}

func parseInt(cgStr []string) (Callgraph, error) {
	var (
		err error
		inp []Input
	)
	for _, line := range cgStr {
		tinp := &Input{}
		err = json.Unmarshal([]byte(line), tinp)
		if err != nil {
			return Callgraph{}, errors.Wrap(err, fmt.Sprintf("data unmarshalling to json failed for line [%s]", line))
		}
		inp = append(inp, *tinp)
	}
	rels, dets := process(inp)
	callgraph := Callgraph{
		Nodes: dets,
		Relns: rels,
	}
	return callgraph, nil
}

func process(inps []Input) ([]Relation, []Node) {
	var rel []Relation
	var node []Node

	relMap, nodeMap := convCallgph(inps)
	// Updating the Relns map
	for k, v := range relMap {
		tRel := Relation{
			Source: k,
			Tests:  v,
		}
		rel = append(rel, tRel)
	}

	// Updating the nodes map
	for _, v := range nodeMap {
		node = append(node, v)
	}
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

func rLine(r *bufio.Reader) (string, error) {
	var (
		isPrefix bool  = true
		err      error = nil
		line, ln []byte
	)
	for isPrefix && err == nil {
		line, isPrefix, err = r.ReadLine()
		ln = append(ln, line...)
	}
	return string(ln), err
}

func rFile(r *bufio.Reader) ([]string, error) {
	var ret []string
	s, e := rLine(r)
	for e == nil {
		ret = append(ret, s)
		s, e = rLine(r)
	}
	return ret, nil
}
