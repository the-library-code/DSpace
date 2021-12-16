package org.dspace.app.rest.submit.step;

import javax.persistence.criteria.CriteriaBuilder;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.step.DataIdentifiers;
import org.dspace.app.rest.submit.AbstractProcessingStep;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.rest.submit.factory.PatchOperationFactory;
import org.dspace.app.rest.submit.factory.impl.PatchOperation;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.identifier.DOI;
import org.dspace.identifier.Handle;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.doi.DOIIdentifierNotApplicableException;
import org.dspace.identifier.factory.IdentifierServiceFactory;
import org.dspace.identifier.service.IdentifierService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.services.model.Request;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ShowMintIdentifierStep extends AbstractProcessingStep {

    private static final Logger log = LogManager.getLogger(ShowMintIdentifierStep.class);

    /**
     * Override DataProcessing.getData, return data identifiers from getIdentifierData()
     * @param submissionService
     *            the submission service
     * @param obj
     *            the in progress submission
     * @param config
     *            the submission step configuration
     * @return
     */
    @Override
    public DataIdentifiers getData(SubmissionService submissionService, InProgressSubmission obj,
                                   SubmissionStepConfig config) throws Exception {
        // We effectively do some 'pre-processing' here, though there should be no need to implement
        // ListenerProcessingStep, we'll just attempt a mint here first. It'll either succeed, fail, or skip
        // due to filter, or skip due to already having a DOI.
        try {
            reserveIdentifier(getContext(), obj);
        } catch (IdentifierException e) {
            // Something really went wrong! Log message and rethrow
            log.error("Identifier exception encountered when reserving item: " + e.getLocalizedMessage());
            throw new Exception(e);
        }

        return getIdentifierData(obj);
    }

    /**
     * Get data about existing identifiers for this in-progress submission item - this method doesn't require
     * submissionService or step config, so can be more easily called from doPatchProcessing as well
     * @param obj
     * @return
     */
    private DataIdentifiers getIdentifierData(InProgressSubmission obj) {
        log.debug("getIdentifierData() called");
        Context context = getContext();
        DataIdentifiers result = new DataIdentifiers();
        // Load identifier service
        IdentifierService identifierService =
                IdentifierServiceFactory.getInstance().getIdentifierService();
        // Attempt to look up handle and DOI identifiers for this item
        String handle = identifierService.lookup(context, obj.getItem(), Handle.class);
        String doi = identifierService.lookup(context, obj.getItem(), DOI.class);

        // Look up all identifiers and if they're not the DOI or handle, add them to the 'other' list
        List<String> otherIdentifiers = new ArrayList<>();
        for (String identifier : identifierService.lookup(context, obj.getItem())) {
            if (!StringUtils.equals(doi, identifier) && !StringUtils.equals(handle, identifier)) {
                otherIdentifiers.add(identifier);
            }
        }

        // If we got a DOI, format it to its external form
        if (StringUtils.isNotEmpty(doi)) {
            try {
                doi = IdentifierServiceFactory.getInstance().getDOIService().DOIToExternalForm(doi);
            } catch (IdentifierException e) {
                log.error("Error formatting DOI: " + doi);
            }
        }

        result.setDoi(doi);
        result.setHandle(handle);
        result.setOtherIdentifiers(otherIdentifiers);

        log.debug(result);

        return result;
    }

    /**
     * TODO: THIS IS CURRENTLY NOT USED - ONLY NEEDED IF WE HAVE A BUTTON TO CLICK FOR FILTER OVERRIDE
     */
    @Override
    public void doPatchProcessing(Context context, HttpServletRequest currentRequest,
                                  InProgressSubmission source, Operation op,
                                  SubmissionStepConfig stepConf) throws Exception {
        DataIdentifiers dataIdentifiers = getIdentifierData(source);
        // If the REST path ends with /mintidentifier, perform the patch operation
        if (op.getPath().endsWith(SHOW_MINT_IDENTIFIER_ENTRY)) {
            if (StringUtils.isEmpty(dataIdentifiers.getDoi())) {
                // No existing DOI, we can proceed
                PatchOperation<String> patchOperation = new PatchOperationFactory()
                        .instanceOf(SHOW_MINT_IDENTIFIER_ENTRY, op.getOp());
                patchOperation.perform(context, currentRequest, source, op);
            } else {
                // We already have a DOI! Log error, ignore request
                log.error("DOI requested but this item already has one: " + dataIdentifiers.getDoi());
            }
        }
    }

    private void reserveIdentifier(Context context, InProgressSubmission obj) throws Exception {
        Item item = obj.getItem();
        log.debug("reserveIdentifier called for item " + obj.getItem().getID());
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
        }
    }

    private Context getContext() {
        Context context = null;
        Request currentRequest = DSpaceServicesFactory.getInstance().getRequestService().getCurrentRequest();
        if (currentRequest != null) {
            HttpServletRequest request = currentRequest.getHttpServletRequest();
            context = ContextUtil.obtainContext(request);
        } else {
            context = new Context();
        }

        return context;
    }
}
