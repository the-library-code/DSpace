/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.sword2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.lang3.StringUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.IngestionCrosswalk;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.factory.CoreServiceFactory;
import org.jdom2.DocType;
import org.jdom2.JDOMException;
import org.swordapp.server.Deposit;
import org.swordapp.server.SwordAuthException;
import org.swordapp.server.SwordError;

/**
 * JATS ZIP content ingester.
 * The zip files are expected to be in a structure of 1 JATS XML descriptive metadata file, and 1 or more files
 * to be ingested as bitstreams.
 *
 * 1. Unzip the zip file and step through each file
 * 2. Sort XML files into one list, others into another
 * 3. For each XML, inspect XML header, if it is JATS then use that as the ingest crosswalk input (one only!)
 *    i. Move all others to the 'regular files' list (could be data xml)
 *    ii. TODO: Should we detect multiple / zero JATS files and throw error? Ie. do we validate first?
 * 4. Instantiate a new item and crosswalk JATS to populate metadata
 * 5. Create new ORIGINAL bundle and add all remaining files to it
 * 6. Set other attributes accordingly (as per any extra specs - owner? etc.)
 */
public class JATSZipContentIngester extends AbstractSwordContentIngester {
    protected BundleService bundleService = ContentServiceFactory.getInstance()
                                                                 .getBundleService();

    protected BitstreamService bitstreamService = ContentServiceFactory
        .getInstance().getBitstreamService();

    protected WorkspaceItemService workspaceItemService = ContentServiceFactory
        .getInstance().getWorkspaceItemService();

    private List<Bitstream> crosswalkAndIngestZip(Context context, Deposit deposit, Item item) throws SQLException, AuthorizeException, IOException, JDOMException, CrosswalkException {
        // Get JATS crosswalk
        IngestionCrosswalk xwalk = (IngestionCrosswalk) getCrosswalk("JATS", IngestionCrosswalk.class);

        // get deposited file as file object
        File depositFile = deposit.getFile();

        // get the original bundle
        List<Bundle> bundles = item.getBundles();
        Bundle original = null;
        for (Bundle bundle : bundles) {
            if (Constants.CONTENT_BUNDLE_NAME.equals(bundle.getName())) {
                original = bundle;
                break;
            }
        }
        if (original == null) {
            original = bundleService
                    .create(context, item, Constants.CONTENT_BUNDLE_NAME);
        }

        ZipFile zipFile = new ZipFile(depositFile);
        Enumeration zipEntries = zipFile.entries();
        List<ZipEntry> filesToIngest = new ArrayList<>();
        boolean metadataCrosswalked = false;
        while (zipEntries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) zipEntries.nextElement();
            InputStream stream = zipFile.getInputStream(entry);
            // Log entry name
            log.info("Processing file: " + entry.getName());
            // Is this an XML file?
            if (StringUtils.endsWith(entry.getName(), ".xml")) {
                // Read stream to new jdom2 XML root element
                org.jdom2.input.SAXBuilder builder = new org.jdom2.input.SAXBuilder();
                // We do not want to load external DTDs as the JATS does refer to random custom DTDs sometimes,
                // e.g. "JATS-archive-oasis-article1-mathml3.dtd"
                builder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
                org.jdom2.Document doc = builder.build(stream);
                org.jdom2.Element root = doc.getRootElement();
                // Log all elements of doc.getDocType for our inspection
                DocType docType = doc.getDocType();
                log.info("XML DocType public id: " + docType.getPublicID());
                // Inspect parsed XML for a doctype header matching "//NLM//DTD JATS (Z39.96)" (JATS)
                if (doc.getDocType() != null &&
                        doc.getDocType().getPublicID().contains("//NLM//DTD JATS (Z39.96)")) {
                    // This is JATS, proceed with ingest then break from loop and continue with bitstreams
                    xwalk.ingest(context, item, root, true);
                    metadataCrosswalked = true;
                } else {
                    // XML, but not JATS. So, we'll add it as a file to ingest instead
                    filesToIngest.add(entry);
                }
            } else {
                // Not XML, we should add it as a bitstream after we're done with metadata
                filesToIngest.add(entry);
            }
        }

        if (!metadataCrosswalked) {
            // Something went wrong - we didn't detect or successfully crosswalk a JATS file.
            throw new CrosswalkException("No JATS metadata detected");
        }

        // Derived resources to be added to SWORD result object
        List<Bitstream> derivedResources = new ArrayList<>();

        // Proceed with bitstream ingest
        for (ZipEntry entry : filesToIngest) {
            InputStream stream = zipFile.getInputStream(entry);
            Bitstream bs = bitstreamService.create(context, original, stream);
            BitstreamFormat format = this.getFormat(context, entry.getName());
            bs.setFormat(context, format);
            bs.setName(context, entry.getName());
            bitstreamService.update(context, bs);
            derivedResources.add(bs);
        }

        itemService
                .addMetadata(context, item, "dc", "description", null, null,
                        "Zip file deposted by SWORD with JATS metadata");

        // Iterate metadata and remove blank or duplicate values
        List<MetadataValue> metadata = itemService.getMetadata(item, Item.ANY, Item.ANY, Item.ANY, Item.ANY);
        Set<String> uniqueValues = new HashSet<>();
        List<MetadataValue> duplicatesOrEmptyValues = new ArrayList<>();
        for (MetadataValue md : metadata) {
            String value = md.getValue();
            if (value == null || value.trim().isEmpty() || uniqueValues.contains(getSignature(md))) {
                duplicatesOrEmptyValues.add(md);
            } else {
                uniqueValues.add(getSignature(md));
            }
        }
        itemService.removeMetadataValues(context, item, duplicatesOrEmptyValues);

        return derivedResources;

    }

    public DepositResult ingestToCollection(Context context, Deposit deposit,
                                            Collection collection, VerboseDescription verboseDescription,
                                            DepositResult result)
        throws DSpaceSwordException, SwordError, SwordAuthException {
        try {


            // get deposited file as file object
            File depositFile = deposit.getFile();

            // decide whether we have a new item or an existing one
            Item item = null;
            WorkspaceItem wsi = null;
            if (result != null) {
                item = result.getItem();
            } else {
                result = new DepositResult();
            }
            if (item == null) {
                // simple zip ingester uses the item template, since there is no native metadata
                wsi = workspaceItemService.create(context, collection, true);
                item = wsi.getItem();
            }

            List<Bitstream> derivedResources = crosswalkAndIngestZip(context, deposit, item);

            // update the item metadata to inclue the current time as
            // the updated date
            this.setUpdatedDate(context, item, verboseDescription);

            // DSpace ignores the slug value as suggested identifier, but
            // it does store it in the metadata
            this.setSlug(context, item, deposit.getSlug(), verboseDescription);

            // in order to write these changes, we need to bypass the
            // authorisation briefly, because although the user may be
            // able to add stuff to the repository, they may not have
            // WRITE permissions on the archive.
            context.turnOffAuthorisationSystem();
            itemService.update(context, item);
            context.restoreAuthSystemState();

            verboseDescription.append("Ingest successful");
            verboseDescription
                    .append("Item created with internal identifier: " +
                            item.getID());

            result.setItem(item);
            result.setTreatment(this.getTreatment());
            result.setDerivedResources(derivedResources);

            return result;

        } catch (AuthorizeException e) {
            throw new SwordAuthException(e);
        } catch (SQLException e) {
            throw new DSpaceSwordException(e);
        } catch (ZipException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JDOMException e) {
            throw new RuntimeException(e);
        } catch (CrosswalkException e) {
            throw new RuntimeException(e);
        }
    }

    private Enumeration<? extends ZipEntry> unzip(File depositFile) {
        try {
            return new ZipFile(depositFile).entries();
        } catch (IOException e) {
            return Collections.emptyEnumeration();
        }
    }

    public DepositResult ingestToItem(Context context, Deposit deposit,
                                      Item item, VerboseDescription verboseDescription,
                                      DepositResult result)
        throws DSpaceSwordException, SwordError, SwordAuthException {
        try {
            // Get JATS crosswalk
            IngestionCrosswalk xwalk = (IngestionCrosswalk) getCrosswalk("JATS", IngestionCrosswalk.class);
            if (result == null) {
                result = new DepositResult();
            }
            result.setItem(item);

            List<Bitstream> derivedResources = crosswalkAndIngestZip(context, deposit, item);

            // update the item metadata to inclue the current time as
            // the updated date
            this.setUpdatedDate(context, item, verboseDescription);

            // in order to write these changes, we need to bypass the
            // authorisation briefly, because although the user may be
            // able to add stuff to the repository, they may not have
            // WRITE permissions on the archive.
            context.turnOffAuthorisationSystem();
            itemService.update(context, item);
            context.restoreAuthSystemState();

            verboseDescription.append("Replace successful");

            result.setItem(item);
            result.setTreatment(this.getTreatment());
            result.setDerivedResources(derivedResources);

            return result;

        } catch (AuthorizeException e) {
            throw new SwordAuthException(e);
        } catch (SQLException e) {
            throw new DSpaceSwordException(e);
        } catch (ZipException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (JDOMException e) {
            throw new RuntimeException(e);
        } catch (CrosswalkException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * The human readable description of the treatment this ingester has
     * put the deposit through
     *
     * @return human readable description
     */
    private String getTreatment() {
        return "The package has been ingested and unpacked into the item.  Template metadata for " +
            "the collection has been used, and a default title with the name of the file has " +
            "been set";
    }

    // Find crosswalk for the indicated metadata type (e.g. "DC", "MODS")
    protected Object getCrosswalk(String type, Class clazz) {
        String xwalkName = configurationService.getProperty("mets.default.ingest.crosswalk." + type);
        if (xwalkName == null) {
            xwalkName = type;
        }
        return CoreServiceFactory.getInstance().getPluginService().getNamedPlugin(clazz, xwalkName);
    }

    /**
     * Basic unique signature string for comparison
     * @param metadataValue
     * @return
     */
    private String getSignature(MetadataValue metadataValue) {
        return metadataValue.getMetadataField().toString('.')
                + metadataValue.getValue() + metadataValue.getLanguage();
    }
}
