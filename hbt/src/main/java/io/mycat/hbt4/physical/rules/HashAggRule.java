/**
 * Copyright (C) <2020>  <chen junwen>
 * <p>
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License along with this program.  If
 * not, see <http://www.gnu.org/licenses/>.
 */
package io.mycat.hbt4.physical.rules;


import io.mycat.hbt4.MycatConvention;
import io.mycat.hbt4.MycatConverterRule;
import io.mycat.hbt4.MycatRules;
import io.mycat.hbt4.physical.HashAgg;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Aggregate;
import org.apache.calcite.tools.RelBuilderFactory;

import java.util.function.Predicate;

public class HashAggRule extends MycatConverterRule {
    public HashAggRule(final MycatConvention out,
                       RelBuilderFactory relBuilderFactory) {
        super(Aggregate.class, (Predicate<Aggregate>) project ->
                        true,
                MycatRules.convention, out, relBuilderFactory, "HashAggRule");
    }

    public RelNode convert(RelNode rel) {
        final Aggregate aggregate = (Aggregate) rel;
        return new HashAgg(aggregate.getCluster(),
                aggregate.getCluster().traitSetOf(out),aggregate.getInput(),
                aggregate.getGroupSet(),
                aggregate.getGroupSets(),
                aggregate.getAggCallList());
    }
}