package dev.fncm.service.javaapi.service.buildinginspectiondocs;

import com.filenet.api.admin.ChoiceList;
import com.filenet.api.collection.EngineCollection;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;
import com.filenet.api.core.ObjectStore;

import java.util.Iterator;

/**
 * Shared query utilities for FileNet JACE operations in this package.
 */
public final class FileNetQueryUtil {

    private FileNetQueryUtil() {}

    /**
     * Escapes single quotes in a value for safe embedding in a FileNet SQL string literal.
     *
     * @param s raw value (e.g. a symbolic name or display name)
     * @return value with {@code '} replaced by {@code ''}
     */
    public static String escapeSql(String s) {
        return s.replace("'", "''");
    }

    /**
     * Finds a {@link ChoiceList} by its display name, or returns {@code null} if not found.
     *
     * @param objectStore connected FileNet object store
     * @param displayName the display name of the choice list to find
     * @return the matching {@link ChoiceList}, or {@code null}
     */
    public static ChoiceList findChoiceListByDisplayName(ObjectStore objectStore, String displayName) {
        String sqlText = "SELECT This FROM ChoiceList WHERE DisplayName = '" + escapeSql(displayName) + "'";
        SearchScope scope = new SearchScope(objectStore);
        EngineCollection results = scope.fetchObjects(new SearchSQL(sqlText), null, null, Boolean.TRUE);
        Iterator<?> it = results.iterator();
        if (it.hasNext()) {
            Object obj = it.next();
            if (obj instanceof ChoiceList) {
                return (ChoiceList) obj;
            }
        }
        return null;
    }
}
