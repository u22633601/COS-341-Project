import java.util.List;


public class NodeType {
    private String nodeType;
    private String varName;
    private List<NodeType> children; // List of child nodes

    public NodeType(String nodeType, String varName, List<NodeType> children) {
        this.nodeType = nodeType;
        this.varName = varName;
        this.children = children;
    }

    public String getType() {
        return nodeType;
    }

    public String getVarName() {
        return varName;
    }

    public List<NodeType> getChildren() {
        return children;
    }

    // Example check to determine if the node is an assignment
    public boolean isAssignment() {
        return nodeType.equals("ASSIGN") || varName.equals("=");
    }

    // Block start/end methods can be defined as per your grammar
    public boolean isBlockStart() {
        return varName.equals("begin");
    }

    public boolean isBlockEnd() {
        return varName.equals("end");
    }
}
