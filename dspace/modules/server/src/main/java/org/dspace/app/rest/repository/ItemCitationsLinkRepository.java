/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.model.CitationListRest;
import org.dspace.app.rest.model.CitationRest;
import org.dspace.app.rest.model.ItemRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.Item;
import org.dspace.content.citation.Citation;
import org.dspace.content.citation.CitationService;
import org.dspace.content.citation.CitationServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository for retrieving a list of citations for a given item.
 * All configured formats are returned in a list.
 * See config/spring/api/citation-service.xml for citation configuration
 *
 * @author Kim Shepherd
 */
@Component(ItemRest.CATEGORY + "." + ItemRest.NAME + "." + ItemRest.CITATIONS)
public class ItemCitationsLinkRepository extends AbstractDSpaceRestRepository implements LinkRestRepository {
    @Autowired
    ItemService itemService;

    CitationService citationService = CitationServiceFactory.getInstance().getCitationService();

    /**
     * Get citations for all enabled citation styles (see
     * @param request
     * @param itemId
     * @param optionalPageable
     * @param projection
     * @return
     */
    @PreAuthorize("hasPermission(#itemId, 'ITEM', 'READ')")
    public CitationListRest getCitations(@Nullable HttpServletRequest request,
                                              UUID itemId,
                                              @Nullable Pageable optionalPageable,
                                              Projection projection) {
        try {
            Context context = obtainContext();
            // Get item
            Item item = itemService.find(context, itemId);
            if (item == null) {
                throw new ResourceNotFoundException("No such item: " + itemId);
            }
            // New list for citations
            List<CitationRest> citations = new ArrayList<>();

            // Get all configured styles
            for (String style : citationService.getStyles()) {
                String text = "";
                try {
                    // Generate the actual citation / bibliography
                    text = citationService.getCitation(context, item, style);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                // Create and populate a new REST model for the citation result and add to the array
                CitationRest citation = new CitationRest(Citation.CSL, style, text);
                if (!StringUtils.isEmpty(text)) {
                    citations.add(citation);
                }
            }

            // Return constructed list
            return new CitationListRest(citations);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
