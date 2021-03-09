package ti

import "errors"

// Callgraph object is used to upload data to TI server
type Callgraph struct {
	Nodes []Node
	Relns []Relation
}

//ToStringMap converts CallgraphDto to map[string]interface{} for encoding
func (cg *Callgraph) ToStringMap() map[string]interface{} {
	var nodes, relations []interface{}
	for _, v := range (*cg).Nodes {
		data := map[string]interface{}{
			"package": v.Pkg,
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
		relations = append(relations, data)
	}

	data := map[string]interface{}{
		"nodes":     nodes,
		"relations": relations,
	}
	return data
}

//FromStringMap Creates Callgraph object of from map[string]interface{}
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
							node.Pkg = v.(string)
						}
					}
					fNodes = append(fNodes, node)
				}
			} else {
				return nil, errors.New("failed to parse ")
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
					fRel = append(fRel, relation)
				}
			}
		}
	}
	return &Callgraph{
		Relns: fRel,
		Nodes: fNodes,
	}, nil
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

// Input is the go representation of each line in callgraph file
type Input struct {
	Test   Node
	Source Node
}

//Relation b/w source and test
type Relation struct {
	Source int   `json:"source"`
	Tests  []int `json:"tests"`
}
