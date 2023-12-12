package pdc.model;

import java.math.BigDecimal;
import java.util.ArrayList;

public class NumericalItem extends DataItem {
        
    private BigDecimal number;

    public NumericalItem(int index, String name, NodeData originalNode, ArrayList<NodeData> storedOn, BigDecimal number) {
        super(index, name, originalNode, storedOn);
        this.type = "Numerical";
        this.number = number;
        this.checksum = getChecksumValue(number.toString().getBytes());
    }

    @Override
    public String toPrintDetailed() {
        return "Data type: "+type+", ID: "+Long.toString(id)+", sent from Node: "+originalNode.getName()+", stored in index: "+index+". Numerical Value: "+number.toString();
    }

    public BigDecimal getNumber(){ return number; }
}
