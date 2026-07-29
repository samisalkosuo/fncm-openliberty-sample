package dev.fncm.service.javaapi.service.buildinginspectiondocs;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import com.filenet.api.admin.ChoiceList;
import com.filenet.api.admin.DocumentClassDefinition;
import com.filenet.api.admin.PropertyDefinition;
import com.filenet.api.admin.PropertyTemplate;
import com.filenet.api.collection.EngineCollection;
import com.filenet.api.collection.PropertyDefinitionList;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.Document;
import com.filenet.api.core.Factory;
import com.filenet.api.core.IndependentObject;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.exception.EngineRuntimeException;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;

public class DeleteBuildingInspectionTypes {

    private static final Logger LOGGER = Logger.getLogger(DeleteBuildingInspectionTypes.class.getName());

    /**
     * Placeholder: document class symbolic names to delete.
     * Add or remove names as needed.
     */
    private static final String[] DOCUMENT_CLASS_NAMES = {
            "BuildingInspectionReport"
            // "AnotherCustomDocumentClass"
    };

    /**
     * Placeholder: property template symbolic names to delete after classes
     * and class-property assignments are removed.
     *
     * Only list templates that are truly safe to delete and are not shared by
     * other remaining classes.
     */
    private static final String[] PROPERTY_TEMPLATE_NAMES = {
            "Municipality",
            "PropertyAddress",
            "InspectionDate",
            "InspectorName",
            "BuildingType",
            "ComplianceStatus"
            // "AnotherTemplate"
    };

    /**
     * Placeholder: property symbolic names that should be removed from each class
     * before deleting the class.
     *
     * Usually these match the class-specific custom properties you created.
     */
    private static final String[] CLASS_PROPERTY_NAMES = {
            "Municipality",
            "PropertyAddress",
            "InspectionDate",
            "BuildingType",
            "ComplianceStatus"
            // "AnotherProperty"
    };

    /**
     * Placeholder: choice list display names to delete.
     * These are the display names of choice lists created for properties.
     * Add or remove names as needed.
     */
    private static final String[] CHOICE_LIST_NAMES = {
            "Building Type Choices",
            "Compliance Status Choices"
            // "AnotherChoiceList"
    };

    public void execute(ObjectStore objectStore) throws Exception {
    
            Set<String> targetProperties = new HashSet<>(Arrays.asList(CLASS_PROPERTY_NAMES));

        for (String className : DOCUMENT_CLASS_NAMES) {
            LOGGER.info("---- Processing class: " + className);

            DocumentClassDefinition classDef = fetchDocumentClass(objectStore, className);
            if (classDef == null) {
                LOGGER.info("Class not found, skipping: " + className);
                continue;
            }

            deleteAllDocumentsOfClass(objectStore, className);
            removeCustomPropertiesFromClass(classDef, targetProperties);
            deleteClassDefinition(classDef);
        }

        for (String templateName : PROPERTY_TEMPLATE_NAMES) {
            deletePropertyTemplateIfExists(objectStore, templateName);
        }

        for (String choiceListName : CHOICE_LIST_NAMES) {
            deleteChoiceListIfExists(objectStore, choiceListName);
        }
    
    }

    private DocumentClassDefinition fetchDocumentClass(ObjectStore objectStore, String className) {
        try {
            return Factory.DocumentClassDefinition.fetchInstance(objectStore, className, null);
        } catch (EngineRuntimeException e) {
            return null;
        }
    }

    /**
     * Deletes all documents of the given class.
     *
     * IBM documents normal document deletion separately from metadata deletion,
     * so this is done first. :contentReference[oaicite:1]{index=1}
     */
    private void deleteAllDocumentsOfClass(ObjectStore objectStore, String className) {
        SearchScope scope = new SearchScope(objectStore);

        // Using SELECT This keeps the result straightforward to cast to Document.
        String sqlText = "SELECT This FROM " + className;
        SearchSQL sql = new SearchSQL(sqlText);

        int deleted = 0;
        boolean foundAny;

        do {
            foundAny = false;

            EngineCollection results = scope.fetchObjects(sql, null, null, Boolean.TRUE);
            Iterator<?> it = results.iterator();

            while (it.hasNext()) {
                foundAny = true;
                Object obj = it.next();

                if (!(obj instanceof Document)) {
                    continue;
                }

                Document doc = (Document) obj;

                try {
                    // Delete the document object.
                    doc.delete();
                    doc.save(RefreshMode.NO_REFRESH);
                    deleted++;
                } catch (Exception e) {
                    System.err.println("Failed to delete document "
                            + safeId(doc) + " of class " + className + ": " + e.getMessage());
                }
            }
        } while (foundAny);

        LOGGER.info("Deleted documents from class " + className + ": " + deleted);
    }

    /**
     * Removes listed custom properties from the class definition.
     *
     * This is needed before deleting the matching property templates, because IBM
     * states a property template can be deleted only when it is no longer assigned
     * to a class. :contentReference[oaicite:2]{index=2}
     */
    private void removeCustomPropertiesFromClass(
            DocumentClassDefinition classDef,
            Set<String> targetPropertyNames) {

        PropertyDefinitionList defs = classDef.get_PropertyDefinitions();
        boolean changed = false;

        for (int i = defs.size() - 1; i >= 0; i--) {
            Object item = defs.get(i);
            if (!(item instanceof PropertyDefinition)) {
                continue;
            }

            PropertyDefinition def = (PropertyDefinition) item;
            String symbolicName = def.get_SymbolicName();

            if (!targetPropertyNames.contains(symbolicName)) {
                continue;
            }

            try {
                defs.remove(i);
                changed = true;
                LOGGER.info("Removed property from class "
                        + classDef.get_SymbolicName() + ": " + symbolicName);
            } catch (Exception e) {
                System.err.println("Failed to remove property "
                        + symbolicName + " from class "
                        + classDef.get_SymbolicName() + ": " + e.getMessage());
            }
        }

        if (changed) {
            classDef.save(RefreshMode.REFRESH);
        }
    }

    /**
     * Deletes the class definition itself.
     */
    private void deleteClassDefinition(DocumentClassDefinition classDef) {
        try {
            String className = classDef.get_SymbolicName();
            classDef.delete();
            classDef.save(RefreshMode.REFRESH);
            LOGGER.info("Deleted class definition: " + className);
        } catch (Exception e) {
            System.err.println("Failed to delete class definition "
                    + classDef.get_SymbolicName() + ": " + e.getMessage());
        }
    }

    /**
     * Deletes a property template by symbolic name if it exists.
     *
     * Placeholder-friendly implementation: it looks up the template with a search.
     */
    private void deletePropertyTemplateIfExists(ObjectStore objectStore, String symbolicName) {
        PropertyTemplate template = findPropertyTemplateBySymbolicName(objectStore, symbolicName);
        if (template == null) {
            LOGGER.info("Property template not found, skipping: " + symbolicName);
            return;
        }

        try {
            template.delete();
            template.save(RefreshMode.REFRESH);
            LOGGER.info("Deleted property template: " + symbolicName);
        } catch (Exception e) {
            System.err.println("Failed to delete property template "
                    + symbolicName + ": " + e.getMessage());
        }
    }

    /**
     * Searches across property template subtypes by symbolic name.
     *
     * You can narrow this if you know all your templates are, for example,
     * PropertyTemplateString / PropertyTemplateDateTime.
     */
    private PropertyTemplate findPropertyTemplateBySymbolicName(
            ObjectStore objectStore, String symbolicName) {

        SearchScope scope = new SearchScope(objectStore);

        String[] classesToTry = {
                "PropertyTemplateString",
                "PropertyTemplateDateTime",
                "PropertyTemplateInteger32",
                "PropertyTemplateBoolean",
                "PropertyTemplateFloat64"
        };

        for (String templateClass : classesToTry) {
            String sqlText = "SELECT This FROM " + templateClass +
                    " WHERE SymbolicName = '" + escapeSql(symbolicName) + "'";
            SearchSQL sql = new SearchSQL(sqlText);

            EngineCollection results = scope.fetchObjects(sql, null, null, Boolean.TRUE);
            Iterator<?> it = results.iterator();
            if (it.hasNext()) {
                Object obj = it.next();
                if (obj instanceof PropertyTemplate) {
                    return (PropertyTemplate) obj;
                }
            }
        }

        return null;
    }

    /**
     * Deletes a choice list by display name if it exists.
     *
     * Choice lists are typically referenced by their display name.
     */
    private void deleteChoiceListIfExists(ObjectStore objectStore, String displayName) {
        ChoiceList choiceList = findChoiceListByDisplayName(objectStore, displayName);
        if (choiceList == null) {
            LOGGER.info("Choice list not found, skipping: " + displayName);
            return;
        }

        try {
            choiceList.delete();
            choiceList.save(RefreshMode.REFRESH);
            LOGGER.info("Deleted choice list: " + displayName);
        } catch (Exception e) {
            System.err.println("Failed to delete choice list "
                    + displayName + ": " + e.getMessage());
        }
    }

    /**
     * Finds a choice list by its display name.
     */
    private ChoiceList findChoiceListByDisplayName(ObjectStore objectStore, String displayName) {
        SearchScope scope = new SearchScope(objectStore);
        String sqlText = "SELECT This FROM ChoiceList WHERE DisplayName = '" + escapeSql(displayName) + "'";
        SearchSQL sql = new SearchSQL(sqlText);

        EngineCollection results = scope.fetchObjects(sql, null, null, Boolean.TRUE);
        Iterator<?> it = results.iterator();
        if (it.hasNext()) {
            Object obj = it.next();
            if (obj instanceof ChoiceList) {
                return (ChoiceList) obj;
            }
        }

        return null;
    }

    private String safeId(IndependentObject obj) {
        try {
            return obj.getProperties().getIdValue("Id").toString();
        } catch (Exception e) {
            return "<unknown-id>";
        }
    }

    private String escapeSql(String s) {
        return s.replace("'", "''");
    }

}
