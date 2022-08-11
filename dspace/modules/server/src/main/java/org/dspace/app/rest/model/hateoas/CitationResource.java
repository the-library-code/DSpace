/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.dspace.app.rest.model.CitationRest;
import org.dspace.app.rest.model.hateoas.annotations.RelNameDSpaceResource;

/**
 *
 * Wrap CitationRest model in a very simple HALResource class
 *
 * @author Kim Shepherd
 */
@RelNameDSpaceResource(CitationRest.NAME)
public class CitationResource extends HALResource<CitationRest> {
    @JsonUnwrapped
    private CitationRest data;

    public CitationResource(CitationRest citationRest) {
        super(citationRest);
    }

    public CitationRest getData() {
        return data;
    }
}
