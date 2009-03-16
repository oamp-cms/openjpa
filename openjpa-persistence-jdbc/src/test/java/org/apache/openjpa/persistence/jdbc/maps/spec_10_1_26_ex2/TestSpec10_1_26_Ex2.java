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
package org.apache.openjpa.persistence.jdbc.maps.spec_10_1_26_ex2;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import junit.framework.Assert;

import org.apache.openjpa.lib.jdbc.AbstractJDBCListener;
import org.apache.openjpa.lib.jdbc.JDBCEvent;
import org.apache.openjpa.lib.jdbc.JDBCListener;
import org.apache.openjpa.persistence.test.SingleEMFTestCase;

public class TestSpec10_1_26_Ex2 extends SingleEMFTestCase {
    public int numDepartments = 2;
    public int numEmployeesPerDept = 2;
    public List<String> namedQueries = new ArrayList<String>();

    public int deptId = 1;
    public int empId = 1;

    protected List<String> sql = new ArrayList<String>();
    protected int sqlCount;

    public void setUp() {
        super.setUp(CLEAR_TABLES,
            Department.class,
            Employee.class,
            EmployeePK.class,
            "openjpa.jdbc.JDBCListeners", 
            new JDBCListener[] { 
            this.new Listener() 
        });
        createObj(emf);
    }

    public void testQualifiedId() throws Exception {
        EntityManager em = emf.createEntityManager();
        String query = "select KEY(e), KEY(e).name from Department d, " +
            " in (d.empMap) e where d.deptId = 1";
        List rs = em.createQuery(query).getResultList(); 
        EmployeePK d = (EmployeePK) ((Object[]) rs.get(0))[0];
        String name = (String) ((Object[]) rs.get(0))[1];
        assertEquals(d.getName(), name);

        em.clear();
        String query4 = "select ENTRY(e) from Department d, " +
            " in (d.empMap) e  where d.deptId = 1";
        List rs4 = em.createQuery(query4).getResultList();
        Map.Entry me = (Map.Entry) rs4.get(0);

        assertTrue(d.equals(me.getKey()));

        // test GROUP BY qualified path
        sql.clear();
        String query5 = "select count(KEY(e).bDay) from Department d " +
            " left join d.empMap e GROUP BY KEY(e).bDay";
        List rs5 = em.createQuery(query5).getResultList();
        assertTrue(sql.get(0).toUpperCase().indexOf(" GROUP BY ") != -1);

        em.close();
    }

    public void testQueryObject() {
        queryObj(emf);
    }

    public List<String> getSql() {
        return sql;
    }

    public int getSqlCount() {
        return sqlCount;
    }

    public void createObj(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        for (int i = 0; i < numDepartments; i++)
            createDepartment(em, deptId++);
        tran.begin();
        em.flush();
        tran.commit();
        em.close();
    }

    public void createDepartment(EntityManager em, int id) {
        Department d = new Department();
        d.setDeptId(id);
        for (int i = 0; i < numEmployeesPerDept; i++) {
            Employee e = createEmployee(em, empId++);
            d.addEmployee(e);
            e.setDepartment(d);
            em.persist(e);
        }
        em.persist(d);
    }

    public Employee createEmployee(EntityManager em, int id) {
        Employee e = new Employee("e" + id, new Date());
        return e;
    }

    public void findObj(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        Department d = em.find(Department.class, 1);
        assertDepartment(d);

        Map emps = d.getEmpMap();
        Set<EmployeePK> keys = emps.keySet();
        for (EmployeePK key : keys) {
            Employee e = em.find(Employee.class, key);
            assertEmployee(e);
        }

        // updateObj by adding a new Employee
        updateObj(em, d);
        deleteObj(em, d);
        em.close();
    }

    public void updateObj(EntityManager em, Department d) {
        EntityTransaction tran = em.getTransaction();

        // add an element
        tran.begin();
        Employee e = createEmployee(em, numDepartments * numEmployeesPerDept
                + 1);
        d.addEmployee(e);
        e.setDepartment(d);
        em.persist(d);
        em.persist(e);
        em.flush();
        tran.commit();

        // remove an element
        tran.begin();
        d.removeEmployee(e.getEmpPK());
        e.setDepartment(null);
        em.persist(d);
        em.persist(e);
        em.flush();
        tran.commit();
    }

    public void deleteObj(EntityManager em, Department d) {
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        em.remove(d);
        tran.commit();
    }

    public void assertDepartment(Department d) {
        int id = d.getDeptId();
        Map es = d.getEmpMap();
        Assert.assertEquals(2, es.size());
        Set keys = es.keySet();
        for (Object obj : keys) {
            EmployeePK empPK = (EmployeePK) obj;
            Employee e = (Employee) es.get(empPK);
            Assert.assertEquals(empPK, e.getEmpPK());
        }
    }

    public void queryObj(EntityManagerFactory emf) {
        queryDepartment(emf);
        queryEmployee(emf);
    }

    public void queryDepartment(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select d from Department d");
        List<Department> ds = q.getResultList();
        for (Department d : ds) {
            assertDepartment(d);
        }
        tran.commit();
        em.close();
    }

    public void queryEmployee(EntityManagerFactory emf) {
        EntityManager em = emf.createEntityManager();
        EntityTransaction tran = em.getTransaction();
        tran.begin();
        Query q = em.createQuery("select e from Employee e");
        List<Employee> es = q.getResultList();
        for (Employee e : es) {
            assertEmployee(e);
        }
        tran.commit();
        em.close();
    }

    public void assertEmployee(Employee e) {
        EmployeePK pk = e.getEmpPK();
        Department d = e.getDepartment();
    }

    public class Listener extends AbstractJDBCListener {
        @Override
        public void beforeExecuteStatement(JDBCEvent event) {
            if (event.getSQL() != null && sql != null) {
                sql.add(event.getSQL());
                sqlCount++;
            }
        }
    }
}
