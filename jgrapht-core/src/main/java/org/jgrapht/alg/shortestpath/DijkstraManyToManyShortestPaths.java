/*
 * (C) Copyright 2019-2019, by Semen Chudakov and Contributors.
 *
 * JGraphT : a free Java graph-theory library
 *
 * See the CONTRIBUTORS.md file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the
 * GNU Lesser General Public License v2.1 or later
 * which is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1-standalone.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR LGPL-2.1-or-later
 */
package org.jgrapht.alg.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.graph.GraphWalk;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Naive algorithm for many-to-many shortest paths problem using {@link DijkstraClosestFirstIterator}.
 *
 * <p>
 * Complexity of the algorithm is $O(min(|S|,|T|)*(V\log V + E))$, where $S$ is the set of source vertices,
 * $T$ is the set of target vertices, $V$ is the set of graph vertices and $E$ is the set of graph edges of
 * the graph.
 *
 * <p>
 * For each source vertex a single source shortest paths search is performed, which is stopped
 * as soon as all target vertices are reached. Shortest paths trees are constructed using
 * {@link DijkstraClosestFirstIterator}.
 *
 * @param <V> the graph vertex type
 * @param <E> the graph edge type
 * @author Semen Chudakov
 */
public class DijkstraManyToManyShortestPaths<V, E> extends BaseManyToManyShortestPaths<V, E> {

    /**
     * Constructs an instance of the algorithm for a given {@code graph}.
     *
     * @param graph underlying graph
     */
    public DijkstraManyToManyShortestPaths(Graph<V, E> graph) {
        super(graph);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ManyToManyShortestPaths<V, E> getManyToManyPaths(Set<V> sources, Set<V> targets) {
        Objects.requireNonNull(sources, "sources cannot be null!");
        Objects.requireNonNull(targets, "targets cannot be null!");

        Map<V, ShortestPathAlgorithm.SingleSourcePaths<V, E>> searchSpaces = new HashMap<>();

        if (sources.size() >= targets.size()) {
            for (V source : sources) {
                searchSpaces.put(source, getShortestPathsTree(graph, source, targets));
            }
            return new DijkstraManyToManyShortestPathsImpl(sources, targets, false, searchSpaces);
        } else {
            Graph<V, E> edgeReversedGraph = new EdgeReversedGraph<>(graph);
            for (V target : targets) {
                searchSpaces.put(target, getShortestPathsTree(edgeReversedGraph, target, sources));
            }
            return new DijkstraManyToManyShortestPathsImpl(sources, targets, true, searchSpaces);
        }
    }

    /**
     * Implementation of the
     * {@link org.jgrapht.alg.interfaces.ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths}.
     * For each source vertex a single source shortest paths tree is stored. It is used to retrieve
     * both actual paths and theirs weights.
     */
    private class DijkstraManyToManyShortestPathsImpl extends BaseManyToManyShortestPathsImpl<V, E> {

        /**
         * Indicates is the search spaces were computed on the edge reversed graph.
         */
        private boolean reversed;

        /**
         * Map from source vertices to corresponding single source shortest path trees.
         */
        private final Map<V, ShortestPathAlgorithm.SingleSourcePaths<V, E>> searchSpaces;

        /**
         * Constructs an instance of the algorithm for the given {@code sources}, {@code reversed}
         * {@code targets} and {@code searchSpaces}.
         *
         * @param sources      source vertices
         * @param targets      target vertices
         * @param reversed     if search spaces are reversed
         * @param searchSpaces single source shortest paths trees map
         */
        DijkstraManyToManyShortestPathsImpl(Set<V> sources, Set<V> targets, boolean reversed,
                                            Map<V, ShortestPathAlgorithm.SingleSourcePaths<V, E>> searchSpaces) {
            super(sources, targets);
            this.reversed = reversed;
            this.searchSpaces = searchSpaces;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public GraphPath<V, E> getPath(V source, V target) {
            assertCorrectSourceAndTarget(source, target);
            if (reversed) {
                GraphPath<V, E> reversedPath = searchSpaces.get(target).getPath(source);
                List<V> vertices = reversedPath.getVertexList();
                List<E> edges = reversedPath.getEdgeList();
                Collections.reverse(vertices);
                Collections.reverse(edges);
                return new GraphWalk<>(graph, source, target, vertices, edges, reversedPath.getWeight());
            } else {
                return searchSpaces.get(source).getPath(target);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public double getWeight(V source, V target) {
            assertCorrectSourceAndTarget(source, target);
            return searchSpaces.get(source).getWeight(target);
        }
    }
}
