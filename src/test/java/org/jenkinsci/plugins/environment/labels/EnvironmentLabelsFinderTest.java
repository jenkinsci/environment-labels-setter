package org.jenkinsci.plugins.environment.labels;

import hudson.EnvVars;
import hudson.model.LabelFinder;
import hudson.model.TaskListener;
import hudson.model.Node;
import hudson.model.Slave;
import hudson.model.labels.LabelAtom;
import hudson.slaves.ComputerListener;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import jenkins.model.Jenkins;

import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;

/**
 * @author lucinka
 */
public class EnvironmentLabelsFinderTest extends HudsonTestCase{

    @Test
    public void testOnOnline() throws IOException, InterruptedException, Exception{
       EnvVars vars = new EnvVars();
       vars.put("JENKINS_SLAVE_LABELS", "testlabe11 testlabel2");
       Slave slave = createOnlineSlave(null, vars);
       ComputerListener.all().get(SlaveVariableLabelListener.class).onOnline(slave.toComputer(), TaskListener.NULL);
       Collection<LabelAtom> labels = LabelFinder.all().get(EnvironmentLabelsFinder.class).findLabels(slave);
       assertTrue("Computer should contains label testlabel1.", labels.contains(Jenkins.getInstance().getLabelAtom("testlabe11")));
       assertTrue("Computer should contains label testlabel2.", labels.contains(Jenkins.getInstance().getLabelAtom("testlabel2")));
       Slave slave2 = createOnlineSlave();
       System.out.println(LabelFinder.all().get(EnvironmentLabelsFinder.class).getCashedLabels());
       labels = LabelFinder.all().get(EnvironmentLabelsFinder.class).findLabels(slave2);
       assertFalse("Computer should not contains label testlabel1.", labels.contains(Jenkins.getInstance().getLabelAtom("testlabel1")));
       assertFalse("Computer should not contains label testlabel2.", labels.contains(Jenkins.getInstance().getLabelAtom("testlabel2")));

    }

    @Test
    public void testRemoveComputer() throws Exception{
        Map<Node, String> cashedLabels = LabelFinder.all().get(EnvironmentLabelsFinder.class).getCashedLabels();
        Slave slave1 = createOnlineSlave();
        Slave slave2 = createOnlineSlave();
        assertTrue("All slaves should be cashed.", cashedLabels.containsKey(slave1) && cashedLabels.containsKey(slave2));
        Jenkins.getInstance().removeNode(slave2);
        cashedLabels = LabelFinder.all().get(EnvironmentLabelsFinder.class).getCashedLabels();
        assertFalse("Cashed Labels should not contains deleted slave.", cashedLabels.containsKey(slave2));
        assertTrue("Cashed Labels should contains " + slave1.getDisplayName() + ".", cashedLabels.containsKey(slave1));
    }

//    @Test
//    public void testRenameComputer() throws Exception{
//        EnvVars vars = new EnvVars();
//        vars.put("JENKINS_SLAVE_LABELS", "testlabe1");
//        Slave slave1 = createOnlineSlave(null, vars);
//        slave1.getNodeProperties().add(new EnvironmentVariablesNodeProperty(new Entry("JENKINS_SLAVE_LABELS", "testlabel")));
//        HtmlForm form = createWebClient().goTo("computer/" + slave1.getDisplayName() + "/configure").getFormByName("config");
//        form.getInputByName("_.name").setValueAttribute("renamed");
//        submit(form);
//        assertTrue("Renaming should not change environmnet labels.", LabelFinder.all().get(EnvironmentLabelsFinder.class).findLabels(Jenkins.getInstance().getNode("renamed")).contains(Jenkins.getInstance().getLabelAtom("testlabe1")));
//    }

}
