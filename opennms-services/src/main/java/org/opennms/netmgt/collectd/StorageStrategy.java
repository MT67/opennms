package org.opennms.netmgt.collectd;

public interface StorageStrategy {
    public String getRelativePathForAttribute(int nodeId, String resource, String attribute);

    public void setResourceTypeName(String name);
}
