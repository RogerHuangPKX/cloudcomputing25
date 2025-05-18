package com.archemy.searchapp.view.utils;

public class PropertyEvaluator {
    private String property;
    private PropertyEvaluator parent;
    public PropertyEvaluator(String propertyName) {
        this(null,propertyName);
    }
    public PropertyEvaluator(PropertyEvaluator base,String propertyName) {
        this.property=propertyName;
        this.parent=base;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getProperty() {
        return property;
    }

    public void setParent(PropertyEvaluator parent) {
        this.parent = parent;
    }

    public PropertyEvaluator getParent() {
        return parent;
    }
}
