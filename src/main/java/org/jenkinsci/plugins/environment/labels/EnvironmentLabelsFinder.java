package org.jenkinsci.plugins.environment.labels;

import hudson.Extension;
import hudson.model.Computer;
import hudson.model.Label;
import hudson.model.LabelFinder;
import hudson.model.Node;
import hudson.model.labels.LabelAtom;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import jenkins.model.Jenkins;

/**
 * 
 * @author Lucie Votypkova
 */
@Extension
public class EnvironmentLabelsFinder extends LabelFinder {
   
    private Map<Node,String> cashedLabels = new ConcurrentHashMap<Node, String>();

    
    public void putLabels(Node node, String labelsInString){
        cashedLabels.put(node, labelsInString);
    }
    
    public Map<Node,String> getCashedLabels(){ 
        return cashedLabels;
    }

    @Override
    public Collection<LabelAtom> findLabels(Node node) {
        Computer computer = node.toComputer();
        if(computer == null || node.getChannel()==null)
            return Collections.EMPTY_LIST;
        if(node.getLabelString().isEmpty()){
            String labelsInString= cashedLabels.get(node);
            if(labelsInString==null)
                return Collections.EMPTY_LIST;
            Set<LabelAtom> labels = Label.parse(labelsInString);
            return labels;
        }
        return Collections.EMPTY_LIST;
        
    }
    
    public void updateComputers(){
        Set<Node> nodes = new HashSet<Node>();
        nodes.addAll(cashedLabels.keySet());
        for(Node node: nodes){
            if(!Jenkins.getInstance().getNodes().contains(node)){
                cashedLabels.remove(node);
            }
        }
    }
}
