package org.unipop.jdbc.simple;

import com.google.common.collect.Iterators;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.step.util.HasContainer;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.DeleteWhereStep;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.unipop.common.util.PredicatesTranslator;
import org.unipop.jdbc.controller.schemas.RowEdgeSchema;
import org.unipop.jdbc.controller.schemas.RowSchema;
import org.unipop.jdbc.controller.schemas.RowVertexSchema;
import org.unipop.query.StepDescriptor;
import org.unipop.query.controller.SimpleController;
import org.unipop.query.mutation.AddEdgeQuery;
import org.unipop.query.mutation.AddVertexQuery;
import org.unipop.query.mutation.PropertyQuery;
import org.unipop.query.mutation.RemoveQuery;
import org.unipop.query.predicates.PredicatesHolder;
import org.unipop.query.predicates.PredicatesHolderFactory;
import org.unipop.query.search.DeferredVertexQuery;
import org.unipop.query.search.SearchQuery;
import org.unipop.query.search.SearchVertexQuery;
import org.unipop.structure.UniEdge;
import org.unipop.structure.UniGraph;
import org.unipop.structure.UniVertex;

import java.sql.Connection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author GurRo
 * @since 6/12/2016
 */
public class RowController implements SimpleController {
    private final DSLContext dslContext;
    private final UniGraph graph;
    private final Set<? extends RowVertexSchema> vertexSchemas;
    private final Set<? extends RowEdgeSchema> edgeSchemas;

    private final PredicatesTranslator<Iterable<Condition>> predicatesTranslator;

//    private final RecordMapper<Record, UniVertex> vertexMapper;

    public RowController(UniGraph graph, Connection conn, Set<RowVertexSchema> vertexSchemas, Set<RowEdgeSchema> edgeSchemas, PredicatesTranslator<Iterable<Condition>> predicatesTranslator) {
        this.graph = graph;
        //TODO: get sql dialect from configuration
        this.dslContext = DSL.using(conn, SQLDialect.DEFAULT);
        this.vertexSchemas = vertexSchemas;
        this.edgeSchemas = edgeSchemas;
        this.predicatesTranslator = predicatesTranslator;
    }

    @Override
    public <E extends Element> Iterator<E> search(SearchQuery<E> uniQuery) {
        Set<? extends RowSchema<E>> schemas = getSchemas(uniQuery.getReturnType());
        PredicatesHolder schemaPredicateHolders = extractPredicatesHolder(uniQuery, schemas);
        return search(schemaPredicateHolders, schemas, uniQuery.getLimit(), uniQuery.getStepDescriptor());
    }

    @Override
    public Edge addEdge(AddEdgeQuery uniQuery) {
        UniEdge edge = new UniEdge(uniQuery.getProperties(), uniQuery.getOutVertex(), uniQuery.getInVertex(), this.graph);
        insert(edgeSchemas, edge);

        return edge;
    }

    @Override
    public Vertex addVertex(AddVertexQuery uniQuery) {
        UniVertex vertex = new UniVertex(uniQuery.getProperties(), this.graph);
        insert(vertexSchemas, vertex);

        return vertex;
    }

    @Override
    public <E extends Element> void property(PropertyQuery<E> uniQuery) {

    }

    @Override
    public <E extends Element> void remove(RemoveQuery<E> uniQuery) {
        uniQuery.getElements().forEach(el -> {
            Set<? extends RowSchema<E>> schemas = this.getSchemas(el.getClass());

            for (RowSchema<E> schema : schemas) {
                DeleteWhereStep delete = this.getDslContext().delete(DSL.table(schema.getTable(el)));

                for (Condition condition : translateElementsToConditions(Collections.singletonList(el))) {
                    delete.where(condition);
                }

                delete.execute();
            }
        });
    }

    private <E extends Element> Iterable<Condition> translateElementsToConditions(List<E> elements) {
        return this.predicatesTranslator.translate(
                new PredicatesHolder(
                        PredicatesHolder.Clause.Or,
                        elements.stream()
                                .map(e -> new HasContainer("ID", P.eq(e.id())))
                                .collect(Collectors.toSet()), Collections.EMPTY_SET));

    }

    @Override
    public void fetchProperties(DeferredVertexQuery query) {

    }

    @Override
    public Iterator<Edge> search(SearchVertexQuery uniQuery) {
        return null;
    }

    public DSLContext getDslContext() {
        return dslContext;
    }

    private <E extends Element> Iterator<E> search(PredicatesHolder allPredicates, Set<? extends RowSchema<E>> schemas, int limit, StepDescriptor stepDescriptor) {
        if (schemas.size() == 0 || allPredicates.isAborted()) {
            return Iterators.emptyIterator();
        }

        Iterable<Condition> conditions = this.predicatesTranslator.translate(allPredicates);
        Iterable<String> databases = schemas.stream().map(RowSchema::getDatabase).collect(Collectors.toList());
        //TODO: Finish

        for (RowSchema<E> schema : schemas) {
//            schema
        }


        throw new NotImplementedException();
    }

    private <E extends Element> Set<RowSchema<E>> getSchemas(Class elementClass) {
        if (Vertex.class.isAssignableFrom(elementClass)) {
            return (Set<RowSchema<E>>) vertexSchemas;
        } else {
            return (Set<RowSchema<E>>) edgeSchemas;
        }
    }

    private <E extends Element> PredicatesHolder extractPredicatesHolder(SearchQuery<E> uniQuery, Set<? extends RowSchema<E>> schemas) {
        Set<PredicatesHolder> schemasPredicates = schemas.stream().map(schema ->
                schema.toPredicates(uniQuery.getPredicates())).collect(Collectors.toSet());
        return PredicatesHolderFactory.or(schemasPredicates);
    }

    private <E extends Element> void insert(Set<? extends RowSchema<E>> schemas, E element) {
        for (RowSchema<E> schema : schemas) {
            RowSchema.Row row = schema.toRow(element);

            this.getDslContext().insertInto(DSL.table(schema.getTable(element)), CollectionUtils.collect(row.getFields().keySet(), DSL::field))
                    .values(row.getFields().values()).execute();
        }
    }
}
