package com.connectsdk.discovery;

import java.util.Objects;

/** @noinspection ALL*/
public class DiscoveryFilter {
    String serviceId;
    String serviceFilter;

    public DiscoveryFilter(String serviceId, String serviceFilter) {
        this.serviceId = serviceId;
        this.serviceFilter = serviceFilter;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getServiceFilter() {
        return serviceFilter;
    }

    public void setServiceFilter(String serviceFilter) {
        this.serviceFilter = serviceFilter;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DiscoveryFilter that = (DiscoveryFilter) o;

        if (!Objects.equals(serviceFilter, that.serviceFilter))
            return false;
        return Objects.equals(serviceId, that.serviceId);
    }

    @Override
    public int hashCode() {
        int result = serviceId != null ? serviceId.hashCode() : 0;
        result = 31 * result + (serviceFilter != null ? serviceFilter.hashCode() : 0);
        return result;
    }
}