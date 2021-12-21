/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.*;
import org.dspace.content.Collection;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.eperson.EPerson;
import org.dspace.kernel.ServiceManager;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.workflow.WorkflowItem;
import org.dspace.xmlworkflow.storedcomponents.service.XmlWorkflowItemService;
import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.sql.SQLException;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test suite for testing the Show Identifiers submission stesp
 * 
 * @author Kim Shepherd
 *
 */
public class SubmissionShowIdentifiersRestIT extends AbstractControllerIntegrationTest {

    @Autowired
    private WorkspaceItemService workspaceItemService;
    @Autowired
    private XmlWorkflowItemService workflowItemService;

    private Collection collection;
    private EPerson submitter;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        ServiceManager serviceManager = DSpaceServicesFactory.getInstance().getServiceManager();

        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Root community").build();

        submitter = EPersonBuilder.createEPerson(context)
                                  .withEmail("submitter.em@test.com")
                                  .withPassword(password)
                                  .build();

        collection = CollectionBuilder.createCollection(context, parentCommunity)
                                      .withName("Collection")
                                      .withEntityType("Publication")
                                      .withSubmissionDefinition("publication")
                                      .withSubmitterGroup(submitter).build();

        context.restoreAuthSystemState();
    }

    @After
    public void after() throws SQLException, IOException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        workflowItemService.deleteByCollection(context, collection);
        workspaceItemService.findAll(context).forEach(this::deleteWorkspaceItem);
        context.restoreAuthSystemState();
    }

    private void deleteWorkspaceItem(WorkspaceItem workspaceItem) {
        try {
            workspaceItemService.deleteAll(context, workspaceItem);
        } catch (SQLException | AuthorizeException | IOException e) {
            throw new RuntimeException();
        }
    }


    @Test
    public void testItemHandleAndDoiReservation() throws Exception {
        // Test publication that should get Handle and DOI
        context.turnOffAuthorisationSystem();
        WorkspaceItem workspaceItem = createWorkspaceItem("Test publication", collection);
        context.restoreAuthSystemState();
        String submitterToken = getAuthToken(submitter.getEmail(), password);
        getClient(submitterToken).perform(get("/api/workspace/workspaceitems/" + workspaceItem.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sections['identifiers'].doi", matchesPattern("^.*doi\\.org.$")))
            .andExpect(jsonPath("$.sections['identifiers'].handle",
                    containsString(workspaceItem.getItem().getHandle())));
    }

    @Test
    public void testItemHandleAndNoDoiReservation() throws Exception {
        // Test publication that should get Handle but NOT a DOI
        context.turnOffAuthorisationSystem();
        WorkspaceItem workspaceItem = createWorkspaceItem("Test publication", collection);
        context.restoreAuthSystemState();
        String submitterToken = getAuthToken(submitter.getEmail(), password);
        getClient(submitterToken).perform(get("/api/workspace/workspaceitems/" + workspaceItem.getID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sections['identifiers'].doi", matchesPattern("^.*doi\\.org.$")))
                .andExpect(jsonPath("$.sections['identifiers'].handle",
                        containsString(workspaceItem.getItem().getHandle())));
    }

    private WorkspaceItem createWorkspaceItem(String title, Collection collection) {
        WorkspaceItem workspaceItem = WorkspaceItemBuilder.createWorkspaceItem(context, collection)
                .withTitle(title)
                .withSubmitter(submitter)
                .build();
        return workspaceItem;
    }

}