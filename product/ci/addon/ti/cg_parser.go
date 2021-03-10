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
// nodes and relations
type Parser interface {
	// Parse and read the file from
	Parse(file string) (*Callgraph, error)
}

// CallGraphParser struct definition
type CallGraphParser struct {
	log *zap.SugaredLogger // logger
}

// NewCallGraphParser returns a new CallGraphParser client
func NewCallGraphParser(log *zap.SugaredLogger) *CallGraphParser {
	return &CallGraphParser{
		log: log,
	}
}

// Parse callgraph and return nodes and relations
func (cg *CallGraphParser) Parse(file string) (*Callgraph, error) {
	f, err := os.Open(file)
	if err != nil {
		return nil, errors.Wrap(err, fmt.Sprintf("failed to open file %s", file))
	}
	r := bufio.NewReader(f)
	cgStr, err := rFile(r)
	if err != nil {
		return nil, errors.Wrap(err, fmt.Sprintf("failed to read file %s", file))
	}
	return parseInt(cgStr)
}

func parseInt(cgStr []string) (*Callgraph, error) {
	var (
		err error
		inp []Input
	)
	for _, line := range cgStr {
		tinp := &Input{}
		err = json.Unmarshal([]byte(line), tinp)
		if err != nil {
			return nil, errors.Wrap(err, fmt.Sprintf("data unmarshalling to json failed for line [%s]", line))
		}
		inp = append(inp, *tinp)
	}
	return process(inp), nil
}

func process(inps []Input) *Callgraph {
	var relns []Relation
	var nodes []Node

	relMap, nodeMap := convCgph(inps)
	// Updating the Relns map
	for k, v := range relMap {
		tRel := Relation{
			Source: k,
			Tests:  v,
		}
		relns = append(relns, tRel)
	}

	// Updating the nodes map
	for _, v := range nodeMap {
		nodes = append(nodes, v)
	}
	return &Callgraph{
		Nodes: nodes,
		Relns: relns,
	}
}

func convCgph(inps []Input) (map[int][]int, map[int]Node) {
	relMap := make(map[int][]int)
	nodeMap := make(map[int]Node)

	for _, inp := range inps {
		// processing nodeMap
		test := inp.Test
		test.Type = "test"
		testID := test.ID
		nodeMap[testID] = test
		// processing relmap
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
