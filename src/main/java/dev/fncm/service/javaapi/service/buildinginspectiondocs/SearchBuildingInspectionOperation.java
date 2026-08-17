package dev.fncm.service.javaapi.service.buildinginspectiondocs;

import com.filenet.api.collection.IndependentObjectSet;
import com.filenet.api.core.Document;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.property.Properties;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;

import dev.fncm.model.SearchBuildingInspectionItem;
import dev.fncm.model.SearchBuildingInspectionResult;
import dev.fncm.service.javaapi.FileNetOperation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

/**
 * Searches all {@code BuildingInspectionReport} documents whose custom string
 * metadata contains the supplied search text.
 *
 * <p>The query matches the search term (case-insensitively via SQL {@code LIKE})
 * against the five string-valued custom properties:
 * <ul>
 *   <li>Municipality</li>
 *   <li>PropertyAddress</li>
 *   <li>InspectorName</li>
 *   <li>BuildingType</li>
 *   <li>ComplianceStatus</li>
 * </ul>
 *
 * <p>Results are capped at {@value #MAX_RESULTS} rows, ordered by
 * {@code DateCreated DESC}.
 */
public class SearchBuildingInspectionOperation implements FileNetOperation<SearchBuildingInspectionResult> {

    private static final Logger LOGGER = Logger.getLogger(SearchBuildingInspectionOperation.class.getName());
    private static final int MAX_RESULTS = 100;

    private final String searchText;

    public SearchBuildingInspectionOperation(String searchText) {
        this.searchText = searchText;
    }

    @Override
    public SearchBuildingInspectionResult execute(ObjectStore os, String username) throws Exception {
        String escaped = FileNetQueryUtil.escapeSql(searchText);
        String like    = "%" + escaped + "%";

        String sql =
            "SELECT TOP " + MAX_RESULTS
            + " d.Id, d.DocumentTitle, d.DateCreated,"
            + " d." + BuildingInspectionConstants.PROP_MUNICIPALITY
            + ", d." + BuildingInspectionConstants.PROP_PROPERTY_ADDRESS
            + ", d." + BuildingInspectionConstants.PROP_INSPECTOR_NAME
            + ", d." + BuildingInspectionConstants.PROP_BUILDING_TYPE
            + ", d." + BuildingInspectionConstants.PROP_COMPLIANCE_STATUS
            + ", d." + BuildingInspectionConstants.PROP_INSPECTION_DATE
            + " FROM " + BuildingInspectionConstants.DOC_CLASS + " d"
            + " WHERE d." + BuildingInspectionConstants.PROP_MUNICIPALITY       + " LIKE '" + like + "'"
            + " OR d."    + BuildingInspectionConstants.PROP_PROPERTY_ADDRESS   + " LIKE '" + like + "'"
            + " OR d."    + BuildingInspectionConstants.PROP_INSPECTOR_NAME     + " LIKE '" + like + "'"
            + " OR d."    + BuildingInspectionConstants.PROP_BUILDING_TYPE      + " LIKE '" + like + "'"
            + " OR d."    + BuildingInspectionConstants.PROP_COMPLIANCE_STATUS  + " LIKE '" + like + "'"
            + " ORDER BY d.DateCreated DESC";

        LOGGER.info("SearchBuildingInspectionOperation — query: " + sql);

        SearchSQL    searchSQL   = new SearchSQL(sql);
        SearchScope  searchScope = new SearchScope(os);
        IndependentObjectSet results = (IndependentObjectSet) searchScope.fetchObjects(searchSQL, null, null, Boolean.TRUE);

        List<SearchBuildingInspectionItem> items = new ArrayList<>();
        Iterator<?> it = results.iterator();
        while (it.hasNext()) {
            Document doc  = (Document) it.next();
            Properties p  = doc.getProperties();

            items.add(new SearchBuildingInspectionItem(
                doc.get_Id().toString(),
                safeString(p, "DocumentTitle"),
                safeString(p, BuildingInspectionConstants.PROP_MUNICIPALITY),
                safeString(p, BuildingInspectionConstants.PROP_PROPERTY_ADDRESS),
                safeString(p, BuildingInspectionConstants.PROP_INSPECTOR_NAME),
                safeString(p, BuildingInspectionConstants.PROP_BUILDING_TYPE),
                safeString(p, BuildingInspectionConstants.PROP_COMPLIANCE_STATUS),
                safeString(p, BuildingInspectionConstants.PROP_INSPECTION_DATE),
                doc.get_DateCreated() != null ? doc.get_DateCreated().toString() : null
            ));
        }

        LOGGER.info("SearchBuildingInspectionOperation — found " + items.size() + " document(s)");
        return new SearchBuildingInspectionResult(items.size(), searchText, items);
    }

    private static String safeString(Properties p, String name) {
        try {
            Object v = p.getObjectValue(name);
            return v != null ? v.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }
}

// Made with Bob
