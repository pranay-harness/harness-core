package ti

// Callgraph object is used to upload data to TI server
type Callgraph struct {
	Nodes     []Node
	Relations []Relation
}

//ToStringMap converts CallgraphDto to map[string]interface{} for encoding
func (cg *Callgraph) ToStringMap() map[string]interface{} {
	var nodes, relations []interface{}
	for _, v := range (*cg).Nodes {
		datumIn := map[string]interface{}{
			"package": v.Pkg,
			"method":  v.Method,
			"id":      v.ID,
			"paramas": v.Params,
			"class":   v.Class,
			"type":    v.Type,
		}
		nodes = append(nodes, datumIn)
	}
	for _, v := range (*cg).Relations {
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
	return datum
}

//FromStringMap Creates Callgraph object of from map[string]interface{}
func FromStringMap(data map[string]interface{}) (*Callgraph, error) {
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
	return &Callgraph{
		Relations: fRelations,
		Nodes:     fNodes,
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
