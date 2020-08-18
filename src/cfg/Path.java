package cfg;

import soot.Unit;

import java.util.ArrayList;
import java.util.List;

public class Path {

    public List<Node> nodes = new ArrayList<>();

    public Path() {

    }

    public boolean hasNode(Unit targetUnit) {
        for(Node node : nodes) {
            if(node.unit.equals(targetUnit)) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<Unit> getBackwardSlice(Unit source) {
        ArrayList<Unit> res = new ArrayList<>();
        for(Node node : nodes) {
            res.add(node.unit);
            if(node.unit.equals(source)) {
                break;
            }
        }
        return res;
    }

    public void addHead(Node node) {
        nodes.add(0, node);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        return sb.toString();
    }

}
