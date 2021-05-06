/*
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 *
 *                                  http://www.jahia.com
 *
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 *
 *     Copyright (C) 2002-2021 Jahia Solutions Group. All rights reserved.
 *
 *     This file is part of a Jahia's Enterprise Distribution.
 *
 *     Jahia's Enterprise Distributions must be used in accordance with the terms
 *     contained in the Jahia Solutions Group Terms &amp; Conditions as well as
 *     the Jahia Sustainable Enterprise License (JSEL).
 *
 *     For questions regarding licensing, support, production usage...
 *     please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 *
 * ==========================================================================================
 */
package org.jahia.modules.jexperienceProfile.initializers;

import org.apache.commons.lang.StringUtils;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRPropertyWrapper;
import org.jahia.services.content.JCRValueWrapper;
import org.jahia.services.content.nodetypes.ExtendedPropertyDefinition;
import org.jahia.services.content.nodetypes.initializers.ChoiceListInitializer;
import org.jahia.services.content.nodetypes.initializers.ChoiceListValue;
import org.jahia.services.content.nodetypes.initializers.ModuleChoiceListInitializer;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Short description of the class
 *
 * @author faissah
 */

@Component(service = ModuleChoiceListInitializer.class, immediate = true)
public class ParentPropertyInitializer implements ModuleChoiceListInitializer {
    private static final Logger logger = LoggerFactory.getLogger(ParentPropertyInitializer.class);
    private String key = "ParentPropertyInitializer";

    @Override public List<ChoiceListValue> getChoiceListValues(ExtendedPropertyDefinition extendedPropertyDefinition, String param,
            List<ChoiceListValue> list, Locale locale, Map<String, Object> context) {

        List<ChoiceListValue> choices = null;


        if (param == null ) {
            throw new IllegalArgumentException(
                    "Parameter is missing, please provide the property name would you like to retrieve from the parent");
        }

        JCRNodeWrapper contextParentNode = context != null ? (JCRNodeWrapper) context.get("contextParent") : null;

        if (contextParentNode!=null) {
            try {
                String targetProperty = param.trim();
                Value[] values = null;
                String valueType = null;

                JCRPropertyWrapper prop = null;
                if (contextParentNode.hasProperty(targetProperty)) {
                    prop = contextParentNode.getProperty(targetProperty);
                    values = prop.isMultiple() ? prop.getValues() : new Value[] { prop.getValue() };
                }


                if (values != null && values.length > 0) {
                    choices = new LinkedList<ChoiceListValue>();
                    boolean isReference = prop.getType() == PropertyType.REFERENCE || prop.getType() == PropertyType.WEAKREFERENCE;
                    for (Value val : values) {
                        if (isReference) {
                            JCRNodeWrapper referencedNode = ((JCRValueWrapper) val).getNode();
                            if (referencedNode != null) {
                                String listValue = null;
                                if (valueType != null) {
                                    if ("name".equalsIgnoreCase(valueType)) {
                                        listValue = referencedNode.getName();
                                    } else if ("path".equalsIgnoreCase(valueType)) {
                                        listValue = referencedNode.getPath();
                                    }
                                }
                                listValue = listValue == null ? referencedNode.getIdentifier() : listValue;

                                choices.add(new ChoiceListValue(referencedNode.getDisplayableName(), listValue));
                            }
                        } else {
                            String listValue = val.getString();
                            choices.add(new ChoiceListValue(listValue, listValue));
                        }
                    }
                }

                return choices;
            } catch (RepositoryException e) {
                // ${TODO} Auto-generated catch block
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * {@inheritDoc}
     */
    public String getKey() {
        return key;
    }

}
