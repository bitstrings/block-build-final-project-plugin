package org.jenkinsci.plugins.blockbuildfinalproject;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import hudson.model.AbstractProject;
import hudson.model.AutoCompletionCandidates;
import hudson.model.Item;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;

/**
 * Unit tests AutoCompleteUtils.
 * 
 * These tests perform extensive testing and code coverage for class AutoCompleteUtils.
 * The following are mocked:
 *   - Jenkins.getInstance() returns the mocked private class variable jenkins.
 *   - JenkinsWrapper.getAbstractProjects() is mocked to return a list of mocked projects.
 *   
 * @author Chad Rosenquist
 *
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({JenkinsWrapper.class, AbstractProject.class, Jenkins.class})
public class AutoCompleteUtilsUnitTest {
    private Jenkins jenkins;
    
    @Before
    public void setUp() {
        PowerMockito.mockStatic(JenkinsWrapper.class);
        
        // Have Jenkins.getInstance() return our mocked up Jenkins.
        PowerMockito.mockStatic(Jenkins.class);
        jenkins = PowerMockito.mock(Jenkins.class);
        PowerMockito.when(Jenkins.getInstance()).thenReturn(jenkins);
    }

    /**
     * If getAbstractProjects() returns null, that an empty list is returned.
     */
    @Test
    public void testAutoCompleteProjects_Null() {
        // Given
        PowerMockito.when(JenkinsWrapper.getAbstractProjects()).thenReturn(null);
        
        // When
        AutoCompletionCandidates candidates = AutoCompleteUtils.autoCompleteProjects("project-a");
        
        // Then
        assertEquals(0, candidates.getValues().size());
    }
    
    /**
     * Tests autoCompleteProjects with a partial match.
     * 
     * Note: Must use PowerMockito with AbstractProject because getFullName() is final.
     */
    @Test
    public void testAutoCompleteProjects_MatchTwoProjects() {
        // Given
        @SuppressWarnings("rawtypes")
        List<AbstractProject> projects = createMockedProjectList();
        PowerMockito.when(JenkinsWrapper.getAbstractProjects()).thenReturn(projects);
        
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
     * No projects are matched.
     */
    @Test
    public void testAutoCompleteProjects_MatchNoProjects() {
        // Given
        @SuppressWarnings("rawtypes")
        List<AbstractProject> projects = createMockedProjectList();
        PowerMockito.when(JenkinsWrapper.getAbstractProjects()).thenReturn(projects);
        
        // When
        AutoCompletionCandidates candidates = AutoCompleteUtils.autoCompleteProjects("hello");
        
        // Then
        List<String> values = candidates.getValues();
        assertEquals(0, values.size());   
    }
 
    /**
     * All projects are matched.
     */
    @Test
    public void testAutoCompleteProjects_MatchAllProjects() {
        // Given
        @SuppressWarnings("rawtypes")
        List<AbstractProject> projects = createMockedProjectList();
        PowerMockito.when(JenkinsWrapper.getAbstractProjects()).thenReturn(projects);
        
        // When
        AutoCompletionCandidates candidates = AutoCompleteUtils.autoCompleteProjects("");
        
        // Then
        List<String> values = candidates.getValues();
        assertEquals(3, values.size());
        assertTrue(values.contains("cool-build"));
        assertTrue(values.contains("cool-test"));
        assertTrue(values.contains("other-project"));
    }
    
    /**
     * Creates a mocked project list
     * @return mocked list
     * 
     * cool-build
     * cool-test
     * other-project
     */
    @SuppressWarnings("rawtypes")
    private List<AbstractProject> createMockedProjectList() {
        List<AbstractProject> projects = new ArrayList<AbstractProject>();
        
        AbstractProject<?, ?> projectCoolBuild = createMockedProject("cool-build");
        AbstractProject<?, ?> projectCoolTest = createMockedProject("cool-test");
        AbstractProject<?, ?> projectOtherProject = createMockedProject("other-project");

        projects.add(projectCoolBuild);
        projects.add(projectCoolTest);
        projects.add(projectOtherProject);
        
        return projects;
    }
    
    /**
     * Creates a simple mocked project
     * 
     * @param fullName  name of the project
     * @return          mocked project
     * 
     * Creates a mocked AbstractProject.  When getFullName() is called, the name is returned.
     */
    private AbstractProject<?, ?> createMockedProject(String fullName) {
        AbstractProject<?, ?> project = PowerMockito.mock(AbstractProject.class);
        PowerMockito.when(project.getFullName()).thenReturn(fullName);
        return project;
    }
    
    /**
     * Tests checkProjects returning OK.
     * 
     * The current project is cool-build.  The end-user is trying to match cool-test.
     */
    @Test
    public void testCheckProjects_OK() {
        // Given
        // Create the project we are on.
        AbstractProject<?, ?> currentProject = createMockedProject("cool-build");
        
        // Create the project we are searching for.
        AbstractProject<?, ?> matchedProject = createMockedProject("cool-test");
        
        // Call to getItem() returns the mocked project we are trying to match.
        PowerMockito.when(jenkins.getItem("cool-test", currentProject, Item.class)).thenReturn(matchedProject);
        
        // When
        FormValidation validation = AutoCompleteUtils.checkProjects("cool-test", currentProject);
        
        // Then
        assertEquals("OK: <div/>", validation.toString());
    }
    
    /**
     * Test checkProjects returning OK when multiple, comma delimited projects are given.
     */
    @Test
    public void testCheckProjects_MultipleOK() {
        // Given
        // Create the project we are on.
        AbstractProject<?, ?> currentProject = createMockedProject("current-project");
        
        // Create projects we are searching for.
        AbstractProject<?, ?> searchOne = createMockedProject("search-one");
        AbstractProject<?, ?> searchTwo = createMockedProject("search-two");
        AbstractProject<?, ?> searchThree = createMockedProject("search-three");
        
        // Call to getItem() returns the mocked project we are trying to match.
        PowerMockito.when(jenkins.getItem("search-one", currentProject, Item.class)).thenReturn(searchOne);
        PowerMockito.when(jenkins.getItem("search-two", currentProject, Item.class)).thenReturn(searchTwo);
        PowerMockito.when(jenkins.getItem("search-three", currentProject, Item.class)).thenReturn(searchThree);
        
        // When
        FormValidation validation = AutoCompleteUtils.checkProjects("  search-one  , search-two, search-three,", currentProject);
        
        // Then
        assertEquals("OK: <div/>", validation.toString());
    }
    
    /**
     * Tests checkProjects returning invalid project.
     */
    @Test
    public void testCheckProjects_InvalidProject() {
        // Given
        // Create the project we are on.
        AbstractProject<?, ?> currentProject = createMockedProject("cool-build");
        
        // Create the project we are searching for.
        AbstractProject<?, ?> matchedProject = createMockedProject("cool-test");
        
        // Call to getItem() returns the mocked project we are trying to match.
        PowerMockito.when(jenkins.getItem("cool-test", currentProject, Item.class)).thenReturn(matchedProject);
        
        // When
        FormValidation validation = AutoCompleteUtils.checkProjects("invalid-project", currentProject);
        
        // Then
        assertEquals("ERROR: Invalid project: invalid-project | invalid-project", validation.toString());
    }
    
    /**
     * Tests checkProjects when a project is not buildable.
     */
    @Test
    public void testCheckProjects_NotBuildable() {
        // Given
        // Create the project we are on.
        AbstractProject<?, ?> currentProject = createMockedProject("cool-build");
        
        // Create the project we are searching for.
        // Item is not buildable.
        Item notBuildable =  Mockito.mock(Item.class);
        
        // Call to getItem() returns the mocked project we are trying to match.
        PowerMockito.when(jenkins.getItem("cool-test", currentProject, Item.class)).thenReturn(notBuildable);
        
        // When
        FormValidation validation = AutoCompleteUtils.checkProjects("cool-test", currentProject);
        
        // Then
        assertEquals("ERROR: Project is not buildable: cool-test", validation.toString());
    }    
     

}
