/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.citation;

import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Abstract factory to get services for the citation package, use CitationServiceFactory.getInstance() to retrieve an implementation
 *
 * @author Vasilii Fedorov at the-library-code.de
 */
public abstract class CitationServiceFactory {

    public abstract CitationService getCitationService();

    public static CitationServiceFactory getInstance() {
        return DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("citationServiceFactory",
                CitationServiceFactory.class);
    }
}