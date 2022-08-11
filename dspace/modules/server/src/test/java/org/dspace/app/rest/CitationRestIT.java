/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.handle.factory.HandleServiceFactory;
import org.junit.Test;

/**
 * Integration test for Citation REST endpoint and services
 */
public class CitationRestIT extends AbstractControllerIntegrationTest {

    @Test
    public void testGetCitations() throws Exception {
        // Create a simple test item
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Item item = ItemBuilder.createItem(context, col1)
                .withTitle("Test Title")
                .withIssueDate("2017-10-17")
                .withAuthor("Smith, Donald")
                .build();
        context.restoreAuthSystemState();

        // Construct expected APA 6th Edition citation text for the test item
        String expectedUrl =
                HandleServiceFactory.getInstance().getHandleService().resolveToURL(context, item.getHandle());
        String expectedApa6CitationText =
                "<div class=\"csl-bib-body\">\n  <div class=\"csl-entry\">Smith, D. (2017). " +
                        "<span style=\"font-style: italic\">Test Title</span>. " +
                        "Retrieved from " + expectedUrl + "</div>\n</div>";

        // Get the formatted citations for this item and test that at least one of them is correct
        getClient().perform(get("/api/core/items/" + item.getID() + "/citations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.citations").isArray())
                .andExpect(jsonPath("$.citations[0].citationType", is("CSL")))
                .andExpect(jsonPath("$.citations[0].style", is("apa-6th-edition")))
                .andExpect(jsonPath("$.citations[0].text", is(expectedApa6CitationText)));

    }

}
