/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.citation;

/**
 * Basic Citation model, for handling and REST projection
 *
 * @author Kim Shepherd
 */
public class Citation {

    public static final String CSL = "CSL";

    // Citation type (eg CSL, BibTeX, RIS)
    private String type = CSL;
    // Citation style (eg. apa6, harvard, ieee)
    private String style;
    // Citation style label (needed as we have some sorting issues in angular)
    private String label;
    // Citation text rendered for display
    private String text;

    public Citation() {}
    public Citation(String type, String style, String text, String label) {
        this.type = type;
        this.style = style;
        this.text = text;
        this.label = label;
    }
    public void setType(String type) {
        this.type = type;
    }
    public String getType() {
        return type;
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
    public String getLabel() {
        return label;
    }
    public void setLabel(String label) {
        this.label = label;
    }
}