package org.hcjf.layers.query;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author javaito
 */
public abstract class BaseEvaluator implements Evaluator {

    /**
     * Return the value to compare with the field's object of the data collection's
     * instance.
     * @param currentResultSetElement Is the result set element to evaluate.
     * @param dataSource Query associated data source.
     * @param consumer Query associated data consumer.
     * @return Object value.
     */
    protected final Object getProcessedValue(Object currentResultSetElement, Object rawValue, Queryable.DataSource dataSource, Queryable.Consumer consumer) {
        Object result = rawValue;

        if(result instanceof FieldEvaluator.UnprocessedValue) {
            result = ((FieldEvaluator.UnprocessedValue)result).process(dataSource, consumer);
        } else if(result instanceof Query.QueryParameter) {
            result = consumer.get(currentResultSetElement, (Query.QueryParameter) result, dataSource);
        } else if(result instanceof Collection) {
            Collection<Object> collectionResult = new ArrayList<>();
            for(Object internalValue : (Collection)result) {
                collectionResult.add(getProcessedValue(currentResultSetElement, internalValue, dataSource, consumer));
            }
            result = collectionResult;
        } else if(result.getClass().isArray()) {
            Collection<Object> collectionResult = new ArrayList<>();
            for (int i = 0; i < Array.getLength(result); i++) {
                collectionResult.add(getProcessedValue(currentResultSetElement, Array.get(result, i), dataSource, consumer));
            }
            result = collectionResult;
        }

        return result;
    }

    /**
     * This kind of values take the true value in the execution time of the query.
     */
    public interface UnprocessedValue {

        /**
         * Return the processed value.
         * @param dataSource Data source of the in-evaluation object.
         * @param consumer Consumer for the object.
         * @return Processed value.
         */
        Object process(Queryable.DataSource dataSource, Queryable.Consumer consumer);

    }

    /**
     * Return the object that is in the specific position into the parameters array.
     */
    public static class ReplaceableValue implements UnprocessedValue {

        private final Integer place;

        public ReplaceableValue(Integer place) {
            this.place = place;
        }

        /**
         * Return the processed value.
         * @param dataSource Data source of the in-evaluation object.
         * @param consumer Consumer for the object.
         * @return Processed value.
         */
        @Override
        public Object process(Queryable.DataSource dataSource, Queryable.Consumer consumer) {
            return consumer.getParameter(place);
        }
    }

    /**
     * This kind of query value represents a sub-query.
     */
    public static class QueryValue implements UnprocessedValue {

        private final Query query;

        public QueryValue(Query query) {
            this.query = query;
        }

        /**
         * Return the sub-query instance.
         * @return Sub-query instance.
         */
        public Query getQuery() {
            return query;
        }

        /**
         * Evaluate the sub-query a return the collection result set as value.
         * The first value of the parameters array (parameters[0]) is the instance of data source to evaluate the sub-query.
         * The second value of the parameters array (parameters[1]) is the instance of the consumer to evaluate the sub-query.
         * The rest of the parameters are the parameter to evaluate the sub-query..
         * @param dataSource Data source of the in-evaluation object.
         * @param consumer Consumer for the object.
         * @return If the return fields size is one then the result will be a a list of values, else if the return fields
         * size is greater than one then the result will be a collection with object instance.
         */
        @Override
        public Object process(Queryable.DataSource dataSource, Queryable.Consumer consumer) {
            Object result;
            Collection<Object> collection;
            Collection<Object> subQueryResult = query.evaluate(dataSource, consumer);
            if(query.getReturnParameters().size() == 1){
                List<Object> listResult = new ArrayList<>();
                for(Object element : subQueryResult) {
                    listResult.add(consumer.get(element, (Query.QueryParameter) query.getReturnParameters().get(0), dataSource));
                }
                collection = listResult;
            } else {
                collection = subQueryResult;
            }

            if(collection.size() == 0) {
                //If the size of the collection result is zero then the result will be null.
                result = null;
            } else if(collection.size() == 1) {
                //If the size of the collection result is one then the result will be the unique instance of the collection.
                result = collection.iterator().next();
            } else {
                //If the size of the collection result is greater than one then the result is the collection.
                result = collection;
            }

            return result;
        }
    }
}