/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Simple model for a list of citations
 *
 * @author Kim Shepherd
 */
public class CitationListRest implements RestModel {

    // Set names used in component wiring
    public static final String NAME = "citations";
    public static final String PLURAL_NAME = "citations";

    private List<CitationRest> citations;

    public CitationListRest() {
        this.citations = new ArrayList<>();
    }
    public CitationListRest(List<CitationRest> citations) {
        this.citations = citations;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getType() {
        return PLURAL_NAME;
    }

    public List<CitationRest> getCitations() {
        return citations;
    }

    public void setCitations(List<CitationRest> citations) {
        this.citations = citations;
    }
}
