/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.citation;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Factory implementation to get services for the identifier package, use CitationServiceFactory.getInstance() to retrieve an implementation
 *
 * @author Vasilii Fedorov at the-library-code.de
 */
public class CitationServiceFactoryImpl extends CitationServiceFactory {

    @Autowired(required = true)
    private CitationService citationService;

    @Override
    public CitationService getCitationService() {
        return citationService;
    }

}