/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.citation;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * CitationService renders citations. The default implementation uses the Citation
 * Style Language and a Citation Processor. You can set styles and render citations
 * for all styles set or request a citation for a specific style. All citations are
 * returned as html.
 *
 * @author Vasilii Fedorov at the-library-code.de
 */
public interface CitationService {
    
    /**
     * Render citations for all styles configured for one specific item.
     * @param context DSpace's context
     * @param item The item to render the citations for
     * @return A map mapping styles on rendered citations in html.
     * @throws IOException
     */
    public Map<String, String> getCitations(Context context, Item item) throws IOException;
    
    /**
     * Render a citation for one item in a specific style. See
     * <a href="https://github.com/citation-style-language/styles">https://github.com/citation-style-language/styles</a>
     * for a list of all supported styles.
     *
     * @param context
     * @param item
     * @param style
     * @return
     * @throws IOException
     */
    public String getCitation(Context context, Item item, String style) throws IOException;
    
    /**
     * Get the list of CSL styles used by default.
     * @return
     */
    public List<String> getStyles();
    
    /**
     * Set the styles of the Citation Style Language to use by default. See
     *      * <a href="https://github.com/citation-style-language/styles">https://github.com/citation-style-language/styles</a>
     *      * for a list of all supported styles.
     * @param cslStyles
     */
    public void setStyles(List<String> cslStyles);

}
