package org.jenkinsci.plugins.environment.labels;

import hudson.Extension;
import hudson.Util;
import hudson.model.LabelFinder;
import hudson.model.TaskListener;
import hudson.model.Computer;
import hudson.model.Label;
import hudson.model.Node;
import hudson.model.labels.LabelAtom;
import hudson.slaves.ComputerListener;
import hudson.slaves.SlaveComputer;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import jenkins.model.Jenkins;

/**
 * @author Lucie Votypkova
 */
@Extension
public class EnvironmentLabelsFinder extends LabelFinder {

    /**
     * Label strings contributed from envvar in their raw form.
     */
    private final Map<Node, String> cashedLabels = new ConcurrentHashMap<Node, String>();

    // for testing only
    /*package*/ Map<Node,String> getCashedLabels(){
        return cashedLabels;
    }

    @Override
    public Collection<LabelAtom> findLabels(Node node) {
        Computer computer = node.toComputer();
        if(computer == null || node.getChannel()==null)
            return Collections.emptyList();

        // Do not contribute anything unless explicitly configured to do so
        PerNodeConfig config = node.getNodeProperties().get(PerNodeConfig.class);
        if (config == null) return Collections.emptyList();

        String labelsInString = cashedLabels.get(node);
        if(labelsInString==null) return Collections.emptyList();

        return Label.parse(labelsInString);
    }

    /**
     * @author Lucie Votypkova
     */
    @Extension
    public static class SlaveVariableLabelListener extends ComputerListener {

        @Override
        public void onOnline(Computer c, TaskListener taskListener) {
            if(c instanceof SlaveComputer){
                try {
                    SlaveComputer slaveComputer = (SlaveComputer) c;
                    String labels = Util.fixEmpty(slaveComputer.getEnvironment().get("JENKINS_SLAVE_LABELS"));
                    if (labels != null) {
                        finder().cashedLabels.put(c.getNode(), labels);
                    }
                } catch (IOException e) {
                    Logger.getLogger(getClass().getName()).log(Level.WARNING, "Unable to load slave environment", e);
                } catch (InterruptedException e) {
                    Logger.getLogger(getClass().getName()).log(Level.WARNING, "Interrupted loading slave environment", e);
                }
            }
        }

        @Override
        public void onConfigurationChange(){
            EnvironmentLabelsFinder finder = finder();

            Set<Node> cachedNodes = new HashSet<Node>(finder.cashedLabels.keySet());
            List<Node> realNodes = Jenkins.getInstance().getNodes();
            for(Node node: cachedNodes){
                if(!realNodes.contains(node)){
                    finder.cashedLabels.remove(node);
                }
            }
        }

        private EnvironmentLabelsFinder finder() {
            return LabelFinder.all().get(EnvironmentLabelsFinder.class);
        }
    }
}
