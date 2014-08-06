package org.jenkinsci.plugins.environment.labels;

import hudson.EnvVars;
import hudson.model.LabelFinder;
import hudson.model.Node;
import hudson.model.Slave;
import hudson.model.labels.LabelAtom;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jenkins.model.Jenkins;

import org.junit.Test;
import org.jvnet.hudson.test.HudsonTestCase;

/**
 * @author lucinka
 */
public class EnvironmentLabelsFinderTest extends HudsonTestCase{

    @Test
    public void testOnOnline() throws IOException, InterruptedException, Exception{
       Slave contributingSlave = createOnlineSlave(null, new EnvVars(
               "JENKINS_SLAVE_LABELS", "testlabel1 testlabel2"
       ));

       assertEquals(labels("slave0", "testlabel1", "testlabel2"), contributingSlave.getAssignedLabels());

       Slave notContributingSlave = createOnlineSlave();

       assertEquals(labels("slave1"), notContributingSlave.getAssignedLabels());
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

    private Set<LabelAtom> labels(String... atoms) {
        HashSet<LabelAtom> ret = new HashSet<LabelAtom>(atoms.length);
        for (String atom: atoms) {
            ret.add(jenkins.getLabelAtom(atom));
        }

        return ret;
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
