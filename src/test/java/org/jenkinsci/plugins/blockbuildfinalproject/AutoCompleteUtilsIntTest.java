package org.jenkinsci.plugins.blockbuildfinalproject;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import hudson.model.AutoCompletionCandidates;
import hudson.model.FreeStyleProject;
import hudson.util.FormValidation;

/**
 * Integration tests tests AutoCompleteUtils.
 * 
 * Tests AutoCompleteUtils using the real Jenkins.
 * Only a few tests are included here.  See AutoCompleteUtilsUnitTest
 * for complete code coverage.
 * 
 * @author Chad Rosenquist
 *
 */
public class AutoCompleteUtilsIntTest {
    
    @Rule
    public JenkinsRule jenkinsRule = new JenkinsRule();

    @Before
    public void setUp() throws Exception {
    }

    /**
     * Tests autoCompleteProjects with a partial match.
     * @throws Exception 
     */
    @Test
    public void testAutoCompleteProjects_MatchTwoProjects() throws Exception {
        // Given
        jenkinsRule.createFreeStyleProject("cool-build");
        jenkinsRule.createFreeStyleProject("cool-test");
        jenkinsRule.createFreeStyleProject("other-project");
        
        // When
        AutoCompletionCandidates candidates = AutoCompleteUtils.autoCompleteProjects("cool");
        
        // Then
        List<String> values = candidates.getValues();
        assertEquals(2, values.size());
        assertTrue(values.contains("cool-build"));
        assertTrue(values.contains("cool-test"));
        assertFalse(values.contains("other-project"));
    }
    
    /**
     * Tests checkProjects returning OK.
     * @throws Exception
     */
    @Test
    public void testCheckProjects_OK() throws Exception {
        // Given
        FreeStyleProject currentProject = jenkinsRule.createFreeStyleProject("current-project");
        jenkinsRule.createFreeStyleProject("search-project");
        
        // When
        FormValidation validation = AutoCompleteUtils.checkProjects("search-project", currentProject);
        
        // Then
        assertEquals("OK: <div/>", validation.toString());
    }

}
