/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.citation;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.undercouch.citeproc.CSL;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.csl.DSpaceListItemDataProvider;
import org.dspace.core.Context;
import org.dspace.services.factory.DSpaceServicesFactory;


/**
 * The main service class used to produce citations
 * @author Vasilii Fedorov at the-library-code.de
 */
public class CitationServiceImpl implements CitationService {

    /** log4j category */
    private static Logger log = LogManager.getLogger(org.dspace.identifier.IdentifierServiceImpl.class);

    private List<String> styles;

    protected CitationServiceImpl() {}

    @Override
    public Map<String, String> getCitations(Context context, Item item) throws IOException {

        Map<String, String> data = new HashMap<>();

        // Get citation service and set context
        // Without setting context it is not possible to add items to the provider
        DSpaceListItemDataProvider dSpaceListItemDataProvider =
                DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("DSpaceListItemDataProvider",
                        DSpaceListItemDataProvider.class);

        for (String style : styles) {
            CSL citeproc = new CSL(dSpaceListItemDataProvider, style);
            citeproc.setOutputFormat("html");
            citeproc.registerCitationItems(item.getID().toString());
            data.put(style, citeproc.makeBibliography().makeString());
        }

        // Remove items when finished
        // TODO: doesn't work the same as TLC, check more to decide how we do this
        dSpaceListItemDataProvider.resetItems();

        return data;
    }

    public String getCitation(Context context, Item item, String style) throws IOException {

        DSpaceListItemDataProvider dSpaceListItemDataProvider =
                DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName("DSpaceListItemDataProvider",
                        DSpaceListItemDataProvider.class);
        dSpaceListItemDataProvider.setContext(context);
        CSL citeproc = new CSL(dSpaceListItemDataProvider, style);
        citeproc.setOutputFormat("html");
        citeproc.registerCitationItems(item.getID().toString());
        return citeproc.makeBibliography().makeString();
    };

    public void setStyles(List<String> cslStyles) {
        this.styles = cslStyles;
    }

    public List<String> getStyles() {
        return styles;
    }
}
