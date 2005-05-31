//
// This file is part of the OpenNMS(R) Application.
//
// OpenNMS(R) is Copyright (C) 2005 The OpenNMS Group, Inc.  All rights reserved.
// OpenNMS(R) is a derivative work, containing both original code, included code and modified
// code that was published under the GNU General Public License. Copyrights for modified 
// and included code are below.
//
// OpenNMS(R) is a registered trademark of The OpenNMS Group, Inc.
//
// Original code base Copyright (C) 1999-2001 Oculan Corp.  All rights reserved.
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
//
// For more information contact:
// OpenNMS Licensing       <license@opennms.org>
//     http://www.opennms.org/
//     http://www.opennms.com/
//
package org.opennms.netmgt.collectd.mock;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.TreeMap;

import org.opennms.netmgt.collectd.SnmpObjId;


public class TestAgent {
    
    private static class Redirect {
        SnmpObjId m_targetObjId;
        public Redirect(SnmpObjId targetObjId) {
            m_targetObjId = targetObjId;
        }
        public SnmpObjId getTargetObjId() {
            return m_targetObjId;
        }

    }

    public static final Object NO_SUCH_INSTANCE = new Object() { public String toString() { return "noSuchInstance"; } };
    public static final Object NO_SUCH_OBJECT = new Object() { public String toString() { return "noSuchObect"; } };
    public static final Object END_OF_MIB = new Object() { public String toString() { return "endOfMibView"; } };
    
    private TreeMap m_agentData;
    private boolean isV1 = true;

    private int m_maxResponseSize = 100; // this is kind of close to reality
 
    Object parseMibValue(String mibVal) {
        return mibVal;
    }

    public Object getValueFor(SnmpObjId id) {
        Object result = m_agentData.get(id);
        if (result == null) {
            generateException(id);
        } else if (result instanceof RuntimeException) {
            throw (RuntimeException)result;
        }
        return result;
    }

    private void generateException(SnmpObjId id) {
        if (m_agentData.size() == 0)
            throw new AgentNoSuchObjectException();
        
        SnmpObjId firstOid = (SnmpObjId)m_agentData.firstKey();
        SnmpObjId lastOid = (SnmpObjId)m_agentData.lastKey();
        if (id.compareTo(firstOid) < 0 || id.compareTo(lastOid) > 0)
            throw new AgentNoSuchObjectException();
        throw new AgentNoSuchInstanceException();
    }

    public void setAgentData(Properties mibData) {
        m_agentData = new TreeMap();
        for (Iterator it = mibData.entrySet().iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry) it.next();
            SnmpObjId objId = SnmpObjId.get((String)entry.getKey());
            
            setAgentValue(objId, parseMibValue((String)entry.getValue()));
        }
    }

    public SnmpObjId getFollowingObjId(SnmpObjId id) {
        try {
            SnmpObjId nextObjId = (SnmpObjId)m_agentData.tailMap(SnmpObjId.get(id, "0")).firstKey();
            Object value = m_agentData.get(nextObjId);
            if (value instanceof Redirect) {
                Redirect redirect = (Redirect) value;
                return redirect.getTargetObjId();
            }
            return nextObjId;
        } catch (NoSuchElementException e) {
            throw new AgentEndOfMibException();   
        }
    }

    public void loadSnmpTestData(Class clazz, String name) throws IOException {
        InputStream dataStream = clazz.getResourceAsStream(name);
        Properties mibData = new Properties();
        mibData.load(dataStream);
        dataStream.close();
    
        setAgentData(mibData);
    }
    
    public void setAgentValue(SnmpObjId objId, Object value) {
        m_agentData.put(objId, value);
    }
    
    /**
     * This simulates send a packet and waiting for a response 
     * @param pdu
     * @return
     */
    public ResponsePdu send(RequestPdu pdu) {
        return pdu.send(this);
    }

    public void setBehaviorToV1() {
        isV1 = true;
    }

    public void setBehaviorToV2() {
        isV1 = false;
    }

    Object handleNoSuchObject(SnmpObjId reqObjId, int errIndex) {
        if (isVersion1())
            throw new AgentNoSuchNameException(errIndex);
        
            
        return NO_SUCH_OBJECT;
    }
    
    Object handleNoSuchInstance(SnmpObjId reqObjId, int errIndex) {
        if (isVersion1())
            throw new AgentNoSuchNameException(errIndex);
        
        return NO_SUCH_INSTANCE;
            
    }

    Object getVarBindValue(SnmpObjId objId, int errIndex) {
        try {
            return getValueFor(objId); 
        } catch (AgentNoSuchInstanceException e) {
            return handleNoSuchInstance(objId, errIndex);
        } catch (AgentNoSuchObjectException e) {
            return handleNoSuchObject(objId, errIndex);
        } catch (Exception e) {
            throw new AgentGenErrException(errIndex);
        }
    }

    TestVarBind getNextResponseVarBind(SnmpObjId lastOid, int errIndex) {
        try {
        SnmpObjId objId = getFollowingObjId(lastOid);
        Object value = getVarBindValue(objId, errIndex);
        return new TestVarBind(objId, value);
        } catch (AgentEndOfMibException e) {
            return handleEndOfMib(lastOid, errIndex);
        }
    }

    private TestVarBind handleEndOfMib(SnmpObjId lastOid, int errIndex) {
        if (isVersion1())
            throw new AgentNoSuchNameException(errIndex);
        
        return new TestVarBind(lastOid, END_OF_MIB);
    }

    TestVarBind getResponseVarBind(SnmpObjId objId, int errIndex) {
        Object value = getVarBindValue(objId, errIndex);
        return new TestVarBind(objId, value);
    }

    public boolean isVersion1() {
        return isV1;
    }

    public void setMaxResponseSize(int maxResponseSize) {
        m_maxResponseSize = maxResponseSize;
    }
    
    public int getMaxResponseSize() {
        return m_maxResponseSize;
    }

    public void introduceSequenceError(SnmpObjId objId, SnmpObjId followingObjId) {
        Redirect redirect = new Redirect(followingObjId);
        setAgentValue(SnmpObjId.get(objId, "0"), redirect);
    }

    public RuntimeException introduceGenErr(SnmpObjId objId) {
        RuntimeException exception = new RuntimeException();
        setAgentValue(objId, exception);
        return exception;
    }
   



}
