import java.util.List;
import java.util.ArrayList; // If you're using ArrayList for node children

public class Node {
    private String nodeType;
    private String varName;
    private List<Node> children; // List of child nodes

    public Node(String nodeType, String varName, List<Node> children) {
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

    public List<Node> getChildren() {
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
