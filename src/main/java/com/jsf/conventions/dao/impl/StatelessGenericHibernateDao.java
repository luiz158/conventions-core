/**
 * 
 */
package com.jsf.conventions.dao.impl;

import com.jsf.conventions.dao.HibernateDao;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import javax.ejb.TransactionAttribute;
import javax.persistence.EntityManager;
import com.jsf.conventions.model.WrappedData;
import com.jsf.conventions.entitymanager.provider.EntityManagerProvider;
import com.jsf.conventions.entitymanager.provider.Type;
import com.jsf.conventions.qualifier.ConventionsEntityManager;
import com.jsf.conventions.qualifier.Log;
import com.jsf.conventions.qualifier.StatelessHibernateDao;
import java.lang.reflect.Field;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.inject.Named;

import org.hibernate.Criteria;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.criterion.*;
import org.hibernate.loader.custom.ScalarReturn;
import org.hibernate.transform.ResultTransformer;
import org.primefaces.model.SortOrder;

/**
 * 
 * @author Rafael M. Pestano Apr 23, 2011 2:34:49 AM
 */
@Named
@Dependent
@StatelessHibernateDao
public class StatelessGenericHibernateDao<T, Id extends Serializable> implements HibernateDao<T, Id>, Serializable {

    private Class<T> persistentClass;
    @Inject @ConventionsEntityManager(type= Type.TRANSACTION)
    private EntityManagerProvider entityManagerProvider;
    private Session session;
    @Inject @Log
    private transient Logger log;
    


    @Override
    public Session getSession() {
        if (session == null || !session.isOpen()) {
            if (getEntityManager().getDelegate() instanceof org.hibernate.ejb.HibernateEntityManager) {
                session = ((org.hibernate.ejb.HibernateEntityManager) getEntityManager().getDelegate()).getSession();
            } else {
                session = (org.hibernate.Session) getEntityManager().getDelegate();
            }
        }
        return session;
    }

    /**
     * set by the serviceLayer
     *
     * @param persistentClass
     */
    @Override
    public void setPersistentClass(Class<T> persistentClass) {
        this.persistentClass = persistentClass;
    }

    @Override
    public Class<T> getPersistentClass() {
        return this.persistentClass;
    }

    @Override
    public T load(Id id) {
        return (T) this.getSession().load(this.persistentClass, id);
    }

    @Override
    public T get(Id id) {
        return (T) this.getSession().get(this.persistentClass, id);
    }

    @Override
    @TransactionAttribute
    public void save(T entity) {
        this.getSession().save(entity);
    }

    @Override
    @TransactionAttribute
    public void update(T entity) {
        this.getSession().update(entity);
    }

    @Override
    @TransactionAttribute
    public void delete(T entity) {
        this.getSession().delete(entity);
    }

    @Override
    @TransactionAttribute
    public T refresh(T entity) {
        this.getSession().refresh(entity);
        return entity;
    }

    @Override
    @TransactionAttribute
    public void saveOrUpdate(T entity) {
        this.getSession().saveOrUpdate(entity);
    }

    @Override
    public List<T> findAll() {
        Criteria criteria = getSession().createCriteria(persistentClass);
        return criteria.list();
    }

    @Override
    public List<T> findAll(Integer first, Integer max) {
        Criteria criteria = getSession().createCriteria(persistentClass);
        criteria.setFirstResult(first);
        criteria.setMaxResults(max);
        return criteria.list();
    }

    @Override
    public List<T> findByExample(final T entity) {
        Criteria criteria = getSession().createCriteria(persistentClass);
        criteria.add(Example.create(entity).enableLike(MatchMode.ANYWHERE).ignoreCase());
        return criteria.list();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public T findOneByExample(final T entity) {
        Criteria criteria = getSession().createCriteria(persistentClass);
        criteria.add(Example.create(entity).enableLike(MatchMode.ANYWHERE).ignoreCase());
        return (T) criteria.uniqueResult();

    }
    
    @Override
    public List<T> findByExample(final T entity, MatchMode matchMode) {
        Criteria criteria = getSession().createCriteria(persistentClass);
        criteria.add(Example.create(entity).enableLike(matchMode).ignoreCase());
        return criteria.list();
    }
    
    @Override
    public List<T> findByExample(final T entity,int maxResult) {
        Criteria criteria = getSession().createCriteria(persistentClass);
        criteria.add(Example.create(entity).enableLike(MatchMode.ANYWHERE).ignoreCase());
        criteria.setMaxResults(maxResult);
        return criteria.list();
    }
    
    @Override
    public List<T> findByExample(final T entity,int maxResult, MatchMode matchMode) {
        Criteria criteria = getSession().createCriteria(persistentClass);
        criteria.add(Example.create(entity).enableLike(matchMode).ignoreCase());
        criteria.setMaxResults(maxResult);
        return criteria.list();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override
    public T findOneByExample(final T entity, MatchMode matchMode) {
        Criteria criteria = getSession().createCriteria(persistentClass);
        criteria.add(Example.create(entity).enableLike(matchMode).ignoreCase());
        return (T) criteria.uniqueResult();

    }

    @SuppressWarnings("unchecked")
    @Override
    public List<T> findByExample(T exampleInstance, String[] excludeProperty) {
        Criteria crit = getSession().createCriteria(persistentClass);
        Example example = Example.create(exampleInstance);
        for (String exclude : excludeProperty) {
            example.excludeProperty(exclude);
        }
        crit.add(example);
        return crit.list();
    }

    /**
     * Hibernate implementation of pagination
     *
     * @param first
     * @param pageSize
     * @param sortField
     * @param sortOrder
     * @param dc
     * @return
     */
    @Override
    public WrappedData<T> findPaginated(final int first, final int pageSize, String sortField, SortOrder sortOrder, final DetachedCriteria dc) {
        
        int size = getRowCount(dc).intValue();
        
        if (sortField != null && !sortOrder.equals(SortOrder.UNSORTED)) {
            if (sortOrder.equals(SortOrder.ASCENDING)) {
                dc.addOrder(Order.asc(sortField));
            } else {
                dc.addOrder(Order.desc(sortField));
            }

        }

        
        List<T> data = this.findByCriteria(dc, first, pageSize);
        
        
        return new WrappedData<T>(data, size);
    }

    /**
     * basic implementation of pagination without restrictions
     *
     * @param first
     * @param pageSize
     * @param sortField
     * @param sortOrder
     * @param filters sortBy in primefaces datatable
     * @param externalFilter filters outside datatable- eg: inputText, sliders,
     * autocomplete etc..
     * @return
     */
    @Override
    public WrappedData<T> configFindPaginated(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, String> filters, Map<String, Object> externalFilter) {
        DetachedCriteria dc = DetachedCriteria.forClass(persistentClass);

        if (externalFilter != null && !externalFilter.isEmpty()) {
            this.addBasicFilterRestrictions(dc, externalFilter);
        }
        if (filters != null && !filters.isEmpty()) {
            this.addBasicFilterRestrictions(dc, filters);
        }
        return this.findPaginated(first, pageSize, sortField, sortOrder, dc);
    }

    @Override
    public Long getRowCount(final DetachedCriteria dc) {
        Criteria criteria = dc.getExecutableCriteria(getSession());
        criteria.setProjection(Projections.rowCount());
        criteria.setFirstResult(0);
        criteria.setMaxResults(1);
        Long result = (Long) criteria.uniqueResult();
        dc.setProjection(null).setResultTransformer(Criteria.ROOT_ENTITY);
        return result;
    }

    @Override
    public int countAll() {
        Criteria criteria = getSession().createCriteria(persistentClass);
        criteria.setProjection(Projections.rowCount());
        criteria.setFirstResult(0);
        criteria.setMaxResults(1);
        return ((Long) criteria.uniqueResult()).intValue();
    }

    @Override
    public List<T> findByCriteria(DetachedCriteria dc, int first, int maxResult) {
        Criteria criteria = dc.getExecutableCriteria(getSession());
        criteria.setFirstResult(first);
        criteria.setMaxResults(maxResult);
        return criteria.list();
    }

    @Override
    public List<T> findByCriteria(DetachedCriteria dc) {
        return dc.getExecutableCriteria(getSession()).list();
    }

    @Override
    public DetachedCriteria getDetachedCriteria() {
        return DetachedCriteria.forClass(persistentClass);
    }

    @Override
    public Criteria getCriteria() {
       return getSession().createCriteria(persistentClass);
    }
    
    /**
     *
     * @param nativeQuery
     * @param params
     * @param class entity
     * @param result transformer
     * @param Scalar
     * @return
     */
    @Override
    public List findByNativeQuery(String nativeQuery, Map params, Class entity, ResultTransformer rt, ScalarReturn scalar) {
        SQLQuery query = getSession().createSQLQuery(nativeQuery);
        if (scalar != null) {
            query.addScalar(scalar.getColumnAlias(), scalar.getType());
        }
        if (rt != null) {
            query.setResultTransformer(rt);
        }
        if (entity != null) {
            query.addEntity(entity);
        }

        Set<Entry> rawParameters = params.entrySet();
        for (Entry entry : rawParameters) {
            query.setParameter(entry.getKey().toString(), entry.getValue());
        }
        return query.list();
    }

    @Override
    public EntityManager getEntityManager() {
        return this.entityManagerProvider.getEntityManager();
    }

    /**
     * try to infer the restrictions, for more complex restriction override
     * configFindPaginated
     *
     * @param dc
     * @param externalFilter
     */
    private void addBasicFilterRestrictions(DetachedCriteria dc, Map externalFilter) {
        for (Iterator<Entry<String, Object>> it = (Iterator<Entry<String, Object>>) externalFilter.entrySet().iterator(); it.hasNext();) {
            Entry<String, Object> entry = it.next();
            if (entry.getValue() != null && !"".equals(entry.getValue())) {
                try {
                    Field f = persistentClass.getDeclaredField(entry.getKey());
                    if (f.getType().isAssignableFrom(String.class)) {
                        dc.add(Restrictions.ilike(entry.getKey(), (String) entry.getValue(), MatchMode.ANYWHERE));
                    } else if (f.getType().isAssignableFrom(Integer.class) || f.getType().isAssignableFrom(int.class)) {
                        dc.add(Restrictions.eq(entry.getKey(), Integer.parseInt((String) entry.getValue())));
                    }
                    else if(f.getType().isAssignableFrom(Long.class) || f.getType().isAssignableFrom(long.class)){
                        dc.add(Restrictions.eq(entry.getKey(), Long.parseLong((String) entry.getValue())));
                    }
                    else if(f.getType().isAssignableFrom(Date.class)){
                        dc.add(Restrictions.eq(entry.getKey(), (Date)entry.getValue()));
                    }
                    else if(f.getType().isAssignableFrom(Calendar.class)){
                        dc.add(Restrictions.eq(entry.getKey(), (Calendar)entry.getValue()));
                    }
                } catch (Exception ex) {
                    if(log.isLoggable(Level.WARNING)){
                        log.log(Level.WARNING, "Problem trying to infer restrictions from filter:" + ex.getMessage(), ex);
                        ex.printStackTrace();
                    }
                }
            }
        }

    }

    

 
}
