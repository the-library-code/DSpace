/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.CitationRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.citation.Citation;
import org.springframework.stereotype.Component;

/**
 * Convert a citation object to a REST resource
 */
@Component
public class CitationConverter implements DSpaceConverter<Citation, CitationRest> {

    @Override
    public CitationRest convert(Citation modelObject, Projection projection) {
        CitationRest rest = new CitationRest();
        rest.setCitationType(modelObject.getType());
        rest.setStyle(modelObject.getStyle());
        rest.setText(modelObject.getText());
        return rest;
    }

    @Override
    public Class<Citation> getModelClass() {
        return Citation.class;
    }

}
