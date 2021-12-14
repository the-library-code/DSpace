/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.doi.DOIIdentifierNotApplicableException;
import org.dspace.identifier.factory.IdentifierServiceFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Patch operation to perform actual DOI (or other Identifier) reservation
 */
public class MintIdentifierPatchOperation extends PatchOperation<String> {

    private static final Logger log = LogManager.getLogger(MintIdentifierPatchOperation.class);

    @Autowired
    ItemService itemService;

    @Override
    protected Class<String[]> getArrayClassForEvaluation() {
        return String[].class;
    }

    @Override
    protected Class<String> getClassForEvaluation() {
        return String.class;
    }

    @Override
    public void perform(Context context, HttpServletRequest currentRequest,
                        InProgressSubmission source, Operation operation) throws Exception {
        Item item = source.getItem();
        if (item == null) {
            log.error("Null item passed to reserve identifier action");
        }
        try {
            // Try an ordinary reservation, obeying the filter if configured
            IdentifierServiceFactory.getInstance().getIdentifierService().reserve(context, item);
        } catch (DOIIdentifierNotApplicableException e) {
            // This is a non-fatal error - it just means the filter has been applied and this item should
            // not receive a DOI
            assert item != null;
            log.info("DOI reservation skipped (filtered) for item: " + item.getID());
        } catch (IdentifierException e) {
            // Something really went wrong! Log message and rethrow
            log.error("Identifier exception encountered when reserving item");
            throw new Exception(e);
        }
    }
}
