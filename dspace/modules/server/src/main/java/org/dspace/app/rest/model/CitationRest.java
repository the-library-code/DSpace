/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.dspace.content.citation.Citation;

/**
 * Simple REST model for a citation.
 *
 * @author Kim Shepherd
 */
public class CitationRest implements RestModel {

    // Set names used in component wiring
    public static final String NAME = "citation";
    public static final String PLURAL_NAME = "citations";

    // Set to name in final HAL response to help with downstream use
    private String type;

    // Citation type (eg CSL, BibTeX, RIS)
    private String citationType = Citation.CSL;
    // Citation style (eg. apa6, harvard, ieee)
    private String style;
    // Citation text rendered for display
    private String text;

    // Empty constructor
    public CitationRest() {}
    // Full constructor to match basic object
    public CitationRest(String type, String style, String text) {
        this.citationType = type;
        this.style = style;
        this.text = text;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCitationType() {
        return citationType;
    }

    public void setCitationType(String type) {
        this.type = type;
    }

    public String getStyle() {
        return style;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

}
