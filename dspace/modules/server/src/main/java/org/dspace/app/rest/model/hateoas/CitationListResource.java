/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.dspace.app.rest.model.CitationListRest;
import org.dspace.app.rest.model.CitationRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;

/**
 *
 * List of citations
 *
 * @author Kim Shepherd
 */
@RelNameDSpaceResource(CitationRest.PLURAL_NAME)
public class CitationListResource extends HALResource<CitationListRest> {
    @JsonUnwrapped
    private CitationListRest data;

    public CitationListResource(CitationListRest citationListRest) {
        super(citationListRest);
    }

    public CitationListRest getData() {
        return data;
    }
}
