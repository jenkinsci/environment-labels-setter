package org.jenkinsci.plugins.environment.labels;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import hudson.EnvVars;
import hudson.model.LabelFinder;
import hudson.model.Computer;
import hudson.model.Node;
import hudson.model.Slave;
import hudson.model.labels.LabelAtom;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jenkins.model.Jenkins;

import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * @author lucinka
 */
public class EnvironmentLabelsFinderTest {

    @Rule public JenkinsRule j = new JenkinsRule();

    @Test
    public void contributeAllLabels() throws Exception {
       Slave slave = slaveContributing("testlabel1 testlabel2");
       allowLabelContribution(slave);

       assertEquals(labels("slave0", "testlabel1", "testlabel2"), slave.getAssignedLabels());
    }

    @Test
    public void appendLabels() throws Exception {
       Slave slave = slaveContributing("env_label common_label");
       slave.setLabelString("hardcoded_label common_label");
       allowLabelContribution(slave);

       assertEquals(labels("slave0", "env_label", "common_label", "hardcoded_label"), slave.getAssignedLabels());
    }

    @Test
    public void ignoreLabelsForNodesNotConfiguredToAccept() throws Exception {
        Slave slave = slaveContributing("env_label common_label");

        assertEquals(labels("slave0"), slave.getAssignedLabels());

        slave = slaveContributing("env_label common_label");
        slave.setLabelString("hardcoded_label common_label");

        assertEquals(labels("slave1", "hardcoded_label", "common_label"), slave.getAssignedLabels());
    }

    @Test
    public void doNotTouchLabelsWhenNothingIsContributed() throws Exception {
        Slave slave = j.createOnlineSlave();
        slave.setLabelString("hardcoded_label");
        allowLabelContribution(slave);

        assertEquals(labels("slave0", "hardcoded_label"), slave.getAssignedLabels());
    }

    @Test
    public void notContributingSlavesDoesNotNeedCacheEntry() throws Exception {
        Slave noEnvvarSlave = j.createOnlineSlave();
        noEnvvarSlave.setLabelString("hardcoded_label");
        allowLabelContribution(noEnvvarSlave);

        assertEquals(null, getCachedLabels().get(noEnvvarSlave));
    }

    @Test
    public void testRemoveComputer() throws Exception{
        Map<Node, String> cashedLabels = getCachedLabels();
        Slave slave1 = slaveContributing("a");
        allowLabelContribution(slave1);
        Slave slave2 = slaveContributing("b");
        allowLabelContribution(slave2);

        assertTrue("All slaves should be cashed.", cashedLabels.containsKey(slave1) && cashedLabels.containsKey(slave2));
        Jenkins.getInstance().removeNode(slave2);
        cashedLabels = getCachedLabels();
        assertFalse("Cashed Labels should not contains deleted slave.", cashedLabels.containsKey(slave2));
        assertTrue("Cashed Labels should contains " + slave1.getDisplayName() + ".", cashedLabels.containsKey(slave1));
    }

    @Test
    public void removeLabelsBetweenReconnects() throws Exception {
        EnvVars vars = new EnvVars("JENKINS_SLAVE_LABELS", "a_label");
        Node slave = j.createOnlineSlave(null, vars);
        Computer computer = slave.toComputer();
        allowLabelContribution(slave);

        assertEquals("a_label", slave.toComputer().getEnvironment().get("JENKINS_SLAVE_LABELS"));
        assertEquals(labels("slave0", "a_label"), slave.getAssignedLabels());

        vars.remove("JENKINS_SLAVE_LABELS");

        reconnect(computer);

        assertEquals(null, computer.getEnvironment().get("JENKINS_SLAVE_LABELS"));
        assertEquals(labels("slave0"), slave.getAssignedLabels());
    }

    @Test
    public void addLabelsBetweenReconnects() throws Exception {
        EnvVars vars = new EnvVars();
        Node slave = j.createOnlineSlave(null, vars);
        Computer computer = slave.toComputer();
        allowLabelContribution(slave);

        assertEquals(null, slave.toComputer().getEnvironment().get("JENKINS_SLAVE_LABELS"));
        assertEquals(labels("slave0"), slave.getAssignedLabels());

        vars.put("JENKINS_SLAVE_LABELS", "a_label");

        reconnect(computer);

        assertEquals("a_label", computer.getEnvironment().get("JENKINS_SLAVE_LABELS"));
        assertEquals(labels("slave0", "a_label"), slave.getAssignedLabels());
    }

    @Test
    public void changeLabelsBetweenReconnects() throws Exception {
        EnvVars vars = new EnvVars("JENKINS_SLAVE_LABELS", "persistent volatile");
        Node slave = j.createOnlineSlave(null, vars);
        Computer computer = slave.toComputer();
        allowLabelContribution(slave);

        assertEquals("persistent volatile", slave.toComputer().getEnvironment().get("JENKINS_SLAVE_LABELS"));
        assertEquals(labels("slave0", "persistent", "volatile"), slave.getAssignedLabels());

        vars.put("JENKINS_SLAVE_LABELS", "persistent replacement");

        reconnect(computer);

        assertEquals("persistent replacement", computer.getEnvironment().get("JENKINS_SLAVE_LABELS"));
        assertEquals(labels("slave0", "persistent", "replacement"), slave.getAssignedLabels());
    }

    private void reconnect(Computer computer) throws InterruptedException {
        computer.disconnect(null);
        computer.waitUntilOffline();
        computer.connect(true);
        computer.waitUntilOnline();

        Thread.sleep(100); // It seems to need some time to propagate
    }

    private Slave slaveContributing(String labels) throws Exception {
        return j.createOnlineSlave(null, new EnvVars("JENKINS_SLAVE_LABELS", labels));
    }

    private void allowLabelContribution(Node slave) throws IOException {
        slave.getNodeProperties().add(new PerNodeConfig());
    }

    private Map<Node, String> getCachedLabels() {
        return LabelFinder.all().get(EnvironmentLabelsFinder.class).getCashedLabels();
    }

    private Set<LabelAtom> labels(String... atoms) {
        HashSet<LabelAtom> ret = new HashSet<LabelAtom>(atoms.length);
        for (String atom: atoms) {
            ret.add(j.jenkins.getLabelAtom(atom));
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
