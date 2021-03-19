package ti

import (
	"errors"
	"fmt"
)

// Callgraph object used to upload data to TI server
type Callgraph struct {
	Nodes []Node
	Relns []Relation
}

//ToStringMap converts Callgraph to map[string]interface{} for encoding
func (cg *Callgraph) ToStringMap() map[string]interface{} {
	var nodes, relns []interface{}
	for _, v := range (*cg).Nodes {
		data := map[string]interface{}{
			"package": v.Package,
			"method":  v.Method,
			"id":      v.ID,
			"params":  v.Params,
			"class":   v.Class,
			"type":    v.Type,
		}
		nodes = append(nodes, data)
	}
	for _, v := range (*cg).Relns {
		data := map[string]interface{}{
			"source": v.Source,
			"tests":  v.Tests,
		}
		relns = append(relns, data)
	}
	data := map[string]interface{}{
		"nodes": nodes,
		"relns": relns,
	}
	return data
}

//FromStringMap creates Callgraph object from map[string]interface{}
func FromStringMap(data map[string]interface{}) (*Callgraph, error) {
	var fNodes []Node
	var fRel []Relation
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
							node.Package = v.(string)
						case "id":
							node.ID = int(v.(int32))
						case "params":
							node.Params = v.(string)
						case "class":
							node.Class = v.(string)
						case "type":
							node.Type = v.(string)
						default:
							return nil, errors.New(fmt.Sprintf("unknown field received: %s", f))
						}
					}
					fNodes = append(fNodes, node)
				}
			} else {
				return nil, errors.New("failed to parse nodes in callgraph")
			}
		case "relns":
			if relns, ok := v.([]interface{}); ok {
				for _, reln := range relns {
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
						default:
							return nil, errors.New(fmt.Sprintf("unknown field received: %s", k))
						}
					}
					fRel = append(fRel, relation)
				}
			} else {
				return nil, errors.New("failed to parse relns in callgraph")
			}
		}
	}
	return &Callgraph{
		Relns: fRel,
		Nodes: fNodes,
	}, nil
}

//Node type represents detail of node in callgraph
type Node struct {
	Package string
	Method  string
	ID      int
	Params  string
	Class   string
	Type    string
}

// Input is the go representation of each line in callgraph file
type Input struct {
	Test   Node
	Source Node
}

//Relation b/w source and test
type Relation struct {
	Source int
	Tests  []int
}
