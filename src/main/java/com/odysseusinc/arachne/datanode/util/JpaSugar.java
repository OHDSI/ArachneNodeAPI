package com.odysseusinc.arachne.datanode.util;

import java.util.function.BiFunction;
import java.util.function.Function;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import org.springframework.data.domain.Pageable;

/**
 * Syntactic sugar to get more expressive semantics on the JPA operations.
 */
public interface JpaSugar {

    /**
     * Creates e select query to fetch all the entities of a given type
     *
     * @param em entity manager to use
     * @param clazz entity class
     * @param <E> entity type
     */
    static <E> TypedQuery<E> selectAll(EntityManager em, Class<E> clazz) {
        return select(em, clazz, (cb, query) -> root -> query);
    }

    /**
     * Creates a simple select query.
     *
     * @param em entity manager to use
     * @param clazz Root class to use in FROM query section
     * @param query query building function. Takes criteria builder, criteria query, root path and produces complete query
     * Normally, it should be adding only WHERE and ORDER BY sections, since grouping is rarely useful for a simple select.
     * @param <T> root entity type
     */
    static <T> TypedQuery<T> select(
            EntityManager em,
            Class<T> clazz,
            BiFunction<CriteriaBuilder, CriteriaQuery<T>, Function<Root<T>, CriteriaQuery<T>>> query
    ) {
        CriteriaQuery<T> criteriaQuery = query(em, clazz, (cb, cq) -> {
            Root<T> root = cq.from(clazz);
            return query.apply(cb, cq.select(root)).apply(root);
        });
        return em.createQuery(criteriaQuery);
    }



    /**
     * The most basic syntactic sugar function that saves the caller the need to write
     * EntityManager.getCriteriaBuilder() and CriteriaQuery.createQuery() calls
     * @param em entity manager to use
     * @param clazz query return class
     * @param query query building function. Takes criteria builder, criteria query, root path and produces complete query
     * @param <T> query return type
     * @param <V> method return type. Since this function does not perform a call to em.createQuery() itself,
     * this allows for flexible return type, so that the caller can do it both inside the query function
     * or as part of processing return value from this method
     */
    static <T, V> V query(EntityManager em, Class<T> clazz, BiFunction<CriteriaBuilder, CriteriaQuery<T>, V> query) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        return query.apply(cb, cb.createQuery(clazz));
    }

    static <E> int update(
            EntityManager em, Class<E> clazz,
            BiFunction<CriteriaBuilder, CriteriaUpdate<E>, Function<Path<E>, CriteriaUpdate<E>>> query
    ) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaUpdate<E> q = cb.createCriteriaUpdate(clazz);
        return em.createQuery(query.apply(cb, q).apply(q.from(clazz))).executeUpdate();
    }

}
