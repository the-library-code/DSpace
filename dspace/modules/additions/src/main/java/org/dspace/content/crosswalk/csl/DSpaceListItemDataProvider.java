/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 * <p>
 * http://www.dspace.org/license/
 */
package org.dspace.content.crosswalk.csl;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.undercouch.citeproc.ItemDataProvider;
import de.undercouch.citeproc.csl.CSLDate;
import de.undercouch.citeproc.csl.CSLDateBuilder;
import de.undercouch.citeproc.csl.CSLItemData;
import de.undercouch.citeproc.csl.CSLItemDataBuilder;
import de.undercouch.citeproc.csl.CSLName;
import de.undercouch.citeproc.csl.CSLNameBuilder;
import de.undercouch.citeproc.csl.CSLType;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.DCDate;
import org.dspace.content.DCPersonName;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.factory.IdentifierServiceFactory;
import org.dspace.identifier.service.DOIService;
import org.dspace.util.UUIDUtils;

/**
 * DSpaceListItemDataProvider class provides data for CSL processor.
 * This class can be initalized by Spring. You then still have to set the DSpace context using the method
 * {@link #setContext(Context)}. If the DSpace context is not set, this class will not be able to load items from the
 * database. The DSpace context is specific for every session and stores information like if a user is logged in.
 * Therefore, instances of this class must not be reused for multiple sessions.
 *
 * This class can be used to load any item from DSpace that the provided context is allowed to read. By using
 * {@link #addItem(Item)} you can limit the items that can be loaded.
 *
 * You can also remove item from list by using {@link #removeItem(Item)}
 *
 * @author Vasilii Fedorov at the-library-code.de
 * @author Pascal-Nicolas Becker, dspace -at- pascal dash becker dot de
 */

public class DSpaceListItemDataProvider implements ItemDataProvider {

    private Context context;
    protected final transient ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    protected final transient AuthorizeService authorizeService =
            AuthorizeServiceFactory.getInstance().getAuthorizeService();
    private static final Logger log = LogManager.getLogger(DSpaceListItemDataProvider.class);

    /** You can set item IDs that shall be served by this provider. If itemIDs are not set, all Items from the DSpace
     * installation will be used.
     */
    List<String> itemIDs;

    protected CSLType defaultCSLType;

    protected List<String> id;
    protected List<String> type;
    protected List<String> categories;
    protected List<String> language;
    protected List<String> journalAbbreviation;
    protected List<String> shortTitle;
    protected List<String> author;
    protected List<String> collectionEditor;
    protected List<String> composer;
    protected List<String> containerAuthor;
    protected List<String> director;
    protected List<String> editor;
    protected List<String> editorialDirector;
    protected List<String> interviewer;
    protected List<String> illustrator;
    protected List<String> originalAuthor;
    protected List<String> recipient;
    protected List<String> reviewedAuthor;
    protected List<String> translator;
    protected List<String> accessed;
    protected List<String> container;
    protected List<String> eventDate;
    protected List<String> issued;
    protected List<String> originalDate;
    protected List<String> submitted;
    protected List<String> abstrct;
    protected List<String> annote;
    protected List<String> archive;
    protected List<String> archiveLocation;
    protected List<String> archivePlace;
    protected List<String> authority;
    protected List<String> callNumber;
    protected List<String> chapterNumber;
    protected List<String> citationNumber;
    protected List<String> citationLabel;
    protected List<String> collectionNumber;
    protected List<String> collectionTitle;
    protected List<String> containerTitle;
    protected List<String> containerTitleShort;
    protected List<String> dimensions;
    protected List<String> DOI;
    protected List<String> edition;
    protected List<String> event;
    protected List<String> eventPlace;
    protected List<String> firstReferenceNoteNumber;
    protected List<String> genre;
    protected List<String> ISBN;
    protected List<String> ISSN;
    protected List<String> issue;
    protected List<String> jurisdiction;
    protected List<String> keyword;
    protected List<String> locator;
    protected List<String> medium;
    protected List<String> note;
    protected List<String> number;
    protected List<String> numberOfPages;
    protected List<String> numberOfVolumes;
    protected List<String> originalPublisher;
    protected List<String> originalPublisherPlace;
    protected List<String> originalTitle;
    protected List<String> page;
    protected List<String> pageFirst;
    protected List<String> PMCID;
    protected List<String> PMID;
    protected List<String> publisher;
    protected List<String> publisherPlace;
    protected List<String> references;
    protected List<String> reviewedTitle;
    protected List<String> scale;
    protected List<String> section;
    protected List<String> source;
    protected List<String> status;
    protected List<String> title;
    protected List<String> titleShort;
    protected List<String> URL;
    protected List<String> version;
    protected List<String> volume;
    protected List<String> yearSuffix;

    protected Map<String, CSLType> CSLTypeMap;

    // CSLItemData is Object that has all the data (from metadata of item) and is ready to be converted into citation
    // Field items from parent class are used

    /**
     * Empty constructor
     */
    public DSpaceListItemDataProvider() {

    }

    /**
     * Constructor that sets DSpace context
     * @param context
     */
    public DSpaceListItemDataProvider(Context context) {
        this.context = context;
    }

    /**
     * Method that sets context
     * @param context
     */
    public void setContext(Context context) {
        this.context = context;
    }

    /**
     * Method that adds item ID to list of served items
     * @param item
     */
    public void addItem(Item item) {
        if (this.itemIDs == null) {
            this.itemIDs = new LinkedList<>();
        }
        this.itemIDs.add(item.getID().toString());
    }

    /**
     * Method that removes all items from list of served items
     */
    public void resetItems() {
        this.itemIDs = null;
    }

    /** Forbid to load any items. */
    public void setItemsEmpty() {
        this.itemIDs = new LinkedList<>();
    }

    /**
     * Method that removes item ID from list of served items
     * @param item
     */
    public void removeItem(Item item) {
        if (this.itemIDs != null) {
            this.itemIDs.remove(item.getID().toString());
            if (itemIDs.size() == 0) {
                this.itemIDs = null;
            }
        }
    }

    /**
     * Retrieve a citation item with a given ID
     *
     * @param id the item's unique ID
     * @return the item
     */
    @Override
    public CSLItemData retrieveItem(String id) {
        if (this.context == null) {
            throw new IllegalStateException("You need to set a DSpaceContext before trying to retrieve items.");
        }
        if (StringUtils.isBlank(id)) {
            throw new IllegalArgumentException("You must provide the ID of the item you want to retrieve.");
        }
        if (this.itemIDs != null && !(this.itemIDs.contains(id))) {
            throw new RuntimeException("This instance of the " + this.getClass().getName()
                    + " is limited to load certain items. The item you wanted to retrieve " +
                    "cannot be provided by this instance.");
        }
        UUID uuid = UUIDUtils.fromString(id);
        Item item = null;
        try {
            item = itemService.find(this.context, uuid);
        } catch (SQLException throwables) {
            throw new RuntimeException("SQLException.", throwables);
        }
        try {
            authorizeService.authorizeAction(this.context, item, Constants.READ);
        } catch (AuthorizeException e) {
            // I'd prefer if AuthorizeException were RuntimeExceptions
            // We cannot declare to throw AuthorizeExceptions as we implement an interface defined by CSL.
            throw new RuntimeException(e);
        } catch (SQLException throwables) {
            throw new RuntimeException(throwables);
        }
        return this.convertItem(item);
    }

    /**
     * Method that gets UUID and retrieves a citation item with a given ID
     * @param id
     */
    public CSLItemData retrieveItem(UUID id) {
        return this.retrieveItem(id.toString());
    }

    /**
     * @return an array of all item IDs this provider can serve
     */
    @Override
    public Collection<String> getIds() {
        if (context == null) {
            throw new IllegalStateException("You need to set a DSpaceContext before trying to retrieve items.");
        }
        List<String> ids = new LinkedList<>();
        if (this.itemIDs != null) {
            ids = this.itemIDs;
        } else {
            try {
                Iterator<Item> itemIterator = this.itemService.findAll(this.context);
                while (itemIterator.hasNext()) {
                    ids.add(itemIterator.next().getID().toString());
                }
            } catch (SQLException throwables) {
                throw new RuntimeException("SQLException.", throwables);
            }
        }
        return ids;
    }

    /**
     * helper method to Split metadata strings into a string array, f.e. "dc.identifier.doi" -> dc, identifier, doi.
     * @param metadataField
     * @return
     */
    public String[] getArrayOfMetadataField(String metadataField) {

        String[] arrayOfMetadata = metadataField.split("\\.");
        return arrayOfMetadata;
    }

    protected String getSingleStringValue(List<String> fields, Item item) {
        String value = null;
        if (fields != null && fields.size() > 0) {
            for (String field : fields) {
                String[] arrayOfMetadata = getArrayOfMetadataField(field);
                value = itemService.getMetadataFirstValue(item, arrayOfMetadata[0], arrayOfMetadata[1],
                        arrayOfMetadata.length > 2 ? arrayOfMetadata[2] : null, Item.ANY);
                if (value != null) {
                    return value;
                }
            }
        }
        return value;
    }

    protected CSLType getCSLType(List<String> fields, Item item) {
        String type = getSingleStringValue(this.type, item);
        if (StringUtils.isBlank(type) && this.defaultCSLType != null) {
            return this.defaultCSLType;
        }
        return CSLTypeMap.get(type);
    }

    protected CSLDate getCSLDate(List<String> fields, Item item) {
        String date = getSingleStringValue(fields, item);
        if (StringUtils.isNotBlank(date)) {
            DCDate dcDate = new DCDate(date);
            CSLDateBuilder dateBuilder = new CSLDateBuilder();
            if (dcDate.getDay() != -1) {
                return dateBuilder.dateParts(dcDate.getYear(), dcDate.getMonth(), dcDate.getDay()).build();
            }
            if (dcDate.getMonth() != -1) {
                return dateBuilder.dateParts(dcDate.getYear(), dcDate.getMonth()).build();
            }
            if (dcDate.getYear() != -1) {
                return dateBuilder.dateParts(dcDate.getYear()).build();
            }
        }
        return null;
    }

    protected CSLName[] getCSLNames(List<String> fields, Item item) {
        List<CSLName> cslNames = new LinkedList<>();
        if (fields != null && fields.size() > 0) {
            for (String field : fields) {
                for (MetadataValue mv : itemService.getMetadataByMetadataString(item, field)) {
                    String author = mv.getValue();
                    CSLNameBuilder nameBuilder = new CSLNameBuilder();
                    // DSpace stores "first name, last name"
                    // check if we have a person
                    if (author.contains(",")) {
                        DCPersonName personName = new DCPersonName(author);
                        nameBuilder.family(personName.getLastName());
                        nameBuilder.given(personName.getFirstNames());
                    } else {
                        // If there is no comma, I would guess it is an institution. This can happen f.e. for editors.
                        // I'm not sure if nameBuilder.literal(...) is the right call here. We must test this.
                        // TODO: TEST THIS!
                        nameBuilder.literal(mv.getValue());
                        nameBuilder.isInstitution(true);
                    }

                    cslNames.add(nameBuilder.build());
                }
            }
            if (cslNames.size() == 0) {
                return null;
            }
        }
        return cslNames.toArray(new CSLName[cslNames.size()]);
    }

    protected Integer getInteger(List<String> fields, Item item) {
        if (fields != null && fields.size() > 0) {
            for (String field : fields) {
                for (MetadataValue mv : itemService.getMetadataByMetadataString(item, field)) {
                    if (mv != null) {
                        try {
                            return Integer.parseInt(mv.getValue());
                        } catch (NumberFormatException | NullPointerException ex) {
                            log.debug("Trying to find a metadata value being a number. '" + mv.getValue() + "' from "
                                    + field + " is not a number.", ex);
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Most versions of DSpace stores DOIs in the same metadata field as handles or other urls.
     * This method tries to ensure we get a valid DOI and not any other identifier.
     * @param fields
     * @param item
     * @return
     */
    protected String getDOI(List<String> fields, Item item) {
        DOIService doiService = IdentifierServiceFactory.getInstance().getDOIService();
        if (fields != null && fields.size() > 0) {
            for (String field : fields) {
                for (MetadataValue mv : itemService.getMetadataByMetadataString(item, field)) {
                    if (mv != null) {
                        String value = mv.getValue();
                        if (value.contains("10.")) {
                            if (doiService != null) {
                                try {
                                    value = doiService.formatIdentifier(value)
                                            .substring(org.dspace.identifier.DOI.SCHEME.length());
                                } catch (IdentifierException ex) {
                                    log.debug("Unable to format DOI " + value + ".", ex);
                                }
                                return value;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    protected String getDOIAsURL(List<String> fields, Item item) {
        DOIService doiService = IdentifierServiceFactory.getInstance().getDOIService();
        String doi = this.getDOI(fields, item);
        if (StringUtils.isNotBlank(doi)) {
            try {
                return doiService.DOIToExternalForm(doi);
            } catch (Exception ex) {
                log.warn("Ignoring exception.", ex);
            }
        }
        return null;
    }

    /**
     * Convert the metadata of a DSpace item into metadata for a CSL processor.
     * @param item The DSpace item to convert.
     * @return ItemData as required by CSL.
     */
    public CSLItemData convertItem(Item item) {
        CSLItemDataBuilder cslItemDataBuilder = new CSLItemDataBuilder();
        CSLType type = getCSLType(this.type, item);
        if (type != null) {
            cslItemDataBuilder.type(type);
        }

        // String fields
        String id = getSingleStringValue(this.id, item);
        if (StringUtils.isNotBlank(id)) {
            cslItemDataBuilder = cslItemDataBuilder.id(id);
        }

        String language = getSingleStringValue(this.language, item);
        if (StringUtils.isNotBlank(language)) {
            cslItemDataBuilder = cslItemDataBuilder.language(language);
        }

        String journalAbbreviation = getSingleStringValue(this.journalAbbreviation, item);
        if (StringUtils.isNotBlank(journalAbbreviation)) {
            cslItemDataBuilder = cslItemDataBuilder.journalAbbreviation(journalAbbreviation);
        }

        String shortTitle = getSingleStringValue(this.shortTitle, item);
        if (StringUtils.isNotBlank(shortTitle)) {
            cslItemDataBuilder = cslItemDataBuilder.shortTitle(shortTitle);
        }

        String abstrct = getSingleStringValue(this.abstrct, item);
        if (StringUtils.isNotBlank(abstrct)) {
            cslItemDataBuilder = cslItemDataBuilder.abstrct(abstrct);
        }

        String annote = getSingleStringValue(this.annote, item);
        if (StringUtils.isNotBlank(annote)) {
            cslItemDataBuilder = cslItemDataBuilder.annote(annote);
        }

        String archive = getSingleStringValue(this.archive, item);
        if (StringUtils.isNotBlank(archive)) {
            cslItemDataBuilder = cslItemDataBuilder.archive(archive);
        }

        String archiveLocation = getSingleStringValue(this.archiveLocation, item);
        if (StringUtils.isNotBlank(archiveLocation)) {
            cslItemDataBuilder = cslItemDataBuilder.archiveLocation(archiveLocation);
        }

        String archivePlace = getSingleStringValue(this.archivePlace, item);
        if (StringUtils.isNotBlank(archivePlace)) {
            cslItemDataBuilder = cslItemDataBuilder.archivePlace(archivePlace);
        }

        String authority = getSingleStringValue(this.authority, item);
        if (StringUtils.isNotBlank(authority)) {
            cslItemDataBuilder = cslItemDataBuilder.authority(authority);
        }

        String callNumber = getSingleStringValue(this.callNumber, item);
        if (StringUtils.isNotBlank(callNumber)) {
            cslItemDataBuilder = cslItemDataBuilder.callNumber(callNumber);
        }

        String chapterNumber = getSingleStringValue(this.chapterNumber, item);
        if (StringUtils.isNotBlank(chapterNumber)) {
            cslItemDataBuilder = cslItemDataBuilder.chapterNumber(chapterNumber);
        }

        String citationNumber = getSingleStringValue(this.citationNumber, item);
        if (StringUtils.isNotBlank(citationNumber)) {
            cslItemDataBuilder = cslItemDataBuilder.citationNumber(citationNumber);
        }

        String citationLabel = getSingleStringValue(this.citationLabel, item);
        if (StringUtils.isNotBlank(citationLabel)) {
            cslItemDataBuilder = cslItemDataBuilder.citationLabel(citationLabel);
        }

        String collectionNumber = getSingleStringValue(this.collectionNumber, item);
        if (StringUtils.isNotBlank(collectionNumber)) {
            cslItemDataBuilder = cslItemDataBuilder.collectionNumber(collectionNumber);
        }

        String collectionTitle = getSingleStringValue(this.collectionTitle, item);
        if (StringUtils.isNotBlank(collectionTitle)) {
            cslItemDataBuilder = cslItemDataBuilder.collectionTitle(collectionTitle);
        }

        String containerTitle = getSingleStringValue(this.containerTitle, item);
        if (StringUtils.isNotBlank(containerTitle)) {
            cslItemDataBuilder = cslItemDataBuilder.containerTitle(containerTitle);
        }

        String containerTitleShort = getSingleStringValue(this.containerTitleShort, item);
        if (StringUtils.isNotBlank(containerTitleShort)) {
            cslItemDataBuilder = cslItemDataBuilder.containerTitleShort(containerTitleShort);
        }

        String dimensions = getSingleStringValue(this.dimensions, item);
        if (StringUtils.isNotBlank(dimensions)) {
            cslItemDataBuilder = cslItemDataBuilder.dimensions(dimensions);
        }

        String DOI = getDOI(this.DOI, item);
        if (StringUtils.isNotBlank(DOI)) {
            cslItemDataBuilder = cslItemDataBuilder.DOI(DOI);
        }

        String edition = getSingleStringValue(this.edition, item);
        if (StringUtils.isNotBlank(edition)) {
            cslItemDataBuilder = cslItemDataBuilder.edition(edition);
        }

        String event = getSingleStringValue(this.event, item);
        if (StringUtils.isNotBlank(event)) {
            cslItemDataBuilder = cslItemDataBuilder.event(event);
        }

        String eventPlace = getSingleStringValue(this.eventPlace, item);
        if (StringUtils.isNotBlank(eventPlace)) {
            cslItemDataBuilder = cslItemDataBuilder.eventPlace(eventPlace);
        }

        String firstReferenceNoteNumber = getSingleStringValue(this.firstReferenceNoteNumber, item);
        if (StringUtils.isNotBlank(firstReferenceNoteNumber)) {
            cslItemDataBuilder = cslItemDataBuilder.firstReferenceNoteNumber(firstReferenceNoteNumber);
        }

        String genre = getSingleStringValue(this.genre, item);
        if (StringUtils.isNotBlank(genre)) {
            cslItemDataBuilder = cslItemDataBuilder.genre(genre);
        }

        String ISBN = getSingleStringValue(this.ISBN, item);
        if (StringUtils.isNotBlank(ISBN)) {
            cslItemDataBuilder = cslItemDataBuilder.ISBN(ISBN);
        }

        String ISSN = getSingleStringValue(this.ISSN, item);
        if (StringUtils.isNotBlank(ISSN)) {
            cslItemDataBuilder = cslItemDataBuilder.ISSN(ISSN);
        }

        String issue = getSingleStringValue(this.issue, item);
        if (StringUtils.isNotBlank(issue)) {
            cslItemDataBuilder = cslItemDataBuilder.issue(issue);
        }

        String jurisdiction = getSingleStringValue(this.jurisdiction, item);
        if (StringUtils.isNotBlank(jurisdiction)) {
            cslItemDataBuilder = cslItemDataBuilder.jurisdiction(jurisdiction);
        }

        String keyword = getSingleStringValue(this.keyword, item);
        if (StringUtils.isNotBlank(keyword)) {
            cslItemDataBuilder = cslItemDataBuilder.keyword(keyword);
        }

        String locator = getSingleStringValue(this.locator, item);
        if (StringUtils.isNotBlank(locator)) {
            cslItemDataBuilder = cslItemDataBuilder.locator(locator);
        }

        String medium = getSingleStringValue(this.medium, item);
        if (StringUtils.isNotBlank(medium)) {
            cslItemDataBuilder = cslItemDataBuilder.medium(medium);
        }

        String note = getSingleStringValue(this.note, item);
        if (StringUtils.isNotBlank(note)) {
            cslItemDataBuilder = cslItemDataBuilder.note(note);
        }

        String number = getSingleStringValue(this.number, item);
        if (StringUtils.isNotBlank(number)) {
            cslItemDataBuilder = cslItemDataBuilder.number(number);
        }

        String numberOfPages = getSingleStringValue(this.numberOfPages, item);
        if (StringUtils.isNotBlank(numberOfPages)) {
            cslItemDataBuilder = cslItemDataBuilder.numberOfPages(numberOfPages);
        }

        String numberOfVolumes = getSingleStringValue(this.numberOfVolumes, item);
        if (StringUtils.isNotBlank(numberOfVolumes)) {
            cslItemDataBuilder = cslItemDataBuilder.numberOfVolumes(numberOfVolumes);
        }

        String originalPublisher = getSingleStringValue(this.originalPublisher, item);
        if (StringUtils.isNotBlank(originalPublisher)) {
            cslItemDataBuilder = cslItemDataBuilder.originalPublisher(originalPublisher);
        }

        String originalPublisherPlace = getSingleStringValue(this.originalPublisherPlace, item);
        if (StringUtils.isNotBlank(originalPublisherPlace)) {
            cslItemDataBuilder = cslItemDataBuilder.originalPublisherPlace(originalPublisherPlace);
        }

        String originalTitle = getSingleStringValue(this.originalTitle, item);
        if (StringUtils.isNotBlank(originalTitle)) {
            cslItemDataBuilder = cslItemDataBuilder.originalTitle(originalTitle);
        }

        String page = getSingleStringValue(this.page, item);
        if (StringUtils.isNotBlank(page)) {
            cslItemDataBuilder = cslItemDataBuilder.page(page);
        }

        String pageFirst = getSingleStringValue(this.pageFirst, item);
        if (StringUtils.isNotBlank(pageFirst)) {
            cslItemDataBuilder = cslItemDataBuilder.pageFirst(pageFirst);
        }

        String PMCID = getSingleStringValue(this.PMCID, item);
        if (StringUtils.isNotBlank(PMCID)) {
            cslItemDataBuilder = cslItemDataBuilder.PMCID(PMCID);
        }

        String PMID = getSingleStringValue(this.PMID, item);
        if (StringUtils.isNotBlank(PMID)) {
            cslItemDataBuilder = cslItemDataBuilder.PMID(PMID);
        }

        String publisher = getSingleStringValue(this.publisher, item);
        if (StringUtils.isNotBlank(publisher)) {
            cslItemDataBuilder = cslItemDataBuilder.publisher(publisher);
        }

        String publisherPlace = getSingleStringValue(this.publisherPlace, item);
        if (StringUtils.isNotBlank(publisherPlace)) {
            cslItemDataBuilder = cslItemDataBuilder.publisherPlace(publisherPlace);
        }

        String references = getSingleStringValue(this.references, item);
        if (StringUtils.isNotBlank(references)) {
            cslItemDataBuilder = cslItemDataBuilder.references(references);
        }

        String reviewedTitle = getSingleStringValue(this.reviewedTitle, item);
        if (StringUtils.isNotBlank(reviewedTitle)) {
            cslItemDataBuilder = cslItemDataBuilder.reviewedTitle(reviewedTitle);
        }

        String scale = getSingleStringValue(this.scale, item);
        if (StringUtils.isNotBlank(scale)) {
            cslItemDataBuilder = cslItemDataBuilder.scale(scale);
        }

        String section = getSingleStringValue(this.section, item);
        if (StringUtils.isNotBlank(section)) {
            cslItemDataBuilder = cslItemDataBuilder.section(section);
        }

        String source = getSingleStringValue(this.source, item);
        if (StringUtils.isNotBlank(source)) {
            cslItemDataBuilder = cslItemDataBuilder.source(source);
        }

        String status = getSingleStringValue(this.status, item);
        if (StringUtils.isNotBlank(status)) {
            cslItemDataBuilder = cslItemDataBuilder.status(status);
        }

        String title = getSingleStringValue(this.title, item);
        if (StringUtils.isNotBlank(title)) {
            cslItemDataBuilder = cslItemDataBuilder.title(title);
        }

        String titleShort = getSingleStringValue(this.titleShort, item);
        if (StringUtils.isNotBlank(titleShort)) {
            cslItemDataBuilder = cslItemDataBuilder.titleShort(titleShort);
        }

        String URL = getDOIAsURL(this.URL, item);
        if (StringUtils.isBlank(URL)) {
            URL = getSingleStringValue(this.URL, item);
        }
        if (StringUtils.isNotBlank(URL)) {
            cslItemDataBuilder = cslItemDataBuilder.URL(URL);
        }

        String version = getSingleStringValue(this.version, item);
        if (StringUtils.isNotBlank(version)) {
            cslItemDataBuilder = cslItemDataBuilder.version(version);
        }

        String volume = getSingleStringValue(this.volume, item);
        if (StringUtils.isNotBlank(volume)) {
            cslItemDataBuilder = cslItemDataBuilder.volume(volume);
        }

        String yearSuffix = getSingleStringValue(this.yearSuffix, item);
        if (StringUtils.isNotBlank(yearSuffix)) {
            cslItemDataBuilder = cslItemDataBuilder.yearSuffix(yearSuffix);
        }

        // Name fields
        CSLName[] authors = getCSLNames(this.author, item);
        if (authors != null && authors.length > 0) {
            cslItemDataBuilder = cslItemDataBuilder.author(authors);
        }

        CSLName[] collectionEditors = getCSLNames(this.collectionEditor, item);
        if (collectionEditors != null && collectionEditors.length > 0) {
            cslItemDataBuilder = cslItemDataBuilder.collectionEditor(collectionEditors);
        }

        CSLName[] composers = getCSLNames(this.composer, item);
        if (composers != null && composers.length > 0) {
            cslItemDataBuilder = cslItemDataBuilder.composer(composers);
        }

        CSLName[] containerAuthors = getCSLNames(this.containerAuthor, item);
        if (containerAuthors != null && containerAuthors.length > 0) {
            cslItemDataBuilder = cslItemDataBuilder.containerAuthor(containerAuthors);
        }

        CSLName[] directors = getCSLNames(this.director, item);
        if (directors != null && directors.length > 0) {
            cslItemDataBuilder = cslItemDataBuilder.director(directors);
        }

        CSLName[] editors = getCSLNames(this.editor, item);
        if (editors != null && editors.length > 0) {
            cslItemDataBuilder = cslItemDataBuilder.editor(editors);
        }

        CSLName[] editorialDirectors = getCSLNames(this.editorialDirector, item);
        if (editorialDirectors != null && editorialDirectors.length > 0) {
            cslItemDataBuilder = cslItemDataBuilder.editorialDirector(editorialDirectors);
        }

        CSLName[] interviewers = getCSLNames(this.interviewer, item);
        if (interviewers != null && interviewers.length > 0) {
            cslItemDataBuilder = cslItemDataBuilder.interviewer(interviewers);
        }

        CSLName[] illustrators = getCSLNames(this.illustrator, item);
        if (illustrators != null && illustrators.length > 0) {
            cslItemDataBuilder = cslItemDataBuilder.illustrator(illustrators);
        }

        CSLName[] originalAuthors = getCSLNames(this.originalAuthor, item);
        if (originalAuthors != null && originalAuthors.length > 0) {
            cslItemDataBuilder = cslItemDataBuilder.originalAuthor(originalAuthors);
        }

        CSLName[] recipients = getCSLNames(this.recipient, item);
        if (recipients != null && recipients.length > 0) {
            cslItemDataBuilder = cslItemDataBuilder.recipient(recipients);
        }

        CSLName[] reviewedAuthors = getCSLNames(this.reviewedAuthor, item);
        if (reviewedAuthors != null && reviewedAuthors.length > 0) {
            cslItemDataBuilder = cslItemDataBuilder.reviewedAuthor(reviewedAuthors);
        }

        CSLName[] translators = getCSLNames(this.translator, item);
        if (translators != null && translators.length > 0) {
            cslItemDataBuilder = cslItemDataBuilder.translator(translators);
        }

        // Date fields
        CSLDate issued = getCSLDate(this.issued, item);
        if (issued != null) {
            cslItemDataBuilder.issued(issued);
        }

        CSLDate accessed = getCSLDate(this.accessed, item);
        if (accessed != null) {
            cslItemDataBuilder.accessed(accessed);
        }

        CSLDate container = getCSLDate(this.container, item);
        if (container != null) {
            cslItemDataBuilder.container(container);
        }

        CSLDate eventDate = getCSLDate(this.eventDate, item);
        if (eventDate != null) {
            cslItemDataBuilder.eventDate(eventDate);
        }

        CSLDate originalDate = getCSLDate(this.originalDate, item);
        if (originalDate != null) {
            cslItemDataBuilder.originalDate(originalDate);
        }

        CSLDate submitted = getCSLDate(this.submitted, item);
        if (submitted != null) {
            cslItemDataBuilder.submitted(submitted);
        }

        return cslItemDataBuilder.build();

    }

    public CSLType getDefaultCSLType() {
        return defaultCSLType;
    }

    public void setDefaultCSLType(CSLType defaultCSLType) {
        this.defaultCSLType = defaultCSLType;
    }

    public void setCSLTypeMap(Map<String, CSLType> cslTypeMap) {
        this.CSLTypeMap = cslTypeMap;
    }

    public Map<String, CSLType> getCSLTypeMap() {
        return CSLTypeMap;
    }

    public List<String> getId() {
        return id;
    }

    public void setId(List<String> id) {
        this.id = id;
    }

    public List<String> getType() {
        return type;
    }

    public void setType(List<String> type) {
        this.type = type;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public List<String> getLanguage() {
        return language;
    }

    public void setLanguage(List<String> language) {
        this.language = language;
    }

    public List<String> getJournalAbbreviation() {
        return journalAbbreviation;
    }

    public void setJournalAbbreviation(List<String> journalAbbreviation) {
        this.journalAbbreviation = journalAbbreviation;
    }

    public List<String> getShortTitle() {
        return shortTitle;
    }

    public void setShortTitle(List<String> shortTitle) {
        this.shortTitle = shortTitle;
    }

    public List<String> getAuthor() {
        return author;
    }

    public void setAuthor(List<String> author) {
        this.author = author;
    }

    public List<String> getCollectionEditor() {
        return collectionEditor;
    }

    public void setCollectionEditor(List<String> collectionEditor) {
        this.collectionEditor = collectionEditor;
    }

    public List<String> getComposer() {
        return composer;
    }

    public void setComposer(List<String> composer) {
        this.composer = composer;
    }

    public List<String> getContainerAuthor() {
        return containerAuthor;
    }

    public void setContainerAuthor(List<String> containerAuthor) {
        this.containerAuthor = containerAuthor;
    }

    public List<String> getDirector() {
        return director;
    }

    public void setDirector(List<String> director) {
        this.director = director;
    }

    public List<String> getEditor() {
        return editor;
    }

    public void setEditor(List<String> editor) {
        this.editor = editor;
    }

    public List<String> getEditorialDirector() {
        return editorialDirector;
    }

    public void setEditorialDirector(List<String> editorialDirector) {
        this.editorialDirector = editorialDirector;
    }

    public List<String> getInterviewer() {
        return interviewer;
    }

    public void setInterviewer(List<String> interviewer) {
        this.interviewer = interviewer;
    }

    public List<String> getIllustrator() {
        return illustrator;
    }

    public void setIllustrator(List<String> illustrator) {
        this.illustrator = illustrator;
    }

    public List<String> getOriginalAuthor() {
        return originalAuthor;
    }

    public void setOriginalAuthor(List<String> originalAuthor) {
        this.originalAuthor = originalAuthor;
    }

    public List<String> getRecipient() {
        return recipient;
    }

    public void setRecipient(List<String> recipient) {
        this.recipient = recipient;
    }

    public List<String> getReviewedAuthor() {
        return reviewedAuthor;
    }

    public void setReviewedAuthor(List<String> reviewedAuthor) {
        this.reviewedAuthor = reviewedAuthor;
    }

    public List<String> getTranslator() {
        return translator;
    }

    public void setTranslator(List<String> translator) {
        this.translator = translator;
    }

    public List<String> getAccessed() {
        return accessed;
    }

    public void setAccessed(List<String> accessed) {
        this.accessed = accessed;
    }

    public List<String> getContainer() {
        return container;
    }

    public void setContainer(List<String> container) {
        this.container = container;
    }

    public List<String> getEventDate() {
        return eventDate;
    }

    public void setEventDate(List<String> eventDate) {
        this.eventDate = eventDate;
    }

    public List<String> getIssued() {
        return issued;
    }

    public void setIssued(List<String> issued) {
        this.issued = issued;
    }

    public List<String> getOriginalDate() {
        return originalDate;
    }

    public void setOriginalDate(List<String> originalDate) {
        this.originalDate = originalDate;
    }

    public List<String> getSubmitted() {
        return submitted;
    }

    public void setSubmitted(List<String> submitted) {
        this.submitted = submitted;
    }

    public List<String> getAbstrct() {
        return abstrct;
    }

    public void setAbstrct(List<String> abstrct) {
        this.abstrct = abstrct;
    }

    public List<String> getAnnote() {
        return annote;
    }

    public void setAnnote(List<String> annote) {
        this.annote = annote;
    }

    public List<String> getArchive() {
        return archive;
    }

    public void setArchive(List<String> archive) {
        this.archive = archive;
    }

    public List<String> getArchiveLocation() {
        return archiveLocation;
    }

    public void setArchiveLocation(List<String> archiveLocation) {
        this.archiveLocation = archiveLocation;
    }

    public List<String> getArchivePlace() {
        return archivePlace;
    }

    public void setArchivePlace(List<String> archivePlace) {
        this.archivePlace = archivePlace;
    }

    public List<String> getAuthority() {
        return authority;
    }

    public void setAuthority(List<String> authority) {
        this.authority = authority;
    }

    public List<String> getCallNumber() {
        return callNumber;
    }

    public void setCallNumber(List<String> callNumber) {
        this.callNumber = callNumber;
    }

    public List<String> getChapterNumber() {
        return chapterNumber;
    }

    public void setChapterNumber(List<String> chapterNumber) {
        this.chapterNumber = chapterNumber;
    }

    public List<String> getCitationNumber() {
        return citationNumber;
    }

    public void setCitationNumber(List<String> citationNumber) {
        this.citationNumber = citationNumber;
    }

    public List<String> getCitationLabel() {
        return citationLabel;
    }

    public void setCitationLabel(List<String> citationLabel) {
        this.citationLabel = citationLabel;
    }

    public List<String> getCollectionNumber() {
        return collectionNumber;
    }

    public void setCollectionNumber(List<String> collectionNumber) {
        this.collectionNumber = collectionNumber;
    }

    public List<String> getCollectionTitle() {
        return collectionTitle;
    }

    public void setCollectionTitle(List<String> collectionTitle) {
        this.collectionTitle = collectionTitle;
    }

    public List<String> getContainerTitle() {
        return containerTitle;
    }

    public void setContainerTitle(List<String> containerTitle) {
        this.containerTitle = containerTitle;
    }

    public List<String> getContainerTitleShort() {
        return containerTitleShort;
    }

    public void setContainerTitleShort(List<String> containerTitleShort) {
        this.containerTitleShort = containerTitleShort;
    }

    public List<String> getDimensions() {
        return dimensions;
    }

    public void setDimensions(List<String> dimensions) {
        this.dimensions = dimensions;
    }

    public List<String> getDOI() {
        return DOI;
    }

    public void setDOI(List<String> DOI) {
        this.DOI = DOI;
    }

    public List<String> getEdition() {
        return edition;
    }

    public void setEdition(List<String> edition) {
        this.edition = edition;
    }

    public List<String> getEvent() {
        return event;
    }

    public void setEvent(List<String> event) {
        this.event = event;
    }

    public List<String> getEventPlace() {
        return eventPlace;
    }

    public void setEventPlace(List<String> eventPlace) {
        this.eventPlace = eventPlace;
    }

    public List<String> getFirstReferenceNoteNumber() {
        return firstReferenceNoteNumber;
    }

    public void setFirstReferenceNoteNumber(List<String> firstReferenceNoteNumber) {
        this.firstReferenceNoteNumber = firstReferenceNoteNumber;
    }

    public List<String> getGenre() {
        return genre;
    }

    public void setGenre(List<String> genre) {
        this.genre = genre;
    }

    public List<String> getISBN() {
        return ISBN;
    }

    public void setISBN(List<String> ISBN) {
        this.ISBN = ISBN;
    }

    public List<String> getISSN() {
        return ISSN;
    }

    public void setISSN(List<String> ISSN) {
        this.ISSN = ISSN;
    }

    public List<String> getIssue() {
        return issue;
    }

    public void setIssue(List<String> issue) {
        this.issue = issue;
    }

    public List<String> getJurisdiction() {
        return jurisdiction;
    }

    public void setJurisdiction(List<String> jurisdiction) {
        this.jurisdiction = jurisdiction;
    }

    public List<String> getKeyword() {
        return keyword;
    }

    public void setKeyword(List<String> keyword) {
        this.keyword = keyword;
    }

    public List<String> getLocator() {
        return locator;
    }

    public void setLocator(List<String> locator) {
        this.locator = locator;
    }

    public List<String> getMedium() {
        return medium;
    }

    public void setMedium(List<String> medium) {
        this.medium = medium;
    }

    public List<String> getNote() {
        return note;
    }

    public void setNote(List<String> note) {
        this.note = note;
    }

    public List<String> getNumber() {
        return number;
    }

    public void setNumber(List<String> number) {
        this.number = number;
    }

    public List<String> getNumberOfPages() {
        return numberOfPages;
    }

    public void setNumberOfPages(List<String> numberOfPages) {
        this.numberOfPages = numberOfPages;
    }

    public List<String> getNumberOfVolumes() {
        return numberOfVolumes;
    }

    public void setNumberOfVolumes(List<String> numberOfVolumes) {
        this.numberOfVolumes = numberOfVolumes;
    }

    public List<String> getOriginalPublisher() {
        return originalPublisher;
    }

    public void setOriginalPublisher(List<String> originalPublisher) {
        this.originalPublisher = originalPublisher;
    }

    public List<String> getOriginalPublisherPlace() {
        return originalPublisherPlace;
    }

    public void setOriginalPublisherPlace(List<String> originalPublisherPlace) {
        this.originalPublisherPlace = originalPublisherPlace;
    }

    public List<String> getOriginalTitle() {
        return originalTitle;
    }

    public void setOriginalTitle(List<String> originalTitle) {
        this.originalTitle = originalTitle;
    }

    public List<String> getPage() {
        return page;
    }

    public void setPage(List<String> page) {
        this.page = page;
    }

    public List<String> getPageFirst() {
        return pageFirst;
    }

    public void setPageFirst(List<String> pageFirst) {
        this.pageFirst = pageFirst;
    }

    public List<String> getPMCID() {
        return PMCID;
    }

    public void setPMCID(List<String> PMCID) {
        this.PMCID = PMCID;
    }

    public List<String> getPMID() {
        return PMID;
    }

    public void setPMID(List<String> PMID) {
        this.PMID = PMID;
    }

    public List<String> getPublisher() {
        return publisher;
    }

    public void setPublisher(List<String> publisher) {
        this.publisher = publisher;
    }

    public List<String> getPublisherPlace() {
        return publisherPlace;
    }

    public void setPublisherPlace(List<String> publisherPlace) {
        this.publisherPlace = publisherPlace;
    }

    public List<String> getReferences() {
        return references;
    }

    public void setReferences(List<String> references) {
        this.references = references;
    }

    public List<String> getReviewedTitle() {
        return reviewedTitle;
    }

    public void setReviewedTitle(List<String> reviewedTitle) {
        this.reviewedTitle = reviewedTitle;
    }

    public List<String> getScale() {
        return scale;
    }

    public void setScale(List<String> scale) {
        this.scale = scale;
    }

    public List<String> getSection() {
        return section;
    }

    public void setSection(List<String> section) {
        this.section = section;
    }

    public List<String> getSource() {
        return source;
    }

    public void setSource(List<String> source) {
        this.source = source;
    }

    public List<String> getStatus() {
        return status;
    }

    public void setStatus(List<String> status) {
        this.status = status;
    }

    public List<String> getTitle() {
        return title;
    }

    public void setTitle(List<String> title) {
        this.title = title;
    }

    public List<String> getTitleShort() {
        return titleShort;
    }

    public void setTitleShort(List<String> titleShort) {
        this.titleShort = titleShort;
    }

    public List<String> getURL() {
        return URL;
    }

    public void setURL(List<String> URL) {
        this.URL = URL;
    }

    public List<String> getVersion() {
        return version;
    }

    public void setVersion(List<String> version) {
        this.version = version;
    }

    public List<String> getVolume() {
        return volume;
    }

    public void setVolume(List<String> volume) {
        this.volume = volume;
    }

    public List<String> getYearSuffix() {
        return yearSuffix;
    }

    public void setYearSuffix(List<String> yearSuffix) {
        this.yearSuffix = yearSuffix;
    }
}
