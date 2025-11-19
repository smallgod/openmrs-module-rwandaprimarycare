package org.openmrs.module.rwandaprimarycare.pojos.location;

import org.openmrs.Location;

public class LocationPojo {

    private Integer locationId;
    private String name;
    private String description;
    private String uuid;

    public LocationPojo() {
    }

    public LocationPojo(Location location) {
        if (location != null) {
            this.locationId = location.getLocationId();
            this.name = location.getName();
            this.description = location.getDescription();
            this.uuid = location.getUuid();
        }
    }

    public Integer getLocationId() {
        return locationId;
    }

    public void setLocationId(Integer locationId) {
        this.locationId = locationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
