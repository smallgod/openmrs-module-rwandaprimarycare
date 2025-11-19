package org.openmrs.module.rwandaprimarycare.pojos.CrResponse;

import java.util.ArrayList;
import java.util.List;

public class Extension {

    private String url;
    private String valueString;
    private String valueCode;
    private Integer valueInteger;
    private Boolean valueBoolean;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getValueString() {
        return valueString;
    }

    public void setValueString(String valueString) {
        this.valueString = valueString;
    }

    public String getValueCode() {
        return valueCode;
    }

    public void setValueCode(String valueCode) {
        this.valueCode = valueCode;
    }

    public Integer getValueInteger() {
        return valueInteger;
    }

    public void setValueInteger(Integer valueInteger) {
        this.valueInteger = valueInteger;
    }

    public Boolean getValueBoolean() {
        return valueBoolean;
    }

    public void setValueBoolean(Boolean valueBoolean) {
        this.valueBoolean = valueBoolean;
    }

    /**
     * Parse extension values to CodeValue pairs
     * Extracts the last segment of URL as code and the value as value
     */
    public List<CodeValue> parseValues() {
        List<CodeValue> values = new ArrayList<CodeValue>();

        if (url != null) {
            // Extract code from URL (last segment after /)
            String code = url.substring(url.lastIndexOf("/") + 1);

            // Get the value (try different value types)
            String value = null;
            if (valueString != null) {
                value = valueString;
            } else if (valueCode != null) {
                value = valueCode;
            } else if (valueInteger != null) {
                value = String.valueOf(valueInteger);
            } else if (valueBoolean != null) {
                value = String.valueOf(valueBoolean);
            }

            if (value != null) {
                values.add(new CodeValue(code, value));
            }
        }

        return values;
    }
}
