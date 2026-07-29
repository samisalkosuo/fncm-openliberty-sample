package dev.fncm.service.javaapi.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import com.filenet.api.admin.ClassDefinition;
import com.filenet.api.collection.ClassDefinitionSet;
import com.filenet.api.core.Factory;
import com.filenet.api.core.ObjectStore;

import dev.fncm.model.DocumentClassItem;
import dev.fncm.model.DocumentClassListResult;
import dev.fncm.service.javaapi.FileNetOperation;

/**
 * Lists document classes in the object store by walking the class hierarchy
 * via the Java API, starting from the built-in "Document" class.
 * Returns a typed {@link DocumentClassListResult} serialised to JSON by Liberty JSON-B.
 */
public class ListDocumentClassesOperation implements FileNetOperation<DocumentClassListResult> {

    private static final Logger LOGGER = Logger.getLogger(ListDocumentClassesOperation.class.getName());

    @Override
    public DocumentClassListResult execute(ObjectStore os, String username) throws Exception {

        ClassDefinition documentClass = Factory.ClassDefinition.fetchInstance(os, "Document", null);
        LOGGER.info("Walking subclass hierarchy of: " + documentClass.get_SymbolicName());

        List<ClassInfo> documentClasses = new ArrayList<>();
        collectSubclasses(documentClass, documentClasses);

        Collections.sort(documentClasses, Comparator.comparing(c -> c.symbolicName));

        LOGGER.info("Document classes found: " + documentClasses.size());

        List<DocumentClassItem> items = new ArrayList<>(documentClasses.size());
        for (ClassInfo ci : documentClasses) {
            items.add(new DocumentClassItem(ci.symbolicName, ci.displayName, ci.description, ci.type));
        }

        return new DocumentClassListResult(items.size(), items);
    }

    /**
     * Recursively collects all non-hidden subclasses of the given class definition.
     */
    private void collectSubclasses(ClassDefinition classDef, List<ClassInfo> result) {
        ClassDefinitionSet subclasses = classDef.get_ImmediateSubclassDefinitions();
        if (subclasses == null) {
            return;
        }

        Iterator<?> it = subclasses.iterator();
        while (it.hasNext()) {
            ClassDefinition sub = (ClassDefinition) it.next();

            Boolean isHidden = sub.get_IsHidden();
            if (Boolean.TRUE.equals(isHidden)) {
                collectSubclasses(sub, result);
                continue;
            }

            String symbolicName = sub.get_SymbolicName();
            String displayName = getLocalizedString(sub.get_DisplayNames());
            String description = getLocalizedString(sub.get_DescriptiveTexts());
            String type = isCustomClass(symbolicName) ? "CUSTOM" : "SYSTEM";

            result.add(new ClassInfo(symbolicName, displayName, description, type));

            collectSubclasses(sub, result);
        }
    }

    /**
     * Returns the first localized text from a collection, or an empty string if none.
     */
    private String getLocalizedString(com.filenet.api.collection.LocalizedStringList list) {
        if (list == null) {
            return "";
        }
        Iterator<?> it = list.iterator();
        if (it.hasNext()) {
            com.filenet.api.admin.LocalizedString ls =
                    (com.filenet.api.admin.LocalizedString) it.next();
            String text = ls.get_LocalizedText();
            return text != null ? text : "";
        }
        return "";
    }

    /**
     * Returns true if the class appears to be user-defined rather than a built-in system class.
     */
    private boolean isCustomClass(String symbolicName) {
        String[] systemClasses = {
                "Document", "Folder", "CustomObject", "Annotation",
                "Email", "WorkflowDefinition", "Queue", "Roster"
        };
        for (String s : systemClasses) {
            if (symbolicName.equals(s)) {
                return false;
            }
        }
        return symbolicName.contains("_") || symbolicName.matches(".*[A-Z][a-z]+[A-Z].*");
    }

    /** Temporary holder used during collection before converting to records. */
    private record ClassInfo(String symbolicName, String displayName, String description, String type) {}
}

// Made with Bob
