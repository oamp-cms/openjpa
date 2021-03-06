/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
 */
package org.apache.openjpa.persistence.relations;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.apache.openjpa.meta.FetchGroup;
import org.apache.openjpa.persistence.OpenJPAEntityManager;
import org.apache.openjpa.persistence.PersistenceException;
import org.apache.openjpa.persistence.datacache.common.apps.FlushDataCacheObject;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestJoinedInheritanceHierarchyWithCache
    extends SingleEMFTestCase {

    public void setUp() {
    	super.setUp(CLEAR_TABLES, InheritanceHierarchyConcrete.class, 
    			InheritanceHierarchyAbstract.class, FlushDataCacheObject.class,
    			"openjpa.DataCache", "true(CacheSize=1, SoftReferenceSize=0)",
    			"openjpa.RemoteCommitProvider", "sjvm");
    }

    public void testCacheSqlGeneration() throws PersistenceException {
    	
		InheritanceHierarchyConcrete parent = new InheritanceHierarchyConcrete();
		InheritanceHierarchyConcrete child = new InheritanceHierarchyConcrete();
		parent.setValue(42);
		child.setValue(21);
		Set<InheritanceHierarchyAbstract> children = new HashSet<InheritanceHierarchyAbstract>();
		children.add(child);
		parent.setChildren(children);
		child.setParent(parent);

    	
    	OpenJPAEntityManager em = emf.createEntityManager();
    	em.getTransaction().begin();
		em.persist(parent);
    	em.getTransaction().commit();
    	em.close();
    	
    	long id = parent.getId();

    	em = emf.createEntityManager();
    	em.getFetchPlan().removeFetchGroup(FetchGroup.NAME_DEFAULT)
			.addFetchGroup("children").addFetchGroup("value");
    	em.createQuery(
    			"SELECT p FROM InheritanceHierarchyConcrete p WHERE p.id=" + id
    			).getResultList().get(0);
    	em.close();
    	
    	em = emf.createEntityManager();
    	em.getFetchPlan().removeFetchGroup(FetchGroup.NAME_DEFAULT)
    		.addFetchGroup("nothing").addFetchGroup("value");
    	em.createQuery(
    			"SELECT p FROM InheritanceHierarchyConcrete p WHERE p.id=" + id
    			).getResultList().get(0);
    	em.close();

    	em = emf.createEntityManager();
    	em.getFetchPlan().removeFetchGroup(FetchGroup.NAME_DEFAULT)
			.addFetchGroup("children").addFetchGroup("value");
    	em.createQuery(
    			"SELECT p FROM InheritanceHierarchyConcrete p WHERE p.id=" + id
    			).getResultList().get(0);
    	em.close();

    }
}

