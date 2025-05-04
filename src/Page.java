
/**
 * Holds mapping information for a virtual page.
 */
public class Page {
    /** Page in memory that this virtual page maps to. Is -1 when no mapping
     * exists, that is, this page is on disk instead. */
    public int physicalPage = -1;
    /** Page on disk that this virtual page maps to. Is -1 when no mapping
     * exists, that is, this page is in memory instead. */
    public int diskPage = -1;
}
