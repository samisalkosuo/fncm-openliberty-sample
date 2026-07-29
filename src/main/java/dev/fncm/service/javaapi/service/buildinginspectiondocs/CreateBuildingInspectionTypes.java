package dev.fncm.service.javaapi.service.buildinginspectiondocs;

import java.util.Iterator;
import java.util.logging.Logger;

import com.filenet.api.admin.Choice;
import com.filenet.api.admin.ChoiceList;
import com.filenet.api.admin.ClassDefinition;
import com.filenet.api.admin.DocumentClassDefinition;
import com.filenet.api.admin.LocalizedString;
import com.filenet.api.admin.PropertyDefinitionDateTime;
import com.filenet.api.admin.PropertyDefinitionString;
import com.filenet.api.admin.PropertyTemplateDateTime;
import com.filenet.api.admin.PropertyTemplateString;
import com.filenet.api.collection.EngineCollection;
import com.filenet.api.collection.LocalizedStringList;
import com.filenet.api.collection.PropertyDefinitionList;
import com.filenet.api.constants.Cardinality;
import com.filenet.api.constants.ChoiceType;
import com.filenet.api.constants.FilteredPropertyType;
import com.filenet.api.constants.RefreshMode;
import com.filenet.api.constants.TypeID;
import com.filenet.api.core.Factory;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.exception.EngineRuntimeException;
import com.filenet.api.property.PropertyFilter;
import com.filenet.api.query.SearchSQL;
import com.filenet.api.query.SearchScope;


public class CreateBuildingInspectionTypes {
    
    private static final Logger LOGGER = Logger.getLogger(CreateBuildingInspectionTypes.class.getName());
    
    private static final String CLASS_SYMBOLIC_NAME = "BuildingInspectionReport";
    private static final String CLASS_DISPLAY_NAME = "Building Inspection Report";

    private static final String[] BUILDING_TYPE_VALUES = {
            "Industrial",
            "Residential",
            "Public",
            "Commercial",
            "Unknown"
    };

    private static final String[] COMPLIANCE_STATUS_VALUES = {
            "Fully Compliant",
            "Mostly Compliant",
            "Partially Compliant",
            "Non-Compliant",
            "Requires Follow-up",
            "Unknown"
    };

    private static final String CLASS_DESCRIPTION = "A formal inspection document detailing the condition, safety, and regulatory compliance of residential, public, or industrial buildings in Finland. It summarizes structural integrity, technical systems, identified issues, recommendations, and overall compliance status.";


    public void execute(ObjectStore objectStore) throws Exception {
        try {
            ClassDefinition existing = Factory.ClassDefinition.fetchInstance(objectStore, CLASS_SYMBOLIC_NAME, null);
            LOGGER.info("Class already exists: " + existing.get_SymbolicName());
            return;
        } catch (EngineRuntimeException e) {
            // Expected if class does not exist.
        }

        ClassDefinition documentClass = Factory.ClassDefinition.fetchInstance(objectStore, "Document", null);
        DocumentClassDefinition newClass = (DocumentClassDefinition) documentClass.createSubclass();

        newClass.set_SymbolicName(CLASS_SYMBOLIC_NAME);
        setDisplayName(newClass, objectStore, CLASS_DISPLAY_NAME);
        setDescription(newClass, objectStore, CLASS_DESCRIPTION);
        newClass.save(RefreshMode.REFRESH);

        PropertyFilter pf = new PropertyFilter();
        pf.addIncludeType(0, null, Boolean.TRUE, FilteredPropertyType.ANY, null);

        newClass = Factory.DocumentClassDefinition.fetchInstance(objectStore, CLASS_SYMBOLIC_NAME, pf);
        PropertyDefinitionList propDefs = newClass.get_PropertyDefinitions();

        propDefs.add(createStringProperty(objectStore, "Municipality", "Municipality", true));
        propDefs.add(createStringProperty(objectStore, "PropertyAddress", "Property Address", true));
        propDefs.add(createStringProperty(objectStore, "InspectorName", "Inspector Name ", true));
        propDefs.add(createDateProperty(objectStore, "InspectionDate", "Inspection Date", true));
        propDefs.add(createChoiceListProperty(
                objectStore,
                "BuildingType",
                "Building Type",
                true,
                BUILDING_TYPE_VALUES));
        propDefs.add(createChoiceListProperty(
                objectStore,
                "ComplianceStatus",
                "Compliance Status",
                true,
                COMPLIANCE_STATUS_VALUES));

        newClass.save(RefreshMode.REFRESH);

        LOGGER.info("Created class: " + newClass.get_SymbolicName());
    }


    private void setDescription(ClassDefinition classDef, ObjectStore objectStore, String description) {
        LocalizedString ls = Factory.LocalizedString.createInstance();
        ls.set_LocalizedText(description);
        ls.set_LocaleName(objectStore.get_LocaleName());

        LocalizedStringList list = Factory.LocalizedString.createList();
        list.add(ls);

        classDef.set_DescriptiveTexts(list);
    }

    private PropertyDefinitionString createStringProperty(
            ObjectStore objectStore,
            String symbolicName,
            String displayName,
            boolean required) throws Exception {

        PropertyTemplateString template = findStringTemplateBySymbolicName(objectStore, symbolicName);
        if (template == null) {
            template = Factory.PropertyTemplateString.createInstance(objectStore);
            template.set_SymbolicName(symbolicName);
            template.set_Cardinality(Cardinality.SINGLE);
            setDisplayName(template, objectStore, displayName);
            template.save(RefreshMode.REFRESH);
        }

        PropertyDefinitionString propDef = (PropertyDefinitionString) template.createClassProperty();
        propDef.set_IsValueRequired(required);
        return propDef;
    }

        private PropertyDefinitionDateTime createDateProperty(
            ObjectStore objectStore,
            String symbolicName,
            String displayName,
            boolean required) throws Exception {

        PropertyTemplateDateTime template = findDateTemplateBySymbolicName(objectStore, symbolicName);
        if (template == null) {
            template = Factory.PropertyTemplateDateTime.createInstance(objectStore);
            template.set_SymbolicName(symbolicName);
            template.set_Cardinality(Cardinality.SINGLE);
            setDisplayName(template, objectStore, displayName);
            template.save(RefreshMode.REFRESH);
        }

        PropertyDefinitionDateTime propDef = (PropertyDefinitionDateTime) template.createClassProperty();
        propDef.set_IsValueRequired(required);
        return propDef;
    }

    private PropertyDefinitionString createChoiceListProperty(
            ObjectStore objectStore,
            String symbolicName,
            String displayName,
            boolean required,
            String[] choiceValues) throws Exception {

        PropertyTemplateString template = findStringTemplateBySymbolicName(objectStore, symbolicName);
        if (template == null) {
            template = Factory.PropertyTemplateString.createInstance(objectStore);
            template.set_SymbolicName(symbolicName);
            template.set_Cardinality(Cardinality.SINGLE);
            setDisplayName(template, objectStore, displayName);

            ChoiceList choiceList = findChoiceListByDisplayName(objectStore, displayName + " Choices");
            if (choiceList == null) {
                choiceList = Factory.ChoiceList.createInstance(objectStore);
                choiceList.set_DataType(TypeID.STRING);
                choiceList.set_DisplayName(displayName + " Choices");
                choiceList.set_ChoiceValues(Factory.Choice.createList());

                for (String value : choiceValues) {
                    Choice choice = Factory.Choice.createInstance();
                    choice.set_ChoiceType(ChoiceType.STRING);
                    choice.set_DisplayName(value);
                    choice.set_ChoiceStringValue(value);
                    choiceList.get_ChoiceValues().add(choice);
                }

                choiceList.save(RefreshMode.REFRESH);
            }

            template.set_ChoiceList(choiceList);
            template.save(RefreshMode.REFRESH);
        }

        PropertyDefinitionString propDef = (PropertyDefinitionString) template.createClassProperty();
        propDef.set_IsValueRequired(required);
        return propDef;
    }

    private PropertyTemplateString findStringTemplateBySymbolicName(ObjectStore objectStore, String symbolicName) {
        String sql = "SELECT This FROM PropertyTemplateString WHERE SymbolicName = '" + escapeSql(symbolicName) + "'";
        SearchSQL searchSQL = new SearchSQL(sql);
        SearchScope scope = new SearchScope(objectStore);

        EngineCollection results = scope.fetchObjects(searchSQL, null, null, Boolean.TRUE);
        Iterator<?> it = results.iterator();
        if (it.hasNext()) {
            return (PropertyTemplateString) it.next();
        }
        return null;
    }

    private PropertyTemplateDateTime findDateTemplateBySymbolicName(ObjectStore objectStore, String symbolicName) {
        String sql = "SELECT This FROM PropertyTemplateDateTime WHERE SymbolicName = '" + escapeSql(symbolicName) + "'";
        SearchSQL searchSQL = new SearchSQL(sql);
        SearchScope scope = new SearchScope(objectStore);

        EngineCollection results = scope.fetchObjects(searchSQL, null, null, Boolean.TRUE);
        Iterator<?> it = results.iterator();
        if (it.hasNext()) {
            return (PropertyTemplateDateTime) it.next();
        }
        return null;
    }

    private ChoiceList findChoiceListByDisplayName(ObjectStore objectStore, String displayName) {
        String sql = "SELECT This FROM ChoiceList WHERE DisplayName = '" + escapeSql(displayName) + "'";
        SearchSQL searchSQL = new SearchSQL(sql);
        SearchScope scope = new SearchScope(objectStore);

        EngineCollection results = scope.fetchObjects(searchSQL, null, null, Boolean.TRUE);
        Iterator<?> it = results.iterator();
        if (it.hasNext()) {
            return (ChoiceList) it.next();
        }
        return null;
    }

    private void setDisplayName(ClassDefinition classDef, ObjectStore objectStore, String displayName) {
        LocalizedString ls = Factory.LocalizedString.createInstance();
        ls.set_LocalizedText(displayName);
        ls.set_LocaleName(objectStore.get_LocaleName());

        classDef.set_DisplayNames(Factory.LocalizedString.createList());
        classDef.get_DisplayNames().add(ls);
    }

    private void setDisplayName(PropertyTemplateString template, ObjectStore objectStore, String displayName) {
        LocalizedString ls = Factory.LocalizedString.createInstance();
        ls.set_LocalizedText(displayName);
        ls.set_LocaleName(objectStore.get_LocaleName());

        template.set_DisplayNames(Factory.LocalizedString.createList());
        template.get_DisplayNames().add(ls);
    }

    private void setDisplayName(PropertyTemplateDateTime template, ObjectStore objectStore, String displayName) {
        LocalizedString ls = Factory.LocalizedString.createInstance();
        ls.set_LocalizedText(displayName);
        ls.set_LocaleName(objectStore.get_LocaleName());

        template.set_DisplayNames(Factory.LocalizedString.createList());
        template.get_DisplayNames().add(ls);
    }

    private String escapeSql(String s) {
        return s.replace("'", "''");
    }
}

